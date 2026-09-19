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

import static net.solarnetwork.util.ByteUtils.objectArray;
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
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.NodeInstructionSerializer;
import net.solarnetwork.codec.jackson.CborUtils;
import net.solarnetwork.codec.jackson.JsonDateUtils;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * Test cases for the {@link NodeInstructionSerializer} class.
 * 
 * @author matt
 * @version 1.2
 */
public class NodeInstructionSerializer_CborTests {

	private static final Instant TEST_DATE = LocalDateTime
			.of(2021, 8, 11, 16, 45, 1, (int) TimeUnit.MICROSECONDS.toNanos(234567))
			.toInstant(ZoneOffset.UTC);

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		SimpleModule mod = new SimpleModule("Test");
		mod.addSerializer(NodeInstruction.class, NodeInstructionSerializer.INSTANCE);
		var builder = CBORMapper.builder(CborUtils.cborFactory());
		JsonUtils.setupMapperBuilder(builder, JsonDateUtils.JAVA_TIME_MODULE, JsonUtils.CORE_MODULE,
				mod);
		return builder.build();
	}

	@BeforeEach
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
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(instr));

		// THEN
		then(cbor).hasSize(287);
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
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setResultParametersJson("{\"message\":\"Hello\"}");
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(instr));

		// THEN
		then(cbor).hasSize(204);
	}

	@Test
	public void serialize_withStatusDate() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		// WHEN
		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setStatusDate(TEST_DATE);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(instr));

		// THEN
		then(cbor).hasSize(327);
	}

	@Test
	public void serialize_withExpirationDate() throws IOException {
		// GIVEN
		final Long id = UUID.randomUUID().getMostSignificantBits();
		final Long nodeId = UUID.randomUUID().getMostSignificantBits();
		final String topic = UUID.randomUUID().toString();

		// WHEN
		NodeInstruction instr = new NodeInstruction(topic, TEST_DATE, nodeId);
		instr.setId(id);
		instr.setCreated(TEST_DATE);
		instr.getInstruction().setState(InstructionState.Completed);
		instr.getInstruction().setExpirationDate(TEST_DATE);
		instr.getInstruction()
				.setParameters(Arrays.asList(new InstructionParameter[] {
						new InstructionParameter("a", UUID.randomUUID().toString()),
						new InstructionParameter("b", UUID.randomUUID().toString()) }));
		Byte[] cbor = objectArray(mapper.writeValueAsBytes(instr));

		// THEN
		then(cbor).hasSize(331);
	}

}
