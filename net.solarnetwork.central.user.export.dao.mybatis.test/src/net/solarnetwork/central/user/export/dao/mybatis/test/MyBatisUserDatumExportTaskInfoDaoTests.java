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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
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
		UserDatumExportTaskInfo conf = new UserDatumExportTaskInfo();
		conf.setId(new UserDatumExportTaskPK(this.user.getId(), ScheduleType.Hourly, date));
		conf.setCreated(new DateTime());
		conf.setTaskId(UUID.randomUUID());
		conf.setConfigJson("{}");

		UserDatumExportTaskPK id = dao.store(conf);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(conf.getId()));

		// stash results for other tests to use
		conf.setId(id);
		this.info = conf;
	}

}
