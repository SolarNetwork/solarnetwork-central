/* ==================================================================
 * NodeInstructionDao.java - Sep 29, 2011 6:47:11 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.dao;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * DAO API for {@link NodeInstruction}.
 * 
 * @author matt
 * @version 2.6
 */
public interface NodeInstructionDao
		extends GenericDao<NodeInstruction, Long>, FilterableDao<EntityMatch, Long, InstructionFilter> {

	/**
	 * Purge instructions that have reached a final state and are older than a
	 * given date.
	 * 
	 * @param olderThanDate
	 *        The maximum date for which to purge completed instructions.
	 * @return The number of instructions deleted.
	 * @since 2.0
	 */
	long purgeCompletedInstructions(Instant olderThanDate);

	/**
	 * Purge instructions that have not reached a final state and are older than
	 * a given date.
	 * 
	 * @param olderThanDate
	 *        The maximum date for which to purge completed instructions.
	 * @return The number of instructions deleted.
	 * @since 2.1
	 */
	long purgeIncompleteInstructions(Instant olderThanDate);

	/**
	 * Transition instructions that have expired to a new state.
	 * 
	 * <p>
	 * The {@link Instruction#getExpirationDate()} will be used as the date
	 * before which expired instructions should be transitioned. The
	 * {@link Instruction#getState()} will be the state the matching
	 * instructions are transitioned to. The
	 * {@link Instruction#getResultParameters()} will be persisted as well.
	 * </p>
	 * 
	 * @param criteria
	 *        the expiration criteria
	 * @return the updated row count
	 * @since 2.5
	 */
	int transitionExpiredInstructions(NodeInstruction criteria);

	/**
	 * Update the state of a node instruction.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param nodeId
	 *        the node ID
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        optional result parameters to include
	 * @return {@literal true} if the instruction was found and updated
	 * @since 1.2
	 */
	boolean updateNodeInstructionState(Long instructionId, Long nodeId, InstructionState state,
			Map<String, ?> resultParameters);

	/**
	 * Update an instruction status only if it currently has an expected state.
	 * 
	 * <p>
	 * This is equivalent to an atomic compare-and-set operation. The given
	 * instruction will be updated only if the instruction with
	 * {@code instructionId} exists and has the {@code expectedState} state.
	 * </p>
	 * 
	 * @param instructionId
	 *        the ID of the instruction to update the status for
	 * @param nodeId
	 *        the node ID
	 * @param expectedState
	 *        the expected state of the instruction
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        optional result parameters to include
	 * @return {@literal true} if the instruction was updated
	 * @since 1.2
	 */
	boolean compareAndUpdateInstructionState(Long instructionId, Long nodeId,
			InstructionState expectedState, InstructionState state, Map<String, ?> resultParameters);

	/**
	 * Find instructions in a given state that are older than a specific date
	 * and update their state to some other state.
	 * 
	 * @param currentState
	 *        the state of instructions to look for
	 * @param olderThanDate
	 *        only update instructions older than this date
	 * @param desiredState
	 *        the state to change the found instructions to
	 * @return The number of instructions deleted.
	 * @since 2.0
	 */
	long updateStaleInstructionsState(InstructionState currentState, Instant olderThanDate,
			InstructionState desiredState);

	/**
	 * API for querying for a filtered stream of {@link NodeInstruction}
	 * entities.
	 * 
	 * @param filter
	 *        the filter
	 * @param processor
	 *        the stream processor
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.2
	 */
	void findFilteredStream(InstructionFilter filter,
			FilteredResultsProcessor<NodeInstruction> processor) throws IOException;

	/**
	 * Update the state of instructions matching a search criteria.
	 * 
	 * <p>
	 * Only instructions that match the given criteria <b>and</b> are not
	 * already in the {@code desiredState} will be updated.
	 * </p>
	 * 
	 * @param userId
	 *        the instruction owner ID
	 * @param filter
	 *        the search criteria
	 * @param desiredState
	 *        the state to change the matching instructions to
	 * @return the instruction IDs that were updated
	 * @since 2.6
	 */
	Collection<Long> updateNodeInstructionsState(Long userId, InstructionFilter filter,
			InstructionState desiredState);
}
