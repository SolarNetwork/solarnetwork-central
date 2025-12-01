/* ==================================================================
 * UserNodeInstructionBiz.java - 16/11/2025 3:00:28 pm
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

import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskSimulationOutput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for user instruction tasks.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserNodeInstructionBiz {

	/**
	 * Get a list of all available control instruction tasks for a given user.
	 *
	 * @param userId
	 *        the user ID to get entities for
	 * @param filter
	 *        an optional filter
	 * @return the available entities, never {@literal null}
	 */
	FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> listControlInstructionTasksForUser(
			Long userId, UserNodeInstructionTaskFilter filter);

	/**
	 * Update the state of a control instruction task.
	 *
	 * @param id
	 *        the ID of the task to update the state of
	 * @param desiredState
	 *        the state to update the task to
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to {@code desiredState}, or {@literal null} if
	 *        the current state of the task does not matter
	 * @return the resulting task, or {@literal null} if no such task exists
	 */
	UserNodeInstructionTaskEntity updateControlInstructionTaskState(UserLongCompositePK id,
			BasicClaimableJobState desiredState, BasicClaimableJobState... expectedStates);

	/**
	 * Save a control instruction task.
	 *
	 * @param id
	 *        the ID of the {@link UserNodeInstructionTaskEntity} to save; the
	 *        entity ID can be set to
	 *        {@link UserLongCompositePK#UNASSIGNED_ENTITY_ID} to create a new
	 *        instruction task entity
	 * @param input
	 *        the info to save
	 * @param expectedStates
	 *        a set of states that must include the task's current state in
	 *        order to change it to the info's given state, or {@literal null}
	 *        if the current state of the task does not matter
	 * @return the resulting task
	 */
	UserNodeInstructionTaskEntity saveControlInstructionTask(UserLongCompositePK id,
			UserNodeInstructionTaskEntityInput input, BasicClaimableJobState... expectedStates);

	/**
	 * Delete a specific control instruction task.
	 *
	 * @param id
	 *        the primary key of the entity to delete
	 */
	void deleteControlInstructionTask(UserLongCompositePK id);

	/**
	 * Simulate the execution of a control instruction task.
	 *
	 * @param id
	 *        the ID of the user simulating the task for
	 * @param input
	 *        the complete node instruction task input to simulate
	 * @return the simulation result, never {@code null}
	 */
	UserNodeInstructionTaskSimulationOutput simulateControlInstructionTaskForUser(Long userId,
			UserNodeInstructionTaskEntityInput input);

}
