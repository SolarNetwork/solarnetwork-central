/* ==================================================================
 * UserEventSerializer.java - 1/08/2022 10:56:17 am
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

package net.solarnetwork.central.support;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.codec.JsonUtils;

/**
 * JSON serializer for {@link UserEvent} objects.
 * 
 * @author matt
 * @version 1.1
 */
public class UserEventSerializer extends StdSerializer<UserEvent> {

	private static final long serialVersionUID = -193553636996367260L;

	/** A default instance. */
	public static final UserEventSerializer INSTANCE = new UserEventSerializer();

	public UserEventSerializer() {
		super(UserEvent.class);
	}

	@Override
	public void serialize(UserEvent event, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		if ( event == null ) {
			generator.writeNull();
			return;
		}

		final boolean haveMessage = event.getMessage() != null && !event.getMessage().isBlank();
		final boolean haveData = event.getData() != null && !event.getData().isBlank();

		generator.writeStartObject(event, 4 + (haveMessage ? 1 : 0) + (haveData ? 1 : 0));
		generator.writeNumberField("userId", event.getUserId());
		generator.writeObjectField("created", event.getCreated());
		generator.writeStringField("eventId", event.getEventId().toString());

		generator.writeFieldName("tags");
		generator.writeArray(event.getTags(), 0, event.getTags().length);

		if ( haveMessage ) {
			generator.writeStringField("message", event.getMessage());
		}
		if ( haveData ) {
			generator.writeFieldName("data");
			try {
				generator.writeRawValue(event.getData());
			} catch ( UnsupportedOperationException e ) {
				// can happen with things like CBOR, so parse as Map
				Map<String, Object> dataMap = JsonUtils.getStringMap(event.getData());
				generator.writeObject(dataMap);
			}
		}
		generator.writeEndObject();
	}

}
