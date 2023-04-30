/* ==================================================================
 * AbstractJunit5CentralTransactionalTest.java - 6/10/2021 4:57:00 PM
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

import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base test for Spring-managed transactional tests in JUnit 5.
 * 
 * <p>
 * This is a transitional class to help migrate JUnit 4 tests to JUnit 5 with
 * minimal changes.
 * <p>
 * 
 * @author matt
 * @version 1.1
 */
@SpringJUnitConfig
@Transactional
@Rollback
public class AbstractJUnit5CentralTransactionalTest implements CentralTestConstants {

	@Autowired
	protected JdbcOperations jdbcTemplate;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);
	}

	/**
	 * Insert a test node into the sn_node table.
	 */
	protected void setupTestNode() {
		setupTestLocation();
		setupTestNode(TEST_NODE_ID);
	}

	/**
	 * Insert a test node into the sn_node table.
	 * 
	 * @param nodeId
	 *        the ID to assign to the node
	 */
	protected void setupTestNode(Long nodeId) {
		setupTestNode(nodeId, TEST_LOC_ID);
	}

	/**
	 * Insert a test node into the sn_node table.
	 * 
	 * @param nodeId
	 *        the ID to assign to the node
	 * @param locationId
	 *        the location ID
	 */
	protected void setupTestNode(Long nodeId, Long locationId) {
		CommonDbTestUtils.insertNode(jdbcTemplate, nodeId, locationId);
	}

	/**
	 * Set the currently authenticated user.
	 * 
	 * @param auth
	 *        the user to set
	 * @since 1.2
	 */
	protected void setAuthenticatedUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Insert a test location into the sn_loc table.
	 */
	protected void setupTestLocation() {
		setupTestLocation(TEST_LOC_ID);
	}

	/**
	 * Insert a test location into the sn_loc table.
	 * 
	 * @param id
	 *        the location ID
	 */
	protected void setupTestLocation(Long id) {
		setupTestLocation(id, TEST_TZ);
	}

	/**
	 * Insert a test location into the sn_loc table and weather location in the
	 * sn_weather_loc table.
	 * 
	 * @param id
	 *        the location ID to use
	 * @param timeZoneId
	 *        the time zone ID to use
	 */
	protected void setupTestLocation(Long id, String timeZoneId) {
		CommonDbTestUtils.insertLocation(jdbcTemplate, id, TEST_LOC_COUNTRY, TEST_LOC_REGION,
				TEST_LOC_POSTAL_CODE, timeZoneId);
	}

	/**
	 * Insert a test user into the {@literal solaruser.sn_user} table.
	 * 
	 * <p>
	 * The username will be {@literal test[userId]@localhost}.
	 * </p>
	 * 
	 * @param userId
	 *        the ID of the user to create
	 */
	protected void setupTestUser(Long userId) {
		setupTestUser(userId, "test" + userId + "@localhost");
	}

	/**
	 * Insert a test user into the {@literal solaruser.sn_user} table.
	 * 
	 * @param userId
	 *        the ID of the user to create
	 * @param username
	 *        the username to use
	 * @since 1.1
	 */
	protected void setupTestUser(Long userId, String username) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id, disp_name, email, password) values (?,?,?,?)",
				userId, "Test User " + userId, username, "password-" + userId);
	}

	/**
	 * Insert a user-node association into the {@literal solaruser.sn_user_node}
	 * table.
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	protected void setupTestUserNode(Long userId, Long nodeId) {
		jdbcTemplate.update("insert into solaruser.user_node (user_id, node_id) values (?,?)", userId,
				nodeId);
	}

}
