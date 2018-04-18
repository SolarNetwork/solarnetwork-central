/* ==================================================================
 * DefaultUserExportJobsServiceTests.java - 19/04/2018 7:05:17 AM
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

package net.solarnetwork.central.user.export.jobs.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;
import net.solarnetwork.central.user.export.jobs.DefaultUserExportJobsService;

/**
 * Test cases for the {@link DefaultUserExportJobsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultUserExportJobsServiceTests {

	private static final Long TEST_USER_ID = -1L;
	private static final AtomicLong ID_GENERATOR = new AtomicLong(-999L);

	private UserDatumExportConfigurationDao configurationDao;
	private UserDatumExportTaskInfoDao taskDao;

	private DefaultUserExportJobsService service;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		taskDao = EasyMock.createMock(UserDatumExportTaskInfoDao.class);

		service = new DefaultUserExportJobsService(configurationDao, taskDao);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, taskDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, taskDao);
	}

	@Test
	public void noConfigurationsFound() {
		// given
		DateTime now = new DateTime();
		expect(configurationDao.findForExecution(now, ScheduleType.Hourly))
				.andReturn(Collections.emptyList());

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(0));
	}

	private UserDatumExportConfiguration createConfiguration() {
		UserDatumExportConfiguration config = new UserDatumExportConfiguration();
		config.setId(ID_GENERATOR.decrementAndGet());
		config.setName("Config" + config.getId());
		config.setUserId(TEST_USER_ID);
		return config;
	}

	@Test
	public void oneConfigurationFound() {
		// given
		DateTime now = new DateTime();
		List<UserDatumExportConfiguration> configs = Arrays.asList(createConfiguration());
		expect(configurationDao.findForExecution(now, ScheduleType.Hourly)).andReturn(configs);

		Capture<UserDatumExportTaskInfo> taskCaptor = new Capture<>();

		DateTime exportDate = ScheduleType.Hourly.exportDate(now);
		expect(taskDao.store(capture(taskCaptor)))
				.andReturn(new UserDatumExportTaskPK(TEST_USER_ID, ScheduleType.Hourly, exportDate));

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", taskCaptor.hasCaptured(), equalTo(true));

		UserDatumExportTaskInfo task = taskCaptor.getValue();
		assertThat("Task user ID", task.getUserId(), equalTo(TEST_USER_ID));
		assertThat("Task schedule", task.getScheduleType(), equalTo(ScheduleType.Hourly));
		assertThat("Task date", task.getExportDate(), equalTo(exportDate));
		assertThat("Task config available", task.getConfig(), notNullValue());
		assertThat("Config name", task.getConfig().getName(), equalTo(configs.get(0).getName()));

	}

}
