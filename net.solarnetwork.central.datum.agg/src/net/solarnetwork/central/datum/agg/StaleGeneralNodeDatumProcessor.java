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

import org.osgi.service.event.EventAdmin;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" general node datum reporting aggregate data.
 * 
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * 
 * If {@code taskCount} is higher than {@code 1} then {@code taskCount} threads
 * will be spawned and each process {@code maximumRowCount / taskCount} rows.
 * 
 * @author matt
 * @version 1.3
 */
public class StaleGeneralNodeDatumProcessor extends TieredStoredProcedureStaleDatumProcessor {

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps, "stale general data");
		setJdbcCall("{? = call solaragg.process_agg_stale_datum(?, ?)}");
	}

	/**
	 * Get the aggregate process type.
	 * 
	 * @return the type
	 */
	public String getAggregateProcessType() {
		return getTierProcessType();
	}

	/**
	 * Set the type of aggregate data to process. This is the first parameter
	 * passed to the JDBC procedure.
	 * 
	 * @param aggregateProcessType
	 *        the type to set
	 */
	public void setAggregateProcessType(String aggregateProcessType) {
		setTierProcessType(aggregateProcessType);
	}

	/**
	 * Get the maximum aggregate rows to process per procedure call.
	 * 
	 * @return the maximum row count
	 */
	public int getAggregateProcessMax() {
		Integer max = getTierProcessMax();
		return (max != null ? max.intValue() : 0);
	}

	/**
	 * Set the maximum number of aggregate rows to process per procedure call.
	 * 
	 * <p>
	 * This is the second parameter passed to the JDBC procedure. Default is
	 * {@code 1}.
	 * </p>
	 * 
	 * @param aggregateProcessMax
	 *        the maximum number of rows
	 */
	public void setAggregateProcessMax(int aggregateProcessMax) {
		setTierProcessMax(aggregateProcessMax);
	}

}
