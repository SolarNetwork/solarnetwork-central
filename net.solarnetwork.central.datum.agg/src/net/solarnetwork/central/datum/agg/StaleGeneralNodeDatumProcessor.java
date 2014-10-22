/* ==================================================================
 * StaleGeneralNodeDatumProcessor.java - Aug 27, 2014 6:18:01 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" general node datum reporting aggregate data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>aggregateProcessType</dt>
 * <dd>The type of aggregate data to process. This is the first parameter passed
 * to the JDBC procedure.</dd>
 * 
 * <dt>aggregateProcessMax</dt>
 * <dd>The maximum number of aggregate rows to process. This is the second
 * parameter passed to the JDBC procedure.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class StaleGeneralNodeDatumProcessor extends StaleDatumProcessor {

	private String aggregateProcessType = "h";
	private int aggregateProcessMax = 1;

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps);
		setJdbcCall("{? = call solaragg.process_agg_stale_datum(?, ?)}");
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		log.debug("Processing at most {} stale general data for aggregate '{}' with call {}",
				aggregateProcessMax, aggregateProcessType, getJdbcCall());
		getJdbcOps().execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection con) throws SQLException, DataAccessException {
				CallableStatement call = con.prepareCall(getJdbcCall());
				call.registerOutParameter(1, Types.INTEGER);
				call.setString(2, aggregateProcessType);
				call.setInt(3, aggregateProcessMax);
				con.setAutoCommit(true); // we want every execution of our loop to commit immediately
				int i = 0;
				int resultCount = 0;
				do {
					call.execute();
					resultCount = call.getInt(1);
					i += resultCount;
				} while ( i < getMaximumRowCount() && resultCount > 0 );
				return null;
			}
		});
		return true;
	}

	public String getAggregateProcessType() {
		return aggregateProcessType;
	}

	public void setAggregateProcessType(String aggregateProcessType) {
		this.aggregateProcessType = aggregateProcessType;
	}

	public int getAggregateProcessMax() {
		return aggregateProcessMax;
	}

	public void setAggregateProcessMax(int aggregateProcessMax) {
		this.aggregateProcessMax = aggregateProcessMax;
	}

}
