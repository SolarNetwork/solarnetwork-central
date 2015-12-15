/* ==================================================================
 * MyBatisUserAlertSituationDaoTests.java - 16/05/2015 5:25:42 pm
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
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserAlertSituationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAlert;
import net.solarnetwork.central.user.domain.UserAlertSituation;
import net.solarnetwork.central.user.domain.UserAlertSituationStatus;
import net.solarnetwork.central.user.domain.UserAlertStatus;
import net.solarnetwork.central.user.domain.UserAlertType;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisUserAlertSituationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserAlertSituationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisSolarNodeDao solarNodeDao;
	private MyBatisUserAlertDao userAlertDao;

	private MyBatisUserAlertSituationDao userAlertSituationDao;

	private User user = null;
	private SolarNode node = null;
	private UserAlert userAlert = null;
	private UserAlertSituation userAlertSituation = null;

	@Before
	public void setUp() throws Exception {
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userAlertDao = new MyBatisUserAlertDao();
		userAlertDao.setSqlSessionFactory(getSqlSessionFactory());
		userAlertSituationDao = new MyBatisUserAlertSituationDao();
		userAlertSituationDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
		//deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		userAlert = null;
		userAlertSituation = null;
	}

	private UserAlert createUserAlert() {
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
		return alert;
	}

	private UserAlertSituation storeNew(UserAlert alert, UserAlertSituationStatus status,
			DateTime notified) {
		UserAlertSituation sit = new UserAlertSituation();
		sit.setCreated(new DateTime());
		sit.setAlert(alert);
		sit.setStatus(status);
		sit.setNotified(notified);

		Map<String, Object> info = new HashMap<String, Object>(4);
		info.put("string", "foo");
		info.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		info.put("list", optionList);

		sit.setInfo(info);

		Long id = userAlertSituationDao.store(sit);
		sit.setId(id);
		return sit;
	}

	@Test
	public void storeNew() {
		UserAlertSituation sit = storeNew(createUserAlert(), UserAlertSituationStatus.Active, null);
		assertNotNull("Primary key should be assigned", sit.getId());
		this.userAlertSituation = sit;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao.get(this.userAlertSituation.getId());
		assertNotNull(sit);
		assertEquals(this.userAlertSituation.getId(), sit.getId());
		assertEquals(this.userAlertSituation.getStatus(), sit.getStatus());
		assertEquals(this.userAlertSituation.getInfo(), sit.getInfo());
		assertNotNull(sit.getAlert());
		assertEquals(this.userAlert.getId(), sit.getAlert().getId());
	}

	@Test
	public void update() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao.get(this.userAlertSituation.getId());

		Map<String, Object> info = sit.getInfo();
		info.put("updated-string", "updated");
		sit.setInfo(info);

		DateTime created = userAlertSituation.getCreated();
		sit.setStatus(UserAlertSituationStatus.Resolved);
		sit.setNotified(created.plusHours(1));
		Long id = userAlertSituationDao.store(sit);
		assertNotNull(id);
		assertEquals(this.userAlertSituation.getId(), id);
		UserAlertSituation updatedSit = userAlertSituationDao.get(id);
		assertNotNull(updatedSit);
		assertEquals(this.userAlertSituation.getId(), updatedSit.getId());
		assertNotNull(updatedSit.getAlert());
		assertEquals(this.userAlert.getId(), updatedSit.getAlert().getId());
		assertEquals(UserAlertSituationStatus.Resolved, updatedSit.getStatus());

		info = updatedSit.getInfo();
		assertEquals("foo", info.get("string"));
		assertEquals(42, info.get("number"));
		assertEquals("updated", info.get("updated-string"));

		assertNotNull(info.get("list"));
		assertEquals(Arrays.asList("first", "second"), info.get("list"));

		assertEquals(created, updatedSit.getCreated());
	}

	@Test
	public void getByActiveAlertNoAlert() {
		UserAlertSituation sit = userAlertSituationDao.getActiveAlertSituationForAlert(123L);
		Assert.assertNull(sit);
	}

	@Test
	public void getByActiveAlertAlert() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao.getActiveAlertSituationForAlert(this.userAlert
				.getId());
		assertNotNull(sit);
		assertEquals(this.userAlertSituation.getId(), sit.getId());
	}

	@Test
	public void getByActiveAlertAlertResolved() {
		storeNew();
		this.userAlertSituation.setStatus(UserAlertSituationStatus.Resolved);
		userAlertSituationDao.store(this.userAlertSituation);
		UserAlertSituation sit = userAlertSituationDao.getActiveAlertSituationForAlert(this.userAlert
				.getId());
		Assert.assertNull(sit);
	}

	@Test
	public void getByActiveAlertAlertAndResolved() {
		storeNew();
		this.userAlertSituation.setStatus(UserAlertSituationStatus.Resolved);
		userAlertSituationDao.store(this.userAlertSituation);

		// store another now, as Active
		storeNew();

		UserAlertSituation sit = userAlertSituationDao.getActiveAlertSituationForAlert(this.userAlert
				.getId());
		assertNotNull(sit);
		assertEquals(this.userAlertSituation.getId(), sit.getId());
	}

	@Test
	public void purgeCompletedInstructionsNone() {
		long result = userAlertSituationDao.purgeResolvedSituations(new DateTime());
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsNoMatchActive() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Active, new DateTime());
		long result = userAlertSituationDao.purgeResolvedSituations(userAlertSituation.getCreated()
				.plusDays(1));
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsNoMatchDateMissing() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved, null);
		long result = userAlertSituationDao.purgeResolvedSituations(userAlertSituation.getCreated()
				.minusDays(1));
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsNoMatchDateMismatch() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved,
				new DateTime());
		long result = userAlertSituationDao.purgeResolvedSituations(userAlertSituation.getCreated()
				.minusDays(1));
		assertEquals(0, result);
	}

	@Test
	public void purgeCompletedInstructionsMatch() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved,
				new DateTime());
		long result = userAlertSituationDao.purgeResolvedSituations(userAlertSituation.getCreated()
				.plusDays(1));
		assertEquals(1, result);
		UserAlertSituation sit = userAlertSituationDao.get(userAlertSituation.getId());
		assertNull("Purged situation is not found", sit);
	}

	@Test
	public void purgeCompletedInstructionsMatchMultiple() {
		List<UserAlertSituation> toPurge = new ArrayList<UserAlertSituation>();
		List<UserAlertSituation> notToPurge = new ArrayList<UserAlertSituation>();
		toPurge.add(storeNew(createUserAlert(), UserAlertSituationStatus.Resolved, new DateTime()));
		notToPurge.add(storeNew(userAlert, UserAlertSituationStatus.Active, new DateTime())); // Active state, should NOT be deleted
		toPurge.add(storeNew(userAlert, UserAlertSituationStatus.Resolved, new DateTime()));
		long result = userAlertSituationDao.purgeResolvedSituations(new DateTime().plusDays(1));
		assertEquals("Purged resolved", toPurge.size(), result);
		for ( UserAlertSituation sit : notToPurge ) {
			UserAlertSituation match = userAlertSituationDao.get(sit.getId());
			assertNotNull("Should not be purged", match);
		}
		for ( UserAlertSituation sit : toPurge ) {
			UserAlertSituation match = userAlertSituationDao.get(sit.getId());
			assertNull("Purged situation is not found", match);
		}
	}

}
