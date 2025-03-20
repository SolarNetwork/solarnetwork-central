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
		NodeInstruction src = new NodeInstruction(randomString(), instrDate, randomLong(),
				instrDate.plusSeconds(1));
		src.setState(InstructionState.Completed);
		src.setStatusDate(instrDate.plusMillis(1));
		src.setParams(Map.of("foo", randomString()));
		src.setResultParameters(Map.of("out", randomString()));

		// WHEN
		NodeInstruction result = src.clone();

		// THEN
		// @formatter:off
		then(result)
			.as("Topic copied")
			.returns(src.getTopic(), from(NodeInstruction::getTopic))
			.as("Instruction date copied")
			.returns(src.getInstructionDate(), from(NodeInstruction::getInstructionDate))
			.as("State copied")
			.returns(src.getState(), from(NodeInstruction::getState))
			.as("Status date copied")
			.returns(src.getStatusDate(), from(NodeInstruction::getStatusDate))
			.as("Instruction parameters copied")
			.returns(src.getParameters(), from(NodeInstruction::getParameters))
			.as("Instruction date copied")
			.returns(src.getResultParameters(), from(NodeInstruction::getResultParameters))
			.as("Node ID copied")
			.returns(src.getNodeId(), from(NodeInstruction::getNodeId))
			.as("Expiration date copied")
			.returns(src.getExpirationDate(), from(NodeInstruction::getExpirationDate))
			;
		// @formatter:on
	}

	@Test
	public void copyConstructor_ok() {
		// GIVEN
		var instrDate = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		NodeInstruction src = new NodeInstruction(randomString(), instrDate, randomLong(),
				instrDate.plusSeconds(1));
		src.setState(InstructionState.Completed);
		src.setStatusDate(instrDate.plusMillis(1));
		src.setParams(Map.of("foo", randomString()));
		src.setResultParameters(Map.of("out", randomString()));

		// WHEN
		NodeInstruction result = new NodeInstruction(src);

		// THEN
		// @formatter:off
		then(result)
			.as("Topic copied")
			.returns(src.getTopic(), from(NodeInstruction::getTopic))
			.as("Instruction date copied")
			.returns(src.getInstructionDate(), from(NodeInstruction::getInstructionDate))
			.as("State copied")
			.returns(src.getState(), from(NodeInstruction::getState))
			.as("Status date copied")
			.returns(src.getStatusDate(), from(NodeInstruction::getStatusDate))
			.as("Instruction parameters copied")
			.returns(src.getParameters(), from(NodeInstruction::getParameters))
			.as("Instruction date copied")
			.returns(src.getResultParameters(), from(NodeInstruction::getResultParameters))
			.as("Node ID copied")
			.returns(src.getNodeId(), from(NodeInstruction::getNodeId))
			.as("Expiration date copied")
			.returns(src.getExpirationDate(), from(NodeInstruction::getExpirationDate))
			;
		// @formatter:on
	}

}
