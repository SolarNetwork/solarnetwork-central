/* ==================================================================
 * CloudDatumStreamPollTaskDao.java - 9/10/2024 9:16:42 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao;

import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link CloudDatumStreamPollTaskEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamPollTaskDao
		extends GenericCompositeKey2Dao<CloudDatumStreamPollTaskEntity, UserLongCompositePK, Long, Long>,
		FilterableDao<CloudDatumStreamPollTaskEntity, UserLongCompositePK, CloudDatumStreamPollTaskFilter> {

	/**
	 * Claim a queued task.
	 *
	 * This method will "claim" a task that is currently in a "queued" state,
	 * changing the state to "claimed".
	 *
	 * @return a claimed task, or {@literal null} if none could be claimed
	 */
	CloudDatumStreamPollTaskEntity claimQueuedTask();

	/**
	 * Update the state of a specific poll task.
	 *
	 * @param id
	 *        the ID of the task to update the state of
	 * @param desiredState
	 *        the state to update the task to
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to {@code desiredState}, or {@literal null} if
	 *        the current state of the task does not matter
	 * @return {@literal true} if the task state was changed
	 */
	boolean updateTaskState(UserLongCompositePK id, BasicClaimableJobState desiredState,
			BasicClaimableJobState... expectedStates);

	/**
	 * Update a specific poll task.
	 *
	 * @param info
	 *        the info to save
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to the info's given state, or {@literal null}
	 *        if the current state of the task does not matter
	 * @return {@literal true} if the task state was changed
	 */
	boolean updateTask(CloudDatumStreamPollTaskEntity info, BasicClaimableJobState... expectedStates);

}
