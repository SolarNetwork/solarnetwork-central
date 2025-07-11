/* ==================================================================
 * NodeInstructionDeserializer.java - 31/05/2025 8:59:48 am
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

package net.solarnetwork.central.instructor.support;

import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.JsonDateUtils.InstantDeserializer;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus;

/**
 * Deserializer for {@link NodeInstruction} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeInstructionDeserializer extends StdDeserializer<NodeInstruction> {

	@Serial
	private static final long serialVersionUID = -3604500447266382990L;

	/** A default instance. */
	public static final NodeInstructionDeserializer INSTANCE = new NodeInstructionDeserializer();

	/**
	 * Constructor.
	 */
	public NodeInstructionDeserializer() {
		super(NodeInstruction.class);
	}

	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	@Override
	public NodeInstruction deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JacksonException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( p.isExpectedStartObjectToken() ) {
			Long id = null;
			Instant created = null;
			Long nodeId = null;
			String topic = null;
			Instant instructionDate = null;
			InstructionStatus.InstructionState state = null;
			Instant statusDate = null;
			Instant expirationDate = null;
			List<InstructionParameter> parameters = null;
			Map<String, Object> resultParameters = null;

			String f;
			while ( (f = p.nextFieldName()) != null ) {
				switch (f) {
					case "id":
						id = JsonUtils.parseLong(p);
						break;

					case "created":
						p.nextToken();
						created = InstantDeserializer.INSTANCE.deserialize(p, ctxt);
						break;

					case "nodeId":
						nodeId = JsonUtils.parseLong(p);
						break;

					case "topic":
						topic = p.nextTextValue();
						break;

					case "instructionDate":
						p.nextToken();
						instructionDate = InstantDeserializer.INSTANCE.deserialize(p, ctxt);
						break;

					case "state":
						try {
							state = InstructionStatus.InstructionState.valueOf(p.nextTextValue());
						} catch ( Exception e ) {
							state = InstructionStatus.InstructionState.Unknown;
						}
						break;

					case "statusDate":
						p.nextToken();
						statusDate = InstantDeserializer.INSTANCE.deserialize(p, ctxt);
						break;

					case "expirationDate":
						p.nextToken();
						expirationDate = InstantDeserializer.INSTANCE.deserialize(p, ctxt);
						break;

					case "params":
					case "parameters":
						parameters = parseParameters(p);
						break;

					case "resultParameters":
						p.nextToken();
						resultParameters = p.readValueAs(JsonUtils.STRING_MAP_TYPE);
						break;

				}
			}

			// jump to end object
			while ( (t = p.currentToken()) != JsonToken.END_OBJECT ) {
				t = p.nextToken();
			}

			NodeInstruction result = new NodeInstruction(topic, instructionDate, nodeId);
			result.setId(id);
			result.setCreated(created);

			Instruction instr = result.getInstruction();
			instr.setState(state);
			instr.setStatusDate(statusDate);
			instr.setExpirationDate(expirationDate);
			instr.setParameters(parameters);
			instr.setResultParameters(resultParameters);

			return result;
		}
		throw new JsonParseException(p, "Unable to parse Instruction (not an object)");
	}

	private static List<InstructionParameter> parseParameters(JsonParser p) throws IOException {
		JsonToken t = p.nextToken();
		return switch (t) {
			case START_ARRAY -> parseParameterList(p);
			case START_OBJECT -> parseParameterMap(p);
			default -> null;
		};
	}

	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	private static List<InstructionParameter> parseParameterList(JsonParser p) throws IOException {
		assert p.currentToken() == JsonToken.START_ARRAY;
		List<InstructionParameter> result = new ArrayList<>(8);
		JsonToken t = null;
		String paramName = null;
		String paramValue = null;
		while ( (t = p.nextToken()) != JsonToken.END_ARRAY ) {
			if ( t == JsonToken.START_OBJECT ) {
				String f;
				while ( (f = p.nextFieldName()) != null ) {
					String v = p.nextTextValue();
					switch (f) {
						case "name":
							paramName = v;
							break;

						case "value":
							paramValue = v;
							break;
					}
				}
				if ( paramName != null && paramValue != null ) {
					result.add(new InstructionParameter(paramName, paramValue));
				}
			}
		}
		return result;
	}

	private static List<InstructionParameter> parseParameterMap(JsonParser p) throws IOException {
		assert p.currentToken() == JsonToken.START_OBJECT;
		List<InstructionParameter> result = new ArrayList<>(8);
		String f;
		while ( (f = p.nextFieldName()) != null ) {
			String s = p.nextTextValue();
			if ( s != null ) {
				result.add(new InstructionParameter(f, s));
			}
		}
		return result;
	}

}
