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

import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" audit datum reporting data.
 * 
 * @author matt
 * @version 2.0
 * @since 1.6
 */
public class StaleAuditDataProcessor extends TieredStoredProcedureStaleDatumProcessor {

	/** The default {@code jdbcCall} value. */
	public static final String DEFAULT_SQL = "{? = call solardatm.process_one_aud_stale_datm(?)}";

	/**
	 * Construct with properties.
	 * 
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StaleAuditDataProcessor(JdbcOperations jdbcOps) {
		super(jdbcOps, "stale audit data");
		setJdbcCall(DEFAULT_SQL);
		setTierProcessMax(null);
	}

}
