/* ==================================================================
 * NodeInstructionTests.java - 20/03/2025 10:48:23 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link NodeInstruction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionTests {

	@Test
	public void clone_ok() {
		// GIVEN
		var instrDate = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		NodeInstruction ni = new NodeInstruction(randomString(), instrDate, randomLong(),
				instrDate.plusSeconds(1));
		ni.getInstruction().setState(InstructionState.Completed);
		ni.getInstruction().setStatusDate(instrDate.plusMillis(1));
		ni.getInstruction().setParams(Map.of("foo", randomString()));
		ni.getInstruction().setResultParameters(Map.of("out", randomString()));

		// WHEN
		NodeInstruction result = ni.clone();

		// THEN
		final Instruction src = result.getInstruction();
		// @formatter:off
		then(result)
			.as("Node ID copied")
			.returns(ni.getNodeId(), from(NodeInstruction::getNodeId))
			.extracting(NodeInstruction::getInstruction)
			.as("Topic copied")
			.returns(src.getTopic(), from(Instruction::getTopic))
			.as("Instruction date copied")
			.returns(src.getInstructionDate(), from(Instruction::getInstructionDate))
			.as("State copied")
			.returns(src.getState(), from(Instruction::getState))
			.as("Status date copied")
			.returns(src.getStatusDate(), from(Instruction::getStatusDate))
			.as("Instruction parameters copied")
			.returns(src.getParameters(), from(Instruction::getParameters))
			.as("Instruction date copied")
			.returns(src.getResultParameters(), from(Instruction::getResultParameters))
			.as("Expiration date copied")
			.returns(src.getExpirationDate(), from(Instruction::getExpirationDate))
			;
		// @formatter:on
	}

	@Test
	public void copyConstructor_ok() {
		// GIVEN
		var instrDate = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		NodeInstruction ni = new NodeInstruction(randomString(), instrDate, randomLong(),
				instrDate.plusSeconds(1));
		ni.getInstruction().setState(InstructionState.Completed);
		ni.getInstruction().setStatusDate(instrDate.plusMillis(1));
		ni.getInstruction().setParams(Map.of("foo", randomString()));
		ni.getInstruction().setResultParameters(Map.of("out", randomString()));

		// WHEN
		NodeInstruction result = new NodeInstruction(ni);

		// THEN
		final Instruction src = result.getInstruction();
		// @formatter:off
		then(result)
			.as("Node ID copied")
			.returns(ni.getNodeId(), from(NodeInstruction::getNodeId))
			.extracting(NodeInstruction::getInstruction)
			.as("Topic copied")
			.returns(src.getTopic(), from(Instruction::getTopic))
			.as("Instruction date copied")
			.returns(src.getInstructionDate(), from(Instruction::getInstructionDate))
			.as("State copied")
			.returns(src.getState(), from(Instruction::getState))
			.as("Status date copied")
			.returns(src.getStatusDate(), from(Instruction::getStatusDate))
			.as("Instruction parameters copied")
			.returns(src.getParameters(), from(Instruction::getParameters))
			.as("Instruction date copied")
			.returns(src.getResultParameters(), from(Instruction::getResultParameters))
			.as("Expiration date copied")
			.returns(src.getExpirationDate(), from(Instruction::getExpirationDate))
			;
		// @formatter:on
	}

}
