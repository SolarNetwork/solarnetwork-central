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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.jobs.DefaultUserExportJobsService;

/**
 * Test cases for the {@link DefaultUserExportJobsService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultUserExportJobsServiceTests {

	private static final Long TEST_USER_ID = -1L;
	private static final AtomicLong ID_GENERATOR = new AtomicLong(-999L);

	private UserDatumExportConfigurationDao configurationDao;
	private UserExportTaskBiz taskBiz;

	private DefaultUserExportJobsService service;

	@Before
	public void setup() {
		configurationDao = EasyMock.createMock(UserDatumExportConfigurationDao.class);
		taskBiz = EasyMock.createMock(UserExportTaskBiz.class);

		service = new DefaultUserExportJobsService(configurationDao, taskBiz);
	}

	private void replayAll() {
		EasyMock.replay(configurationDao, taskBiz);
	}

	@After
	public void teardown() {
		EasyMock.verify(configurationDao, taskBiz);
	}

	@Test
	public void noConfigurationsFound() {
		// given
		Instant now = Instant.now();
		expect(configurationDao.findForExecution(now, ScheduleType.Hourly))
				.andReturn(Collections.emptyList());

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now, ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(0));
	}

	private UserDatumExportConfiguration createConfiguration(String tzId) {
		UserDatumExportConfiguration config = new UserDatumExportConfiguration();
		config.setId(ID_GENERATOR.decrementAndGet());
		config.setName("Config" + config.getId());
		config.setUserId(TEST_USER_ID);
		config.setTimeZoneId(tzId);
		return config;
	}

	@Test
	public void oneConfigurationFound() {
		// given
		UserDatumExportConfiguration config = createConfiguration("Pacific/Auckland");
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		ZonedDateTime minExportDate = ScheduleType.Hourly.previousExportDate(now);
		config.setMinimumExportDate(minExportDate.toInstant());
		expect(configurationDao.findForExecution(now.toInstant(), ScheduleType.Hourly))
				.andReturn(Collections.singletonList(config));

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		Capture<UserDatumExportConfiguration> configCaptor = new Capture<>(CaptureType.ALL);
		expect(taskBiz.submitDatumExportConfiguration(capture(configCaptor),
				eq(minExportDate.toInstant()))).andReturn(task);

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now.toInstant(), ScheduleType.Hourly);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", configCaptor.hasCaptured(), equalTo(true));
	}

	@Test
	public void oneConfigurationFoundDifferentTimeZone() {
		// given
		String tzId = (TimeZone.getDefault().getID().equals("Pacific/Auckland") ? "America/Los_Angeles"
				: "Pacific/Auckland");

		UserDatumExportConfiguration config = createConfiguration(tzId);
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		ZonedDateTime minExportDate = ScheduleType.Daily.previousExportDate(now);
		config.setMinimumExportDate(minExportDate.toInstant());
		expect(configurationDao.findForExecution(now.toInstant(), ScheduleType.Daily))
				.andReturn(Collections.singletonList(config));

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		Capture<UserDatumExportConfiguration> configCaptor = new Capture<>(CaptureType.ALL);
		expect(taskBiz.submitDatumExportConfiguration(capture(configCaptor),
				eq(minExportDate.toInstant()))).andReturn(task);

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now.toInstant(), ScheduleType.Daily);

		// then
		assertThat("Result", count, equalTo(1));
		assertThat("Task created", configCaptor.hasCaptured(), equalTo(true));
	}

	@Test
	public void oneConfigurationFoundMultipleExports() {
		// given
		UserDatumExportConfiguration config = createConfiguration("Pacific/Auckland");
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		ZonedDateTime minExportDate = ScheduleType.Hourly.exportDate(now);
		minExportDate = minExportDate.plus(-3, ScheduleType.Hourly.temporalUnit());
		config.setMinimumExportDate(minExportDate.toInstant());
		List<UserDatumExportConfiguration> configs = Arrays.asList(config);
		expect(configurationDao.findForExecution(now.toInstant(), ScheduleType.Hourly))
				.andReturn(configs);

		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime exportDate = minExportDate.plus(i, ScheduleType.Hourly.temporalUnit());
			UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
			expect(taskBiz.submitDatumExportConfiguration(config, exportDate.toInstant()))
					.andReturn(task);

		}

		// when
		replayAll();
		int count = service.createExportExecutionTasks(now.toInstant(), ScheduleType.Hourly);

		// then
		assertThat("Result count of configs found", count, is(equalTo(1)));
	}

	@Test
	public void backfill_hourly() {
		// GIVEN
		final int expectedHourCount = 26;
		UserDatumExportConfiguration config = createConfiguration("Pacific/Auckland");
		ZonedDateTime now = ZonedDateTime.now(config.zone());
		ZonedDateTime minExportDate = now.truncatedTo(ChronoUnit.HOURS).minusHours(expectedHourCount);
		config.setMinimumExportDate(minExportDate.toInstant());
		expect(configurationDao.findForExecution(now.toInstant(), ScheduleType.Hourly))
				.andReturn(Collections.singletonList(config));

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		for ( int i = 0; i < expectedHourCount; i++ ) {
			expect(taskBiz.submitDatumExportConfiguration(config,
					minExportDate.plusHours(i).toInstant())).andReturn(task);
		}

		// WHEN
		replayAll();
		int count = service.createExportExecutionTasks(now.toInstant(), ScheduleType.Hourly);

		// THEN
		assertThat("Result count of configs found", count, is(equalTo(1)));
	}

}
