/* ==================================================================
 * NodeInstructionSerializer.java - 17/01/2023 3:32:53 pm
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

package net.solarnetwork.central.instructor.support;

import java.util.Map;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for {@link NodeInstruction} objects.
 *
 * @author matt
 * @version 3.0
 */
public class NodeInstructionSerializer extends StdSerializer<NodeInstruction> {

	/** A default instance. */
	public static final NodeInstructionSerializer INSTANCE = new NodeInstructionSerializer();

	public NodeInstructionSerializer() {
		super(NodeInstruction.class);
	}

	@Override
	public void serialize(NodeInstruction nodeInstruction, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		if ( nodeInstruction == null ) {
			generator.writeNull();
			return;
		}

		final Instruction instr = nodeInstruction.getInstruction();

		final boolean hasParameters = instr.getParameters() != null && !instr.getParameters().isEmpty();
		final String resultParamsJson = instr.getResultParametersJson();
		final boolean hasResultParams = resultParamsJson != null && !resultParamsJson.isEmpty();

		// @formatter:off
		int size =
				  (nodeInstruction.getId() != null ? 1 : 0)
				+ (nodeInstruction.getCreated() != null ? 1 : 0)
				+ (nodeInstruction.getNodeId() != null ? 1 : 0)
				+ (instr.getTopic() != null ? 1 : 0)
				+ (instr.getInstructionDate() != null ? 1 : 0)
				+ (instr.getState() != null ? 1 : 0)
				+ (instr.getStatusDate() != null ? 1 : 0)
				+ (instr.getExpirationDate() != null ? 1 : 0)
				+ (hasParameters ? 1 : 0)
				+ (hasResultParams ? 1 : 0)
				;
		// @formatter:on
		generator.writeStartObject(instr, size);
		if ( nodeInstruction.getId() != null ) {
			generator.writeNumberProperty("id", nodeInstruction.getId());
		}
		if ( nodeInstruction.getCreated() != null ) {
			generator.writePOJOProperty("created", nodeInstruction.getCreated());
		}
		if ( nodeInstruction.getNodeId() != null ) {
			generator.writeNumberProperty("nodeId", nodeInstruction.getNodeId());
		}
		if ( instr.getTopic() != null ) {
			generator.writeStringProperty("topic", instr.getTopic());
		}
		if ( instr.getInstructionDate() != null ) {
			generator.writePOJOProperty("instructionDate", instr.getInstructionDate());
		}
		if ( instr.getState() != null ) {
			generator.writeStringProperty("state", instr.getState().toString());
		}
		if ( instr.getStatusDate() != null ) {
			generator.writePOJOProperty("statusDate", instr.getStatusDate());
		}
		if ( instr.getExpirationDate() != null ) {
			generator.writePOJOProperty("expirationDate", instr.getExpirationDate());
		}
		if ( hasParameters ) {
			generator.writeName("parameters");
			generator.writeStartArray(instr.getParameters(), instr.getParameters().size());
			for ( InstructionParameter p : instr.getParameters() ) {
				generator.writeStartObject(p, 2);
				generator.writeStringProperty("name", p.getName());
				generator.writeStringProperty("value", p.getValue());
				generator.writeEndObject();
			}
			generator.writeEndArray();
		}
		if ( hasResultParams ) {
			generator.writeName("resultParameters");
			try {
				generator.writeRawValue(resultParamsJson);
			} catch ( UnsupportedOperationException e ) {
				// can happen with things like CBOR, so parse as Map
				Map<String, Object> dataMap = JsonUtils.getStringMap(resultParamsJson);
				generator.writePOJO(dataMap);
			}
		}
		generator.writeEndObject();
	}

}
