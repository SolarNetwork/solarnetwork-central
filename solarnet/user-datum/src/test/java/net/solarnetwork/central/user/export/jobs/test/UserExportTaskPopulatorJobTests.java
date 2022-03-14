/* ==================================================================
 * UserExportTaskPopulatorJobTests.java - 19/04/2018 6:25:11 AM
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.export.jobs.UserExportJobsService;
import net.solarnetwork.central.user.export.jobs.UserExportTaskPopulatorJob;

/**
 * Test cases for the [@link UserExportTaskPopulatorJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserExportTaskPopulatorJobTests {

	private static final String JOB_ID = "test.job";

	private UserExportJobsService jobsService;

	private UserExportTaskPopulatorJob job;

	private UserExportTaskPopulatorJob jobForSchedule(ScheduleType type) {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(type, jobsService);
		job.setId(JOB_ID);
		return job;
	}

	@Before
	public void setup() {
		jobsService = EasyMock.createMock(UserExportJobsService.class);

		job = jobForSchedule(ScheduleType.Hourly);
	}

	private void replayAll() {
		EasyMock.replay(jobsService);
	}

	@After
	public void teardown() {
		EasyMock.verify(jobsService);
	}

	@Test
	public void executeHourlyJob() {
		// given
		Capture<Instant> dateCaptor = new Capture<>();
		expect(jobsService.createExportExecutionTasks(capture(dateCaptor), eq(ScheduleType.Hourly)))
				.andReturn(0);

		// when
		replayAll();
		Instant now = Instant.now();
		job.run();

		// then
		assertThat("Date minutes OK", (int) ChronoUnit.MINUTES.between(dateCaptor.getValue(), now),
				equalTo(0));
	}

	@Test
	public void executeDailyJob() {
		// given
		job = jobForSchedule(ScheduleType.Daily);
		Capture<Instant> dateCaptor = new Capture<>();
		expect(jobsService.createExportExecutionTasks(capture(dateCaptor), eq(ScheduleType.Daily)))
				.andReturn(0);

		// when
		replayAll();
		Instant now = Instant.now();
		job.run();

		// then
		assertThat("Date minutes OK", (int) ChronoUnit.MINUTES.between(dateCaptor.getValue(), now),
				equalTo(0));
	}

}
