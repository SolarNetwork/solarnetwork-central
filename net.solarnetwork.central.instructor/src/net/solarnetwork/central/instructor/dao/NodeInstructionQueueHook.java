/* ==================================================================
 * InstructorQueueHook.java - 12/06/2018 6:42:38 AM
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

package net.solarnetwork.central.instructor.dao;

import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * API for a "hook" into queuing node instructions.
 * 
 * <p>
 * It is expected that implementations of {@link InstructorBiz} can make use of
 * this API if also using {@link NodeInstructionDao} for persistence.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
public interface NodeInstructionQueueHook {

	/**
	 * Invoked when a node instruction is to be queued.
	 * 
	 * <p>
	 * This hook allows inspecting and/or modifying an instruction, as well as
	 * preventing the instruction from being enqueued by returning
	 * {@literal null}.
	 * </p>
	 * 
	 * @param instruction
	 *        the instruction to enqueue
	 * @return the actual instruction to enqueue, which can be the same
	 *         instruction passed in or a new instance, or {@literal null} to
	 *         <b>not</b> enqueue the instruction
	 */
	NodeInstruction willQueueNodeInstruction(NodeInstruction instruction);

	/**
	 * Invoked after a node instruction has been queued.
	 * 
	 * @param instruction
	 *        the instruction to enqueue
	 * @param instructionId
	 *        the ID of the enqueued instruction
	 */
	void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId);

}
