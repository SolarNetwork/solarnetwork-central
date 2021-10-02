/* ==================================================================
 * StaleDatumProcessor.java - Aug 1, 2013 4:27:13 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" reporting aggregate data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * </p>
 * 
 * @author matt
 * @version 1.3
 */
public class StaleDatumProcessor extends JdbcCallJob {

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps);
		setMaximumIterations(5);
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		final int maximumRowCount = getMaximumIterations();
		int i = 0;
		int resultCount = 0;
		do {
			resultCount = getJdbcOps().execute(new CallableStatementCreator() {

				@Override
				public CallableStatement createCallableStatement(Connection con) throws SQLException {
					CallableStatement call = con.prepareCall(getJdbcCall());
					call.registerOutParameter(1, Types.INTEGER);
					return call;
				}
			}, new CallableStatementCallback<Integer>() {

				@Override
				public Integer doInCallableStatement(CallableStatement cs)
						throws SQLException, DataAccessException {
					cs.execute();
					return cs.getInt(1);
				}
			});
			i += resultCount;
		} while ( i < maximumRowCount && resultCount > 0 );
		return true;
	}

	/**
	 * Get the maximum number of rows to process, as returned by the stored
	 * procedure.
	 * 
	 * @return the maximum row count; defaults to {@literal 5}
	 * @deprecated since 1.3
	 * @see #getMaximumIterations()
	 */
	@Deprecated
	public int getMaximumRowCount() {
		return getMaximumIterations();
	}

	/**
	 * Set the maximum number of rows to process, as returned by the stored
	 * procedure.
	 * 
	 * @param maximumRowCount
	 *        the maximum row count
	 * @deprecated since 1.3
	 * @see #setMaximumIterations(int)
	 */
	@Deprecated
	public void setMaximumRowCount(int maximumRowCount) {
		setMaximumIterations(maximumRowCount);
	}

}
