/* ==================================================================
 * JdbcCallJob.java - 25/05/2020 9:26:34 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.scheduler.JobSupport;

/**
 * Job to execute a stored procedure that returns a BIGINT result.
 * 
 * @author matt
 * @version 2.0
 * @since 1.10
 */
public class JdbcCallJob extends JobSupport {

	private final JdbcOperations jdbcOps;
	private String jdbcCall;

	/**
	 * Construct with properties.
	 * 
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcCallJob(JdbcOperations jdbcOps) {
		super();
		this.jdbcOps = requireNonNullArgument(jdbcOps, "jdbcOps");
		setGroupId("Datum");
		setMaximumWaitMs(1800000L);
	}

	@Override
	public void run() {
		log.info("Job {} executing JDBC call {}...", getId(), getJdbcCall());
		final Long result = jdbcOps.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement call = con.prepareCall(getJdbcCall());
				call.registerOutParameter(1, Types.BIGINT);
				return call;
			}
		}, new CallableStatementCallback<Long>() {

			@Override
			public Long doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.execute();
				return cs.getLong(1);
			}
		});
		log.info("Job {} JDBC call result: {}", getId(), result);
	}

	/**
	 * Get the JDBC operations.
	 * 
	 * @return the operations
	 */
	public JdbcOperations getJdbcOps() {
		return jdbcOps;
	}

	/**
	 * Get the JDBC call.
	 * 
	 * @return the call
	 */
	public String getJdbcCall() {
		return jdbcCall;
	}

	/**
	 * Set the JDBC call.
	 * 
	 * @param jdbcCall
	 *        the call
	 */
	public void setJdbcCall(String jdbcCall) {
		this.jdbcCall = jdbcCall;
	}

}
