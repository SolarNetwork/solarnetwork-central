/* ==================================================================
 * StaleGeneralLocationDatumProcessor.java - Oct 23, 2014 6:54:04 AM
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
 * Job to process "stale" general location datum reporting aggregate data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class StaleGeneralLocationDatumProcessor extends StaleGeneralNodeDatumProcessor {

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleGeneralLocationDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps);
		setJdbcCall("{? = call solaragg.process_agg_stale_loc_datum(?, ?)}");
	}

}
