/* ==================================================================
 * InstructionTests.java - 27/01/2021 4:59:33 PM
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

package net.solarnetwork.central.instructor.domain.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link Instruction} class.
 * 
 * @author matt
 * @version 1.1
 */
public class InstructionTests {

	@Test
	public void setParams() {
		// GIVEN
		Instruction instr = new Instruction("test", Instant.now());

		// WHEN
		Map<String, String> params = new LinkedHashMap<>(2);
		params.put("foo", "bar");
		params.put("bar", "bam");
		instr.setParams(params);

		// THEN
		// @formatter:off
		then(instr.getParameters())
			.as("InstructionParameter instances created")
			.hasSize(2)
			.satisfies(list -> {
				then(list).element(0)
					.returns("foo", from(InstructionParameter::getName))
					.returns("bar", from(InstructionParameter::getValue))
					;
				then(list).element(1)
					.returns("bar", from(InstructionParameter::getName))
					.returns("bam", from(InstructionParameter::getValue))
					;
			})
			;
	}

	@Test
	public void getParams() {
		// GIVEN
		Instruction instr = new Instruction("test", Instant.now());
		instr.addParameter("foo", "bar");
		instr.addParameter("bar", "bam");

		// WHEN
		Map<String, String> params = instr.getParams();

		// THEN
		// @formatter:off
		then(params)
			.as("Parameters available")
			.containsEntry("foo", "bar")
			.containsEntry("bar", "bam")
			;
		// @formatter:on
	}

	@Test
	public void getParams_multiKey() {
		// GIVEN
		Instruction instr = new Instruction("test", Instant.now());
		instr.addParameter("foo", "bar");
		instr.addParameter("foo", "bam");

		// WHEN
		Map<String, String> params = instr.getParams();

		// THEN
		// @formatter:off
		then(params)
			.as("Multiple parameters for same key merged")
			.containsEntry("foo", "barbam")
			;
		// @formatter:on
	}

	@Test
	public void copy() {
		// GIVEN
		Instruction instr = new Instruction("test", Instant.now());
		instr.setId(randomLong());
		instr.setCreated(instr.getInstructionDate().plusSeconds(1L));
		instr.setExpirationDate(instr.getInstructionDate().plusSeconds(2L));
		instr.setStatusDate(instr.getInstructionDate().plusSeconds(3L));
		instr.setState(InstructionState.Completed);
		instr.addParameter("foo", "bar");
		instr.addParameter("foo", "bam");
		instr.setResultParameters(Map.of("foo", "bar"));

		// WHEN
		Instruction result = new Instruction(instr);

		// THEN
		// @formatter:off
		then(result)
			.as("Topic copied")
			.returns(instr.getTopic(), from(Instruction::getTopic))
			.as("Instruction date copied")
			.returns(instr.getInstructionDate(), from(Instruction::getInstructionDate))
			.as("ID copied")
			.returns(instr.getId(), from(Instruction::getId))
			.as("Created copied")
			.returns(instr.getCreated(), from(Instruction::getCreated))
			.as("Expiration date copied")
			.returns(instr.getExpirationDate(), from(Instruction::getExpirationDate))
			.as("Status date copied")
			.returns(instr.getStatusDate(), from(Instruction::getStatusDate))
			.as("State copied")
			.returns(instr.getState(), from(Instruction::getState))
			;

		then(result.getParameters())
			.as("Parameters list copied")
			.isEqualTo(instr.getParameters())
			.as("List is new instance")
			.isNotSameAs(instr.getParameters())
			;
		
		then(result.getResultParameters())
			.as("Result parameters list copied")
			.isEqualTo(instr.getResultParameters())
			.as("Map is new instance")
			.isNotSameAs(instr.getResultParameters())
			;
		// @formatter:on
	}

}
