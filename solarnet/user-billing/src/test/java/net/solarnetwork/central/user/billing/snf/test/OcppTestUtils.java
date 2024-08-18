/* ==================================================================
 * OcppTestUtils.java - 19/08/2024 8:39:29 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.test;

import org.springframework.jdbc.core.JdbcOperations;

/**
 * Testing utilities for OCPP billing.
 *
 * @author matt
 * @version 1.0
 */
public final class OcppTestUtils {

	private OcppTestUtils() {
		// not available
	}

	/**
	 * Create an OCPP charger record.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param ident
	 *        the identifier
	 * @param vendor
	 *        the vendor
	 * @param enabled
	 *        the enabled flag
	 * @param model
	 *        the model
	 */
	public static void saveOcppCharger(JdbcOperations jdbcOps, Long userId, Long nodeId, String ident,
			String vendor, String model, boolean enabled) {
		jdbcOps.update("""
				INSERT INTO solarev.ocpp_charge_point (user_id,node_id,ident,vendor,model,enabled)
				VALUES (?,?,?,?,?,?)
				""", userId, nodeId, ident, vendor, model, enabled);
	}

}
