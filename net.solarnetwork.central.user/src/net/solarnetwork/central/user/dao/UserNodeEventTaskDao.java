/* ==================================================================
 * UserNodeEventTaskDao.java - 8/06/2020 10:12:18 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao;

import java.time.Instant;
import net.solarnetwork.central.user.domain.UserNodeEvent;
import net.solarnetwork.central.user.domain.UserNodeEventTask;
import net.solarnetwork.central.user.domain.UserNodeEventTaskState;

/**
 * DAO for managing {@link UserNodeEventTask} entities.
 * 
 * @author matt
 * @version 1.0
 * @since 2.2
 */
public interface UserNodeEventTaskDao {

	/**
	 * Claim a user node event task, so that it can be processed.
	 * 
	 * <p>
	 * It is expected that a transaction exists before calling this method, and
	 * that {@link #taskCompleted(UserNodeEventTask)} will be invoked within the
	 * same transaction.
	 * </p>
	 * 
	 * @param topic
	 *        the event topic to claim a task from
	 * @return the claimed task and configuration, or {@literal null} if no task
	 *         is available
	 */
	UserNodeEvent claimQueuedTask(String topic);

	/**
	 * Mark a task as completed.
	 * 
	 * @param task
	 *        the task to mark as completed
	 * @see #claimQueuedTask(String)
	 */
	void taskCompleted(UserNodeEventTask task);

	/**
	 * Delete tasks that have reached a {@link UserNodeEventTaskState#Completed}
	 * state and are older than a given date.
	 * 
	 * @param olderThanDate
	 *        the maximum date for which to purge completed tasks
	 * @return the number of tasks deleted
	 */
	long purgeCompletedTasks(Instant olderThanDate);

}
