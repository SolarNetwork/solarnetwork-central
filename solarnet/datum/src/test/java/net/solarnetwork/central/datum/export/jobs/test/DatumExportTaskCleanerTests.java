/* ==================================================================
 * DatumExportTaskCleanerTests.java - 28/04/2018 3:50:53 PM
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

package net.solarnetwork.central.datum.export.jobs.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.jobs.DatumExportTaskCleanerJob;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link DatumExportTaskCleanerJob} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumExportTaskCleanerTests {

	private static final String JOB_ID = "test.job";
	private static final int EXPIRE_MINS = 10;

	private EventAdmin eventAdmin;
	private DatumExportTaskInfoDao taskDao;

	private DatumExportTaskCleanerJob job;

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		taskDao = EasyMock.createMock(DatumExportTaskInfoDao.class);

		job = new DatumExportTaskCleanerJob(eventAdmin, taskDao);
		job.setJobId(JOB_ID);
		job.setMinimumAgeMinutes(EXPIRE_MINS);
		job.setExecutorService(new CallingThreadExecutorService());
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, taskDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, taskDao);
	}

	@Test
	public void executeJob() {
		// GIVEN
		Capture<Instant> timeCaptor = new Capture<>();
		expect(taskDao.purgeCompletedTasks(capture(timeCaptor))).andReturn(1L);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		final Instant now = Instant.now();
		Map<String, Object> jobProps = new HashMap<>();
		jobProps.put(SchedulerConstants.JOB_ID, JOB_ID);
		Event event = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, jobProps);
		job.handleEvent(event);

		// THEN
		assertThat("Complete event posted", eventCaptor.hasCaptured(), is(true));
		Event completedEvent = eventCaptor.getValue();
		assertThat(completedEvent.getTopic(), is(SchedulerConstants.TOPIC_JOB_COMPLETE));
		assertThat(completedEvent.getProperty(SchedulerConstants.JOB_ID), is(JOB_ID));
		assertThat("Purge date is about EXPIRE_MINS old",
				(int) ChronoUnit.MINUTES.between(timeCaptor.getValue(), now.plus(1, ChronoUnit.MINUTES)),
				is(EXPIRE_MINS));
	}

}
