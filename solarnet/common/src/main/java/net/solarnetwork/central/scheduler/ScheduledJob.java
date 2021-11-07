/* ==================================================================
 * ScheduledJob.java - 7/11/2021 2:09:15 PM
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
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * A scheduled task.
 * 
 * @author matt
 * @version 1.0
 */
public class ScheduledJob extends BasicJobInfo implements Runnable {

	private final JobKey key;
	private final Runnable task;
	private final Trigger trigger;

	private ScheduledFuture<?> future;
	private volatile boolean paused = false;
	private volatile Throwable exception;
	private volatile Instant lastActualExecutionTime;
	private volatile Instant lastCompletionTime;

	/**
	 * Constructor.
	 * 
	 * @param groupId
	 *        the group ID
	 * @param id
	 *        the job ID
	 * @param task
	 *        the job task
	 * @param trigger
	 *        the job trigger
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ScheduledJob(JobKey key, Runnable task, Trigger trigger) {
		super(requireNonNullArgument(key, "key").getGroupId(), key.getId(),
				extractExecutionScheduleDescription(requireNonNullArgument(trigger, "trigger")));
		this.key = key;
		this.task = requireNonNullArgument(task, "task");
		this.trigger = trigger;
	}

	@Override
	public void run() {
		if ( paused ) {
			return;
		}
		lastActualExecutionTime = Instant.now();
		exception = null;
		try {
			task.run();
		} catch ( Exception e ) {
			exception = e;
			throw e;
		} finally {
			lastCompletionTime = Instant.now();
		}
	}

	@Override
	public JobStatus getJobStatus() {
		if ( exception != null ) {
			return JobStatus.Error;
		} else if ( paused ) {
			return JobStatus.Paused;
		}
		return JobStatus.Scheduled;
	}

	@Override
	public boolean isExecuting() {
		final Instant lastStart = getPreviousExecutionTime();
		final Instant lastComplete = getLastCompletionTime();
		return (lastStart != null && (lastComplete == null || lastComplete.isBefore(lastStart)));
	}

	@Override
	public Instant getPreviousExecutionTime() {
		return lastActualExecutionTime;
	}

	@Override
	public Instant getNextExecutionTime() {
		if ( paused ) {
			return null;
		}
		final Instant lastStart = getPreviousExecutionTime();
		final Instant lastComplete = getLastCompletionTime();
		Date next = trigger.nextExecutionTime(
				new SimpleTriggerContext(null, lastStart != null ? Date.from(lastStart) : null,
						lastComplete != null ? Date.from(lastComplete) : null));
		return (next != null ? next.toInstant() : null);
	}

	/**
	 * Get the job key.
	 * 
	 * @return the job key
	 */
	public JobKey getKey() {
		return key;
	}

	/**
	 * Get the trigger.
	 * 
	 * @return the trigger
	 */
	public Trigger getTrigger() {
		return trigger;
	}

	/**
	 * Get the last completion time.
	 * 
	 * @return the last completion time, or {@literal null} if never completed
	 */
	public Instant getLastCompletionTime() {
		return lastCompletionTime;
	}

	/**
	 * Get the paused flag.
	 * 
	 * @return {@literal true} if this job is paused
	 */
	public boolean isPaused() {
		return paused;
	}

	/**
	 * Set the paused flag.
	 * 
	 * @param paused
	 *        {@literal true} if this job is paused
	 */
	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	/**
	 * Job future.
	 * 
	 * @return the job future
	 */
	public ScheduledFuture<?> getFuture() {
		return future;
	}

	/**
	 * Set the job future.
	 * 
	 * @param future
	 *        the job future
	 */
	public void setFuture(ScheduledFuture<?> future) {
		this.future = future;
	}

}
