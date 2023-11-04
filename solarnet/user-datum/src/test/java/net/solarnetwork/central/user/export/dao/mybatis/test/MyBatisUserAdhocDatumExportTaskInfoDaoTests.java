/* ==================================================================
 * MyBatisUserAdhocDatumExportTaskInfoDaoTests.java - 18/04/2018 9:43:43 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.dao.mybatis.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mybatis.spring.MyBatisSystemException;
import net.solarnetwork.central.datum.export.dao.mybatis.MyBatisDatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * Test cases for the {@link MyBatisUserAdhocDatumExportTaskInfoDao} class.
 * 
 * @author matt
 * @version 2.1
 * @since 1.1
 */
public class MyBatisUserAdhocDatumExportTaskInfoDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisUserDatumExportConfigurationDao confDao;
	private MyBatisDatumExportTaskInfoDao datumTaskDao;
	private MyBatisUserAdhocDatumExportTaskInfoDao dao;

	private User user;
	private UserAdhocDatumExportTaskInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserAdhocDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		confDao = new MyBatisUserDatumExportConfigurationDao();
		confDao.setSqlSessionFactory(getSqlSessionFactory());

		datumTaskDao = new MyBatisDatumExportTaskInfoDao();
		datumTaskDao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user", this.user, notNullValue());

		info = null;
	}

	private UserDatumExportConfiguration createNewUserDatumExportConfig() {
		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Weekly);
		return conf;
	}

	private UUID storeTask(Long userId) {
		UserAdhocDatumExportTaskInfo info = new UserAdhocDatumExportTaskInfo();
		info.setUserId(this.user.getId());
		info.setConfig(createNewUserDatumExportConfig());
		return dao.store(info);
	}

	@Test
	public void storeNew() {
		UserAdhocDatumExportTaskInfo info = new UserAdhocDatumExportTaskInfo();
		info.setConfig(createNewUserDatumExportConfig());
		info.setUserId(this.user.getId());

		UUID id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void storeNew_withToken() {
		// GIVEN
		UserAdhocDatumExportTaskInfo info = new UserAdhocDatumExportTaskInfo();
		info.setConfig(createNewUserDatumExportConfig());
		info.setUserId(this.user.getId());
		info.setTokenId(randomString());

		// WHEN
		UUID id = dao.store(info);

		// THEN
		then(id).as("Primary key assigned").isNotNull();

		List<Map<String, Object>> userTaskRows = this.jdbcTemplate
				.queryForList("select * from solaruser.user_adhoc_export_task");
		// @formatter:off
		then(userTaskRows)
				.as("User export task row created")
				.hasSize(1)
				.element(0, map(String.class, Object.class))
				.as("Datum task_id populated")
				.containsKey("task_id")
				.as("Token saved")
				.containsEntry("auth_token", info.getTokenId())
				;

		List<Map<String, Object>> exportTaskRows = this.jdbcTemplate
				.queryForList("select * from solarnet.sn_datum_export_task");
		then(exportTaskRows)
				.as("Datum export task row created")
				.hasSize(1)
				.element(0, map(String.class, Object.class))
				.as("ID same as in user export task_id")
				.containsEntry("id", userTaskRows.get(0).get("task_id"))
				;

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserAdhocDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.user.getId()));
		assertThat("Schedule type", info.getScheduleType(), equalTo(ScheduleType.Adhoc));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Modified date not used", info.getModified(), nullValue());

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey_withToken() {
		// GIVEN
		storeNew_withToken();

		// WHEN
		UserAdhocDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());

		// THEN
		// @formatter:off
		then(info)
			.as("Found by PK")
			.isNotNull()
			.as("Primary key")
			.returns(this.info.getId(), UserAdhocDatumExportTaskInfo::getId)
			.as("User ID saved")
			.returns(this.info.getUserId(), UserAdhocDatumExportTaskInfo::getUserId)
			.as("Schedule type saved")
			.returns(ScheduleType.Adhoc, UserAdhocDatumExportTaskInfo::getScheduleType)
			.as("Token saved")
			.returns(this.info.getTokenId(), UserAdhocDatumExportTaskInfo::getTokenId)
			;

		// stash results for other tests to use
		this.info = info;
	}

	@Test(expected = MyBatisSystemException.class)
	public void update() {
		storeNew();

		// updates are actually NOT allowed; we will get back the same task ID and 
		// no properties will change when we check

		UserAdhocDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());
		info.setCreated(Instant.now());
		((BasicConfiguration) info.getConfig()).setHourDelayOffset(1);

		dao.store(info);
	}

	@Test
	public void purgeCompletedNoneCompleted() {
		getByPrimaryKey();
		long result = dao.purgeCompletedTasks(Instant.now());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompletedNoneExpired() {
		getByPrimaryKey();

		long result = dao.purgeCompletedTasks(
				Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompleted() {
		getByPrimaryKey();
		DatumExportTaskInfo datumTask = datumTaskDao.get(this.info.getId());
		datumTask.setStatus(DatumExportState.Completed);
		datumTask.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		datumTaskDao.store(datumTask);

		UserAdhocDatumExportTaskInfo info = new UserAdhocDatumExportTaskInfo();
		info.setUserId(this.info.getUserId());
		info.setConfig(createNewUserDatumExportConfig());
		info = dao.get(dao.store(info), this.user.getId());
		datumTask = datumTaskDao.get(info.getId());
		datumTask.setStatus(DatumExportState.Completed);
		datumTask.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		datumTaskDao.store(datumTask);

		long result = dao.purgeCompletedTasks(
				Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(2L));
	}

	@Test
	public void findForUserNone() {
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), null, null);
		assertThat("Empty result", tasks, hasSize(0));
	}

	@Test
	public void findForUserAndStatesNone() {
		Set<DatumExportState> states = EnumSet.of(DatumExportState.Completed);
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), states, null);
		assertThat("Empty result", tasks, hasSize(0));
	}

	@Test
	public void findForUserAndStatesAndSuccessNone() {
		Set<DatumExportState> states = EnumSet.of(DatumExportState.Completed);
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), states, false);
		assertThat("Empty result", tasks, hasSize(0));
	}

	@Test
	public void findForUser() {
		List<UUID> pks = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			pks.add(storeTask(this.user.getId()));
		}
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), null, null);
		assertThat("Result count", tasks, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			UserAdhocDatumExportTaskInfo info = tasks.get(i);
			assertThat("Result PK " + i, info.getId(), equalTo(pks.get(i)));
		}
	}

	@Test
	public void findForUserAndStates() {
		List<UUID> pks = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			pks.add(storeTask(this.user.getId()));
			if ( i == 1 ) {
				// update first two tasks to 'complete' state
				jdbcTemplate.update("update solarnet.sn_datum_export_task set status = 'c'");
			}
		}
		Set<DatumExportState> states = EnumSet.of(DatumExportState.Completed);
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), states, null);
		assertThat("Result count", tasks, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			UserAdhocDatumExportTaskInfo info = tasks.get(i);
			assertThat("Result PK " + i, info.getId(), equalTo(pks.get(i)));
		}
	}

	@Test
	public void findForUserAndStatesAndSuccess() {
		List<UUID> pks = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			pks.add(storeTask(this.user.getId()));
			if ( i == 0 ) {
				// update first task to 'failed' success
				jdbcTemplate.update("update solarnet.sn_datum_export_task set success = FALSE");
			} else if ( i == 1 ) {
				// update first two tasks to 'complete' state
				jdbcTemplate.update("update solarnet.sn_datum_export_task set status = 'c'");

				// update second task to 'ok' success
				jdbcTemplate.update("update solarnet.sn_datum_export_task set success = TRUE "
						+ "where id = ?::uuid", pks.get(i));
			}
		}
		Set<DatumExportState> states = EnumSet.of(DatumExportState.Completed);
		List<UserAdhocDatumExportTaskInfo> tasks = dao.findTasksForUser(user.getId(), states, false);
		assertThat("Result count", tasks, hasSize(1));
		UserAdhocDatumExportTaskInfo info = tasks.get(0);
		assertThat("Failed result PK", info.getId(), equalTo(pks.get(0)));

		tasks = dao.findTasksForUser(user.getId(), states, true);
		assertThat("Result count", tasks, hasSize(1));
		info = tasks.get(0);
		assertThat("Ok result PK", info.getId(), equalTo(pks.get(1)));
	}
}
