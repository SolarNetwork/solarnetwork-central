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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
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
	private UserDao userDao;
	private UserExportTaskBiz taskBiz;

	private DefaultUserExportJobsService service;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		userDao = EasyMock.createMock(UserDao.class);
		taskBiz = EasyMock.createMock(UserExportTaskBiz.class);

		service = new DefaultUserExportJobsService(configurationDao, userDao, taskBiz);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, userDao, taskBiz);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, userDao, taskBiz);
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

	private User createUser(String tzId) {
		User user = new User(TEST_USER_ID, "test@localhost");
		SolarLocation loc = new SolarLocation();
		loc.setTimeZoneId(tzId);
		user.setLocation(loc);
		return user;
	}

	@Test
	public void oneConfigurationFound() {
		// given
		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		DateTime minExportDate = ScheduleType.Hourly.previousExportDate(now);
		config.setMinimumExportDate(minExportDate);
		expect(configurationDao.findForExecution(now, ScheduleType.Hourly))
				.andReturn(Collections.singletonList(config));

		User user = createUser("Pacific/Auckland");
		expect(userDao.get(TEST_USER_ID)).andReturn(user);

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		Capture<UserDatumExportConfiguration> configCaptor = new Capture<>(CaptureType.ALL);
		expect(taskBiz.submitDatumExportConfiguration(capture(configCaptor), eq(minExportDate)))
				.andReturn(task);

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", configCaptor.hasCaptured(), equalTo(true));
	}

	@Test
	public void oneConfigurationFoundDifferentTimeZone() {
		// given
		User user = createUser(
				TimeZone.getDefault().getID().equals("Pacific/Auckland") ? "America/Los_Angeles"
						: "Pacific/Auckland");

		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		DateTime minExportDate = ScheduleType.Daily
				.previousExportDate(now.withZone(DateTimeZone.forTimeZone(user.getTimeZone())));
		config.setMinimumExportDate(minExportDate);
		expect(configurationDao.findForExecution(now, ScheduleType.Daily))
				.andReturn(Collections.singletonList(config));

		expect(userDao.get(TEST_USER_ID)).andReturn(user);

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		Capture<UserDatumExportConfiguration> configCaptor = new Capture<>(CaptureType.ALL);
		expect(taskBiz.submitDatumExportConfiguration(capture(configCaptor), eq(minExportDate)))
				.andReturn(task);

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Daily);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", configCaptor.hasCaptured(), equalTo(true));
	}

	@Test
	public void oneConfigurationFoundMultipleExports() {
		// given
		DateTime now = new DateTime();
		UserDatumExportConfiguration config = createConfiguration();
		DateTime minExportDate = ScheduleType.Hourly.exportDate(now);
		minExportDate = minExportDate.withFieldAdded(ScheduleType.Hourly.durationFieldType(), -3);
		config.setMinimumExportDate(minExportDate);
		List<UserDatumExportConfiguration> configs = Arrays.asList(config);
		expect(configurationDao.findForExecution(now, ScheduleType.Hourly)).andReturn(configs);

		User user = createUser("Pacific/Auckland");
		expect(userDao.get(TEST_USER_ID)).andReturn(user);

		Capture<UserDatumExportConfiguration> configCaptor = new Capture<>(CaptureType.ALL);
		for ( int i = 0; i < 3; i++ ) {
			DateTime exportDate = minExportDate.withFieldAdded(ScheduleType.Hourly.durationFieldType(),
					i);
			UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
			expect(taskBiz.submitDatumExportConfiguration(capture(configCaptor), eq(exportDate)))
					.andReturn(task);

		}

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", configCaptor.hasCaptured(), equalTo(true));
		assertThat("Multiple tasks created for exports", configCaptor.getValues().size(), equalTo(3));
	}

}
