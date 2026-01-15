/* ==================================================================
 * ChargePointStatusSerializer.java - 17/11/2022 6:30:03 pm
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

import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.codec.jackson.JsonDateUtils.InstantSerializer;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * JSON serializer for {@link ChargePointStatus} objects.
 *
 * @author matt
 * @version 2.0
 */
public class ChargePointStatusSerializer extends StdSerializer<ChargePointStatus> {

	/** A default instance. */
	public static final ValueSerializer<ChargePointStatus> INSTANCE = new ChargePointStatusSerializer();

	public ChargePointStatusSerializer() {
		super(ChargePointStatus.class);
	}

	@Override
	public void serialize(ChargePointStatus status, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		if ( status == null ) {
			generator.writeNull();
			return;
		}

		// @formatter:off
		final int size = 2
				+ (status.getCreated() != null ? 1 : 0)
				+ (status.getConnectedTo() != null ? 1 : 0)
				+ (status.getSessionId() != null ? 1 : 0)
				+ (status.getConnectedDate() != null ? 1 : 0)
				;
		// @formatter:on

		generator.writeStartObject(status, size);
		if ( status.getCreated() != null ) {
			generator.writeName("created");
			InstantSerializer.INSTANCE.serialize(status.getCreated(), generator, provider);
		}
		generator.writeNumberProperty("userId", status.getUserId());
		generator.writeNumberProperty("chargePointId", status.getChargePointId());
		if ( status.getConnectedTo() != null ) {
			generator.writeStringProperty("connectedTo", status.getConnectedTo());
		}
		if ( status.getSessionId() != null ) {
			generator.writeStringProperty("sessionId", status.getSessionId());
		}
		if ( status.getConnectedDate() != null ) {
			generator.writeName("connectedDate");
			InstantSerializer.INSTANCE.serialize(status.getConnectedDate(), generator, provider);
		}
		generator.writeEndObject();
	}

}
