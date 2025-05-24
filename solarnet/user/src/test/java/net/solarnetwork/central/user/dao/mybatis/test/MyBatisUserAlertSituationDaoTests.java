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

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Test cases for the {@link MyBatisUserAlertSituationDao} class.
 * 
 * @author matt
 * @version 2.1
 */
public class MyBatisUserAlertSituationDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisSolarNodeDao solarNodeDao;
	private MyBatisUserAlertDao userAlertDao;

	private MyBatisUserAlertSituationDao userAlertSituationDao;

	private User user = null;
	private SolarNode node = null;
	private UserAlert userAlert = null;
	private UserAlertSituation userAlertSituation = null;

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
		userAlertSituation = null;
	}

	private UserAlert createUserAlert() {
		UserAlert alert = new UserAlert();
		alert.setCreated(Instant.now());
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

		Long id = userAlertDao.save(alert);
		then(id).isNotNull();
		alert.setId(id);
		this.userAlert = alert;
		return alert;
	}

	private UserAlertSituation storeNew(UserAlert alert, UserAlertSituationStatus status,
			Instant notified) {
		UserAlertSituation sit = new UserAlertSituation();
		sit.setCreated(Instant.now());
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

		Long id = userAlertSituationDao.save(sit);
		sit.setId(id);
		return sit;
	}

	@Test
	public void storeNew() {
		UserAlertSituation sit = storeNew(createUserAlert(), UserAlertSituationStatus.Active, null);
		then(sit.getId()).as("Primary key should be assigned").isNotNull();
		this.userAlertSituation = sit;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao.get(this.userAlertSituation.getId());
		then(sit).isNotNull();
		then(this.userAlertSituation.getId()).isEqualTo(sit.getId());
		then(this.userAlertSituation.getStatus()).isEqualTo(sit.getStatus());
		then(this.userAlertSituation.getInfo()).isEqualTo(sit.getInfo());
		then(sit.getAlert()).isNotNull();
		then(sit.getAlert().getId()).isEqualTo(this.userAlert.getId());
	}

	@Test
	public void update() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao.get(this.userAlertSituation.getId());

		Map<String, Object> info = sit.getInfo();
		info.put("updated-string", "updated");
		sit.setInfo(info);

		Instant created = userAlertSituation.getCreated();
		sit.setStatus(UserAlertSituationStatus.Resolved);
		sit.setNotified(created.plus(1, ChronoUnit.HOURS));
		Long id = userAlertSituationDao.save(sit);
		then(id).isNotNull().isEqualTo(this.userAlertSituation.getId());
		UserAlertSituation updatedSit = userAlertSituationDao.get(id);
		// @formatter:off
		then(updatedSit)
			.isNotNull()
			.returns(created, from(UserAlertSituation::getCreated))
			.returns(UserAlertSituationStatus.Resolved, from(UserAlertSituation::getStatus))
			.isEqualTo(userAlertSituation)
			.returns(userAlert, from(UserAlertSituation::getAlert))
			;

		then(updatedSit.getInfo())
			.containsEntry("string", "foo")
			.containsEntry("number", 42)
			.containsEntry("updated-string", "updated")
			.containsEntry("list", List.of("first", "second"))
			;
		// @formatter:on
	}

	@Test
	public void getByActiveAlertNoAlert() {
		UserAlertSituation sit = userAlertSituationDao.getActiveAlertSituationForAlert(123L);
		then(sit).isNull();
	}

	@Test
	public void getByActiveAlertAlert() {
		storeNew();
		UserAlertSituation sit = userAlertSituationDao
				.getActiveAlertSituationForAlert(this.userAlert.getId());
		then(sit).isNotNull().isEqualTo(userAlertSituation);
	}

	@Test
	public void getByActiveAlertAlertResolved() {
		storeNew();
		this.userAlertSituation.setStatus(UserAlertSituationStatus.Resolved);
		userAlertSituationDao.save(this.userAlertSituation);
		UserAlertSituation sit = userAlertSituationDao
				.getActiveAlertSituationForAlert(this.userAlert.getId());
		then(sit).isNull();
	}

	@Test
	public void getByActiveAlertAlertAndResolved() {
		storeNew();
		this.userAlertSituation.setStatus(UserAlertSituationStatus.Resolved);
		userAlertSituationDao.save(this.userAlertSituation);

		// store another now, as Active
		storeNew();

		UserAlertSituation sit = userAlertSituationDao
				.getActiveAlertSituationForAlert(this.userAlert.getId());
		then(sit).isNotNull().isEqualTo(userAlertSituation);
	}

	@Test
	public void purgeCompletedInstructionsNone() {
		long result = userAlertSituationDao.purgeResolvedSituations(Instant.now());
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsNoMatchActive() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Active, Instant.now());
		long result = userAlertSituationDao
				.purgeResolvedSituations(userAlertSituation.getCreated().plus(1, ChronoUnit.DAYS));
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsNoMatchDateMissing() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved, null);
		long result = userAlertSituationDao
				.purgeResolvedSituations(userAlertSituation.getCreated().minus(1, ChronoUnit.DAYS));
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsNoMatchDateMismatch() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved,
				Instant.now());
		long result = userAlertSituationDao
				.purgeResolvedSituations(userAlertSituation.getCreated().minus(1, ChronoUnit.DAYS));
		then(result).isZero();
	}

	@Test
	public void purgeCompletedInstructionsMatch() {
		userAlertSituation = storeNew(createUserAlert(), UserAlertSituationStatus.Resolved,
				Instant.now());
		long result = userAlertSituationDao
				.purgeResolvedSituations(userAlertSituation.getCreated().plus(1, ChronoUnit.DAYS));
		then(result).isOne();
		UserAlertSituation sit = userAlertSituationDao.get(userAlertSituation.getId());
		then(sit).as("Purged situation is not found").isNull();
	}

	@Test
	public void purgeCompletedInstructionsMatchMultiple() {
		List<UserAlertSituation> toPurge = new ArrayList<UserAlertSituation>();
		List<UserAlertSituation> notToPurge = new ArrayList<UserAlertSituation>();
		toPurge.add(storeNew(createUserAlert(), UserAlertSituationStatus.Resolved, Instant.now()));
		notToPurge.add(storeNew(userAlert, UserAlertSituationStatus.Active, Instant.now())); // Active state, should NOT be deleted
		toPurge.add(storeNew(userAlert, UserAlertSituationStatus.Resolved, Instant.now()));
		long result = userAlertSituationDao
				.purgeResolvedSituations(Instant.now().plus(1, ChronoUnit.DAYS));
		then(result).as("Purged resolved").isEqualTo(toPurge.size());
		for ( UserAlertSituation sit : notToPurge ) {
			UserAlertSituation match = userAlertSituationDao.get(sit.getId());
			then(match).as("Should not be purged").isNotNull();
		}
		for ( UserAlertSituation sit : toPurge ) {
			UserAlertSituation match = userAlertSituationDao.get(sit.getId());
			then(match).as("Purged situation is not found").isNull();
		}
	}

}
