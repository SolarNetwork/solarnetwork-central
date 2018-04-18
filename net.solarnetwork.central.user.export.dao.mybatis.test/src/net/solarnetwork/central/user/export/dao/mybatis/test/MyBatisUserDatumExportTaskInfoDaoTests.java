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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.dao.mybatis.MyBatisUserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * Test cases for the {@link MyBatisUserDatumExportTaskInfoDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisUserDatumExportTaskInfoDaoTests extends AbstractMyBatisUserDaoTestSupport {

	private MyBatisUserDatumExportTaskInfoDao dao;

	private User user;
	private UserDatumExportTaskInfo info;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisUserDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		this.user = createNewUser(TEST_EMAIL);
		assertNotNull(this.user);
		info = null;
	}

	@Test
	public void storeNew() {
		DateTime date = new DateTime(2017, 4, 18, 9, 0, 0, DateTimeZone.UTC);
		UserDatumExportTaskInfo info = new UserDatumExportTaskInfo();
		info.setId(new UserDatumExportTaskPK(this.user.getId(), ScheduleType.Hourly, date));
		info.setTaskId(UUID.randomUUID());
		info.setConfigJson("{}");

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
		assertThat("Task ID", info.getTaskId(), nullValue());
	}

	@Test
	public void update() {
		storeNew();

		// updates are actually NOT allowed; we will get back the same task ID and 
		// no properties will change when we check

		UserDatumExportTaskInfo info = dao.get(this.info.getId(), this.user.getId());
		DateTime originalCreated = info.getCreated();
		info.setCreated(new DateTime());
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
}
