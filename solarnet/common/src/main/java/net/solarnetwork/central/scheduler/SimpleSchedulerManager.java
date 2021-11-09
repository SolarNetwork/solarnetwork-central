/* ==================================================================
 * SimpleSchedulerManager.java - 7/11/2021 11:19:29 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.scheduler;

import static net.solarnetwork.central.scheduler.SchedulerUtils.extractExecutionScheduleDescription;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Implementation of {@link SchedulerManager} using a {@link TaskScheduler}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleSchedulerManager implements SchedulerManager, PingTest, ServiceLifecycleObserver {

	/**
	 * The default {@code pingTestMaximumExecutionMilliseconds} property value.
	 */
	public static final long DEFUALT_PING_TEST_MAX_EXECUTION = 2000;

	/** The default {@code blockedJobMaxSeconds} property value. */
	public static final long DEFAULT_BLOCKED_JOB_MAX_SECONDS = 1800;

	private static final Logger log = LoggerFactory.getLogger(SimpleSchedulerManager.class);

	private final ConcurrentNavigableMap<JobKey, ScheduledJob> jobs = new ConcurrentSkipListMap<>();
	private final TaskScheduler taskScheduler;
	private long blockedJobMaxSeconds = DEFAULT_BLOCKED_JOB_MAX_SECONDS;
	private long pingTestMaximumExecutionMilliseconds = DEFUALT_PING_TEST_MAX_EXECUTION;

	private SchedulerStatus status = SchedulerStatus.Starting;

	/**
	 * Constructor.
	 * 
	 * @param taskScheduler
	 *        the scheduler
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleSchedulerManager(TaskScheduler taskScheduler) {
		super();
		this.taskScheduler = requireNonNullArgument(taskScheduler, "taskScheduler");
	}

	@Override
	public void serviceDidStartup() {
		status = SchedulerStatus.Running;
	}

	@Override
	public void serviceDidShutdown() {
		status = SchedulerStatus.Destroyed;
	}

	@Override
	public SchedulerStatus currentStatus() {
		return status;
	}

	@Override
	public void updateStatus(SchedulerStatus desiredStatus) {
		this.status = desiredStatus;
	}

	@Override
	public Collection<JobInfo> allJobInfos() {
		return new ArrayList<>(jobs.values());
	}

	@Override
	public void pauseJob(String groupId, String id) {
		ScheduledJob job = jobs.get(new JobKey(groupId, id));
		if ( job != null ) {
			job.setPaused(true);
		}
	}

	@Override
	public void resumeJob(String groupId, String id) {
		ScheduledJob job = jobs.get(new JobKey(groupId, id));
		if ( job != null ) {
			job.setPaused(false);
		}
	}

	@Override
	public synchronized ScheduledFuture<?> scheduleJob(String groupId, String id, Runnable task,
			Trigger trigger) {
		try {
			final JobKey key = new JobKey(groupId, id);
			unscheduleJob(key);
			log.info("Scheduling job {} @ {}", key.getDescription(),
					extractExecutionScheduleDescription(trigger));
			ScheduledJob job = new ScheduledJob(key, task, trigger);
			ScheduledFuture<?> f = taskScheduler.schedule(job, trigger);
			job.setFuture(f);
			jobs.put(key, job);
			return f;
		} catch ( Exception e ) {
			log.error("Error scheduling job [{}.{}]: {}", groupId, id, e.toString(), e);
			throw e;
		}
	}

	@Override
	public synchronized boolean unscheduleJob(String groupId, String id) {
		final JobKey key = new JobKey(groupId, id);
		return unscheduleJob(key);
	}

	private synchronized boolean unscheduleJob(final JobKey key) {
		ScheduledJob job = jobs.get(key);
		if ( job != null ) {
			log.info("Unscheduling job {}", key.getDescription());
			job.setPaused(true);
			ScheduledFuture<?> f = job.getFuture();
			if ( f != null ) {
				f.cancel(false);
				job.setFuture(null);
			}
		}
		return job != null;
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
		return pingTestMaximumExecutionMilliseconds;
	}

	@Override
	public PingTest.Result performPingTest() throws Exception {
		if ( status == SchedulerStatus.Paused ) {
			return new PingTestResult(false, "Scheduler is paused");
		} else if ( status == SchedulerStatus.Destroyed ) {
			return new PingTestResult(false, "Scheduler is shut down");
		}

		int triggerCount = 0;
		final String stateErrorTemplate = "Trigger %s.%s is in the %s state, since %s.";
		for ( Entry<JobKey, ScheduledJob> entry : jobs.entrySet() ) {
			JobKey key = entry.getKey();
			ScheduledJob job = entry.getValue();
			triggerCount += 1;
			JobStatus triggerState = job.getJobStatus();
			Instant lastFireTime = job.getPreviousExecutionTime();
			String sinceTime = (lastFireTime != null ? DateTimeFormatter.ISO_INSTANT.format(lastFireTime)
					: "");
			if ( triggerState == JobStatus.Error ) {
				return new PingTestResult(false, String.format(stateErrorTemplate, key.getGroupId(),
						key.getId(), "ERROR", sinceTime));
			} else if ( job.isExecuting() && (System.currentTimeMillis()
					- lastFireTime.toEpochMilli()) > (blockedJobMaxSeconds * 1000L) ) {
				return new PingTestResult(false, String.format(stateErrorTemplate, key.getGroupId(),
						key.getId(), "BLOCKED", sinceTime));
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

	/**
	 * Set the maximum ping test execution time.
	 * 
	 * @param pingTestMaximumExecutionMilliseconds
	 *        the maximum execution time, in milliseconds; defaults to
	 *        {@link #DEFUALT_PING_TEST_MAX_EXECUTION}
	 * @since 1.7
	 */
	public void setPingTestMaximumExecutionMilliseconds(long pingTestMaximumExecutionMilliseconds) {
		this.pingTestMaximumExecutionMilliseconds = pingTestMaximumExecutionMilliseconds;
	}

}
