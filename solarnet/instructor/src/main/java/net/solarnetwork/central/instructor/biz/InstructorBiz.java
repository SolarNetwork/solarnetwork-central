/* ==================================================================
 * InstructorBiz.java - Mar 1, 2011 11:20:40 AM
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

package net.solarnetwork.central.instructor.biz;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * API for central instruction service.
 * 
 * @author matt
 * @version 2.1
 */
public interface InstructorBiz {

	/**
	 * The node audit service name for instruction added.
	 * 
	 * @since 1.5
	 */
	String INSTRUCTION_ADDED_AUDIT_SERVICE = "inst";

	/**
	 * API for querying for a filtered set of node instructions, streaming the
	 * results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param processor
	 *        the processor for the results
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.4
	 */
	void findFilteredNodeInstructions(InstructionFilter filter,
			FilteredResultsProcessor<NodeInstruction> processor) throws IOException;

	/**
	 * Queue an instruction for a specific node. The instruction will be put
	 * into the {@link InstructionState#Queued} state.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param instruction
	 *        the instruction
	 * @return the persisted instruction, or {@literal null} if not accepted
	 */
	NodeInstruction queueInstruction(Long nodeId, Instruction instruction);

	/**
	 * Queue an instruction for multiple nodes. The instruction will be put into
	 * the {@link InstructionState#Queued} state.
	 * 
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param instruction
	 *        the instruction
	 * @return the persisted instructions, in iteration order of {@code nodeIds}
	 * @since 1.3
	 */
	List<NodeInstruction> queueInstructions(Set<Long> nodeIds, Instruction instruction);

	/**
	 * Get a specific instruction.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @return the found instruction, or {@literal null} if not found
	 */
	NodeInstruction getInstruction(Long instructionId);

	/**
	 * Update the state of a specific instruction.
	 * 
	 * <p>
	 * As an instruction is processed, for example by a node, the state should
	 * be updated by that processor.
	 * </p>
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param state
	 *        the new state
	 */
	void updateInstructionState(Long instructionId, InstructionState state);

	/**
	 * Update the state of a set of instructions.
	 * 
	 * <p>
	 * As an instruction is processed, for example by a node, the state should
	 * be updated by that processor.
	 * </p>
	 * 
	 * @param instructionIds
	 *        the instruction IDs to update
	 * @param state
	 *        the new state
	 * @since 1.3
	 */
	void updateInstructionsState(Set<Long> instructionIds, InstructionState state);

	/**
	 * Update the state of a set of instructions matching search criteria.
	 * 
	 * @param userId
	 *        the owner user ID
	 * @param filter
	 *        the search criteria to match
	 * @param state
	 *        the new state to update matching instructions to
	 * @return the IDs of all updated instructions
	 * @since 2.1
	 */
	Collection<Long> updateInstructionsStateForUser(Long userId, InstructionFilter filter,
			InstructionState state);

	/**
	 * Update the state of a specific instruction.
	 * 
	 * <p>
	 * As an instruction is processed, for example by a node, the state should
	 * be updated by that processor.
	 * </p>
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        optional result parameters to include
	 * @since 1.2
	 */
	void updateInstructionState(Long instructionId, InstructionState state,
			Map<String, ?> resultParameters);

	/**
	 * Update the state of a specific instruction.
	 * 
	 * <p>
	 * As an instruction is processed, for example by a node, the state should
	 * be updated by that processor.
	 * </p>
	 * 
	 * @param instructionIds
	 *        the instruction IDs
	 * @param state
	 *        the new state
	 * @param resultParameters
	 *        optional result parameters to include, with top level instruction
	 *        ID keys and associated result parameter values
	 * @since 1.3
	 */
	void updateInstructionsState(Set<Long> instructionIds, InstructionState state,
			Map<Long, Map<String, ?>> resultParameters);

	/**
	 * Create a result parameter map for an error message and/or code.
	 * 
	 * @param message
	 *        the message
	 * @param code
	 *        the code
	 * @return the map, never {@literal null}
	 * @since 1.6
	 */
	static Map<String, Object> createErrorResultParameters(String message, String code) {
		Map<String, Object> result = new LinkedHashMap<>(2);
		if ( message != null && !message.isEmpty() ) {
			result.put(InstructionStatus.MESSAGE_RESULT_PARAM, message);
		}
		if ( code != null && !code.isEmpty() ) {
			result.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, code);
		}
		return result;
	}

}
