/* ==================================================================
 * InstructorBizSecurityContract.java - 22/09/2026 2:19:25 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.aop.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.Set;
import java.util.function.Consumer;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Security contract for {@link InstructorBiz}.
 *
 * <p>
 * The contract is enforced by {@code InstructorSecurityAspect}. Instructions
 * require write access to their nodes, and updating instructions by ID
 * requires write access to the node of each instruction, which the aspect
 * looks up. The proxy must include a {@link NodeInstructionDao} mock.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class InstructorBizSecurityContract {

	private InstructorBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<InstructorBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final Long instructionId = randomLong();
		final NodeInstruction instruction = instruction(instructionId, a.privateNodeId());
		final Consumer<SecuredProxy<InstructorBiz>> instructionExists = p -> given(
				p.mock(NodeInstructionDao.class).get(instructionId)).willReturn(instruction);
		final Set<Long> bothNodes = Set.of(a.privateNodeId(), b.privateNodeId());

		// @formatter:off
		return SecurityContract.forApi(InstructorBiz.class, tenants)
				.nodeWrite(biz -> biz.findFilteredNodeInstructions(nodeFilter(a.privateNodeId()), null))
				.allowing(biz -> biz.findFilteredNodeInstructions(
						nodeFilter(a.privateNodeId(), b.privateNodeId()), null))
					.as("other user node")
				.nodeWrite(biz -> biz.findFilteredNodeInstructions(instructionFilter(instructionId),
						null))
					.given(instructionExists)
					.as("instruction")
				.allowing(biz -> biz.findFilteredNodeInstructions(new SimpleInstructionFilter(), null))
					.as("no node or instruction")
				.nodeWrite(biz -> biz.queueInstruction(a.privateNodeId(), newInstruction()))
				.nodeWrite(biz -> biz.queueInstructions(Set.of(a.privateNodeId()), newInstruction()))
				.allowing(biz -> biz.queueInstructions(bothNodes, newInstruction()))
					.as("other user node")
				.nodeWrite(biz -> biz.getInstruction(instructionId))
					.given(p -> given(p.target().getInstruction(instructionId)).willReturn(instruction))
					.targetInvokedOnDeny()
				.nodeWrite(biz -> biz.updateInstructionState(instructionId, InstructionState.Declined))
					.given(instructionExists)
				.nodeWrite(biz -> biz.updateInstructionState(instructionId, InstructionState.Declined,
						null))
					.given(instructionExists)
				.nodeWrite(biz -> biz.updateInstructionsState(Set.of(instructionId),
						InstructionState.Declined))
					.given(instructionExists)
				.nodeWrite(biz -> biz.updateInstructionsState(Set.of(instructionId),
						InstructionState.Declined, null))
					.given(instructionExists)
				.userWrite(biz -> biz.updateInstructionsStateForUser(a.userId(),
						new SimpleInstructionFilter(), InstructionState.Declined))
				.build();
		// @formatter:on
	}

	private static Instruction newInstruction() {
		return new Instruction("Test", Instant.now());
	}

	private static NodeInstruction instruction(Long instructionId, Long nodeId) {
		final NodeInstruction instruction = new NodeInstruction("Test", Instant.now(), nodeId);
		instruction.setId(instructionId);
		return instruction;
	}

	private static SimpleInstructionFilter nodeFilter(Long... nodeIds) {
		final SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static SimpleInstructionFilter instructionFilter(Long... instructionIds) {
		final SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(instructionIds);
		return filter;
	}

}
