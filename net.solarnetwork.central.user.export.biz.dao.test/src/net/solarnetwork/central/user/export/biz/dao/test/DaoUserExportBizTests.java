/* ==================================================================
 * DaoUserExportBizTests.java - 24/04/2018 10:41:30 AM
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

package net.solarnetwork.central.user.export.biz.dao.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.dao.DaoUserExportBiz;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;

/**
 * Test cases for the {@link UserExportBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserExportBizTests {

	private static final Long TEST_USER_ID = -1L;
	private static final Long TEST_NODE_ID = -2L;
	private static final AtomicLong ID_GENERATOR = new AtomicLong(-999L);

	private UserDatumExportConfigurationDao configurationDao;
	private UserDataConfigurationDao dataConfigurationDao;
	private UserDestinationConfigurationDao destConfigurationDao;
	private UserOutputConfigurationDao outputConfigurationDao;
	private UserDatumExportTaskInfoDao taskDao;
	private UserNodeDao userNodeDao;

	private DaoUserExportBiz biz;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		dataConfigurationDao = EasyMock.createMock(UserDataConfigurationDao.class);
		destConfigurationDao = EasyMock.createMock(UserDestinationConfigurationDao.class);
		outputConfigurationDao = EasyMock.createMock(UserOutputConfigurationDao.class);
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);

		biz = new DaoUserExportBiz(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, dataConfigurationDao, destConfigurationDao,
				outputConfigurationDao, taskDao, userNodeDao);
	}

	private UserDatumExportConfiguration createConfiguration() {
		UserDatumExportConfiguration config = new UserDatumExportConfiguration();
		config.setId(ID_GENERATOR.decrementAndGet());
		config.setName("Config" + config.getId());
		config.setUserId(TEST_USER_ID);
		config.setSchedule(ScheduleType.Hourly);
		return config;
	}

	@Test
	public void submitTask() {
		// given
		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		expect(userNodeDao.findNodeIdsForUser(TEST_USER_ID))
				.andReturn(Collections.singleton(TEST_NODE_ID));

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		DateTime exportDate = ScheduleType.Hourly.exportDate(now);
		expect(taskDao.store(capture(taskCaptor)))
				.andReturn(new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate));

		// when
		replayAll();
		UserDatumExportTaskInfo task = biz.submitDatumExportConfiguration(config, exportDate);

		// then
		assertThat("Task created", task, notNullValue());

		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(config.getName()));
		assertThat("Node ID populated",
				task.getConfig().getDataConfiguration().getDatumFilter().getNodeIds(),
				arrayContaining(TEST_NODE_ID));
	}

}
