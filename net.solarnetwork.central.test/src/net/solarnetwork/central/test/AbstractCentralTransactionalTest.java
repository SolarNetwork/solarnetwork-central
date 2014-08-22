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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base test class for transactional unit tests.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration(locations = { "classpath:/net/solarnetwork/central/test/test-context.xml" })
@TransactionConfiguration(transactionManager = "txManager", defaultRollback = true)
@Transactional
public abstract class AbstractCentralTransactionalTest extends
		AbstractTransactionalJUnit4SpringContextTests {

	/** A test Node ID. */
	public static final Long TEST_NODE_ID = -1L;

	/** A test Weather Source ID. */
	public static final Long TEST_WEATHER_SOURCE_ID = -1L;

	/** A test Weather Source name. */
	public static final String TEST_WEATHER_SOURCE_NAME = "Test weather source";

	/** A test Price Source ID. */
	public static final Long TEST_PRICE_SOURCE_ID = -1L;

	/** A test Price Source name. */
	public static final String TEST_PRICE_SOURCE_NAME = "Test price source";

	/** A test Location ID. */
	public static final Long TEST_LOC_ID = -1L;

	/** A test location country. */
	public static final String TEST_LOC_COUNTRY = "NZ";

	/** A test location name */
	public static final String TEST_LOC_NAME = "Test Location";

	/** A test location region */
	public static final String TEST_LOC_REGION = "Wellington";

	/** A test location postal code */
	public static final String TEST_LOC_POSTAL_CODE = "6011";

	/** A test weather Location ID. */
	public static final Long TEST_WEATHER_LOC_ID = -1L;

	/** A test hardware ID. */
	public static final Long TEST_HARDWARE_ID = -1L;

	/** A test hardware manufacturer. */
	public static final String TEST_HARDWARE_MANUFACTURER = "Test Manufacturer";

	/** A test hardware model. */
	public static final String TEST_HARDWARE_MODEL = "Test Model";

	/** A test hardware control ID. */
	public static final Long TEST_HARDWARE_CONTROL_ID = -1L;

	/** A test TimeZone ID. */
	public static final String TEST_TZ = "Pacific/Auckland";

	/** A date + time format. */
	public final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/** A test currency. */
	public static final String TEST_CURRENCY = "NZD";

	/** A class-level logger. */
	protected final Logger log = Logger.getLogger(getClass());

	protected JdbcTemplate jdbcTemplate;

	@Override
	@Autowired
	public void setDataSource(DataSource dataSource) {
		super.setDataSource(dataSource);
		jdbcTemplate = new JdbcTemplate(dataSource);
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
		setupTestNode(nodeId, TEST_LOC_ID, TEST_WEATHER_LOC_ID);
	}

	/**
	 * Insert a test node into the sn_node table.
	 * 
	 * @param nodeId
	 *        the ID to assign to the node
	 * @param locId
	 *        the location ID
	 * @param weatherLocationId
	 *        the weather location ID
	 */
	protected void setupTestNode(Long nodeId, Long locationId, Long weatherLocationId) {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id, wloc_id) values (?,?,?)",
				nodeId, locationId, weatherLocationId);
		int count = jdbcTemplate.queryForInt("select count(*) from solarnet.sn_node where node_id = ?",
				nodeId);
		log.debug("Test SolarNode [" + nodeId + "] created: " + count);
	}

	/**
	 * Insert a test location into the sn_loc table.
	 */
	protected void setupTestLocation() {
		setupTestLocation(TEST_LOC_ID, TEST_WEATHER_LOC_ID, TEST_LOC_NAME);
	}

	/**
	 * Insert a test location into the sn_loc table and weather location in the
	 * sn_weather_loc table.
	 */
	protected void setupTestLocation(Long id, Long weatherLocId, String name) {
		jdbcTemplate
				.update("insert into solarnet.sn_loc (id,loc_name,country,region,postal_code,time_zone) values (?,?,?,?,?,?)",
						id, name, TEST_LOC_COUNTRY, TEST_LOC_REGION, TEST_LOC_POSTAL_CODE, TEST_TZ);
		jdbcTemplate.update("insert into solarnet.sn_weather_source (id,sname) values (?,?)",
				TEST_WEATHER_SOURCE_ID, TEST_WEATHER_SOURCE_NAME);
		jdbcTemplate.update("insert into solarnet.sn_weather_loc (id,loc_id,source_id) values (?,?,?)",
				weatherLocId, id, TEST_WEATHER_SOURCE_ID);
	}

	/**
	 * Insert a default test price source and location into the database.
	 */
	protected void setupTestPriceLocation() {
		setupTestPriceLocation(TEST_LOC_ID, TEST_LOC_NAME, TEST_PRICE_SOURCE_ID, TEST_PRICE_SOURCE_NAME);
	}

	/**
	 * Insert a test price source location into the sn_price_source and
	 * sn_price_loc tables.
	 * 
	 * @param id
	 *        the location ID
	 * @param name
	 *        the location name
	 * @param sourceId
	 *        the source ID
	 * @param sourceName
	 *        the source name
	 */
	protected void setupTestPriceLocation(Long id, String name, Long sourceId, String sourceName) {
		jdbcTemplate.update("insert into solarnet.sn_price_source (id,sname) values (?,?)", sourceId,
				sourceName);
		jdbcTemplate
				.update("insert into solarnet.sn_price_loc (id,loc_name,source_id,currency,unit,time_zone) values (?,?,?,?,?,?)",
						id, name, sourceId, TEST_CURRENCY, "MWh", "Pacific/Auckland");
	}

	/**
	 * Insert a test hardware into the sn_hardware table.
	 */
	protected void setupTestHardware() {
		setupTestHardware(TEST_HARDWARE_ID, TEST_HARDWARE_MANUFACTURER, TEST_HARDWARE_MODEL);
	}

	/**
	 * Insert a test hardware into the sn_hardware table.
	 * 
	 * @param id
	 *        the primary key
	 * @param manufacturer
	 *        the manufacturer
	 * @param model
	 *        the model
	 */
	protected void setupTestHardware(Long id, String manufacturer, String model) {
		jdbcTemplate.update("insert into solarnet.sn_hardware (id,manufact,model) values (?,?,?)", id,
				manufacturer, model);
	}

	/**
	 * Insert a test hardware control into the sn_hardware_control table.
	 */
	protected void setupTestHardwareControl() {
		setupTestHardwareControl(TEST_HARDWARE_ID, TEST_HARDWARE_CONTROL_ID);
	}

	/**
	 * Insert a test hardware control into the sn_hardware_control table.
	 * 
	 * @param hardwareId
	 *        the hardware primary key
	 * @param controlId
	 *        the control primary key
	 */
	protected void setupTestHardwareControl(Long hardwareId, Long controlId) {
		jdbcTemplate.update(
				"insert into solarnet.sn_hardware_control (id,hw_id,ctl_name,unit) values (?,?,?,?)",
				controlId, hardwareId, "Test Hardware Control", "W");
	}

	protected void processReportingStaleData() {
		List<SqlParameter> params = Collections.emptyList();
		jdbcTemplate.call(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				return con.prepareCall("{call solarrep.process_rep_stale_node_datum()}");
			}
		}, params);
	}

	/**
	 * Call the {@code solaragg.process_agg_stale_datum} procedure to populate
	 * reporting data.
	 * 
	 * @since 1.1
	 */
	protected void processAggregateStaleData() {
		jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con
						.prepareCall("{call solaragg.process_agg_stale_datum(?, ?)}");
				stmt.setString(1, "h");
				stmt.setInt(2, -1);
				return stmt;
			}
		}, new CallableStatementCallback<Object>() {

			@Override
			public Object doInCallableStatement(CallableStatement cs) throws SQLException,
					DataAccessException {
				cs.setString(1, "h");
				cs.setInt(2, -1);
				cs.execute();
				cs.setString(1, "d");
				cs.execute();
				return null;
			}
		});
	}

}
