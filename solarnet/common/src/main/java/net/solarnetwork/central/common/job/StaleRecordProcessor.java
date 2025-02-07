/* ==================================================================
 * StaleRecordProcessor.java - Aug 1, 2013 4:27:13 PM
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

package net.solarnetwork.central.common.job;

import java.sql.CallableStatement;
import java.sql.Types;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" record data.
 *
 * <p>
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public class StaleRecordProcessor extends JdbcCallJob {

	/**
	 * Construct with properties.
	 *
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StaleRecordProcessor(JdbcOperations jdbcOps) {
		super(jdbcOps);
		setMaximumIterations(5);
	}

	@Override
	public void run() {
		final int maximumRowCount = getMaximumIterations();
		int i = 0;
		int resultCount;
		do {
			resultCount = getJdbcOps().execute(con -> {
				CallableStatement call = con.prepareCall(getJdbcCall());
				call.registerOutParameter(1, Types.INTEGER);
				return call;
			}, (CallableStatementCallback<Integer>) cs -> {
				cs.execute();
				return cs.getInt(1);
			});
			i += resultCount;
		} while ( i < maximumRowCount && resultCount > 0 );
	}

}
