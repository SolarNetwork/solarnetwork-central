/* ==================================================================
 * NodeInstructionDeserializerTests.java - 31/05/2025 9:36:21 am
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

package net.solarnetwork.central.instructor.support.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.NodeInstructionDeserializer;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link NodeInstructionDeserializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionDeserializerTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MICROSECONDS.toNanos(234567))
			.toInstant(ZoneOffset.UTC);
	private static final String TEST_DATE_STRING = "2021-08-11 16:45:01.234567Z";

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = JsonUtils.newObjectMapper();
		SimpleModule mod = new SimpleModule("Test");
		mod.addDeserializer(NodeInstruction.class, NodeInstructionDeserializer.INSTANCE);
		m.registerModule(mod);
		return m;
	}

	@BeforeEach
	public void setup() {
		mapper = createObjectMapper();
	}

	@Test
	public void deserialize_typical() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));

		// WHEN
		// @formatter:off
		final String json = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"parameters\":["
					 +"{\"name\":\"a\",\"value\":\"" + instr.getInstruction().getParameters().get(0).getValue() +"\"}"
					+",{\"name\":\"b\",\"value\":\"" + instr.getInstruction().getParameters().get(1).getValue() +"\"}"
				+ "]}";
		// @formatter:on

		NodeInstruction result = mapper.readValue(json, NodeInstruction.class);

		// THEN
		// @formatter:off
		then(result)
			.as("JSON parsed into instance")
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(instr)
			;
		// @formatter:on
	}

	@Test
	public void deserialize_paramsMap() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));

		// WHEN
		// @formatter:off
		final String json = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"params\":{"
					 +"\"a\":\"" + instr.getInstruction().getParameters().get(0).getValue() +"\","
					 +"\"b\":\"" + instr.getInstruction().getParameters().get(1).getValue() +"\""
				+ "}}";
		// @formatter:on

		NodeInstruction result = mapper.readValue(json, NodeInstruction.class);

		// THEN
		// @formatter:off
		then(result)
			.as("JSON parsed into instance")
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(instr)
			;
		// @formatter:on
	}

	@Test
	public void deserialize_withResultParameters() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setResultParametersJson("{\"message\":\"Hello\"}");

		// WHEN
		// @formatter:off
		final String json = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"resultParameters\":" +instr.getInstruction().getResultParametersJson()
				+ "}";
		// @formatter:on

		NodeInstruction result = mapper.readValue(json, NodeInstruction.class);

		// THEN
		// @formatter:off
		then(result)
			.as("JSON parsed into instance")
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(instr)
			;
		// @formatter:on
	}

	@Test
	public void deserialize_withStatusDate() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setStatusDate(TEST_DATE);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));

		// WHEN
		// @formatter:off
		final String json = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"statusDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"parameters\":["
					 +"{\"name\":\"a\",\"value\":\"" + instr.getInstruction().getParameters().get(0).getValue() +"\"}"
					+",{\"name\":\"b\",\"value\":\"" + instr.getInstruction().getParameters().get(1).getValue() +"\"}"
				+ "]}";
		// @formatter:on

		NodeInstruction result = mapper.readValue(json, NodeInstruction.class);

		// THEN
		// THEN
		// @formatter:off
		then(result)
			.as("JSON parsed into instance")
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(instr)
			;
		// @formatter:on
	}

	@Test
	public void deserialize_withExpirationDate() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setExpirationDate(TEST_DATE);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));

		// WHEN
		// @formatter:off
		final String json = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"expirationDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"parameters\":["
					 +"{\"name\":\"a\",\"value\":\"" + instr.getInstruction().getParameters().get(0).getValue() +"\"}"
					+",{\"name\":\"b\",\"value\":\"" + instr.getInstruction().getParameters().get(1).getValue() +"\"}"
				+ "]}";
		// @formatter:on

		NodeInstruction result = mapper.readValue(json, NodeInstruction.class);

		// THEN
		// @formatter:off
		then(result)
			.as("JSON parsed into instance")
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(instr)
			;
		// @formatter:on
	}
}
