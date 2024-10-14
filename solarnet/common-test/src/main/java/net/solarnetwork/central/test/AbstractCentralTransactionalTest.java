/* ==================================================================
 * AbstractCentralTransactionalTest.java - Jan 11, 2010 9:59:13 AM
 *
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Base test class for transactional unit tests.
 *
 * @author matt
 * @version 3.0
 */
@Rollback
public abstract class AbstractCentralTransactionalTest
		extends AbstractTransactionalJUnit4SpringContextTests implements CentralTestConstants {

	/** A date + time format. */
	public final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);
	}

	/**
	 * Setup the {@link #dateTimeFormat} timezone.
	 */
	@BeforeTransaction
	public void setupDateTime() {
		dateTimeFormat.setTimeZone(TimeZone.getTimeZone(TEST_TZ));
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
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", nodeId,
				locationId);
		int count = jdbcTemplate.queryForObject(
				"select count(*) from solarnet.sn_node where node_id = ?", Integer.class, nodeId);
		log.debug("Test SolarNode [" + nodeId + "] created: " + count);
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

	/**
	 * Create a test user account in the user table.
	 *
	 * @param userId
	 *        the user ID
	 */
	protected String setupTestUser(Long userId) {
		String password = String.format("password-%d", userId);
		String username = String.format("test%d@localhost", userId);
		return setupTestUser(userId, username, password);
	}

	/**
	 * Create a test user account in the user table.
	 *
	 * @param userId
	 *        the user ID
	 * @param username
	 *        the username to use
	 */
	protected String setupTestUser(Long userId, String username) {
		String password = String.format("password-%d", userId);
		return setupTestUser(userId, username, password);
	}

	/**
	 * Create a test user account in the user table.
	 *
	 * @param userId
	 *        the user ID
	 */
	protected String setupTestUser(Long userId, String username, String password) {
		String dispName = String.format("Tester %d", userId);
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_user (id,disp_name,email,password,enabled) VALUES (?,?,?,?,?)",
				userId, dispName, username, password, Boolean.TRUE);
		return username;
	}

	/**
	 * Insert a new user-node association into the user_node table.
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
		jdbcTemplate.update(
				"insert into solarnet.sn_loc (id,country,region,postal_code,time_zone) values (?,?,?,?,?)",
				id, TEST_LOC_COUNTRY, TEST_LOC_REGION, TEST_LOC_POSTAL_CODE, timeZoneId);
	}

}
