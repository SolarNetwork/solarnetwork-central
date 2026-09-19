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

import static tools.jackson.core.StreamWriteCapability.CAN_WRITE_BINARY_NATIVELY;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * JSON serializer for {@link UserEvent} objects.
 * 
 * <p>
 * The {@code data} property is presumed to be a JSON string, and serialized as
 * raw JSON. If the generator does not support writing raw JSON, {@code data}
 * will be parsed as a basic {@link Map} and that will be serialized, achieving
 * essentially the same output.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public final class UserEventSerializer extends StdSerializer<@Nullable UserEvent> {

	/** A default instance. */
	public static final UserEventSerializer INSTANCE = new UserEventSerializer();

	public UserEventSerializer() {
		super(UserEvent.class);
	}

	@Override
	public void serialize(@Nullable UserEvent event, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		if ( event == null ) {
			generator.writeNull();
			return;
		}

		final boolean haveMessage = event.getMessage() != null && !event.getMessage().isBlank();
		final boolean haveData = event.getData() != null && !event.getData().isBlank();

		generator.writeStartObject(event, 4 + (haveMessage ? 1 : 0) + (haveData ? 1 : 0));
		generator.writeNumberProperty("userId", event.getUserId());
		generator.writePOJOProperty("created", event.getCreated());
		generator.writeStringProperty("eventId", event.getEventId().toString());

		generator.writeName("tags");
		generator.writeArray(event.getTags(), 0, event.getTags().length);

		if ( haveMessage ) {
			generator.writeStringProperty("message", event.getMessage());
		}
		if ( haveData ) {
			generator.writeName("data");
			// test for CAN_WRITE_BINARY_NATIVELY to avoid throwing UnsupportedOperationException
			// in writeRawValue() as CBOR does not support that
			if ( generator.streamWriteCapabilities().isEnabled(CAN_WRITE_BINARY_NATIVELY) ) {
				writeEventDataMap(event, generator);
			} else {
				try {
					generator.writeRawValue(event.getData());
				} catch ( UnsupportedOperationException e ) {
					writeEventDataMap(event, generator);
				}
			}
		}
		generator.writeEndObject();
	}

	private static void writeEventDataMap(UserEvent event, JsonGenerator generator) {
		Map<String, Object> dataMap = JsonUtils.getStringMap(event.getData());
		generator.writePOJO(dataMap);
	}

}
