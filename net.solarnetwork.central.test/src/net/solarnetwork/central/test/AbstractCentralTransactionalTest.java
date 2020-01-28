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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.security.AuthenticatedNode;

/**
 * Base test class for transactional unit tests.
 * 
 * @author matt
 * @version 2.0
 */
@ContextConfiguration(locations = { "classpath:/net/solarnetwork/central/test/test-context.xml",
		"classpath:/net/solarnetwork/central/test/test-tx-context.xml" })
@Transactional(transactionManager = "txManager")
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
	 * @param locId
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
	 * Set the currently authenticated user to an {@link AuthenticatedNode} with
	 * the given ID.
	 * 
	 * @param nodeId
	 *        the node ID to use
	 * @return the AuthenticatedNode
	 * @since 1.2
	 */
	protected AuthenticatedNode setAuthenticatedNode(final Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, null, false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(node, "foobar", "ROLE_NODE");
		setAuthenticatedUser(auth);
		return node;
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

	/**
	 * Call the {@code solaragg.process_agg_stale_datum} and
	 * {@code solaragg.process_agg_stale_loc_datum} procedures to populate
	 * reporting data.
	 * 
	 * @since 1.1
	 */
	protected void processAggregateStaleData() {
		log.debug("Stale datum at start: " + jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum ORDER BY ts_start, node_id, source_id"));

		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con
						.prepareCall("{? = call solaragg.process_agg_stale_datum(?, ?)}");
				return stmt;
			}
		}, new CallableStatementCallback<Object>() {

			@Override
			public Object doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.registerOutParameter(1, Types.INTEGER);
				cs.setString(2, "h");
				cs.setInt(3, -1);
				cs.execute();
				int processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale hourly datum");

				cs.setString(2, "d");
				cs.execute();
				processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale daily datum");

				cs.setString(2, "m");
				cs.execute();
				processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale monthly datum");
				return null;
			}
		});

		log.debug("Stale location datum at start: "
				+ jdbcTemplate.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum"));

		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con
						.prepareCall("{? = call solaragg.process_agg_stale_loc_datum(?, ?)}");
				return stmt;
			}
		}, new CallableStatementCallback<Object>() {

			@Override
			public Object doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				cs.registerOutParameter(1, Types.INTEGER);
				cs.setString(2, "h");
				cs.setInt(3, -1);
				cs.execute();
				int processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale hourly location datum");

				cs.setString(2, "d");
				cs.execute();
				processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale daily location datum");

				cs.setString(2, "m");
				cs.execute();
				processed = cs.getInt(1);
				log.debug("Processed " + processed + " stale monthly location datum");
				return null;
			}
		});
	}

}
