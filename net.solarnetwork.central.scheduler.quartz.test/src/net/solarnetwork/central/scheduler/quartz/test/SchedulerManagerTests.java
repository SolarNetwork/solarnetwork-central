/* ==================================================================
 * SchedulerManagerTests.java - 25/01/2018 7:28:01 AM
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

package net.solarnetwork.central.scheduler.quartz.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.StringMatcher.StringOperatorName;
import org.quartz.impl.triggers.CronTriggerImpl;
import net.solarnetwork.central.scheduler.JobInfo;
import net.solarnetwork.central.scheduler.SchedulerStatus;
import net.solarnetwork.central.scheduler.internal.QuartzJobInfo;
import net.solarnetwork.central.scheduler.internal.SchedulerManager;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.test.Assertion;
import net.solarnetwork.test.EasyMockUtils;

/**
 * Test cases for the {@link SchedulerManager} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SchedulerManagerTests extends AbstractCentralTest {

	private Scheduler scheduler;
	private EventAdmin eventAdmin;

	private SchedulerManager service;

	@Before
	public void setup() {
		scheduler = EasyMock.createMock(Scheduler.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		service = new SchedulerManager(scheduler, eventAdmin);
	}

	@After
	public void teardown() {
		EasyMock.verify(scheduler, eventAdmin);
	}

	private void replayAll() {
		EasyMock.replay(scheduler, eventAdmin);
	}

	@Test
	public void statusShutdown() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(true).anyTimes();

		replayAll();

		assertThat(service.currentStatus(), equalTo(SchedulerStatus.Destroyed));
	}

	@Test
	public void statusStarting() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		expect(scheduler.isInStandbyMode()).andReturn(true).anyTimes();
		expect(scheduler.isStarted()).andReturn(false).anyTimes();

		replayAll();

		assertThat(service.currentStatus(), equalTo(SchedulerStatus.Starting));
	}

	@Test
	public void statusStarted() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		expect(scheduler.isInStandbyMode()).andReturn(false).anyTimes();
		expect(scheduler.isStarted()).andReturn(true).anyTimes();

		replayAll();

		assertThat(service.currentStatus(), equalTo(SchedulerStatus.Running));
	}

	@Test
	public void statusPaused() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		expect(scheduler.isInStandbyMode()).andReturn(true).anyTimes();
		expect(scheduler.isStarted()).andReturn(true).anyTimes();

		replayAll();

		assertThat(service.currentStatus(), equalTo(SchedulerStatus.Paused));
	}

	@Test
	public void statusUnknown() throws SchedulerException {
		expect(scheduler.isShutdown()).andThrow(new SchedulerException("Test"));

		replayAll();

		assertThat(service.currentStatus(), equalTo(SchedulerStatus.Unknown));
	}

	@Test
	public void updateStatusPausedToRunning() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		expect(scheduler.isInStandbyMode()).andReturn(true).anyTimes();
		scheduler.start();

		replayAll();

		service.updateStatus(SchedulerStatus.Running);
	}

	@Test
	public void updateStatusRunningToPaused() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		expect(scheduler.isInStandbyMode()).andReturn(false).anyTimes();
		scheduler.standby();

		replayAll();

		service.updateStatus(SchedulerStatus.Paused);
	}

	@Test
	public void updateStatusRunningToDestroyed() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(false).anyTimes();
		scheduler.shutdown(true);

		replayAll();

		service.updateStatus(SchedulerStatus.Destroyed);
	}

	@Test
	public void updateStatusDestroyedToRunning() throws SchedulerException {
		expect(scheduler.isShutdown()).andReturn(true).anyTimes();

		replayAll();

		service.updateStatus(SchedulerStatus.Running);
	}

	@Test
	public void allJobInfosCronTrigger() throws Exception {
		final TriggerKey triggerKey = new TriggerKey("t1", "g1");
		expect(scheduler
				.getTriggerKeys(EasyMockUtils.assertWith(new Assertion<GroupMatcher<TriggerKey>>() {

					@Override
					public void check(GroupMatcher<TriggerKey> argument) throws Throwable {
						assertThat(argument.getCompareWithOperator(),
								equalTo(StringOperatorName.ANYTHING));
					}
				}))).andReturn(Collections.singleton(triggerKey));

		final Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
				.withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")
						.withMisfireHandlingInstructionDoNothing())
				.forJob(triggerKey.getName(), triggerKey.getGroup()).build();
		final Date firstFireTime = ((CronTriggerImpl) trigger).computeFirstFireTime(null);
		expect(scheduler.getTrigger(triggerKey)).andReturn(trigger);

		// for isExecuting call
		expect(scheduler.getCurrentlyExecutingJobs())
				.andReturn(Collections.<JobExecutionContext> emptyList());

		replayAll();

		Collection<JobInfo> infos = service.allJobInfos();
		assertThat(infos, hasSize(1));

		JobInfo info = infos.iterator().next();
		assertThat(info, is(instanceOf(QuartzJobInfo.class)));
		assertThat("Job ID", info.getId(), equalTo(triggerKey.getName()));
		assertThat("Job group ID", info.getGroupId(), equalTo(triggerKey.getGroup()));
		assertThat("Schedule desc", info.getExecutionScheduleDescription(),
				equalTo("cron: 0 * * * * ?"));
		assertThat("Next exec time", info.getNextExecutionTime(), equalTo(new DateTime(firstFireTime)));
		assertThat("Executing", info.isExecuting(), equalTo(false));
	}

	@Test
	public void allJobInfosCronTriggerExecuting() throws Exception {
		final TriggerKey triggerKey = new TriggerKey("t1", "g1");
		expect(scheduler
				.getTriggerKeys(EasyMockUtils.assertWith(new Assertion<GroupMatcher<TriggerKey>>() {

					@Override
					public void check(GroupMatcher<TriggerKey> argument) throws Throwable {
						assertThat(argument.getCompareWithOperator(),
								equalTo(StringOperatorName.ANYTHING));
					}
				}))).andReturn(Collections.singleton(triggerKey));

		final Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
				.withSchedule(CronScheduleBuilder.cronSchedule("0 * * * * ?")
						.withMisfireHandlingInstructionDoNothing())
				.forJob(triggerKey.getName(), triggerKey.getGroup()).build();
		expect(scheduler.getTrigger(triggerKey)).andReturn(trigger);

		// for isExecuting call
		JobExecutionContext runningJob = EasyMock.createMock(JobExecutionContext.class);
		expect(runningJob.getTrigger()).andReturn(trigger);
		expect(scheduler.getCurrentlyExecutingJobs()).andReturn(Collections.singletonList(runningJob));

		replayAll();
		EasyMock.replay(runningJob);

		Collection<JobInfo> infos = service.allJobInfos();
		assertThat(infos, hasSize(1));

		JobInfo info = infos.iterator().next();
		assertThat(info, is(instanceOf(QuartzJobInfo.class)));
		assertThat("Job ID", info.getId(), equalTo(triggerKey.getName()));
		assertThat("Job group ID", info.getGroupId(), equalTo(triggerKey.getGroup()));
		assertThat("Executing", info.isExecuting(), equalTo(true));

		EasyMock.verify(runningJob);
	}

}
