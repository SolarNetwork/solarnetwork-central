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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.biz.NodeServiceAuditor;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * DAO based implementation of {@link InstructorBiz}.
 *
 * @author matt
 * @version 2.5
 */
public class DaoInstructorBiz implements InstructorBiz {

	/**
	 * The default value for the {@code maxParamValueLength} property.
	 *
	 * @since 1.8
	 */
	public static final int DEFAULT_MAX_PARAM_VALUE_LENGTH = 256;

	/**
	 * A maximum number of filter results to return.
	 *
	 * @since 2.1
	 */
	public static final Integer MAX_FILTER_RESULTS = 2000;

	private final NodeInstructionDao nodeInstructionDao;
	private final List<NodeInstructionQueueHook> queueHooks;
	private final NodeServiceAuditor nodeServiceAuditor;
	private int maxParamValueLength = DEFAULT_MAX_PARAM_VALUE_LENGTH;

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
		this(nodeInstructionDao, queueHooks, null);
	}

	/**
	 * Constructor.
	 *
	 * @param nodeInstructionDao
	 *        the DAO to use
	 * @param queueHooks
	 *        the queue hooks to use (may be {@literal null}
	 * @param nodeServiceAuditor
	 *        the node service auditor to use (may be {@literal null})
	 * @since 2.2
	 */
	public DaoInstructorBiz(NodeInstructionDao nodeInstructionDao,
			List<NodeInstructionQueueHook> queueHooks, NodeServiceAuditor nodeServiceAuditor) {
		super();
		this.nodeInstructionDao = nodeInstructionDao;
		this.queueHooks = (queueHooks != null ? queueHooks : Collections.emptyList());
		this.nodeServiceAuditor = nodeServiceAuditor;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public NodeInstruction getInstruction(Long instructionId) {
		return nodeInstructionDao.get(instructionId);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public void findFilteredNodeInstructions(InstructionFilter filter,
			FilteredResultsProcessor<NodeInstruction> processor) throws IOException {
		nodeInstructionDao.findFilteredStream(filter, processor);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public NodeInstruction queueInstruction(Long nodeId, Instruction instruction) {
		log.debug("Received node {} instruction {}", nodeId, instruction.getTopic());
		NodeInstruction nodeInstruction = new NodeInstruction(instruction.getTopic(),
				instruction.getInstructionDate(), nodeId);
		final Instruction instr = nodeInstruction.getInstruction();
		if ( instr.getInstructionDate() == null ) {
			instr.setInstructionDate(Instant.now());
		}
		instr.setState(InstructionState.Queued);
		if ( instruction.getParameters() != null ) {
			for ( InstructionParameter param : instruction.getParameters() ) {
				if ( param == null || param.getName() == null || param.getName().isEmpty()
						|| param.getValue() == null ) {
					continue;
				}
				String v = param.getValue();
				while ( v.length() > maxParamValueLength ) {
					instr.addParameter(param.getName(), v.substring(0, maxParamValueLength));
					v = v.substring(maxParamValueLength);
				}
				if ( !v.isEmpty() ) {
					instr.addParameter(param.getName(), v);
				}
			}
		}
		if ( instruction.getExpirationDate() != null ) {
			instr.setExpirationDate(instruction.getExpirationDate());
		}
		if ( log.isTraceEnabled() ) {
			log.trace("Processing instruction {} with {} hooks", instr.getId(), queueHooks.size());
		}
		if ( nodeServiceAuditor != null ) {
			nodeServiceAuditor.auditNodeService(nodeId, INSTRUCTION_ADDED_AUDIT_SERVICE, 1);
		}
		for ( NodeInstructionQueueHook hook : queueHooks ) {
			nodeInstruction = hook.willQueueNodeInstruction(nodeInstruction);
			if ( nodeInstruction == null ) {
				return null;
			}
		}
		Long id = nodeInstructionDao.save(nodeInstruction);
		for ( NodeInstructionQueueHook hook : queueHooks ) {
			hook.didQueueNodeInstruction(nodeInstruction, id);
		}
		return nodeInstructionDao.get(id);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<NodeInstruction> queueInstructions(Set<Long> nodeIds, Instruction instruction) {
		List<NodeInstruction> results = new ArrayList<>(nodeIds.size());
		for ( Long nodeId : nodeIds ) {
			NodeInstruction instr = queueInstruction(nodeId, instruction);
			results.add(instr);
		}
		return results;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateInstructionState(Long instructionId, InstructionState state) {
		final NodeInstruction nodeInstruction = nodeInstructionDao.get(instructionId);
		if ( nodeInstruction == null ) {
			return;
		}
		final Instruction instr = nodeInstruction.getInstruction();
		if ( instr == null ) {
			return;
		}
		if ( !state.equals(instr.getState()) ) {
			instr.setState(state);
			nodeInstructionDao.save(nodeInstruction);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state) {
		for ( Long id : instructionIds ) {
			updateInstructionState(id, state);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateInstructionState(Long instructionId, InstructionState state,
			Map<String, ?> resultParameters) {
		final NodeInstruction nodeInstruction = nodeInstructionDao.get(instructionId);
		if ( nodeInstruction == null ) {
			return;
		}
		final Instruction instr = nodeInstruction.getInstruction();
		if ( instr == null ) {
			return;
		}
		if ( !state.equals(instr.getState()) ) {
			instr.setState(state);
			if ( resultParameters != null ) {
				Map<String, Object> params = instr.getResultParameters();
				if ( params == null ) {
					params = new LinkedHashMap<>();
				}
				params.putAll(resultParameters);
				instr.setResultParameters(params);
			}
			nodeInstructionDao.save(nodeInstruction);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public void updateInstructionsState(Set<Long> instructionIds, InstructionState state,
			Map<Long, Map<String, ?>> resultParameters) {
		for ( Long id : instructionIds ) {
			Map<String, ?> params = (resultParameters != null ? resultParameters.get(id) : null);
			updateInstructionState(id, state, params);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Collection<Long> updateInstructionsStateForUser(Long userId, InstructionFilter filter,
			InstructionState state) {
		return nodeInstructionDao.updateNodeInstructionsState(requireNonNullArgument(userId, "userId"),
				filter, state);
	}

	/**
	 * Get the maximum parameter value length.
	 *
	 * @return the length; defaults to {@link #DEFAULT_MAX_PARAM_VALUE_LENGTH}
	 * @since 1.8
	 */
	public int getMaxParamValueLength() {
		return maxParamValueLength;
	}

	/**
	 * Set the maximum parameter value length.
	 *
	 * @param maxParamValueLength
	 *        the length to set
	 * @throws IllegalArgumentException
	 *         if {@code maxParamValueLength} is 0 or less
	 * @since 1.8
	 */
	public void setMaxParamValueLength(int maxParamValueLength) {
		if ( maxParamValueLength < 1 ) {
			throw new IllegalArgumentException("The maxParamValueLength value must be greater than 0.");
		}
		this.maxParamValueLength = maxParamValueLength;
	}

}
