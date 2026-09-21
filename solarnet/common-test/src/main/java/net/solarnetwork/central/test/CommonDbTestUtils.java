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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import java.time.Clock;
import java.time.InstantSource;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Common DB test utilities.
 *
 * @author matt
 * @version 1.5
 */
public final class CommonDbTestUtils {

	/**
	 * A millisecond precise clock to avoid database precision differences
	 * (nanos vs micros).
	 */
	public static InstantSource MS_CLOCK = Clock.tickMillis(ZoneOffset.UTC);

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
		Long newId = randomLong();
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
		Long newId = randomLong();
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
				id, username, DigestUtils.sha256Hex(password), displayName, true);
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
	public static void insertUserNode(JdbcOperations jdbcTemplate, Long userId, Long nodeId,
			@Nullable String name, boolean requiresAuth, boolean archived) {
		insertUserNode(jdbcTemplate, userId, nodeId, name, null, requiresAuth, archived);
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
	 * @since 1.3
	 */
	public static void insertUserNode(JdbcOperations jdbcTemplate, Long userId, Long nodeId,
			@Nullable String name, @Nullable String description, boolean requiresAuth,
			boolean archived) {
		jdbcTemplate.update(
				"insert into solaruser.user_node (user_id,node_id,disp_name,description,private,archived) values (?,?,?,?,?,?)",
				userId, nodeId, name, description, requiresAuth, archived);
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
	 *        {@code net.solarnetwork.central.security.SecurityTokenStatus}
	 * @param type
	 *        the type, i.e.
	 *        {@code  net.solarnetwork.central.security.SecurityTokenType}
	 * @param policy
	 *        the policy
	 * @since 1.2
	 */
	public static void insertSecurityToken(JdbcOperations jdbcTemplate, String tokenId,
			String tokenSecret, Long userId, Enum<?> status, Enum<?> type, @Nullable String policy) {
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, status.name(), type.name(),
				policy);
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
			String tokenSecret, Long userId, String status, String type, @Nullable String policy) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
						+ " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::jsonb)",
				tokenId, tokenSecret, userId, status, type, policy);
	}

	/**
	 * Insert a new location with a randomly assigned ID.
	 *
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID to use
	 * @return the assigned ID
	 */
	public static Long insertLocation(JdbcOperations jdbcTemplate, String country, String timeZoneId) {
		Long newId = randomLong();
		insertLocation(jdbcTemplate, newId, country, timeZoneId);
		return newId;
	}

	/**
	 * Insert a location into the {@code sn_loc} table.
	 *
	 * @param id
	 *        the location ID to use
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID to use
	 */
	public static void insertLocation(JdbcOperations jdbcTemplate, Long id, String country,
			String timeZoneId) {
		insertLocation(jdbcTemplate, id, country, null, null, timeZoneId);
	}

	/**
	 * Insert a location into the {@code sn_loc} table.
	 *
	 * @param id
	 *        the location ID to use
	 * @param country
	 *        the country
	 * @param region
	 *        the region
	 * @param postalCode
	 *        the postal code
	 * @param timeZoneId
	 *        the time zone ID to use
	 */
	public static void insertLocation(JdbcOperations jdbcTemplate, Long id, String country,
			@Nullable String region, @Nullable String postalCode, String timeZoneId) {
		jdbcTemplate.update(
				"insert into solarnet.sn_loc (id,country,region,postal_code,time_zone) values (?,?,?,?,?)",
				id, country, region, postalCode, timeZoneId);
	}

	/**
	 * Insert a new node with a randomly assigned ID.
	 *
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param locationId
	 *        the location ID
	 * @return the assigned ID
	 */
	public static Long insertNode(JdbcOperations jdbcTemplate, Long locationId) {
		Long newId = randomLong();
		insertNode(jdbcTemplate, newId, locationId);
		return newId;
	}

	/**
	 * Insert a test node into the sn_node table.
	 *
	 * @param nodeId
	 *        the ID to assign to the node
	 * @param locationId
	 *        the location ID
	 */
	public static void insertNode(JdbcOperations jdbcTemplate, Long nodeId, Long locationId) {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", nodeId,
				locationId);
	}

	/**
	 * Get the raw content of a table.
	 *
	 * @param log
	 *        the logger to log to
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param table
	 *        the name of the table
	 * @param order
	 *        the order column expression
	 * @return the rows
	 * @since 1.1
	 */
	public static List<Map<String, @Nullable Object>> allTableData(Logger log, JdbcOperations jdbcOps,
			String table, String order) {
		List<Map<String, @Nullable Object>> data = jdbcOps
				.queryForList("SELECT * FROM %s ORDER BY %s".formatted(table, order));
		log.debug("%s table has {} items: [{}]".formatted(table), data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Insert roles for a given user ID.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the user ID
	 * @param roles
	 *        the roles to insert
	 * @since 1.2
	 */
	public static void insertUserRoles(JdbcOperations jdbcOps, Long userId, String... roles) {
		jdbcOps.execute("""
				INSERT INTO solaruser.user_role (user_id, role_name)
				VALUES (?,?)
				""", (PreparedStatementCallback<?>) ps -> {
			ps.setObject(1, userId);
			for ( String role : roles ) {
				ps.setString(2, role);
				ps.executeUpdate();
			}
			return true;
		});
	}

	/**
	 * Set the enabled state of a SolarNetwork user.
	 *
	 * @param jdbcOps
	 *        the JDBC ops
	 * @param userId
	 *        the user ID
	 * @param enabled
	 *        {@literal true} to enable the user, {@literal false} to disable
	 * @since 1.4
	 */
	public static void setUserEnabled(JdbcOperations jdbcOps, Long userId, boolean enabled) {
		jdbcOps.update("UPDATE solaruser.user_user SET enabled = ? WHERE id = ?", enabled, userId);
	}

	/**
	 * Insert a security token with an optional security policy.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
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
	 *        the optional policy
	 * @since 1.5
	 */
	public static void insertSecurityTokenWithPolicy(JdbcOperations jdbcOps, String tokenId,
			String tokenSecret, Long userId, String status, String type,
			@Nullable SecurityPolicy policy) {
		insertSecurityToken(jdbcOps, tokenId, tokenSecret, userId, status, type,
				policy != null ? JsonUtils.getJSONString(policy) : null);
	}

	/**
	 * Insert node metadata.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param nodeId
	 *        the node ID
	 * @param meta
	 *        the metadata
	 * @since 1.5
	 */
	public static void insertNodeMetadata(JdbcOperations jdbcOps, Long nodeId,
			GeneralDatumMetadata meta) {
		jdbcOps.update("""
				INSERT INTO solarnet.sn_node_meta (node_id, created, updated, jdata)
				VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?::jsonb)
				""", nodeId, requireNonNull(JsonUtils.getJSONString(meta), "meta"));
	}

	/**
	 * Insert user metadata.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the user ID
	 * @param meta
	 *        the metadata
	 * @since 1.5
	 */
	public static void insertUserMetadata(JdbcOperations jdbcOps, Long userId,
			GeneralDatumMetadata meta) {
		jdbcOps.update("""
				INSERT INTO solaruser.user_meta (user_id, created, updated, jdata)
				VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?::jsonb)
				""", userId, requireNonNull(JsonUtils.getJSONString(meta), "meta"));
	}

	/**
	 * Insert a node instruction.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param nodeId
	 *        the node ID
	 * @param topic
	 *        the instruction topic
	 * @param state
	 *        the delivery state, i.e.
	 *        {@code net.solarnetwork.domain.InstructionStatus.InstructionState.name()}
	 * @return the instruction ID
	 * @since 1.5
	 */
	public static Long insertNodeInstruction(JdbcOperations jdbcOps, Long nodeId, String topic,
			String state) {
		return requireNonNull(jdbcOps.queryForObject("""
				INSERT INTO solarnet.sn_node_instruction (node_id, topic, instr_date, deliver_state)
				VALUES (?, ?, CURRENT_TIMESTAMP, ?::solarnet.instruction_delivery_state)
				RETURNING id
				""", Long.class, nodeId, topic, state));
	}

	/**
	 * Insert an active node stale data user alert.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the optional node ID
	 * @return the alert ID
	 * @since 1.5
	 */
	public static Long insertUserAlert(JdbcOperations jdbcOps, Long userId, @Nullable Long nodeId) {
		return requireNonNull(jdbcOps.queryForObject("""
				INSERT INTO solaruser.user_alert (user_id, node_id, alert_type, status, alert_opt)
				VALUES (?, ?, 'NodeStaleData'::solaruser.user_alert_type
					, 'Active'::solaruser.user_alert_status, '{"age":1800}'::json)
				RETURNING id
				""", Long.class, userId, nodeId));
	}

	/**
	 * Insert an active user alert situation.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param alertId
	 *        the alert ID
	 * @return the situation ID
	 * @since 1.5
	 */
	public static Long insertUserAlertSituation(JdbcOperations jdbcOps, Long alertId) {
		return requireNonNull(jdbcOps.queryForObject("""
				INSERT INTO solaruser.user_alert_sit (alert_id, status)
				VALUES (?, 'Active'::solaruser.user_alert_sit_status)
				RETURNING id
				""", Long.class, alertId));
	}

	/**
	 * Insert a pending user node confirmation (a node invitation).
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the user ID
	 * @return the confirmation ID
	 * @since 1.5
	 */
	public static Long insertUserNodeConfirmation(JdbcOperations jdbcOps, Long userId) {
		return requireNonNull(jdbcOps.queryForObject("""
				INSERT INTO solaruser.user_node_conf (user_id, conf_key, sec_phrase, country, time_zone)
				VALUES (?, ?, ?, 'NZ', 'Pacific/Auckland')
				RETURNING id
				""", Long.class, userId, randomString(), randomString()));
	}

	/**
	 * Insert a user node ownership transfer request.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param userId
	 *        the ID of the user requesting the transfer
	 * @param nodeId
	 *        the node ID
	 * @param recipient
	 *        the recipient email
	 * @since 1.5
	 */
	public static void insertUserNodeTransfer(JdbcOperations jdbcOps, Long userId, Long nodeId,
			String recipient) {
		jdbcOps.update(
				"INSERT INTO solaruser.user_node_xfer (user_id, node_id, recipient) VALUES (?, ?, ?)",
				userId, nodeId, recipient);
	}

}
