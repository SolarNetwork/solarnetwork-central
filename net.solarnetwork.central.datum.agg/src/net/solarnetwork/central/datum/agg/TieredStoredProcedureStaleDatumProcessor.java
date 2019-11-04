/* ==================================================================
 * TieredStoredProcedureStaleDatumProcessor.java - 1/11/2019 3:22:26 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.datum.agg;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Tiered stale datum processor that calls a stored procedure to process stale
 * rows.
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
public class TieredStoredProcedureStaleDatumProcessor extends TieredStaleDatumProcessor {

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param taskDescription
	 *        a description of the task to use in log statements
	 */
	public TieredStoredProcedureStaleDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
			String taskDescription) {
		super(eventAdmin, jdbcOps, taskDescription);
	}

	@Override
	protected final int execute(final AtomicInteger remainingCount) {
		final String tierProcessType = getTierProcessType();
		final Integer tierProcessMax = getTierProcessMax();
		return getJdbcOps().execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				CallableStatement call = con.prepareCall(getJdbcCall());
				call.registerOutParameter(1, Types.INTEGER);
				call.setString(2, tierProcessType);
				if ( tierProcessMax != null ) {
					call.setInt(3, tierProcessMax);
				}
				con.setAutoCommit(true); // we want every execution of our loop to commit immediately
				int resultCount = 0;
				int processedCount = 0;
				do {
					call.execute();
					resultCount = call.getInt(1);
					processedCount += resultCount;
					remainingCount.addAndGet(-resultCount);
				} while ( resultCount > 0 && remainingCount.get() > 0 );
				return processedCount;
			}
		});
	}

}
