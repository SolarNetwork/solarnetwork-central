/* ==================================================================
 * StaleAuditDataProcessor.java - 3/07/2018 9:46:25 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
 * Job to process "stale" audit datum reporting data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public class StaleAuditDataProcessor extends TieredStaleDatumProcessor {

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleAuditDataProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps, "stale audit data");
		setJdbcCall("{? = call solaragg.process_one_aud_datum_daily_stale(?)}");
		setTierProcessMax(null);
	}

}
