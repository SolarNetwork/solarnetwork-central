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
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Base test for Spring-managed transactional tests in JUnit 5.
 *
 * <p>
 * This is a transitional class to help migrate JUnit 4 tests to JUnit 5 with
 * minimal changes.
 * <p>
 *
 * @author matt
 * @version 1.5
 */
@SpringJUnitConfig
@Transactional
@Rollback
public class AbstractJUnit5CentralTransactionalTest implements CentralTestConstants {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	protected ApplicationContext applicationContext;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);
	}

	/**
	 * Execute the given SQL script.
	 * <p>
	 * Use with caution outside of a transaction!
	 * <p>
	 * The script will normally be loaded by classpath.
	 * <p>
	 * <b>Do not use this method to execute DDL if you expect rollback.</b>
	 *
	 * @param sqlResourcePath
	 *        the Spring resource path for the SQL script
	 * @param continueOnError
	 *        whether to continue without throwing an exception in the event of
	 *        an error
	 * @throws DataAccessException
	 *         if there is an error executing a statement
	 * @see ResourceDatabasePopulator
	 */
	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError)
			throws DataAccessException {
		DataSource ds = this.jdbcTemplate.getDataSource();
		Assert.state(ds != null, "No DataSource set");
		Assert.state(this.applicationContext != null, "No ApplicationContext set");
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(continueOnError, false, "UTF-8", resource).execute(ds);
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
	 * @return the username
	 */
	protected String setupTestUser(Long userId) {
		return setupTestUser(userId, "test" + userId + "@localhost");
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
	 * @param locationId
	 *        the location ID
	 * @since 1.2
	 */
	protected String setupTestUser(Long userId, Long locationId) {
		return setupTestUser(userId, "test" + userId + "@localhost", locationId);
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
	protected String setupTestUser(Long userId, String username) {
		return setupTestUser(userId, username, null);
	}

	/**
	 * Create a test user account in the user table.
	 *
	 * @param userId
	 *        the user ID
	 * @param username
	 *        the username to use
	 * @param password
	 *        the password to use
	 * @param locationId
	 *        the location ID to use
	 * @since 1.3
	 */
	protected String setupTestUser(Long userId, String username, String password, Long locationId) {
		String dispName = String.format("Tester %d", userId);
		jdbcTemplate.update(
				"insert into solaruser.user_user (id, disp_name, email, password, loc_id) values (?,?,?,?,?)",
				userId, dispName, username, password, locationId);
		return username;
	}

	/**
	 * Insert a test user into the {@literal solaruser.sn_user} table.
	 *
	 * @param userId
	 *        the ID of the user to create
	 * @param username
	 *        the username to use
	 * @param locationId
	 *        the location ID to use
	 * @since 1.2
	 */
	protected String setupTestUser(Long userId, String username, Long locationId) {
		return setupTestUser(userId, username, "password-" + userId, locationId);
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
		CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);
	}

	/**
	 * Set a node's name in the sn_node table.
	 *
	 * @param nodeId
	 *        the ID of the node to save the name for
	 * @param name
	 *        the name
	 */
	protected void saveNodeName(Long nodeId, String name) {
		jdbcTemplate.update("update solaruser.user_node set disp_name = ? where node_id = ?", name,
				nodeId);
	}

}
