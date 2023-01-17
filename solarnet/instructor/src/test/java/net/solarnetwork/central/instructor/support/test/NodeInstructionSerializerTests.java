/* ==================================================================
 * NodeInstructionSerializerTests.java - 17/01/2023 3:46:11 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.NodeInstructionSerializer;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link NodeInstructionSerializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionSerializerTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MICROSECONDS.toNanos(234567))
			.toInstant(ZoneOffset.UTC);
	private static final String TEST_DATE_STRING = "2021-08-11 16:45:01.234567Z";

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = JsonUtils.newObjectMapper();
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(NodeInstruction.class, NodeInstructionSerializer.INSTANCE);
		m.registerModule(mod);
		return m;
	}

	@Before
	public void setup() {
		mapper = createObjectMapper();
	}

	@Test
	public void serialize_typical() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		// WHEN
		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.setState(InstructionState.Completed);
		instr.setParameters(Arrays.asList(
				new InstructionParameter[] { new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));
		String json = mapper.writeValueAsString(instr);

		// THEN
		// @formatter:off
		final String expectedJson = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"parameters\":["
					 +"{\"name\":\"a\",\"value\":\"" + instr.getParameters().get(0).getValue() +"\"}"
					+",{\"name\":\"b\",\"value\":\"" + instr.getParameters().get(1).getValue() +"\"}"
				+ "]}";
		// @formatter:on
		assertThat("JSON", json, is(equalTo(expectedJson)));
	}

	@Test
	public void serialize_withResultParameters() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		// WHEN
		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.setState(InstructionState.Completed);
		instr.setResultParametersJson("{\"message\":\"Hello\"}");
		String json = mapper.writeValueAsString(instr);

		// THEN
		// @formatter:off
		final String expectedJson = "{\"id\":" + id						
				+ ",\"created\":\"" + TEST_DATE_STRING + "\""
				+ ",\"nodeId\":" + nodeId
				+ ",\"topic\":\"" + topic + "\""
				+ ",\"instructionDate\":\"" + TEST_DATE_STRING + "\""
				+ ",\"state\":\"" + InstructionState.Completed.name() + "\""
				+ ",\"resultParameters\":" +instr.getResultParametersJson()
				+ "}";
		// @formatter:on
		assertThat("JSON", json, is(equalTo(expectedJson)));
	}

}
