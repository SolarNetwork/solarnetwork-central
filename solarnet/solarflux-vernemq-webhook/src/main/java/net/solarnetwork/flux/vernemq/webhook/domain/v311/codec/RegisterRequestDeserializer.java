/* ==================================================================
 * RegisterRequestDeserializer.java - 16/01/2026 10:23:14 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.flux.vernemq.webhook.domain.v311.codec;

import net.solarnetwork.flux.vernemq.webhook.domain.v311.RegisterRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Deserialize {@link RegisterRequest} objects.
 * 
 * @author matt
 * @version 1.0
 */
public final class RegisterRequestDeserializer extends StdDeserializer<RegisterRequest> {

	/**
	 * Constructor.
	 */
	public RegisterRequestDeserializer() {
		super(RegisterRequest.class);
	}

	@Override
	public RegisterRequest deserialize(JsonParser p, DeserializationContext ctxt)
			throws JacksonException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( !p.isExpectedStartObjectToken() ) {
			throw MismatchedInputException.from(p, "Unable to parse RegisterRequest (not an object)");
		}

		final RegisterRequest.Builder builder = RegisterRequest.builder();

		while ( (t = p.nextToken()) != JsonToken.END_OBJECT ) {
			String f = p.currentName();
			switch (f) {
				case "client_id" -> builder.withClientId(p.nextStringValue());
				case "mountpoint" -> builder.withMountpoint(p.nextStringValue());
				case "username" -> builder.withUsername(p.nextStringValue());
				case "peer_addr" -> builder.withPeerAddress(p.nextStringValue());
				case "peer_port" -> builder.withPeerPort(p.nextIntValue(0));
				case "password" -> builder.withPassword(p.nextStringValue());
				case "clean_session", "clean_start" -> builder.withCleanSession(p.nextBooleanValue());
				default -> {
					p.nextValue();
					p.skipChildren();
				}
			}
		}

		return builder.build();
	}

}
