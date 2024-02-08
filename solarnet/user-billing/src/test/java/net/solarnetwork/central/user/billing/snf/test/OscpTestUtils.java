/* ==================================================================
 * OscpTestUtils.java - 17/08/2022 3:37:12 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * Testing utilities for Flexibility Provider.
 * 
 * @author matt
 * @version 1.1
 */
public final class OscpTestUtils {

	private OscpTestUtils() {
		// not available
	}

	/**
	 * Save a new Flexibility Provider authorization ID.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param token
	 *        the token
	 * @return the new ID
	 */
	public static Long saveFlexibilityProviderAuthId(JdbcOperations jdbcOps, Long userId, String token) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO solaroscp.oscp_fp_token (user_id, token) VALUES (?, ?) RETURNING id",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setString(2, token);
			return stmt;
		}, holder);
		return holder.getKeyAs(Long.class);
	}

	/**
	 * Save a new Capacity Provider.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param fpId
	 *        the Flexibility Provider ID
	 * @param name
	 *        the display name
	 * @return the new ID
	 */
	public static Long saveCapacityProvider(JdbcOperations jdbcOps, Long userId, Long fpId,
			String name) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO solaroscp.oscp_cp_conf (user_id, fp_id, enabled, reg_status, cname) VALUES (?, ?, ?, ?, ?) RETURNING id",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setObject(2, fpId, Types.BIGINT);
			stmt.setBoolean(3, true);
			stmt.setInt(4, 1);
			stmt.setString(5, name);
			return stmt;
		}, holder);
		return holder.getKeyAs(Long.class);
	}

	/**
	 * Save a new Capacity Optimizer.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param fpId
	 *        the Flexibility Provider ID
	 * @param name
	 *        the display name
	 * @return the new ID
	 */
	public static Long saveCapacityOptimizer(JdbcOperations jdbcOps, Long userId, Long fpId,
			String name) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO solaroscp.oscp_co_conf (user_id, fp_id, enabled, reg_status, cname) VALUES (?, ?, ?, ?, ?) RETURNING id",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setObject(2, fpId, Types.BIGINT);
			stmt.setBoolean(3, true);
			stmt.setInt(4, 1);
			stmt.setString(5, name);
			return stmt;
		}, holder);
		return holder.getKeyAs(Long.class);
	}

	/**
	 * Save a new Capacity Optimizer.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the display name
	 * @param ident
	 *        the unique idnentifier
	 * @param cpId
	 *        the Capacity Provider ID
	 * @param coId
	 *        the Capacity Optimizer ID
	 * @return the new ID
	 */
	public static Long saveCapacityGroup(JdbcOperations jdbcOps, Long userId, String name, String ident,
			Long cpId, Long coId) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"""
							INSERT INTO solaroscp.oscp_cg_conf (user_id, enabled, cname, ident, cp_id, co_id, cp_meas_secs, co_meas_secs)
							VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
							""",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setBoolean(2, true);
			stmt.setString(3, name);
			stmt.setString(4, ident);
			stmt.setObject(5, cpId, Types.BIGINT);
			stmt.setObject(6, coId, Types.BIGINT);
			stmt.setInt(7, 60);
			stmt.setInt(8, 60);
			return stmt;
		}, holder);
		return holder.getKeyAs(Long.class);
	}

	/**
	 * Save a new Capacity Optimizer.
	 * 
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the display name
	 * @param ident
	 *        the unique idnentifier
	 * @param cgId
	 *        the Capacity Group ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param iProps
	 *        the instantaneous properties
	 * @param eProps
	 *        the accumulating properties
	 * @return the new ID
	 * @since 1.1
	 */
	public static Long saveFlexibilityAsset(JdbcOperations jdbcOps, Long userId, String name,
			String ident, Long cgId, Long nodeId, String sourceId, String[] iProps, String[] eProps) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement("""
					INSERT INTO solaroscp.oscp_asset_conf (
						  user_id, enabled, cname, ident, cg_id, node_id, source_id
						, iprops, iprops_unit, iprops_mult
						, eprops, eprops_unit, eprops_mult
						, audience, category)
					VALUES (
					      ?, ?, ?, ?, ?, ?, ?
						, ?, ?, ?
						, ?, ?, ?
						, ?, ?
						) RETURNING id
					""", Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setBoolean(2, true);
			stmt.setString(3, name);
			stmt.setString(4, ident);
			stmt.setObject(5, cgId, Types.BIGINT);
			stmt.setObject(6, nodeId, Types.BIGINT);
			stmt.setString(7, sourceId);

			Array iPropsArray = con.createArrayOf("text", iProps);
			stmt.setArray(8, iPropsArray);
			iPropsArray.free();

			stmt.setInt(9, 'P'); // kW
			stmt.setBigDecimal(10, new BigDecimal("0.001"));

			Array ePropsArray = con.createArrayOf("text", eProps);
			stmt.setArray(11, ePropsArray);
			ePropsArray.free();

			stmt.setInt(12, 'E'); // kWh
			stmt.setBigDecimal(13, new BigDecimal("0.001"));

			stmt.setInt(14, 'o'); // Capacity Optimizer
			stmt.setInt(15, 'v'); // Charging

			return stmt;
		}, holder);
		return holder.getKeyAs(Long.class);
	}

}
