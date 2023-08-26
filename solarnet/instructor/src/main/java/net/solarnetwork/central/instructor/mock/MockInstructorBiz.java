/* ==================================================================
 * MockInstructorBiz.java - Mar 1, 2011 2:50:14 PM
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

package net.solarnetwork.central.instructor.mock;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Mock implementation of {@link InstructorBiz}.
 * 
 * @author matt
 * @version 2.1
 */
public class MockInstructorBiz implements InstructorBiz {

	private final AtomicLong counter = new AtomicLong(0);

	@Override
	public List<Instruction> getActiveInstructionsForNode(Long nodeId) {
		NodeInstruction instr = new NodeInstruction("Mock/Topic", Instant.now(), nodeId);
		instr.setId(counter.incrementAndGet());
		instr.addParameter("test.param.1", "One");
		instr.addParameter("test.param.2", String.valueOf(55));
		List<Instruction> result = new ArrayList<Instruction>();
		result.add(instr);
		return result;
	}

	@Override
	public List<Instruction> getPendingInstructionsForNode(Long nodeId) {
		return getActiveInstructionsForNode(nodeId);
	}

	@Override
	public NodeInstruction queueInstruction(Long nodeId, Instruction instruction) {
		NodeInstruction instr = new NodeInstruction(instruction.getTopic(),
				instruction.getInstructionDate(), nodeId);
		if ( instruction.getParameters() != null ) {
			for ( InstructionParameter param : instruction.getParameters() ) {
				instr.addParameter(param.getName(), param.getValue());
			}
		}
		instr.setId(counter.incrementAndGet());
		return instr;
	}

	@Override
	public void updateInstructionState(Long instructionId, InstructionState state) {
		// nothing to do here
	}

	@Override
	public void updateInstructionState(Long instructionId, InstructionState state,
			Map<String, ?> resultParameters) {
		// nothing to do here
	}

	@Override
	public NodeInstruction getInstruction(Long instructionId) {
		return null;
	}

	@Override
	public List<NodeInstruction> getActiveInstructionsForNodes(Set<Long> nodeIds) {
		return Collections.emptyList();
	}

	@Override
	public List<NodeInstruction> getPendingInstructionsForNodes(Set<Long> nodeIds) {
		return Collections.emptyList();
	}

	@Override
	public List<NodeInstruction> queueInstructions(Set<Long> nodeIds, Instruction instruction) {
		List<NodeInstruction> result = new ArrayList<NodeInstruction>(nodeIds.size());
		for ( Long nodeId : nodeIds ) {
			result.add(queueInstruction(nodeId, instruction));
		}
		return result;
	}

	@Override
	public List<NodeInstruction> getInstructions(Set<Long> instructionIds) {
		return Collections.emptyList();
	}

	@Override
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state) {
		// nothing to do here
	}

	@Override
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state,
			Map<Long, Map<String, ?>> resultParameters) {
		// nothing to do here
	}

	@Override
	public void findFilteredNodeInstructions(InstructionFilter filter,
			FilteredResultsProcessor<NodeInstruction> processor) throws IOException {
		// nothing to do here		
	}

}
