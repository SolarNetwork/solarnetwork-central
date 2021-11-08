/* ==================================================================
 * ManagedJob.java - 8/11/2021 8:58:30 AM
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

/**
 * A managed job.
 * 
 * @author matt
 * @version 1.0
 */
public interface ManagedJob extends Runnable {

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
	 * Get the job schedule, either as a cron expression or millisecond period.
	 * 
	 * @return the job schedule
	 */
	String getSchedule();

}
