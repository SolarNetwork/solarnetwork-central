/* ==================================================================
 * UserNodeInstructionService.java - 18/11/2025 11:08:12 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz;

import java.time.Instant;
import java.util.concurrent.Future;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;

/**
 * Service to manage user instruction tasks.
 *
 * <p>
 * A user instruction task generates instructions on a schedule.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeInstructionService {

	/**
	 * Claim a queued task.
	 *
	 * <p>
	 * This method will "claim" a task that is currently in a "queued" state,
	 * changing the state to "claimed".
	 * </p>
	 *
	 * @return a claimed task, or {@code null} if none could be claimed
	 */
	UserNodeInstructionTaskEntity claimQueuedTask();

	/**
	 * Execute a task.
	 *
	 * @param task
	 *        the task to execute
	 * @return the task future
	 */
	Future<UserNodeInstructionTaskEntity> executeTask(UserNodeInstructionTaskEntity task);

	/**
	 * Reset tasks that are in the executing state but have an execute date
	 * older than a given date.
	 *
	 * <p>
	 * The intention of this method is to "reset" a task that was inadvertently
	 * left in an executing state, for example after a server restart.
	 * </p>
	 *
	 * @return the number of tasks reset
	 */
	int resetAbandondedExecutingTasks(Instant olderThan);

}
