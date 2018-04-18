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
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.test.CallingThreadExecutorService;
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

	private EventAdmin eventAdmin;
	private UserExportJobsService jobsService;

	private UserExportTaskPopulatorJob job;

	private UserExportTaskPopulatorJob jobForSchedule(ScheduleType type) {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(eventAdmin, type, jobsService);
		job.setJobId(JOB_ID);
		job.setExecutorService(new CallingThreadExecutorService());
		return job;
	}

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		jobsService = EasyMock.createMock(UserExportJobsService.class);

		job = jobForSchedule(ScheduleType.Hourly);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, jobsService);
	}

	@After
	public void teardown() {
		DateTimeUtils.setCurrentMillisSystem();
		EasyMock.verify(eventAdmin, jobsService);
	}

	@Test
	public void executeHourlyJob() {
		// given
		DateTime now = new DateTime();
		DateTimeUtils.setCurrentMillisFixed(now.getMillis());
		expect(jobsService.createExportExecutionTasks(now, ScheduleType.Hourly)).andReturn(0);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		// when
		replayAll();
		Map<String, Object> jobProps = new HashMap<>();
		jobProps.put(SchedulerConstants.JOB_ID, JOB_ID);
		Event event = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, jobProps);
		job.handleEvent(event);

		// then
		assertThat("Complete event posted", eventCaptor.hasCaptured(), equalTo(true));
		Event completedEvent = eventCaptor.getValue();
		assertThat(completedEvent.getTopic(), equalTo(SchedulerConstants.TOPIC_JOB_COMPLETE));
		assertThat(completedEvent.getProperty(SchedulerConstants.JOB_ID), equalTo(JOB_ID));
	}

	@Test
	public void executeDailyJob() {
		// given
		job = jobForSchedule(ScheduleType.Daily);
		DateTime now = new DateTime();
		DateTimeUtils.setCurrentMillisFixed(now.getMillis());
		expect(jobsService.createExportExecutionTasks(now, ScheduleType.Daily)).andReturn(0);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// when
		replayAll();
		Map<String, Object> jobProps = new HashMap<>();
		jobProps.put(SchedulerConstants.JOB_ID, JOB_ID);
		Event event = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, jobProps);
		job.handleEvent(event);

		// then
		assertThat("Complete event posted", eventCaptor.hasCaptured(), equalTo(true));
		Event completedEvent = eventCaptor.getValue();
		assertThat(completedEvent.getTopic(), equalTo(SchedulerConstants.TOPIC_JOB_COMPLETE));
		assertThat(completedEvent.getProperty(SchedulerConstants.JOB_ID), equalTo(JOB_ID));
	}

}
