/* ==================================================================
 * DaoInstructorBizTests.java - 28/01/2021 5:06:49 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.biz.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.biz.NodeServiceAuditor;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.biz.dao.DaoInstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link DaoInstructorBiz} class.
 * 
 * @author matt
 * @version 1.3
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoInstructorBizTests {

	@Mock
	private NodeInstructionDao nodeInstructionDao;

	@Mock
	private NodeServiceAuditor auditor;

	@Captor
	private ArgumentCaptor<NodeInstruction> nodeInstructionCaptor;

	private List<NodeInstructionQueueHook> hooks;
	private DaoInstructorBiz biz;

	@BeforeEach
	public void setup() {
		hooks = new ArrayList<>();
		biz = new DaoInstructorBiz(nodeInstructionDao, hooks, auditor);
	}

	@Test
	public void addInstruction() {
		// GIVEN
		final Long nodeId = UUID.randomUUID().getLeastSignificantBits();
		final Long instrId = UUID.randomUUID().getLeastSignificantBits();

		final Instruction instr = new Instruction("foo", Instant.now());
		instr.addParameter("foo", "bar");

		given(nodeInstructionDao.save(any())).willReturn(instrId);

		NodeInstruction dbInstr = new NodeInstruction(instr.getTopic(), instr.getCreated(), nodeId);
		dbInstr.setState(InstructionState.Queued);
		dbInstr.setParameters(instr.getParameters());
		given(nodeInstructionDao.get(instrId)).willReturn(dbInstr);

		// WHEN
		NodeInstruction result = biz.queueInstruction(nodeId, instr);

		// THEN
		then(auditor).should().auditNodeService(nodeId, InstructorBiz.INSTRUCTION_ADDED_AUDIT_SERVICE,
				1);

		// @formatter:off
		then(nodeInstructionDao).should().save(nodeInstructionCaptor.capture());
		and.then(nodeInstructionCaptor.getValue())
			.as("Stored same topic")
			.returns(instr.getTopic(), from(NodeInstruction::getTopic))
			.as("Stored same date")
			.returns(instr.getCreated(), from(NodeInstruction::getCreated))
			.as("Stored same node ID")
			.returns(nodeId, from(NodeInstruction::getNodeId))
			.as("Stored same parameters")
			.returns(instr.getParameters(), from(NodeInstruction::getParameters))
			;
		
		and.then(result)
			.as("DAO result returned")
			.isSameAs(dbInstr)
			;
		// @formatter:on
	}

	@Test
	public void addInstruction_largeParameterValue() {
		// GIVEN
		final Long nodeId = UUID.randomUUID().getLeastSignificantBits();
		final Long instrId = UUID.randomUUID().getLeastSignificantBits();

		final Instruction instr = new Instruction("foo", Instant.now());
		final StringBuilder buf = new StringBuilder(800);
		for ( int i = 0; i < 800; i++ ) {
			buf.append((char) ('0' + (i % 10)));
		}
		instr.addParameter("foo", buf.toString());

		given(nodeInstructionDao.save(any())).willReturn(instrId);

		NodeInstruction dbInstr = new NodeInstruction(instr.getTopic(), instr.getCreated(), nodeId);
		dbInstr.setState(InstructionState.Queued);
		for ( int i = 0; i < buf.length(); i += biz.getMaxParamValueLength() ) {
			dbInstr.addParameter("foo",
					buf.substring(i, i + Math.min(biz.getMaxParamValueLength(), buf.length() - i)));
		}
		given(nodeInstructionDao.get(instrId)).willReturn(dbInstr);

		// WHEN
		NodeInstruction result = biz.queueInstruction(nodeId, instr);

		// THEN
		// @formatter:off
		then(auditor).should().auditNodeService(nodeId, InstructorBiz.INSTRUCTION_ADDED_AUDIT_SERVICE, 1);
		
		then(nodeInstructionDao).should().save(nodeInstructionCaptor.capture());
		and.then(nodeInstructionCaptor.getValue())
			.as("Stored same topic")
			.returns(instr.getTopic(), from(NodeInstruction::getTopic))
			.as("Stored same date")
			.returns(instr.getCreated(), from(NodeInstruction::getCreated))
			.as("Stored same node ID")
			.returns(nodeId, from(NodeInstruction::getNodeId))
			.as("Stored SPLIT parameters")
			.returns(dbInstr.getParameters(), from(NodeInstruction::getParameters))
			;
		
		and.then(result)
			.as("DAO result returned")
			.isSameAs(dbInstr)
			.extracting(NodeInstruction::getParams, map(String.class, String.class))
			.as("Auto-merge result same as initial value")
			.containsEntry("foo", buf.toString())
			;
		
		// @formatter:on
	}

	@Test
	public void updateInstructions_filter() {
		// GIVEN
		final Long userId = randomLong();

		final List<Long> updatedIds = List.of(randomLong(), randomLong());

		final SimpleInstructionFilter filter = new SimpleInstructionFilter();
		final InstructionState desiredState = InstructionState.Declined;

		given(nodeInstructionDao.updateNodeInstructionsState(eq(userId), same(filter), eq(desiredState)))
				.willReturn(updatedIds);

		// WHEN
		Collection<Long> result = biz.updateInstructionsStateForUser(userId, filter, desiredState);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(updatedIds)
			;
		// @formatter:on
	}

}
