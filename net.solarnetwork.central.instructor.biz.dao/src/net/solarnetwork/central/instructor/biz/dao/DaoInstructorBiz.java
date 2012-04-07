/* ==================================================================
 * DaoInstructorBiz.java - Sep 30, 2011 11:31:38 AM
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

package net.solarnetwork.central.instructor.biz.dao;

import java.util.ArrayList;
import java.util.List;

import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO based implementation of {@link InstructorBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
@Service
public class DaoInstructorBiz implements InstructorBiz {

	@Autowired private NodeInstructionDao nodeInstructionDao;
	
	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Instruction> getActiveInstructionsForNode(Long nodeId) {
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(nodeId);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(
				filter, null, null, null);
		List<Instruction> results = new ArrayList<Instruction>(matches.getReturnedResultCount());
		for ( EntityMatch match : matches.getResults() ) {
			if ( match instanceof Instruction ) {
				results.add((Instruction)match);
			} else {
				results.add(nodeInstructionDao.get(match.getId()));
			}
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NodeInstruction queueInstruction(Long nodeId, Instruction instruction) {
		NodeInstruction instr = new NodeInstruction(instruction.getTopic(),
				instruction.getInstructionDate(), nodeId);
		if ( instr.getInstructionDate() == null ) {
			instr.setInstructionDate(new DateTime());
		}
		instr.setState(InstructionState.Queued);
		if ( instruction.getParameters() != null ) {
			for ( InstructionParameter param : instruction.getParameters() ) {
				instr.addParameter(param.getName(), param.getValue());
			}
		}
		Long id = nodeInstructionDao.store(instr);
		return nodeInstructionDao.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateInstructionState(Long instructionId,
			InstructionState state) {
		NodeInstruction instr = nodeInstructionDao.get(instructionId);
		if ( instr != null ) {
			if ( !state.equals(instr.getState()) ) {
				instr.setState(state);
				nodeInstructionDao.store(instr);
			}
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Instruction getInstruction(Long instructionId) {
		return nodeInstructionDao.get(instructionId);
	}

}
