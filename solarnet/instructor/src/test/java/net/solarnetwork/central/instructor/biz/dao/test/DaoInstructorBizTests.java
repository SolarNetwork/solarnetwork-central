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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.biz.NodeServiceAuditor;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.biz.dao.DaoInstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link DaoInstructorBiz} class.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoInstructorBizTests {

	private NodeInstructionDao nodeInstructionDao;
	private List<NodeInstructionQueueHook> hooks;
	private NodeServiceAuditor auditor;
	private DaoInstructorBiz biz;

	@Before
	public void setup() {
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);
		hooks = new ArrayList<>();
		auditor = EasyMock.createMock(NodeServiceAuditor.class);
		biz = new DaoInstructorBiz(nodeInstructionDao, hooks, auditor);
	}

	@After
	public void teardown() {
		EasyMock.verify(nodeInstructionDao, auditor);
	}

	private void replayAll() {
		EasyMock.replay(nodeInstructionDao, auditor);
	}

	@Test
	public void addInstruction() {
		// GIVEN
		final Long nodeId = UUID.randomUUID().getLeastSignificantBits();
		final Long instrId = UUID.randomUUID().getLeastSignificantBits();

		final Instruction instr = new Instruction("foo", Instant.now());
		instr.addParameter("foo", "bar");

		Capture<NodeInstruction> nodeInstructionCaptor = new Capture<>();
		expect(nodeInstructionDao.store(capture(nodeInstructionCaptor))).andReturn(instrId);

		NodeInstruction dbInstr = new NodeInstruction(instr.getTopic(), instr.getCreated(), nodeId);
		dbInstr.setState(InstructionState.Queued);
		dbInstr.setParameters(instr.getParameters());
		expect(nodeInstructionDao.get(instrId)).andReturn(dbInstr);

		auditor.auditNodeService(nodeId, InstructorBiz.INSTRUCTION_ADDED_AUDIT_SERVICE, 1);

		// WHEN
		replayAll();
		NodeInstruction result = biz.queueInstruction(nodeId, instr);

		// THEN
		assertThat("Stored same topic", nodeInstructionCaptor.getValue().getTopic(),
				equalTo(instr.getTopic()));
		assertThat("Stored same date", nodeInstructionCaptor.getValue().getCreated(),
				equalTo(instr.getCreated()));
		assertThat("Stored same node ID", nodeInstructionCaptor.getValue().getNodeId(), equalTo(nodeId));
		assertThat("Stored same parameters", nodeInstructionCaptor.getValue().getParameters(),
				equalTo(instr.getParameters()));

		assertThat("Returned DB result", result, sameInstance(dbInstr));
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

		Capture<NodeInstruction> nodeInstructionCaptor = new Capture<>();
		expect(nodeInstructionDao.store(capture(nodeInstructionCaptor))).andReturn(instrId);

		NodeInstruction dbInstr = new NodeInstruction(instr.getTopic(), instr.getCreated(), nodeId);
		dbInstr.setState(InstructionState.Queued);
		for ( int i = 0; i < buf.length(); i += biz.getMaxParamValueLength() ) {
			dbInstr.addParameter("foo",
					buf.substring(i, i + Math.min(biz.getMaxParamValueLength(), buf.length() - i)));
		}
		expect(nodeInstructionDao.get(instrId)).andReturn(dbInstr);

		auditor.auditNodeService(nodeId, InstructorBiz.INSTRUCTION_ADDED_AUDIT_SERVICE, 1);

		// WHEN
		replayAll();
		NodeInstruction result = biz.queueInstruction(nodeId, instr);

		// THEN
		assertThat("Stored same topic", nodeInstructionCaptor.getValue().getTopic(),
				equalTo(instr.getTopic()));
		assertThat("Stored same date", nodeInstructionCaptor.getValue().getCreated(),
				equalTo(instr.getCreated()));
		assertThat("Stored same node ID", nodeInstructionCaptor.getValue().getNodeId(), equalTo(nodeId));

		assertThat("Stored SPLIT parameters", nodeInstructionCaptor.getValue().getParameters(),
				equalTo(dbInstr.getParameters()));

		assertThat("Returned DB result", result, sameInstance(dbInstr));
		assertThat("Auto-merge result same as initial value", dbInstr.getParams(),
				hasEntry("foo", buf.toString()));
	}

}
