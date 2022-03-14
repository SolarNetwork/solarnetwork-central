/* ==================================================================
 * MyBatisUserDatumExportTaskInfoDaoTests.java - 18/04/2018 9:43:43 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.dao.mybatis.MyBatisDatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * Test cases for the {@link MyBatisUserDatumExportTaskInfoDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MyBatisUserDatumExportTaskInfoDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisUserDatumExportConfigurationDao confDao;
	private MyBatisDatumExportTaskInfoDao datumTaskDao;
	private MyBatisUserDatumExportTaskInfoDao dao;

	private User user;
	private UserDatumExportConfiguration userDatumExportConfig;
	private UserDatumExportTaskInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		confDao = new MyBatisUserDatumExportConfigurationDao();
		confDao.setSqlSessionFactory(getSqlSessionFactory());

		datumTaskDao = new MyBatisDatumExportTaskInfoDao();
		datumTaskDao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertThat("Test user", this.user, notNullValue());

		this.userDatumExportConfig = createNewUserDatumExportConfig();
		assertThat("Test user datum export config", this.userDatumExportConfig, notNullValue());

		info = null;
	}

	private UserDatumExportConfiguration createNewUserDatumExportConfig() {
		UserDatumExportConfiguration conf = new UserDatumExportConfiguration();
		conf.setCreated(Instant.now());
		conf.setUserId(this.user.getId());
		conf.setName(TEST_NAME);
		conf.setHourDelayOffset(2);
		conf.setSchedule(ScheduleType.Weekly);

		Long id = confDao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());

		conf.setId(id);
		return conf;
	}

	@Test
	public void storeNew() {
		Instant date = LocalDateTime.of(2017, 4, 18, 9, 0, 0).toInstant(ZoneOffset.UTC);
		UserDatumExportTaskInfo info = new UserDatumExportTaskInfo();
		info.setId(new UserDatumExportTaskPK(this.user.getId(), ScheduleType.Hourly, date));
		info.setConfig(this.userDatumExportConfig);

		UserDatumExportTaskPK id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		info.setId(id);
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		UserDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("User ID", info.getUserId(), equalTo(this.user.getId()));
		assertThat("Schedule type", info.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Export date", info.getExportDate(), sameInstance(info.getId().getDate()));
		assertThat("Modified date not used", info.getModified(), nullValue());
		assertThat("Task ID", info.getTaskId(), notNullValue());
		assertThat("Config ID", info.getUserDatumExportConfigurationId(),
				equalTo(this.userDatumExportConfig.getId()));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void update() {
		storeNew();

		// updates are actually NOT allowed; we will get back the same task ID and 
		// no properties will change when we check

		UserDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());
		Instant originalCreated = info.getCreated();
		info.setCreated(Instant.now());
		((BasicConfiguration) info.getConfig()).setHourDelayOffset(1);

		UserDatumExportTaskPK id = dao.store(info);
		assertThat("PK unchanged", id, equalTo(this.info.getId()));

		UserDatumExportTaskInfo updatedConf = dao.get(id, this.user.getId());
		assertThat("Found by PK", updatedConf, notNullValue());
		assertThat("New entity returned", updatedConf, not(sameInstance(this.info)));
		assertThat("PK", updatedConf.getId(), equalTo(this.info.getId()));
		assertThat("Created unchanged", updatedConf.getCreated(), equalTo(originalCreated));
		assertThat("Config unchanged", updatedConf.getConfig().getHourDelayOffset(),
				equalTo(this.info.getConfig().getHourDelayOffset()));
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
		DatumExportTaskInfo datumTask = datumTaskDao.get(this.info.getTaskId());
		datumTask.setStatus(DatumExportState.Completed);
		datumTask.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		datumTaskDao.store(datumTask);

		UserDatumExportTaskInfo info = new UserDatumExportTaskInfo();
		info.setId(new UserDatumExportTaskPK(this.user.getId(), ScheduleType.Hourly,
				LocalDateTime.of(2017, 4, 18, 10, 0, 0).toInstant(ZoneOffset.UTC)));
		info.setConfig(this.userDatumExportConfig);
		info = dao.get(dao.store(info), this.user.getId());
		datumTask = datumTaskDao.get(info.getTaskId());
		datumTask.setStatus(DatumExportState.Completed);
		datumTask.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		datumTaskDao.store(datumTask);

		long result = dao.purgeCompletedTasks(
				Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(2L));
	}

	@Test
	public void delete() {
		// GIVEN
		storeNew();

		// WHEN
		dao.delete(this.info);

		List<Map<String, Object>> rows = jdbcTemplate
				.queryForList("select * from solaruser.user_export_task");
		assertThat("Task row deleted", rows, hasSize(0));
	}

	@Test
	public void deleteConfCascadeToTask() {
		// GIVEN
		storeNew();

		// WHEN
		confDao.delete(this.userDatumExportConfig);

		List<Map<String, Object>> rows = jdbcTemplate
				.queryForList("select * from solaruser.user_export_task");
		assertThat("Task row deleted with conf", rows, hasSize(0));
	}

}
