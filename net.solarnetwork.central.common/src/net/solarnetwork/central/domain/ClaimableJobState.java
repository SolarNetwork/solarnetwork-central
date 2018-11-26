/* ==================================================================
 * ClaimableJobState.java - 26/11/2018 10:01:33 AM
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

package net.solarnetwork.central.domain;

/**
 * API for a claimable job state.
 * 
 * <p>
 * At a basic level, jobs are added to a work queue and start in a "queued"
 * state. Some worker process can then claim ownership a "queued" job and change
 * the state to "claimed" and then "executing". Once the job is finished, the
 * worker process changes the state to "completed".
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.44
 */
public interface ClaimableJobState {

	/** The standard key for a "unknwon" state. */
	char UNKNOWN_KEY = 'u';

	/** The standard key for a "queued" state. */
	char QUEUED_KEY = 'q';

	/** The standard key for a "claimed" state. */
	char CLAIMED_KEY = 'p';

	/** The standard key for a "executing" state. */
	char EXECUTING_KEY = 'e';

	/** The standard key for a "completed" state. */
	char COMPLETED_KEY = 'c';

	/**
	 * Get a unique key for this state.
	 * 
	 * @return the state key
	 */
	char getKey();

}
