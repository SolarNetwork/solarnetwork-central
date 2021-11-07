/* ==================================================================
 * SchedulerManager.java - 24/01/2018 2:42:48 PM
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

package net.solarnetwork.central.scheduler;

import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.Trigger;

/**
 * API for management of the SolarNet scheduler.
 * 
 * @author matt
 * @version 2.0
 * @since 1.37
 */
public interface SchedulerManager {

	/**
	 * Get the current status of the scheduler.
	 * 
	 * @return the current status
	 */
	SchedulerStatus currentStatus();

	/**
	 * Change the status of the scheduler.
	 * 
	 * @param desiredStatus
	 *        the desired status to set
	 */
	void updateStatus(SchedulerStatus desiredStatus);

	/**
	 * Get a collection of all available scheduled jobs.
	 * 
	 * @return the collection of jobs; never {@literal null}
	 */
	Collection<JobInfo> allJobInfos();

	/**
	 * Pause a specific job.
	 * 
	 * @param groupId
	 *        the job group ID
	 * @param id
	 *        the job ID
	 */
	void pauseJob(String groupId, String id);

	/**
	 * Resume a paused job.
	 * 
	 * @param groupId
	 *        the job group ID
	 * @param id
	 *        the job ID
	 */
	void resumeJob(String groupId, String id);

	/**
	 * Schedule a job.
	 * 
	 * <p>
	 * If a job with the same group ID and ID has previously been scheduled, it
	 * will be re-scheduled using the given trigger.
	 * </p>
	 * 
	 * @param groupId
	 *        the job group ID
	 * @param id
	 *        the job ID
	 * @param runnable
	 *        the job task
	 * @param trigger
	 *        the desired trigger
	 * @return the scheduled future
	 */
	ScheduledFuture<?> scheduleJob(String groupId, String id, Runnable task, Trigger trigger);

	/**
	 * Unschedule a job.
	 * 
	 * @param groupId
	 *        the job group ID
	 * @param id
	 *        the job ID
	 * @return {@literal true} if a job with matching group ID and ID was
	 *         successfully unscheduled
	 */
	boolean unscheduleJob(String groupId, String id);

}
