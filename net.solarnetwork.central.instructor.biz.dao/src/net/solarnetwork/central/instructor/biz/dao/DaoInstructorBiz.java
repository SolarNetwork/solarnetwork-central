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
 */

package net.solarnetwork.central.instructor.biz.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;

/**
 * DAO based implementation of {@link InstructorBiz}.
 * 
 * @author matt
 * @version 1.7
 */
@Service
public class DaoInstructorBiz implements InstructorBiz {

	private final NodeInstructionDao nodeInstructionDao;
	private final List<NodeInstructionQueueHook> queueHooks;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Construct without queue hooks.
	 * 
	 * @param nodeInstructionDao
	 *        the DAO to use
	 */
	public DaoInstructorBiz(NodeInstructionDao nodeInstructionDao) {
		this(nodeInstructionDao, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeInstructionDao
	 *        the DAO to use
	 * @param queueHooks
	 *        the queue hooks to use (may be {@literal null}
	 */
	public DaoInstructorBiz(NodeInstructionDao nodeInstructionDao,
			List<NodeInstructionQueueHook> queueHooks) {
		super();
		this.nodeInstructionDao = nodeInstructionDao;
		this.queueHooks = (queueHooks != null ? queueHooks
				: Collections.<NodeInstructionQueueHook> emptyList());
	}

	private List<Instruction> asResultList(FilterResults<EntityMatch> matches) {
		List<Instruction> results = new ArrayList<Instruction>(matches.getReturnedResultCount());
		for ( EntityMatch match : matches.getResults() ) {
			if ( match instanceof Instruction ) {
				results.add((Instruction) match);
			} else {
				results.add(nodeInstructionDao.get(match.getId()));
			}
		}
		return results;
	}

	private List<NodeInstruction> asNodeInstructionList(FilterResults<EntityMatch> matches) {
		List<NodeInstruction> results = new ArrayList<NodeInstruction>(matches.getReturnedResultCount());
		for ( EntityMatch match : matches.getResults() ) {
			if ( match instanceof NodeInstruction ) {
				results.add((NodeInstruction) match);
			} else {
				results.add(nodeInstructionDao.get(match.getId()));
			}
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NodeInstruction getInstruction(Long instructionId) {
		return nodeInstructionDao.get(instructionId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<NodeInstruction> getInstructions(Set<Long> instructionIds) {
		Long[] ids = instructionIds.toArray(new Long[instructionIds.size()]);
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(ids);
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(filter, null, null, null);
		return asNodeInstructionList(matches);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Instruction> getActiveInstructionsForNode(Long nodeId) {
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(nodeId);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(filter, null, null, null);
		return asResultList(matches);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<NodeInstruction> getActiveInstructionsForNodes(Set<Long> nodeIds) {
		Long[] ids = nodeIds.toArray(new Long[nodeIds.size()]);
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(ids);
		filter.setState(InstructionState.Queued);
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(filter, null, null, null);
		return asNodeInstructionList(matches);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<Instruction> getPendingInstructionsForNode(Long nodeId) {
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(nodeId);
		filter.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(filter, null, null, null);
		return asResultList(matches);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public List<NodeInstruction> getPendingInstructionsForNodes(Set<Long> nodeIds) {
		Long[] ids = nodeIds.toArray(new Long[nodeIds.size()]);
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(ids);
		filter.setStateSet(EnumSet.of(InstructionState.Queued, InstructionState.Received,
				InstructionState.Executing));
		FilterResults<EntityMatch> matches = nodeInstructionDao.findFiltered(filter, null, null, null);
		return asNodeInstructionList(matches);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public NodeInstruction queueInstruction(Long nodeId, Instruction instruction) {
		log.debug("Received node {} instruction {}", nodeId, instruction.getTopic());
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
		if ( log.isTraceEnabled() ) {
			log.trace("Processing instruction {} with {} hooks", instr.getId(), queueHooks.size());
		}
		for ( NodeInstructionQueueHook hook : queueHooks ) {
			instr = hook.willQueueNodeInstruction(instr);
			if ( instr == null ) {
				return null;
			}
		}
		Long id = nodeInstructionDao.store(instr);
		for ( NodeInstructionQueueHook hook : queueHooks ) {
			hook.didQueueNodeInstruction(instr, id);
		}
		return nodeInstructionDao.get(id);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public List<NodeInstruction> queueInstructions(Set<Long> nodeIds, Instruction instruction) {
		List<NodeInstruction> results = new ArrayList<NodeInstruction>(nodeIds.size());
		for ( Long nodeId : nodeIds ) {
			NodeInstruction copy = new NodeInstruction(instruction.getTopic(),
					instruction.getInstructionDate(), nodeId);
			copy.setParameters(instruction.getParameters());
			NodeInstruction instr = queueInstruction(nodeId, copy);
			results.add(instr);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateInstructionState(Long instructionId, InstructionState state) {
		NodeInstruction instr = nodeInstructionDao.get(instructionId);
		if ( instr != null ) {
			if ( !state.equals(instr.getState()) ) {
				instr.setState(state);
				nodeInstructionDao.store(instr);
			}
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state) {
		for ( Long id : instructionIds ) {
			updateInstructionState(id, state);
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateInstructionState(Long instructionId, InstructionState state,
			Map<String, ?> resultParameters) {
		NodeInstruction instr = nodeInstructionDao.get(instructionId);
		if ( instr != null ) {
			if ( !state.equals(instr.getState()) ) {
				instr.setState(state);
				if ( resultParameters != null ) {
					Map<String, Object> params = instr.getResultParameters();
					if ( params == null ) {
						params = new LinkedHashMap<String, Object>();
					}
					params.putAll(resultParameters);
					instr.setResultParameters(params);
				}
				nodeInstructionDao.store(instr);
			}
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state,
			Map<Long, Map<String, ?>> resultParameters) {
		for ( Long id : instructionIds ) {
			Map<String, ?> params = (resultParameters != null ? resultParameters.get(id) : null);
			updateInstructionState(id, state, params);
		}
	}

}
