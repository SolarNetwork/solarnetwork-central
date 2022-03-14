/* ==================================================================
 * SchedulerStatus.java - 24/01/2018 2:46:55 PM
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

/**
 * Status for the scheduler.
 * 
 * @author matt
 * @version 1.0
 * @since 1.37
 */
public enum SchedulerStatus {

	/** The scheduler is starting up, but not yet executing any jobs. */
	Starting,

	/** The scheduler has started and is scheduling jobs normally. */
	Running,

	/**
	 * The scheduler has been paused, is not executing any more jobs, and can be
	 * re-started.
	 */
	Paused,

	/**
	 * The scheduler has been stopped and will not execute any more jobs without
	 * manual intervention.
	 */
	Destroyed,

	/**
	 * The scheduler is not in any known state.
	 */
	Unknown;

}
