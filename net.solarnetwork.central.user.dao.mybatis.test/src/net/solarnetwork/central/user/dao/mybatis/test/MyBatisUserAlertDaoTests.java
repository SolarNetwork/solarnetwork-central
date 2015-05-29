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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
	private MyBatisUserAlertSituationDao userAlertSituationDao;

	private User user = null;
	private SolarNode node = null;
	private UserAlert userAlert = null;

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
	public void updateValidTo() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());
		DateTime old = alert.getValidTo();
		DateTime v = old.plusDays(1);
		userAlertDao.updateValidTo(alert.getId(), v);
		UserAlert updated = userAlertDao.get(alert.getId());
		assertEquals("ValidTo date updated", v, updated.getValidTo());
	}

	@Test
	public void delete() {
		storeNew();
		userAlertDao.delete(userAlert);
		UserAlert alert = userAlertDao.get(userAlert.getId());
		assertNull("Deleted alert not found", alert);
	}

	@Test
	public void deleteWithSituation() {
		getAlertWithSituationSituationActiveAndResolved();
		userAlertDao.delete(userAlert);
		UserAlert alert = userAlertDao.get(userAlert.getId());
		assertNull("Deleted alert not found", alert);
	}

	@Test
	public void deleteForNodeNone() {
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		assertEquals("None deleted", 0, deleted);
	}

	@Test
	public void deleteForNode() {
		storeNew();
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		assertEquals("1 deleted", 1, deleted);
	}

	@Test
	public void deleteForNodeMulti() {
		final int numAlerts = 5;
		for ( int i = 0; i < numAlerts; i++ ) {
			storeNew();
		}
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		assertEquals("All alerts for user deleted", numAlerts, deleted);
	}

	@Test
	public void deleteForNodeNotOtherNodes() {
		storeNew();

		// setup an alert for a 2nd node, to make sure that alert is NOT deleted
		setupTestNode(-2L); // add a 2nd
		UserAlert alert = new UserAlert();
		alert.setCreated(new DateTime());
		alert.setUserId(this.user.getId());
		alert.setNodeId(-2L);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);
		Long secondAlertId = userAlertDao.store(alert);

		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		assertEquals("Only 1 deleted", 1, deleted);

		UserAlert secondAlert = userAlertDao.get(secondAlertId);
		assertNotNull("Other alert not deleted", secondAlert);
	}

	@Test
	public void findAlertsToProcessNone() {
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null, null);
		assertNotNull("Results should not be null", results);
		assertEquals(0, results.size());
	}

	@Test
	public void findAlertsToProcessOne() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null, null);
		assertNotNull("Results should not be null", results);
		assertEquals(1, results.size());
		assertEquals(userAlert, results.get(0));
	}

	@Test
	public void findAlertsToProcessMulti() {
		List<UserAlert> alerts = new ArrayList<UserAlert>(5);
		for ( int i = 0; i < 5; i++ ) {
			storeNew();
			alerts.add(this.userAlert);
		}
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null, null);
		assertNotNull("Results should not be null", results);
		assertEquals(alerts.size(), results.size());
		assertEquals(alerts, results);
	}

	@Test
	public void findAlertsToProcessBatch() {
		List<UserAlert> alerts = new ArrayList<UserAlert>(12);
		for ( int i = 0; i < 12; i++ ) {
			storeNew();
			alerts.add(this.userAlert);
		}
		List<UserAlert> results = new ArrayList<UserAlert>(12);
		Long startingId = null;
		final DateTime batchTime = new DateTime();
		final Integer max = 5;
		for ( int i = 0; i < 4; i++ ) {
			List<UserAlert> batch = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
					startingId, batchTime, max);
			assertNotNull("Results should not be null", batch);
			if ( i < 3 ) {
				assertTrue("Batch results available", batch.size() > 0);
			} else {
				assertEquals("No more batch results available", 0, batch.size());
			}
			results.addAll(batch);
			if ( batch.size() > 0 ) {
				startingId = batch.get(batch.size() - 1).getId();
			}
		}
		assertEquals(alerts.size(), results.size());
		assertEquals(alerts, results);
	}

	@Test
	public void findAlertsToProcessBatchWithDisabled() {
		List<UserAlert> alerts = new ArrayList<UserAlert>(12);
		for ( int i = 0; i < 24; i++ ) {
			storeNew();
			if ( (i % 2) == 0 ) {
				this.userAlert.setStatus(UserAlertStatus.Disabled);
				userAlertDao.store(this.userAlert);
			} else {
				alerts.add(this.userAlert);
			}
		}
		List<UserAlert> results = new ArrayList<UserAlert>(12);
		Long startingId = null;
		final DateTime batchTime = new DateTime();
		final Integer max = 5;
		for ( int i = 0; i < 4; i++ ) {
			List<UserAlert> batch = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
					startingId, batchTime, max);
			assertNotNull("Results should not be null", batch);
			if ( i < 3 ) {
				assertTrue("Batch results available", batch.size() > 0);
			} else {
				assertEquals("No more batch results available", 0, batch.size());
			}
			results.addAll(batch);
			if ( batch.size() > 0 ) {
				startingId = batch.get(batch.size() - 1).getId();
			}
		}
		assertEquals(alerts.size(), results.size());
		assertEquals(alerts, results);
	}

	@Test
	public void findAlertsForUserNoResults() {
		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		assertNotNull("Results should never be null", results);
		assertEquals("No alerts available", 0, results.size());
	}

	@Test
	public void findAlertsForUserNoMatch() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsForUser(Long.MIN_VALUE);
		assertNotNull("Results should never be null", results);
		assertEquals("No alerts available for user", 0, results.size());
	}

	@Test
	public void findAlertsForUserNoSituation() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		assertNotNull("Results should never be null", results);
		assertEquals("Alerts available for user", 1, results.size());
		UserAlert found = results.get(0);
		assertEquals(this.userAlert.getId(), found.getId());
		assertNull(found.getSituation());
	}

	@Test
	public void findAlertsForUserWithSituationResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(new DateTime());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.store(resolved));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		assertNotNull("Results should never be null", results);
		assertEquals("Alerts available for user", 1, results.size());
		UserAlert found = results.get(0);
		assertEquals(this.userAlert.getId(), found.getId());
		assertNull(found.getSituation());
	}

	@Test
	public void findAlertsForUserWithSituationActive() {
		storeNew();

		// create an active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(new DateTime());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.store(sit));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		assertNotNull("Results should never be null", results);
		assertEquals("Alerts available for user", 1, results.size());
		UserAlert found = results.get(0);
		assertEquals(this.userAlert.getId(), found.getId());

		assertNotNull("Situation associated", found.getSituation());
		assertEquals(sit.getId(), found.getSituation().getId());
	}

	@Test
	public void findAlertsForUserWithSituationActiveAndResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(new DateTime());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.store(resolved));

		// create an Active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(new DateTime());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.store(sit));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		assertNotNull("Results should never be null", results);
		assertEquals("Alerts available for user", 1, results.size());
		UserAlert found = results.get(0);
		assertEquals(this.userAlert.getId(), found.getId());

		assertNotNull("Situation associated", found.getSituation());
		assertEquals(sit.getId(), found.getSituation().getId());
	}

	@Test
	public void getAlertWithSituationNoSituation() {
		storeNew();
		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		assertNotNull("Alert available", found);
		assertEquals(this.userAlert.getId(), found.getId());
		assertNull("No active situation", found.getSituation());
	}

	@Test
	public void getAlertWithSituationSituationResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(new DateTime());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.store(resolved));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		assertNotNull("Alert available", found);
		assertEquals(this.userAlert.getId(), found.getId());
		assertNull("No active situation", found.getSituation());
	}

	@Test
	public void getAlertWithSituationSituationActive() {
		storeNew();

		// create an active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(new DateTime());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.store(sit));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		assertNotNull("Alert available", found);
		assertEquals(this.userAlert.getId(), found.getId());

		assertNotNull("Situation associated", found.getSituation());
		assertEquals(sit.getId(), found.getSituation().getId());
	}

	@Test
	public void getAlertWithSituationSituationActiveAndResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(new DateTime());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.store(resolved));

		// create an Active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(new DateTime());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.store(sit));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		assertNotNull("Alert available", found);
		assertEquals(this.userAlert.getId(), found.getId());

		assertNotNull("Situation associated", found.getSituation());
		assertEquals(sit.getId(), found.getSituation().getId());
	}
}
