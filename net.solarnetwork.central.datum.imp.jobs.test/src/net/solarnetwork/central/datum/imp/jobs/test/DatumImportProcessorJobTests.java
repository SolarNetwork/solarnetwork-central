/* ==================================================================
 * DatumImportProcessorJobTests.java - 13/11/2018 4:50:39 PM
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

package net.solarnetwork.central.datum.imp.jobs.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.jobs.DatumImportProcessorJob;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * Test cases for the {@link DatumImportProcessorJob} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumImportProcessorJobTests {

	private static final String JOB_ID = "test.job";
	private static final Long TEST_USER_ID = 123L;

	private EventAdmin eventAdmin;
	private DatumImportJobBiz importJobBiz;
	private DatumImportJobInfoDao jobInfoDao;

	private DatumImportProcessorJob job;

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		importJobBiz = EasyMock.createMock(DatumImportJobBiz.class);
		jobInfoDao = EasyMock.createMock(DatumImportJobInfoDao.class);

		job = new DatumImportProcessorJob(eventAdmin, importJobBiz, jobInfoDao);
		job.setJobId(JOB_ID);
		job.setMaximumClaimCount(2);
		job.setExecutorService(new CallingThreadExecutorService());
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, importJobBiz, jobInfoDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, importJobBiz, jobInfoDao);
	}

	@Test
	public void executeJob() {
		// given
		DatumImportJobInfo info1 = new DatumImportJobInfo();
		info1.setId(new UserUuidPK(TEST_USER_ID, UUID.randomUUID()));
		expect(jobInfoDao.claimQueuedJob()).andReturn(info1);
		DatumImportStatus status1 = EasyMock.createNiceMock(DatumImportStatus.class);
		expect(importJobBiz.performImport(info1.getId())).andReturn(status1);

		DatumImportJobInfo info2 = new DatumImportJobInfo();
		expect(jobInfoDao.claimQueuedJob()).andReturn(info2);
		DatumImportStatus status2 = EasyMock.createNiceMock(DatumImportStatus.class);
		expect(importJobBiz.performImport(info2.getId())).andReturn(status2);

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
