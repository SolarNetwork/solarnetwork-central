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

/**
 * API for management of the SolarNet scheduler.
 * 
 * @author matt
 * @version 1.0
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

}
