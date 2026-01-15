/* ==================================================================
 * ChargePointActionStatusSerializer.java - 17/11/2022 6:30:03 pm
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

package net.solarnetwork.central.ocpp.util;

import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * JSON serializer for {@link ChargePointActionStatus} objects.
 *
 * @author matt
 * @version 2.0
 */
public class ChargePointActionStatusSerializer extends StdSerializer<ChargePointActionStatus> {

	/** A default instance. */
	public static final ValueSerializer<ChargePointActionStatus> INSTANCE = new ChargePointActionStatusSerializer();

	public ChargePointActionStatusSerializer() {
		super(ChargePointActionStatus.class);
	}

	@Override
	public void serialize(ChargePointActionStatus status, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		if ( status == null ) {
			generator.writeNull();
			return;
		}

		// @formatter:off
		final int size = 5
				+ (status.getCreated() != null ? 1 : 0)
				+ (status.getTimestamp() != null ? 1 : 0)
				;
		// @formatter:on

		generator.writeStartObject(status, size);
		if ( status.getCreated() != null ) {
			generator.writePOJOProperty("created", status.getCreated());
		}
		generator.writeNumberProperty("userId", status.getUserId());
		generator.writeNumberProperty("chargePointId", status.getChargePointId());
		generator.writeNumberProperty("connectorId", status.getConnectorId());
		generator.writeStringProperty("action", status.getAction());
		generator.writeStringProperty("messageId", status.getMessageId());
		if ( status.getTimestamp() != null ) {
			generator.writePOJOProperty("ts", status.getTimestamp());
		}
		generator.writeEndObject();
	}

}
