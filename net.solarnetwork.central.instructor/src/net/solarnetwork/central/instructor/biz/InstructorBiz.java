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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.instructor.biz;

import java.util.List;
import java.util.Map;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * API for central instruction service.
 * 
 * @author matt
 * @version 1.2
 */
public interface InstructorBiz {

	/**
	 * Return any active instructions for a specific node. An instruction is
	 * considered <em>active</em> if it is in the
	 * {@link InstructionState#Queued} state.
	 * 
	 * @param node
	 *        the node to get active instructions for
	 * @return the instructions
	 */
	List<Instruction> getActiveInstructionsForNode(Long nodeId);

	/**
	 * Return any pending instructions for a specific node. An instruction is
	 * considered <em>pending</em> if it is in {@link InstructionState#Queued},
	 * {@link InstructionState#Received}, or {@link InstructionState#Executing}
	 * states.
	 * 
	 * @param node
	 *        the node to get pending instructions for
	 * @return the instructions
	 * @since 1.1
	 */
	List<Instruction> getPendingInstructionsForNode(Long nodeId);

	/**
	 * Queue an instruction for a specific node. The instruction will be put
	 * into the {@link InstructionState#Queued} state.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param instruction
	 *        the instruction
	 * @return the persisted instruction
	 */
	NodeInstruction queueInstruction(Long nodeId, Instruction instruction);

	/**
	 * Get a specific instruction.
	 * 
	 * @param instructionId
	 *        the instruction ID
	 * @return the found instruction, or <em>null</em> if not found
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

}
