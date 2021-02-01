/* ==================================================================
 * BaseDatumJdbcTestSupport.java - 4/11/2019 9:21:05 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.jdbc.test;

import static java.util.stream.Collectors.toList;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

/**
 * Base class for datum JDBC test support.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class BaseDatumJdbcTestSupport extends AbstractCentralTransactionalTest {

	public static final Long TEST_USER_ID = Long.valueOf(-9999);
	public static final String TEST_USERNAME = "unittest@localhost";
	public static final Long TEST_PRICE_SOURCE_ID = Long.valueOf(-9998);
	public static final String TEST_PRICE_SOURCE_NAME = "Test Source";
	public static final Long TEST_PRICE_LOC_ID = Long.valueOf(-9997);
	public static final String TEST_PRICE_LOC_NAME = "Test Price Location";
	public static final String TEST_SOURCE_ID = "test.source";

	@Autowired
	protected PlatformTransactionManager txManager;

	/**
	 * Insert a test user into the solaruser.user_user table.
	 * 
	 * <p>
	 * This will use {@link #TEST_USER_ID} and {@link #TEST_USERNAME} values.
	 * </p>
	 */
	protected void setupTestUser() {
		setupTestUser(TEST_USER_ID, TEST_USERNAME);
	}

	/**
	 * Insert a test user into the solaruser.user_user table.
	 * 
	 * @param id
	 *        the user ID
	 * @param username
	 *        the user username
	 */
	protected void setupTestUser(Long id, String username) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id,email,password,disp_name,enabled) values (?,?,?,?,?)",
				id, username, DigestUtils.sha256Hex("password"), "Unit Test", Boolean.TRUE);
	}

	/**
	 * Insert a test UserNode into the solaruser.user_node table. The user and
	 * node must already exist.
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param name
	 *        the display name
	 * @since 1.1
	 */
	protected void setupTestUserNode(Long userId, Long nodeId, String name) {
		jdbcTemplate.update("insert into solaruser.user_node (user_id,node_id,disp_name) values (?,?,?)",
				userId, nodeId, name);
	}

	/**
	 * Insert a test price source into the solarnet.sn_price_source table.
	 * 
	 * @param id
	 *        the source ID
	 * @param name
	 *        the source name
	 */
	protected void setupTestPriceSource(Long id, String name) {
		jdbcTemplate.update("insert into solarnet.sn_price_source (id,sname) values (?,?)", id, name);
	}

	/**
	 * Insert a test price location into the solarnet.sn_price_loc table.
	 * 
	 * @param id
	 *        the ID
	 * @param name
	 *        the name
	 * @param sourceId
	 *        the price source ID
	 */
	protected void setupTestPriceLocation(Long id, Long locationId, String name, Long sourceId) {
		jdbcTemplate.update(
				"insert into solarnet.sn_price_loc (id,loc_id,loc_name,source_id,currency,unit) values (?,?,?,?,?,?)",
				id, locationId, name, sourceId, "NZD", "kWh");
	}

	/**
	 * Insert a test node record into the database.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param locationId
	 *        the node's location ID
	 */
	@Override
	protected void setupTestNode(Long nodeId, Long locationId) {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", nodeId,
				locationId);
	}

	/**
	 * Insert a test location record into the database.
	 * 
	 * @param id
	 *        the location ID
	 * @param timeZoneId
	 *        the time zone to use
	 */
	@Override
	protected void setupTestLocation(Long id, String timeZoneId) {
		jdbcTemplate.update(
				"insert into solarnet.sn_loc (id,country,region,postal_code,time_zone) values (?,?,?,?,?)",
				id, TEST_LOC_COUNTRY, TEST_LOC_REGION, TEST_LOC_POSTAL_CODE, timeZoneId);
	}

	/**
	 * Insert a test UserNode record into the database.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param userId
	 *        the user ID
	 */
	protected void setupUserNodeEntity(Long nodeId, Long userId) {
		jdbcTemplate.update("INSERT INTO solaruser.user_node (node_id, user_id) VALUES (?,?)", nodeId,
				userId);
	}

	protected Date[] sqlDates(String... str) {
		return Arrays.stream(str)
				.map(s -> new Date(ISODateTimeFormat.localDateParser().parseLocalDate(s)
						.toDateTimeAtStartOfDay().getMillis()))
				.collect(Collectors.toList()).toArray(new Date[str.length]);
	}

	protected List<Date> sqlDatesFromLocalDates(List<Map<String, Object>> rows) {
		return rows.stream().map(d -> (Date) d.get("local_date")).collect(toList());
	}

}
