/* ==================================================================
 * OcppInstructionUtils.java - 28/01/2021 9:39:19 AM
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

package net.solarnetwork.central.ocpp.util;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ocpp.domain.Action;
import ocpp.domain.SchemaValidationException;
import ocpp.json.ActionPayloadDecoder;

/**
 * Utilities for OCPP instruction handling.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public final class OcppInstructionUtils {

	/** A node instruction topic for OCPP v1.6 actions. */
	public static final String OCPP_V16_TOPIC = "OCPP_v16";

	/** A node instruction parameter name for an OCPP action name. */
	public static final String OCPP_ACTION_PARAM = "action";

	/** A node instruction parameter name for an OCPP ChargePoint identifier. */
	public static final String OCPP_CHARGER_IDENTIFIER_PARAM = "chargerIdentifier";

	/** A node instruction parameter name for an OCPP ChargePoint entity ID. */
	public static final String OCPP_CHARGE_POINT_ID_PARAM = "chargePointId";

	/** A node instruction parameter name for an OCPP message (e.g. JSON). */
	public static final String OCPP_MESSAGE_PARAM = "msg";

	/**
	 * API for handling the results of decoding a JSON OCPP instruction.
	 * 
	 * @param <T>
	 *        the return type
	 */
	@FunctionalInterface
	public static interface JsonOcppInstructionMessageHandler<T> {

		/**
		 * Handle the results of decoding a JSON OCPP instruction message.
		 * 
		 * @param e
		 *        if any error occurs
		 * @param jsonPayload
		 *        the raw JSON message, or {@literal null} if an error occurred
		 * @param payload
		 *        the decoded OCPP message payload, or {@literal null} if an
		 *        error occurred
		 * @return the result
		 */
		T handleMessage(Exception e, ObjectNode jsonPayload, Object payload);
	}

	/**
	 * Decode a JSON OCPP instruction message.
	 * 
	 * <p>
	 * This method will decode the <code>Instruction</code> parameters of a JSON
	 * OCPP action message into an OCPP message instance. If <code>params</code>
	 * contains a {@link #OCPP_MESSAGE_PARAM} value, that will be treated as an
	 * OCPP JSON message and decoded directly. Otherwise the keys of
	 * <code>params</code> will be used as JavaBean style property values for
	 * the OCPP message.
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
	 * @param chargePointActionPayloadDecoder
	 *        the action decoder to use
	 * @param handler
	 *        a handler for the results
	 * @return the handler result
	 */
	public static <T> T decodeJsonOcppInstructionMessage(ObjectMapper objectMapper, Action action,
			Map<String, String> params, ActionPayloadDecoder chargePointActionPayloadDecoder,
			JsonOcppInstructionMessageHandler<T> handler) {
		if ( handler == null ) {
			throw new IllegalArgumentException("The handler argument must be provided.");
		}
		ObjectNode jsonPayload;
		Object payload;
		try {
			if ( params.containsKey(OCPP_MESSAGE_PARAM) ) {
				JsonNode jsonNode = objectMapper.readTree(params.get(OCPP_MESSAGE_PARAM));
				if ( jsonNode.isNull() ) {
					jsonPayload = null;
				} else if ( jsonNode instanceof ObjectNode ) {
					jsonPayload = (ObjectNode) jsonNode;
				} else {
					throw new IOException(
							"OCPP " + OCPP_MESSAGE_PARAM + " parameter must be a JSON object.");
				}
			} else {
				jsonPayload = objectMapper.valueToTree(params);
			}
			if ( chargePointActionPayloadDecoder != null ) {
				payload = chargePointActionPayloadDecoder.decodeActionPayload(action, false,
						jsonPayload);
			} else {
				payload = params;
			}
			return handler.handleMessage(null, jsonPayload, payload);
		} catch ( IOException | SchemaValidationException e ) {
			return handler.handleMessage(e, null, null);
		}

	}

}
