/* ==================================================================
 * JobStatus.java - 26/01/2018 2:15:47 PM
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
 * Status for a job.
 * 
 * @author matt
 * @version 1.0
 * @since 1.37
 */
public enum JobStatus {

	/** The job is scheduled to run normally. */
	Scheduled,

	/**
	 * The job will not be executed in the future, but can be resumed.
	 */
	Paused,

	/**
	 * The job has finished executing and is not scheduled to run again.
	 */
	Complete,

	/**
	 * The job encountered an error.
	 */
	Error,

	/**
	 * The job is not in any known state.
	 */
	Unknown;

}
