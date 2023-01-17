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

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.JsonUtils;

/**
 * Serializer for {@link NodeInstruction} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionSerializer extends StdSerializer<NodeInstruction> {

	private static final long serialVersionUID = 5889973152713872817L;

	/** A default instance. */
	public static final NodeInstructionSerializer INSTANCE = new NodeInstructionSerializer();

	public NodeInstructionSerializer() {
		super(NodeInstruction.class);
	}

	@Override
	public void serialize(NodeInstruction instr, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		if ( instr == null ) {
			generator.writeNull();
			return;
		}
		generator.writeStartObject(instr, 7);
		generator.writeNumberField("id", instr.getId());
		generator.writeObjectField("created", instr.getCreated());
		generator.writeNumberField("nodeId", instr.getNodeId());
		generator.writeStringField("topic", instr.getTopic());
		generator.writeObjectField("instructionDate", instr.getInstructionDate());
		if ( instr.getState() != null ) {
			generator.writeStringField("state", instr.getState().toString());
		}
		if ( instr.getParameters() != null && !instr.getParameters().isEmpty() ) {
			generator.writeFieldName("parameters");
			generator.writeStartArray(instr.getParameters(), instr.getParameters().size());
			for ( InstructionParameter p : instr.getParameters() ) {
				generator.writeStartObject(p, 2);
				generator.writeStringField("name", p.getName());
				generator.writeStringField("value", p.getValue());
				generator.writeEndObject();
			}
			generator.writeEndArray();
		}
		String resultParamsJson = instr.getResultParametersJson();
		if ( resultParamsJson != null && !resultParamsJson.isEmpty() ) {
			generator.writeFieldName("resultParameters");
			try {
				generator.writeRawValue(resultParamsJson);
			} catch ( UnsupportedOperationException e ) {
				// can happen with things like CBOR, so parse as Map
				Map<String, Object> dataMap = JsonUtils.getStringMap(resultParamsJson);
				generator.writeObject(dataMap);
			}
		}
		generator.writeEndObject();
	}

}
