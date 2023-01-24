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
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;

/**
 * API for central instruction service.
 * 
 * @author matt
 * @version 1.5
 */
public interface InstructorBiz {

	/**
	 * The node audit service name for instruction added.
	 * 
	 * @since 1.5
	 */
	String INSTRUCTION_ADDED_AUDIT_SERVICE = "inst";

	/**
	 * Return any active instructions for a specific node.
	 * 
	 * <p>
	 * An instruction is considered <em>active</em> if it is in the
	 * {@link InstructionState#Queued} state.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node to get active instructions for
	 * @return the instructions
	 */
	List<Instruction> getActiveInstructionsForNode(Long nodeId);

	/**
	 * Return any active instructions for a set of nodes.
	 * 
	 * <p>
	 * An instruction is considered <em>active</em> if it is in the
	 * {@link InstructionState#Queued} state.
	 * </p>
	 * 
	 * @param nodeIds
	 *        the IDs of the nodes to get active instructions for
	 * @return the instructions
	 * @since 1.3
	 */
	List<NodeInstruction> getActiveInstructionsForNodes(Set<Long> nodeIds);

	/**
	 * Return any pending instructions for a specific node.
	 * 
	 * <p>
	 * An instruction is considered <em>pending</em> if it is in
	 * {@link InstructionState#Queued}, {@link InstructionState#Received}, or
	 * {@link InstructionState#Executing} states.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node to get pending instructions for
	 * @return the instructions
	 * @since 1.1
	 */
	List<Instruction> getPendingInstructionsForNode(Long nodeId);

	/**
	 * Return any pending instructions for a set of nodes.
	 * 
	 * <p>
	 * An instruction is considered <em>pending</em> if it is in
	 * {@link InstructionState#Queued}, {@link InstructionState#Received}, or
	 * {@link InstructionState#Executing} states.
	 * </p>
	 * 
	 * @param nodeIds
	 *        the IDs of the nodes to get pending instructions for
	 * @return the instructions
	 * @since 1.3
	 */
	List<NodeInstruction> getPendingInstructionsForNodes(Set<Long> nodeIds);

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
	 * Get a set of instructions.
	 * 
	 * @param instructionIds
	 *        the instruction IDs to fetch
	 * @return the found instructions, or {@literal null} if not found
	 * @since 1.3
	 */
	List<NodeInstruction> getInstructions(Set<Long> instructionIds);

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

}
