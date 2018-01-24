/* ==================================================================
 * JobInfo.java - 24/01/2018 3:04:21 PM
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

import org.joda.time.DateTime;

/**
 * Information about a scheduled job.
 * 
 * <p>
 * Jobs are uniquely defined by a group name plus a job name.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.37
 */
public interface JobInfo {

	/**
	 * Get the group this job belongs to.
	 * 
	 * @return the group ID
	 */
	String getGroupId();

	/**
	 * Get the ID of this job, unique to the job's group.
	 * 
	 * @return the job ID
	 */
	String getId();

	/**
	 * Flag indicating the job is currently executing.
	 * 
	 * @return {@literal true} if the job is executing
	 */
	boolean isExecuting();

	/**
	 * Get the next execution time of the job.
	 * 
	 * @return the next execution time, or {@literal null} if no more executions
	 *         are scheduled
	 */
	DateTime getNextExecutionTime();

	/**
	 * Get a description of the execution schedule of the job.
	 * 
	 * <p>
	 * The description might be a period like "every 10 minutes" or a cron
	 * expression, for example.
	 * </p>
	 * 
	 * @return a description of the execution schedule
	 */
	String getExecutionScheduleDescription();

}
