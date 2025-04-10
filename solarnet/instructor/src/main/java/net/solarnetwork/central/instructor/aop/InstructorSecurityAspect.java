/* ==================================================================
 * InstructorSecurityAspect.java - Nov 27, 2012 8:57:43 AM
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

package net.solarnetwork.central.instructor.aop;

import java.util.List;
import java.util.Set;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationSupport;

/**
 * Security aspect for {@link InstructorBiz}.
 *
 * @author matt
 * @version 2.2
 */
@Aspect
@Component
public class InstructorSecurityAspect extends AuthorizationSupport {

	private final NodeInstructionDao nodeInstructionDao;

	/**
	 * Constructor.
	 *
	 * @param nodeOwnershipDao
	 *        the ownership DAO to use
	 * @param nodeInstructionDao
	 *        the instruction DAO to use
	 */
	public InstructorSecurityAspect(SolarNodeOwnershipDao nodeOwnershipDao,
			NodeInstructionDao nodeInstructionDao) {
		super(nodeOwnershipDao);
		this.nodeInstructionDao = nodeInstructionDao;
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.get*ForNode(..)) && args(nodeId)")
	public void instructionsForNode(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.get*ForNodes(..)) && args(nodeIds)")
	public void instructionsForNodes(Set<Long> nodeIds) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.queueInstruction(..)) && args(nodeId,..)")
	public void queueInstruction(Long nodeId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.queueInstructions(..)) && args(nodeIds,..)")
	public void queueInstructions(Set<Long> nodeIds) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.getInstruction(..)) && args(instructionId,..)")
	public void viewInstruction(Long instructionId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.getInstructions(..)) && args(instructionIds,..)")
	public void viewInstructions(Set<Long> instructionIds) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.updateInstructionState(..)) && args(instructionId,..)")
	public void updateInstructionState(Long instructionId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.*.updateInstructionsState(..)) && args(instructionIds,..)")
	public void updateInstructionsState(Set<Long> instructionIds) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.InstructorBiz.update*ForUser(..)) && args(userId,..)")
	public void updateInstructionsForUser(Long userId) {
	}

	@Pointcut("execution(* net.solarnetwork.central.instructor.biz.InstructorBiz.findFilteredNodeInstructions(..)) && args(filter,..)")
	public void findFilteredInstructions(InstructionFilter filter) {
	}

	/**
	 * Allow the current user (or current node) access to node instructions.
	 *
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before(value = "instructionsForNode(nodeId) || queueInstruction(nodeId)", argNames = "nodeId")
	public void instructionsForNodeCheck(Long nodeId) {
		if ( nodeId == null ) {
			return;
		}
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Allow the current user (or current node) access to node instructions.
	 *
	 * @param nodeIds
	 *        the IDs of the nodes to verify
	 */
	@Before(value = "instructionsForNodes(nodeIds) || queueInstructions(nodeIds)", argNames = "nodeIds")
	public void instructionsForNodesCheck(Set<Long> nodeIds) {
		if ( nodeIds == null ) {
			return;
		}
		for ( Long nodeId : nodeIds ) {
			instructionsForNodeCheck(nodeId);
		}
	}

	/**
	 * Allow the current user to filter instructions.
	 *
	 * @param filter
	 *        the filter
	 */
	@Before(value = "findFilteredInstructions(filter)", argNames = "filter")
	public void findFilteredInstructionsCheck(InstructionFilter filter) {
		if ( filter == null || !(filter.hasNodeIdCriteria() || filter.hasInstructionIdCriteria()) ) {
			log.warn("Access DENIED; no node ID provided");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		if ( filter.hasNodeIdCriteria() ) {
			for ( Long nodeId : filter.getNodeIds() ) {
				requireNodeWriteAccess(nodeId);
			}
		}
		if ( filter.hasInstructionIdCriteria() ) {
			for ( Long instrId : filter.getInstructionIds() ) {
				updateInstructionAccessCheck(instrId);
			}
		}
	}

	/**
	 * Allow the current user (or current node) access to viewing instructions
	 * by ID.
	 *
	 * @param instructionId
	 *        the instruction ID
	 * @param instruction
	 *        the instruction
	 */
	@AfterReturning(pointcut = "viewInstruction(instructionId)", returning = "instruction",
			argNames = "instructionId,instruction")
	public void viewInstructionAccessCheck(Long instructionId, NodeInstruction instruction) {
		if ( instructionId == null ) {
			return;
		}
		final Long nodeId = (instruction != null ? instruction.getNodeId() : null);
		if ( nodeId == null ) {
			return;
		}
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Allow the current user (or current node) access to viewing instructions
	 * by IDs.
	 *
	 * @param instructionIds
	 *        the instruction IDs
	 * @param instructions
	 *        the instructions
	 */
	@AfterReturning(pointcut = "viewInstructions(instructionIds)", returning = "instructions",
			argNames = "instructionIds,instructions")
	public void viewInstructionsAccessCheck(Set<Long> instructionIds,
			List<NodeInstruction> instructions) {
		if ( instructionIds == null || instructions == null ) {
			return;
		}
		for ( NodeInstruction instr : instructions ) {
			viewInstructionAccessCheck(instr.getNodeId(), instr);
		}
	}

	/**
	 * Allow the current user (or current node) access to updating instructions
	 * by ID.
	 *
	 * @param instructionId
	 *        the ID of the instruction being updated
	 */
	@Before(value = "updateInstructionState(instructionId)", argNames = "instructionId")
	public void updateInstructionAccessCheck(Long instructionId) {
		if ( instructionId == null ) {
			return;
		}
		final NodeInstruction instruction = nodeInstructionDao.get(instructionId);
		if ( instruction == null ) {
			return;
		}
		final Long nodeId = instruction.getNodeId();
		if ( nodeId == null ) {
			return;
		}
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Allow the current user (or current node) access to updating instructions
	 * by ID.
	 *
	 * @param instructionIds
	 *        the IDs of the instructions being updated
	 */
	@Before(value = "updateInstructionsState(instructionIds)", argNames = "instructionIds")
	public void updateInstructionsAccessCheck(Set<Long> instructionIds) {
		if ( instructionIds == null ) {
			return;
		}
		for ( Long instructionId : instructionIds ) {
			updateInstructionAccessCheck(instructionId);
		}
	}

	/**
	 * Allow the current user access to updating instructions in their account.
	 *
	 * @param userId
	 *        the user ID
	 */
	@Before(value = "updateInstructionsForUser(userId)", argNames = "userId")
	public void updateInstructionsForUserAccessCheck(Long userId) {
		if ( userId == null ) {
			return;
		}
		requireUserWriteAccess(userId);
	}

}
