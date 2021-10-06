/* ==================================================================
 * CommonDbTestUtils.java - 7/10/2021 7:05:54 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test;

import java.security.SecureRandom;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Common DB test utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonDbTestUtils {

	/**
	 * Insert a new user with a randomly assigned user ID and username.
	 * 
	 * <p>
	 * The username will be in the form {@literal testX@localhost} where
	 * {@literal X} is the assigned ID. The password will be
	 * {@literal password}.
	 * </p>
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @return the assigned user ID
	 */
	public static Long insertUser(JdbcOperations jdbcTemplate) {
		Long newId = new SecureRandom().nextLong();
		insertUser(jdbcTemplate, newId, String.format("test%d@localhost", newId), "password",
				String.format("Test User %d", newId));
		return newId;
	}

	/**
	 * Insert a new user with a randomly assigned user ID.
	 * 
	 * <p>
	 * The password will be {@literal password}.
	 * </p>
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @return the assigned user ID
	 */
	public static Long insertUser(JdbcOperations jdbcTemplate, String username) {
		Long newId = new SecureRandom().nextLong();
		insertUser(jdbcTemplate, newId, username, "password", String.format("Test User %d", newId));
		return newId;
	}

	/**
	 * Insert a new user.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param id
	 *        the ID
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @param displayName
	 *        the display name
	 */
	public static void insertUser(JdbcOperations jdbcTemplate, Long id, String username, String password,
			String displayName) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id,email,password,disp_name,enabled) values (?,?,?,?,?)",
				id, username, DigestUtils.sha256Hex(password), displayName, Boolean.TRUE);
	}

	/**
	 * Insert a new user-node mapping.
	 * 
	 * <p>
	 * The display name will be in the form {@literal User X node Y} where
	 * {@literal X} is the user ID and {@literal Y} the node ID. The
	 * {@code requiresAuth} flag will be set to {@literal false}. The
	 * {@code archived} flag will be set to {@literal false}.
	 * </p>
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	public static void insertUserNode(JdbcOperations jdbcTemplate, Long userId, Long nodeId) {
		insertUserNode(jdbcTemplate, userId, nodeId, false);
	}

	/**
	 * Insert a new user-node mapping.
	 * 
	 * <p>
	 * The display name will be in the form {@literal User X node Y} where
	 * {@literal X} is the user ID and {@literal Y} the node ID. The
	 * {@code archived} flag will be set to {@literal false}.
	 * </p>
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param requiresAuth
	 *        {@literal true} for a "private" node
	 */
	public static void insertUserNode(JdbcOperations jdbcTemplate, Long userId, Long nodeId,
			boolean requiresAuth) {
		insertUserNode(jdbcTemplate, userId, nodeId, String.format("User %d node %d", userId, nodeId),
				requiresAuth, false);
	}

	/**
	 * Insert a new user-node mapping.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param name
	 *        the name
	 * @param requiresAuth
	 *        {@literal true} for a "private" node
	 * @param archived
	 *        {@literal true} to make the node "archived"
	 */
	public static void insertUserNode(JdbcOperations jdbcTemplate, Long userId, Long nodeId, String name,
			boolean requiresAuth, boolean archived) {
		jdbcTemplate.update(
				"insert into solaruser.user_node (user_id,node_id,disp_name,private,archived) values (?,?,?,?,?)",
				userId, nodeId, name, requiresAuth, archived);
	}

	/**
	 * Insert a security token.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param tokenId
	 *        the token ID
	 * @param tokenSecret
	 *        the token secret
	 * @param userId
	 *        the owner user ID
	 * @param status
	 *        the status, i.e.
	 *        {@code net.solarnetwork.central.security.SecurityTokenStatus.name()}
	 * @param type
	 *        the type, i.e.
	 *        {@code  net.solarnetwork.central.security.SecurityTokenType.name()}
	 * @param policy
	 *        the policy
	 */
	public static void insertSecurityToken(JdbcOperations jdbcTemplate, String tokenId,
			String tokenSecret, Long userId, String status, String type, String policy) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
						+ " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::jsonb)",
				tokenId, tokenSecret, userId, status, type, policy);
	}

}
