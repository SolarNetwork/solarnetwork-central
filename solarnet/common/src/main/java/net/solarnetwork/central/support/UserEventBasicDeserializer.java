/* ==================================================================
 * UserEventBasicDeserializer.java - 18/03/2026 6:17:59 pm
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

package net.solarnetwork.central.support;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Deserializer for {@link UserEvent} objects.
 *
 * @author matt
 * @version 1.0
 */
public class UserEventBasicDeserializer extends StdDeserializer<UserEvent> {

	/** A default instance. */
	public static final ValueDeserializer<UserEvent> INSTANCE = new UserEventBasicDeserializer();

	/**
	 * Constructor.
	 */
	public UserEventBasicDeserializer() {
		super(UserEvent.class);
	}

	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	@Override
	public @Nullable UserEvent deserialize(JsonParser p, DeserializationContext ctxt)
			throws JacksonException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( p.isExpectedStartObjectToken() ) {
			t = p.nextToken(); // userId:
			Long userId = p.nextLongValue(0L);
			t = p.nextToken(); // eventId:
			UUID eventId = UUID.fromString(p.nextStringValue());
			t = p.nextToken(); // tags:
			String[] tags = JsonUtils.parseStringArrayStrict(p);
			String message = null;
			String data = null;
			while ( (t = p.nextToken()) != JsonToken.END_OBJECT ) {
				String f = p.currentName();
				switch (f) {
					case "message":
						message = p.nextStringValue();
						break;

					case "data":
						data = p.nextStringValue();
						break;
				}
			}
			return new UserEvent(userId, eventId, tags != null ? tags : new String[0], message, data);
		}
		throw MismatchedInputException.from(p, "Unable to parse UserEvent (not an object)");
	}

}
