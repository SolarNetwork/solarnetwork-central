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
import java.util.concurrent.ScheduledFuture;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A scheduled task.
 * 
 * @author matt
 * @version 1.1
 */
public class ScheduledJob extends BasicJobInfo implements Runnable {

	private final JobKey key;
	private final Runnable task;
	private final Trigger trigger;

	private @Nullable ScheduledFuture<?> future;
	private volatile boolean paused = false;
	private volatile @Nullable Throwable exception;
	private volatile @Nullable Instant lastActualExecutionTime;
	private volatile @Nullable Instant lastCompletionTime;

	/**
	 * Constructor.
	 * 
	 * @param key
	 *        the job key
	 * @param task
	 *        the job task
	 * @param trigger
	 *        the job trigger
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ScheduledJob(JobKey key, Runnable task, Trigger trigger) {
		super(requireNonNullArgument(key, "key").getGroupId(), key.getId(),
				extractExecutionScheduleDescription(requireNonNullArgument(trigger, "trigger")));
		this.key = requireNonNullArgument(key, "key");
		this.task = requireNonNullArgument(task, "task");
		this.trigger = requireNonNullArgument(trigger, "trigger");
	}

	@Override
	public final void run() {
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
	public final JobStatus getJobStatus() {
		if ( exception != null ) {
			return JobStatus.Error;
		} else if ( paused ) {
			return JobStatus.Paused;
		}
		return JobStatus.Scheduled;
	}

	@Override
	public final boolean isExecuting() {
		final Instant lastStart = getPreviousExecutionTime();
		final Instant lastComplete = getLastCompletionTime();
		return (lastStart != null && (lastComplete == null || lastComplete.isBefore(lastStart)));
	}

	@Override
	public final @Nullable Instant getPreviousExecutionTime() {
		return lastActualExecutionTime;
	}

	@Override
	public final @Nullable Instant getNextExecutionTime() {
		if ( paused ) {
			return null;
		}
		final Instant lastStart = getPreviousExecutionTime();
		final Instant lastComplete = getLastCompletionTime();
		return trigger.nextExecution(new SimpleTriggerContext(null, lastStart, lastComplete));
	}

	/**
	 * Get the job key.
	 * 
	 * @return the job key
	 */
	@JsonIgnore
	public final JobKey getKey() {
		return key;
	}

	/**
	 * Get the trigger.
	 * 
	 * @return the trigger
	 */
	public final Trigger getTrigger() {
		return trigger;
	}

	/**
	 * Get the last completion time.
	 * 
	 * @return the last completion time, or {@code null} if never completed
	 */
	public final @Nullable Instant getLastCompletionTime() {
		return lastCompletionTime;
	}

	/**
	 * Get the paused flag.
	 * 
	 * @return {@literal true} if this job is paused
	 */
	public final boolean isPaused() {
		return paused;
	}

	/**
	 * Set the paused flag.
	 * 
	 * @param paused
	 *        {@literal true} if this job is paused
	 */
	public final void setPaused(boolean paused) {
		this.paused = paused;
	}

	/**
	 * Job future.
	 * 
	 * @return the job future
	 */
	@JsonIgnore
	public final @Nullable ScheduledFuture<?> getFuture() {
		return future;
	}

	/**
	 * Set the job future.
	 * 
	 * @param future
	 *        the job future
	 */
	public final void setFuture(@Nullable ScheduledFuture<?> future) {
		this.future = future;
	}

}
