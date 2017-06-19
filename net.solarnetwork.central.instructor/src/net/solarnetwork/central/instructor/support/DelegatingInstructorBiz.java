/* ==================================================================
 * DelegatingInstructorBiz.java - Nov 27, 2012 7:37:48 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.support;

import java.util.List;
import java.util.Map;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;

/**
 * Delegates to another InstructorBiz, designed for AOP use.
 * 
 * @author matt
 * @version 1.2
 */
public class DelegatingInstructorBiz implements InstructorBiz {

	private final InstructorBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingInstructorBiz(InstructorBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public List<Instruction> getActiveInstructionsForNode(Long nodeId) {
		return delegate.getActiveInstructionsForNode(nodeId);
	}

	@Override
	public NodeInstruction queueInstruction(Long nodeId, Instruction instruction) {
		return delegate.queueInstruction(nodeId, instruction);
	}

	@Override
	public NodeInstruction getInstruction(Long instructionId) {
		return delegate.getInstruction(instructionId);
	}

	@Override
	public void updateInstructionState(Long instructionId, InstructionState state) {
		delegate.updateInstructionState(instructionId, state);
	}

	@Override
	public List<Instruction> getPendingInstructionsForNode(Long nodeId) {
		return delegate.getPendingInstructionsForNode(nodeId);
	}

	@Override
	public void updateInstructionState(Long instructionId, InstructionState state,
			Map<String, ?> resultParameters) {
		delegate.updateInstructionState(instructionId, state, resultParameters);
	}

}
