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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.BDDAssertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * Test cases for the {@link MyBatisUserAlertDao} class.
 * 
 * @author matt
 * @version 2.2
 */
public class MyBatisUserAlertDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisSolarNodeDao solarNodeDao;
	private MyBatisUserAlertDao userAlertDao;
	private MyBatisUserAlertSituationDao userAlertSituationDao;

	private User user = null;
	private SolarNode node = null;
	private UserAlert userAlert = null;

	@BeforeEach
	public void setUp() throws Exception {
		solarNodeDao = new MyBatisSolarNodeDao();
		solarNodeDao.setSqlSessionFactory(getSqlSessionFactory());
		userAlertDao = new MyBatisUserAlertDao();
		userAlertDao.setSqlSessionFactory(getSqlSessionFactory());
		userAlertSituationDao = new MyBatisUserAlertSituationDao();
		userAlertSituationDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		then(this.node).isNotNull();
		//deleteFromTables(DELETE_TABLES);
		this.user = createNewUser(TEST_EMAIL);
		then(this.user).isNotNull();
		userAlert = null;
	}

	@Test
	public void storeNew() {
		UserAlert alert = new UserAlert();
		alert.setCreated(Instant.now());
		alert.setUserId(this.user.getId());
		alert.setNodeId(TEST_NODE_ID);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);
		alert.setValidTo(alert.getCreated());

		Map<String, Object> options = new HashMap<String, Object>(4);
		options.put("string", "foo");
		options.put("number", 42);

		List<String> optionList = new ArrayList<String>(4);
		optionList.add("first");
		optionList.add("second");
		options.put("list", optionList);

		alert.setOptions(options);

		Long id = userAlertDao.save(alert);
		then(id).isNotNull();
		alert.setId(id);
		this.userAlert = alert;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());
		// @formatter:off
		then(alert)
			.isNotNull()
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(user.getId(), from(UserAlert::getUserId))
			.returns(TEST_NODE_ID, from(UserAlert::getNodeId))
			.returns(userAlert.getType(), from(UserAlert::getType))
			.returns(userAlert.getStatus(), from(UserAlert::getStatus))
			.extracting(UserAlert::getOptions)
			.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
			.containsEntry("string", "foo")
			.containsEntry("number", 42)
			.containsEntry("list", Arrays.asList("first", "second"))
			;
		// @formatter:on
	}

	@Test
	public void update() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());

		Instant created = userAlert.getCreated();
		alert.setStatus(UserAlertStatus.Disabled);

		Map<String, Object> options = alert.getOptions();
		options.put("updated-string", "updated");
		alert.setOptions(options);

		Long id = userAlertDao.save(alert);
		then(id).isNotNull();
		then(id).isEqualTo(this.userAlert.getId());
		UserAlert updatedUserAlert = userAlertDao.get(id);
		// @formatter:off
		then(updatedUserAlert)
			.isNotNull()
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(user.getId(), from(UserAlert::getUserId))
			.returns(TEST_NODE_ID, from(UserAlert::getNodeId))
			.returns(userAlert.getType(), from(UserAlert::getType))
			.returns(UserAlertStatus.Disabled, from(UserAlert::getStatus))
			.returns(created, from(UserAlert::getCreated))
			.extracting(UserAlert::getOptions)
			.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
			.containsEntry("string", "foo")
			.containsEntry("number", 42)
			.containsEntry("updated-string", "updated")
			.containsEntry("list", Arrays.asList("first", "second"))
			;
		// @formatter:on
	}

	@Test
	public void updateValidTo() {
		storeNew();
		UserAlert alert = userAlertDao.get(this.userAlert.getId());
		Instant old = alert.getValidTo();
		Instant v = old.plus(1, ChronoUnit.DAYS);
		userAlertDao.updateValidTo(alert.getId(), v);
		UserAlert updated = userAlertDao.get(alert.getId());
		then(updated.getValidTo()).as("ValidTo date updated").isEqualTo(v);
	}

	@Test
	public void delete() {
		storeNew();
		userAlertDao.delete(userAlert);
		UserAlert alert = userAlertDao.get(userAlert.getId());
		then(alert).as("Deleted alert not found").isNull();
	}

	@Test
	public void deleteWithSituation() {
		getAlertWithSituationSituationActiveAndResolved();
		userAlertDao.delete(userAlert);
		UserAlert alert = userAlertDao.get(userAlert.getId());
		then(alert).as("Deleted alert not found").isNull();
	}

	@Test
	public void deleteForNodeNone() {
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		then(deleted).as("None deleted").isZero();
	}

	@Test
	public void deleteForNode() {
		storeNew();
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		then(deleted).as("1 deleted").isOne();
	}

	@Test
	public void deleteForNodeMulti() {
		final int numAlerts = 5;
		for ( int i = 0; i < numAlerts; i++ ) {
			storeNew();
		}
		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		then(deleted).as("All alerts for user deleted").isEqualTo(numAlerts);
	}

	@Test
	public void deleteForNodeNotOtherNodes() {
		storeNew();

		// setup an alert for a 2nd node, to make sure that alert is NOT deleted
		setupTestNode(-2L); // add a 2nd
		UserAlert alert = new UserAlert();
		alert.setCreated(Instant.now());
		alert.setUserId(this.user.getId());
		alert.setNodeId(-2L);
		alert.setType(UserAlertType.NodeStaleData);
		alert.setStatus(UserAlertStatus.Active);
		Long secondAlertId = userAlertDao.save(alert);

		int deleted = userAlertDao.deleteAllAlertsForNode(user.getId(), node.getId());
		then(deleted).as("Only 1 deleted").isOne();

		UserAlert secondAlert = userAlertDao.get(secondAlertId);
		then(secondAlert).as("Other alert not deleted").isNotNull();
	}

	@Test
	public void findAlertsToProcessNone() {
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null, null);
		then(results).as("Results should not be null").isNotNull().isEmpty();
	}

	@Test
	public void findAlertsToProcessOne() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData, null,
				null, null);
		then(results).as("Results should not be null").hasSize(1).element(0).isEqualTo(userAlert);
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
		then(results).as("Results should not be null").containsExactlyElementsOf(alerts);
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
		final Instant batchTime = Instant.now();
		final Integer max = 5;
		for ( int i = 0; i < 4; i++ ) {
			List<UserAlert> batch = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
					startingId, batchTime, max);
			then(batch).as("Results should not be null").isNotNull();
			if ( i < 3 ) {
				then(batch).as("Batch results available").isNotEmpty();
			} else {
				then(batch).as("No more batch results available").isEmpty();
			}
			results.addAll(batch);
			if ( batch.size() > 0 ) {
				startingId = batch.get(batch.size() - 1).getId();
			}
		}
		then(results).containsExactlyElementsOf(alerts);
	}

	@Test
	public void findAlertsToProcessBatchWithDisabled() {
		List<UserAlert> alerts = new ArrayList<UserAlert>(12);
		for ( int i = 0; i < 24; i++ ) {
			storeNew();
			if ( (i % 2) == 0 ) {
				this.userAlert.setStatus(UserAlertStatus.Disabled);
				userAlertDao.save(this.userAlert);
			} else {
				alerts.add(this.userAlert);
			}
		}
		List<UserAlert> results = new ArrayList<UserAlert>(12);
		Long startingId = null;
		final Instant batchTime = Instant.now();
		final Integer max = 5;
		for ( int i = 0; i < 4; i++ ) {
			List<UserAlert> batch = userAlertDao.findAlertsToProcess(UserAlertType.NodeStaleData,
					startingId, batchTime, max);
			then(batch).as("Results should not be null").isNotNull();
			if ( i < 3 ) {
				then(batch).as("Batch results available").isNotEmpty();
			} else {
				then(batch).as("No more batch results available").isEmpty();
			}
			results.addAll(batch);
			if ( batch.size() > 0 ) {
				startingId = batch.get(batch.size() - 1).getId();
			}
		}
		then(results).containsExactlyElementsOf(alerts);
	}

	@Test
	public void findAlertsForUserNoResults() {
		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		then(results).as("Results should never be null").isNotNull();
		then(results).as("No alerts available").isEmpty();
	}

	@Test
	public void findAlertsForUserNoMatch() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsForUser(Long.MIN_VALUE);
		then(results).as("Results should never be null").isNotNull();
		then(results).as("No alerts available").isEmpty();
	}

	@Test
	public void findAlertsForUserNoSituation() {
		storeNew();
		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		// @formatter:off
		then(results).as("Results should never be null")
			.isNotNull()
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(null, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void findAlertsForUserWithSituationResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.save(resolved));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		// @formatter:off
		then(results).as("Results should never be null")
			.isNotNull()
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(null, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void findAlertsForUserWithSituationActive() {
		storeNew();

		// create an active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(Instant.now());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.save(sit));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		// @formatter:off
		then(results).as("Results should never be null")
			.isNotNull()
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(sit, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void findAlertsForUserWithSituationActiveAndResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.save(resolved));

		// create an Active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(Instant.now());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.save(sit));

		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());
		// @formatter:off
		then(results).as("Results should never be null")
			.isNotNull()
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.returns(userAlert.getId(), from(UserAlert::getId))
			.returns(sit, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void findAlertsForUser_multiAlerts_someWithSituation() {
		// GIVEN
		final int alertCount = 3;
		final List<UserAlert> alerts = new ArrayList<>(alertCount);

		for ( int i = 0; i < alertCount; i++ ) {
			storeNew();
			alerts.add(this.userAlert);
		}

		// create an Active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(alerts.get(1));
		sit.setCreated(Instant.now());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.save(sit));

		// WHEN
		List<UserAlert> results = userAlertDao.findAlertsForUser(user.getId());

		// THEN
		// @formatter:off
		then(results)
			.as("Results returned for all alerts")
			.containsExactlyElementsOf(alerts)
			.element(1)
			.returns(sit, BDDAssertions.from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void getAlertWithSituationNoSituation() {
		storeNew();
		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		// @formatter:off
		then(found)
			.as("Alert available")
			.isEqualTo(userAlert)
			.returns(null, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void getAlertWithSituationSituationResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.save(resolved));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		// @formatter:off
		then(found)
			.as("Alert available")
			.isEqualTo(userAlert)
			.returns(null, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void getAlertWithSituationSituationActive() {
		storeNew();

		// create an active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(Instant.now());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.save(sit));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		// @formatter:off
		then(found)
			.as("Alert available")
			.isEqualTo(userAlert)
			.as("Situation associated")
			.returns(sit, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void getAlertWithSituationSituationActiveAndResolved() {
		storeNew();

		// create a Resolved situation
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(userAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Resolved);
		resolved.setId(userAlertSituationDao.save(resolved));

		// create an Active situation
		UserAlertSituation sit = new UserAlertSituation();
		sit.setAlert(userAlert);
		sit.setCreated(Instant.now());
		sit.setStatus(UserAlertSituationStatus.Active);
		sit.setId(userAlertSituationDao.save(sit));

		UserAlert found = userAlertDao.getAlertSituation(userAlert.getId());
		// @formatter:off
		then(found)
			.as("Alert available")
			.isEqualTo(userAlert)
			.as("Situation associated")
			.returns(sit, from(UserAlert::getSituation))
			;
		// @formatter:on
	}

	@Test
	public void findCountForUserWithSituationResolved() {
		findAlertsForUserWithSituationResolved();

		int count = userAlertDao.alertSituationCountForUser(user.getId());
		then(count).as("Active count").isZero();
	}

	@Test
	public void findCountForUserWithSituationActiveAndResolved() {
		findAlertsForUserWithSituationActiveAndResolved();

		int count = userAlertDao.alertSituationCountForUser(user.getId());
		then(count).as("Active count").isOne();
	}

	@Test
	public void findForActiveSituationsForUserResolved() {
		findAlertsForUserWithSituationResolved();

		List<UserAlert> results = userAlertDao.findActiveAlertSituationsForUser(user.getId());
		then(results).as("No active situations").isNotNull().isEmpty();
	}

	@Test
	public void findForActiveSituationsForUserActiveAndResolved() {
		findAlertsForUserWithSituationActiveAndResolved();

		List<UserAlert> results = userAlertDao.findActiveAlertSituationsForUser(user.getId());
		// @formatter:off
		then(results)
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.extracting(UserAlert::getSituation)
			.as("Situation associated")
			.isNotNull()
			.returns(UserAlertSituationStatus.Active, from(UserAlertSituation::getStatus))
			;
		// @formatter:on
	}

	@Test
	public void findForActiveSituationsForNodeResolved() {
		findAlertsForUserWithSituationResolved();

		// add 2nd node to verify filter working as expected
		setupTestNode(-200L);

		// create an alert for 2nd node
		UserAlert otherNodeAlert = new UserAlert();
		otherNodeAlert.setCreated(Instant.now());
		otherNodeAlert.setNodeId(-200L);
		otherNodeAlert.setUserId(user.getId());
		otherNodeAlert.setStatus(UserAlertStatus.Active);
		otherNodeAlert.setType(UserAlertType.NodeStaleData);
		otherNodeAlert.setValidTo(Instant.now());
		otherNodeAlert.setId(userAlertDao.save(otherNodeAlert));

		// create an Active situation, but for other node ID
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(otherNodeAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Active);
		resolved.setId(userAlertSituationDao.save(resolved));

		List<UserAlert> results = userAlertDao.findActiveAlertSituationsForNode(node.getId());
		then(results).as("No active situations").isNotNull().isEmpty();
	}

	@Test
	public void findForActiveSituationsForNodeActiveAndResolved() {
		findAlertsForUserWithSituationActiveAndResolved();

		// add 2nd node to verify filter working as expected
		setupTestNode(-200L);

		// create an alert for 2nd node
		UserAlert otherNodeAlert = new UserAlert();
		otherNodeAlert.setCreated(Instant.now());
		otherNodeAlert.setNodeId(-200L);
		otherNodeAlert.setUserId(user.getId());
		otherNodeAlert.setStatus(UserAlertStatus.Active);
		otherNodeAlert.setType(UserAlertType.NodeStaleData);
		otherNodeAlert.setValidTo(Instant.now());
		otherNodeAlert.setId(userAlertDao.save(otherNodeAlert));

		// create an Active situation, but for other node ID
		UserAlertSituation resolved = new UserAlertSituation();
		resolved.setAlert(otherNodeAlert);
		resolved.setCreated(Instant.now());
		resolved.setStatus(UserAlertSituationStatus.Active);
		resolved.setId(userAlertSituationDao.save(resolved));

		List<UserAlert> results = userAlertDao.findActiveAlertSituationsForNode(node.getId());
		// @formatter:off
		then(results)
			.as("Alert available")
			.hasSize(1)
			.element(0)
			.extracting(UserAlert::getSituation)
			.as("Situation associated")
			.isNotNull()
			.returns(UserAlertSituationStatus.Active, from(UserAlertSituation::getStatus))
			;
		// @formatter:on
	}
}
