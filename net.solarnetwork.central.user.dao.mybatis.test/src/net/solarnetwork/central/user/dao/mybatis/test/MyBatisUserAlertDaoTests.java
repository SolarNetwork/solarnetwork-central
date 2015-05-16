/* ==================================================================
 * MyBatisUserAlertDaoTests.java - 16/05/2015 5:24:47 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisUserAlertDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserAlertDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisSolarNodeDao solarNodeDao;

	private MyBatisUserAlertDao userAlertDao;

	private User user = null;
	private SolarNode node = null;
	private UserAlert userAlert = null;

	@Before
	public void setUp() throws Exception {
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userAlertDao = new MyBatisUserAlertDao();
		userAlertDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		//deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		userAlert = null;
	}

	@Test
	public void storeNew() {
		UserAlert alert = new UserAlert();
		alert.setCreated(new DateTime());
		alert.setUserId(this.user.getId());
		alert.setNodeId(TEST_NODE_ID);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);

		Map<String, Object> options = new HashMap<String, Object>(4);
		options.put("string", "foo");
		options.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		options.put("list", optionList);

		alert.setOptions(options);

		Long id = userAlertDao.store(alert);
		assertNotNull(id);
		alert.setId(id);
		this.userAlert = alert;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());
		assertNotNull(alert);
		assertEquals(this.userAlert.getId(), alert.getId());
		assertEquals(this.user.getId(), alert.getUserId());
		assertEquals(TEST_NODE_ID, alert.getNodeId());
		assertEquals(this.userAlert.getType(), alert.getType());
		assertEquals(this.userAlert.getStatus(), alert.getStatus());
		assertNotNull(alert.getOptions());

		Map<String, Object> options = alert.getOptions();
		assertEquals("foo", options.get("string"));
		assertEquals(42, options.get("number"));

		assertNotNull(options.get("list"));
		assertEquals(Arrays.asList("first", "second"), options.get("list"));
	}

	@Test
	public void update() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());

		DateTime created = userAlert.getCreated();
		alert.setStatus(UserAlertStatus.Disabled);
		alert.getOptions().put("updated-string", "updated");

		Long id = userAlertDao.store(alert);
		assertNotNull(id);
		assertEquals(this.userAlert.getId(), id);
		UserAlert updatedUserAlert = userAlertDao.get(id);
		assertNotNull(updatedUserAlert);
		assertEquals(this.userAlert.getId(), updatedUserAlert.getId());
		assertEquals(this.user.getId(), updatedUserAlert.getUserId());
		assertEquals(TEST_NODE_ID, updatedUserAlert.getNodeId());
		assertEquals(this.userAlert.getType(), updatedUserAlert.getType());
		assertEquals(UserAlertStatus.Disabled, updatedUserAlert.getStatus());
		assertNotNull(alert.getOptions());

		Map<String, Object> options = alert.getOptions();
		assertEquals("foo", options.get("string"));
		assertEquals(42, options.get("number"));
		assertEquals("updated", options.get("updated-string"));

		assertNotNull(options.get("list"));
		assertEquals(Arrays.asList("first", "second"), options.get("list"));

		assertEquals(created, updatedUserAlert.getCreated());
	}

	@Test
	public void findAlertsToProcessNone() {
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null);
		assertNotNull("Results should not be null", results);
		assertEquals(0, results.size());
	}

	@Test
	public void findAlertsToProcessOne() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null);
		assertNotNull("Results should not be null", results);
		assertEquals(1, results.size());
		assertEquals(userAlert, results.get(0));
	}
}
