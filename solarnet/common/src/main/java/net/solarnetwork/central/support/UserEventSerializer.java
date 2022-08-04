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
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.domain.UserEvent;

/**
 * JSON serializer for {@link UserEvent} objects.
 * 
 * @author matt
 * @version 1.0
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
		generator.writeStartObject(event, 6);
		generator.writeNumberField("userId", event.getUserId());
		generator.writeObjectField("created", event.getCreated());
		generator.writeStringField("eventId", event.getEventId().toString());

		generator.writeFieldName("tags");
		generator.writeArray(event.getTags(), 0, event.getTags().length);

		if ( event.getMessage() != null && !event.getMessage().isBlank() ) {
			generator.writeStringField("message", event.getMessage());
		}
		if ( event.getData() != null && !event.getData().isBlank() ) {
			generator.writeFieldName("data");
			generator.writeRawValue(event.getData());
		}
		generator.writeEndObject();
	}

}
