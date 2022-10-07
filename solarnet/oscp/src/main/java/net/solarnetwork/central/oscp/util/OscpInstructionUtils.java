/* ==================================================================
 * OscpInstructionUtils.java - 6/10/2022 4:55:09 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.util;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

/**
 * Utilities for OSCP instruction support.
 * 
 * @author matt
 * @version 1.0
 */
public final class OscpInstructionUtils {

	/** A node instruction topic for OSCP v2.0 actions. */
	public static final String OSCP_V20_TOPIC = "OSCP_v20";

	/** A node instruction parameter name for an OSCP Capacity Optimizer ID. */
	public static final String OSCP_CAPACITY_OPTIMIZER_ID_PARAM = "coId";

	/**
	 * A node instruction parameter name for an OSCP Capacity Group identifier.
	 */
	public static final String OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM = "cgIdentifier";

	/** A node instruction parameter name for an OSCP action name. */
	public static final String OSCP_ACTION_PARAM = "action";

	/** A node instruction parameter name for an OSCP message (e.g. JSON). */
	public static final String OSCP_MESSAGE_PARAM = "msg";

	private static final Pattern OSCP_ACTION_KEBAB_CASE_REPLACE = Pattern.compile("(?<=[a-z])([A-Z])");

	/**
	 * A function that converts an OSCP action name to an equivalent JSON schema
	 * name.
	 */
	public static Function<String, String> OSCP_ACTION_TO_JSON_SCHEMA_NAME = (s) -> {
		if ( s == null ) {
			return null;
		}
		return "http://www.openchargealliance.org/schemas/oscp/2.0/"
				+ OSCP_ACTION_KEBAB_CASE_REPLACE.matcher(s).replaceAll("-$1").toLowerCase() + ".json";
	};

	private OscpInstructionUtils() {
		// not available
	}

	/**
	 * Decode a JSON OSCP instruction message.
	 * 
	 * <p>
	 * This method will decode the {@code Instruction} parameters of a JSON OSCP
	 * action message into an OSCP message instance. If {@code params} contains
	 * a {@link #OCPP_MESSAGE_PARAM} value that is a string, that will be
	 * treated as an OCPP JSON message and decoded directly. If the value is
	 * already a {@link ObjectNode} it will be used as-is.
	 * </p>
	 * 
	 * @param <T>
	 *        the return type
	 * @param objectMapper
	 *        the mapper to use
	 * @param action
	 *        the message action to decode
	 * @param params
	 *        the instruction parameters
	 * @return the result
	 * @throws IllegalArgumentException
	 *         if the instruction parameters do not describe a supported OSCP
	 *         message
	 */
	public static Object decodeJsonOscp20InstructionMessage(ObjectMapper objectMapper,
			Map<String, ?> params, JsonSchemaFactory validator) {
		final String action = requireNonNullArgument(params.get(OSCP_ACTION_PARAM), "action").toString();
		final Class<?> actionClass = switch (action) {
			case "AdjustGroupCapacityForecast" -> oscp.v20.AdjustGroupCapacityForecast.class;
			case "GroupCapacityComplianceError" -> oscp.v20.GroupCapacityComplianceError.class;
			case "UpdateAssetMeasurement" -> oscp.v20.UpdateAssetMeasurement.class;
			case "UpdateGroupCapacityForecast" -> oscp.v20.UpdateGroupCapacityForecast.class;
			case "UpdateGroupMeasurements" -> oscp.v20.UpdateGroupMeasurements.class;
			default -> throw new IllegalArgumentException(
					"Unsupported OSCP 2.0 action [%s]".formatted(action));
		};
		try {
			ObjectNode jsonPayload;
			Object msgObj = params.get(OSCP_MESSAGE_PARAM);
			if ( msgObj instanceof String msgString ) {
				JsonNode jsonNode = objectMapper.readTree(msgString);
				if ( jsonNode.isNull() ) {
					jsonPayload = null;
				} else if ( jsonNode instanceof ObjectNode ) {
					jsonPayload = (ObjectNode) jsonNode;
				} else {
					throw new IOException(
							"OSCP " + OSCP_MESSAGE_PARAM + " parameter must be a JSON object.");
				}
			} else if ( msgObj instanceof ObjectNode msgNode ) {
				jsonPayload = msgNode;
			} else {
				throw new IllegalArgumentException("Missing [%s] parameter for OSCP 2.0 action [%s]"
						.formatted(OSCP_MESSAGE_PARAM, action));
			}
			if ( validator != null ) {
				String schemaId = OSCP_ACTION_TO_JSON_SCHEMA_NAME.apply(action);
				JsonSchema schema = validator.getSchema(URI.create(schemaId));
				Set<ValidationMessage> errors = schema.validate(jsonPayload);
				if ( !errors.isEmpty() ) {
					throw new IllegalArgumentException(
							"JSON schema validation error on [%s] OSCP action: %s.".formatted(action,
									errors.stream().map(Object::toString)
											.collect(Collectors.joining(", "))));
				}
			}
			return objectMapper.treeToValue(jsonPayload, actionClass);
		} catch ( IOException e ) {
			throw new IllegalArgumentException(
					"Invalid JSON for [%s] OSCP action: %s".formatted(action, e.getMessage()));
		}
	}

}
