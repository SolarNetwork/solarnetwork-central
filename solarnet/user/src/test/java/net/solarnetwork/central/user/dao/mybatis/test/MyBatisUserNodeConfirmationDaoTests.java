/* ==================================================================
 * MyBatisUserNodeConfirmationDaoTests.java - Nov 11, 2014 9:36:23 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import static org.junit.Assert.assertNull;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;

/**
 * Test cases for the {@link MyBatisUserNodeConfirmationDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserNodeConfirmationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	/**
	 * The tables to delete from at the start of the tests (within a
	 * transaction).
	 */
	private static final String[] DELETE_TABLES = new String[] { "solaruser.user_node_conf",
			"solaruser.user_node", "solaruser.user_user" };

	private static final Long TEST_ID_2 = -2L;

	private MyBatisUserNodeConfirmationDao userNodeConfirmationDao;
	private MyBatisSolarNodeDao solarNodeDao;

	private User user = null;
	private SolarNode node = null;
	private UserNodeConfirmation userNodeConf = null;
	private Long testNodeId;

	@Before
	public void setUp() throws Exception {
		userNodeConfirmationDao = new MyBatisUserNodeConfirmationDao();
		userNodeConfirmationDao.setSqlSessionFactory(getSqlSessionFactory());
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		userNodeConf = null;
		testNodeId = TEST_NODE_ID;
	}

	@Test
	public void storeNew() {
		UserNodeConfirmation newUserNodeConf = new UserNodeConfirmation();
		newUserNodeConf.setCreated(Instant.now());
		newUserNodeConf.setUser(this.user);
		newUserNodeConf.setConfirmationKey(String.valueOf(testNodeId));
		newUserNodeConf.setSecurityPhrase("test phrase");
		newUserNodeConf.setCountry("NZ");
		newUserNodeConf.setTimeZoneId("Pacific/Auckland");
		Long id = userNodeConfirmationDao.store(newUserNodeConf);
		assertNotNull(id);
		this.userNodeConf = userNodeConfirmationDao.get(id);
	}

	private void validate(UserNodeConfirmation conf, UserNodeConfirmation entity) {
		assertNotNull("UserNodeConfirmation should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(conf.getConfirmationDate(), entity.getConfirmationDate());
		assertEquals(conf.getConfirmationKey(), entity.getConfirmationKey());
		assertEquals(conf.getSecurityPhrase(), entity.getSecurityPhrase());
		assertEquals(conf.getCountry(), entity.getCountry());
		assertEquals(conf.getTimeZoneId(), entity.getTimeZoneId());
		assertEquals(conf.getId(), entity.getId());
		assertEquals(conf.getNodeId(), entity.getNodeId());
		assertEquals(conf.getUser(), entity.getUser());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserNodeConfirmation conf = userNodeConfirmationDao.get(userNodeConf.getId());
		validate(this.userNodeConf, conf);
	}

	@Test
	public void update() {
		storeNew();
		setupTestNode(TEST_ID_2);
		UserNodeConfirmation conf = userNodeConfirmationDao.get(userNodeConf.getId());
		assertNull(conf.getNodeId());
		conf.setNodeId(testNodeId);
		Long id = userNodeConfirmationDao.store(conf);
		assertNotNull(id);
		assertEquals(userNodeConf.getId(), id);
		UserNodeConfirmation updated = userNodeConfirmationDao.get(userNodeConf.getId());
		validate(conf, updated);
	}

	@Test
	public void getConfirmationForKey() {
		storeNew();
		UserNodeConfirmation conf = userNodeConfirmationDao.getConfirmationForKey(
				userNodeConf.getUser().getId(), userNodeConf.getConfirmationKey());
		validate(userNodeConf, conf);
	}

	@Test
	public void findPendingConfirmationsForUserEmpty() {
		List<UserNodeConfirmation> results = userNodeConfirmationDao
				.findPendingConfirmationsForUser(user);
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	@Test
	public void findPendingConfirmationsForUserSingle() {
		storeNew();
		List<UserNodeConfirmation> results = userNodeConfirmationDao
				.findPendingConfirmationsForUser(user);
		assertNotNull(results);
		assertEquals(1, results.size());
		UserNodeConfirmation conf = results.get(0);
		validate(userNodeConf, conf);
	}

	@Test
	public void findPendingConfirmationsForUserMultiple() {
		storeNew();
		UserNodeConfirmation conf0 = this.userNodeConf;
		testNodeId = TEST_ID_2;
		storeNew();
		List<UserNodeConfirmation> results = userNodeConfirmationDao
				.findPendingConfirmationsForUser(user);
		assertNotNull(results);
		assertEquals(2, results.size());
		validate(conf0, results.get(0));
		validate(userNodeConf, results.get(1));
	}

	@Test
	public void findPendingConfirmationsForUserMultipleMixed() {
		storeNew();

		// make the confirmation no longer pending
		UserNodeConfirmation conf0 = this.userNodeConf;
		conf0.setConfirmationDate(Instant.now());
		userNodeConfirmationDao.store(conf0);

		// now add a 2nd confirmation that is still pending
		testNodeId = TEST_ID_2;
		storeNew();
		List<UserNodeConfirmation> results = userNodeConfirmationDao
				.findPendingConfirmationsForUser(user);
		assertNotNull(results);
		assertEquals(1, results.size());
		validate(userNodeConf, results.get(0));
	}

}
