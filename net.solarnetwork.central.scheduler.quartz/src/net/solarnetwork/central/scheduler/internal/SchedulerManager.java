/* ==================================================================
 * SchedulerManager.java - Jun 29, 2011 4:22:24 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.scheduler.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import net.solarnetwork.central.scheduler.EventHandlerSupport;
import net.solarnetwork.central.scheduler.JobInfo;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.scheduler.SchedulerStatus;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import net.solarnetwork.domain.PingTest;
import net.solarnetwork.domain.PingTestResult;

/**
 * Manage the lifecycle of the Quartz Scheduler.
 * 
 * @author matt
 * @version 1.6
 */
public class SchedulerManager extends EventHandlerSupport
		implements ApplicationListener<ContextRefreshedEvent>, EventHandler, PingTest,
		net.solarnetwork.central.scheduler.SchedulerManager {

	private static final String TEST_TOPIC = "net/solarnetwork/central/scheduler/TEST";

	private final Scheduler scheduler;
	private final EventAdmin eventAdmin;
	private long blockedJobMaxSeconds = 300;

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the Scheduler
	 */
	public SchedulerManager(Scheduler scheduler, EventAdmin eventAdmin) {
		this.scheduler = scheduler;
		this.eventAdmin = eventAdmin;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			for ( String triggerGroup : scheduler.getTriggerGroupNames() ) {
				for ( TriggerKey triggerKey : scheduler
						.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroup)) ) {
					Trigger t = scheduler.getTrigger(triggerKey);
					log.debug("Found trigger: " + t);
				}
			}

			// fire off test Trigger to verify scheduler operational
			Map<String, Object> props = new HashMap<String, Object>(5);
			props.put(SchedulerConstants.JOB_ID, "SchedulerManager.Startup");
			props.put(SchedulerConstants.JOB_DATE, new Date().getTime() + 5000);
			props.put(SchedulerConstants.JOB_GROUP, "Test");
			props.put(SchedulerConstants.JOB_TOPIC, TEST_TOPIC);

			Event e = new Event(SchedulerConstants.TOPIC_JOB_REQUEST, props);
			eventAdmin.postEvent(e);

		} catch ( SchedulerException e ) {
			log.error("Exception finding triggers", e);
		}
	}

	@Override
	protected void handleEventInternal(Event event) throws Exception {
		log.debug("Got event: {}", event.getTopic());
		if ( SchedulerConstants.TOPIC_JOB_COMPLETE.equals(event.getTopic()) ) {
			NotificationJob job = getRunningJob(event);
			if ( job != null ) {
				job.finish(event);
			}
		} else if ( SchedulerConstants.TOPIC_JOB_FAILURE.equals(event.getTopic()) ) {
			NotificationJob job = getRunningJob(event);
			if ( job != null ) {
				job.finish(event);
			}
		} else if ( TEST_TOPIC.equals(event.getTopic()) ) {
			Event ack = SchedulerUtils.createJobCompleteEvent(event);
			eventAdmin.postEvent(ack);

			// post "we're ready" event
			Map<String, Object> props = new HashMap<String, Object>(1);
			Event e = new Event(SchedulerConstants.TOPIC_SCHEDULER_READY, props);
			eventAdmin.postEvent(e);
		}
	}

	private NotificationJob getRunningJob(Event event) throws SchedulerException {
		final String jobId = (String) event.getProperty(SchedulerConstants.JOB_ID);
		if ( jobId == null ) {
			log.debug("Can't find running job for event because JOB_ID missing");
			return null;
		}
		final String jobGroup = (String) event.getProperty(SchedulerConstants.JOB_GROUP);
		for ( JobExecutionContext jec : scheduler.getCurrentlyExecutingJobs() ) {
			Trigger t = jec.getTrigger();
			if ( jobId.equals(t.getKey().getName())
					&& (jobGroup == null || jobGroup.equals(t.getKey().getGroup())) ) {
				return (NotificationJob) jec.getJobInstance();
			}
		}
		log.warn("Running job {} in group {} not found", jobId, jobGroup);
		return null;
	}

	@Override
	public SchedulerStatus currentStatus() {
		try {
			if ( scheduler.isShutdown() ) {
				return SchedulerStatus.Destroyed;
			} else if ( scheduler.isInStandbyMode() && scheduler.isStarted() ) {
				return SchedulerStatus.Paused;
			} else if ( scheduler.isInStandbyMode() ) {
				return SchedulerStatus.Starting;
			}
			return SchedulerStatus.Running;
		} catch ( SchedulerException e ) {
			log.error("Error getting Quartz scheduler status", e);
		}
		return SchedulerStatus.Unknown;
	}

	@Override
	public void updateStatus(SchedulerStatus desiredStatus) {
		try {
			if ( desiredStatus == SchedulerStatus.Destroyed ) {
				if ( !scheduler.isShutdown() ) {
					scheduler.shutdown(true);
				}
			} else if ( desiredStatus == SchedulerStatus.Paused ) {
				if ( !scheduler.isShutdown() && !scheduler.isInStandbyMode() ) {
					scheduler.standby();
				}
			} else if ( desiredStatus == SchedulerStatus.Running ) {
				if ( !scheduler.isShutdown() && scheduler.isInStandbyMode() ) {
					scheduler.start();
				}
			}
		} catch ( SchedulerException e ) {
			log.error("Error updating Quartz scheduler status to {}", desiredStatus, e);
		}
	}

	@Override
	public Collection<JobInfo> allJobInfos() {
		try {
			Set<TriggerKey> triggerKeys = scheduler.getTriggerKeys(GroupMatcher.<TriggerKey> anyGroup());
			List<JobInfo> result = new ArrayList<JobInfo>(triggerKeys.size());
			for ( TriggerKey triggerKey : triggerKeys ) {
				Trigger t = scheduler.getTrigger(triggerKey);
				result.add(new QuartzJobInfo(t, scheduler));
			}
			return result;
		} catch ( SchedulerException e ) {
			log.error("Error getting Quartz scheduler trigger details", e);
		}
		return null;
	}

	@Override
	public void pauseJob(String groupId, String id) {
		final TriggerKey tk = new TriggerKey(id, groupId);
		try {
			scheduler.pauseTrigger(tk);
		} catch ( SchedulerException e ) {
			log.error("Error pausing trigger [" + tk + "]", e);
		}
	}

	@Override
	public void resumeJob(String groupId, String id) {
		final TriggerKey tk = new TriggerKey(id, groupId);
		try {
			scheduler.resumeTrigger(tk);
		} catch ( SchedulerException e ) {
			log.error("Error pausing trigger [" + tk + "]", e);
		}
	}

	// PingTest support

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "Job Scheduler";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 2000;
	}

	@Override
	public PingTest.Result performPingTest() throws Exception {
		Scheduler s = scheduler;
		if ( s.isInStandbyMode() ) {
			return new PingTestResult(false, "Scheduler is in standby mode");
		}
		if ( s.isShutdown() ) {
			return new PingTestResult(false, "Scheduler is shut down");
		}

		int triggerCount = 0;
		long now = System.currentTimeMillis();
		final String stateErrorTemplate = "Trigger %s.%s is in the %s state, since %tc";
		for ( String triggerGroup : s.getTriggerGroupNames() ) {
			for ( TriggerKey triggerKey : scheduler
					.getTriggerKeys(GroupMatcher.triggerGroupEquals(triggerGroup)) ) {
				triggerCount += 1;
				Trigger.TriggerState triggerState = s.getTriggerState(triggerKey);
				Date lastFireTime = null;
				Trigger trigger = null;
				switch (triggerState) {
					case BLOCKED:
						trigger = s.getTrigger(triggerKey);
						lastFireTime = trigger.getPreviousFireTime();
						if ( lastFireTime != null
								&& lastFireTime.getTime() + (blockedJobMaxSeconds * 1000) < now ) {
							return new PingTestResult(false, String.format(stateErrorTemplate,
									triggerGroup, triggerKey.getName(), "BLOCKED", lastFireTime));
						}
						break;

					case ERROR:
						trigger = s.getTrigger(triggerKey);
						lastFireTime = trigger.getPreviousFireTime();
						return new PingTestResult(false, String.format(stateErrorTemplate, triggerGroup,
								triggerKey.getName(), "ERROR", lastFireTime));

					default:
						// no error
				}
			}
		}

		String msg = String.format("Scheduler is running as expected; %d triggers configured.",
				triggerCount);
		return new PingTestResult(true, msg);
	}

	public long getBlockedJobMaxSeconds() {
		return blockedJobMaxSeconds;
	}

	/**
	 * A minimum amount of seconds before a blocked job results in an error.
	 * 
	 * @param blockedJobMaxSeconds
	 *        The number of seconds.
	 */
	public void setBlockedJobMaxSeconds(long blockedJobMaxSeconds) {
		this.blockedJobMaxSeconds = blockedJobMaxSeconds;
	}

}
