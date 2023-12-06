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

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;
import net.solarnetwork.codec.JsonDateUtils.InstantSerializer;

/**
 * JSON serializer for {@link ChargePointStatus} objects.
 * 
 * @author matt
 * @version 1.2
 */
public class ChargePointStatusSerializer extends StdSerializer<ChargePointStatus> {

	private static final long serialVersionUID = -7679360882593716740L;

	/** A default instance. */
	public static final StdSerializer<ChargePointStatus> INSTANCE = new ChargePointStatusSerializer();

	public ChargePointStatusSerializer() {
		super(ChargePointStatus.class);
	}

	@Override
	public void serialize(ChargePointStatus status, JsonGenerator generator, SerializerProvider provider)
			throws IOException, JsonGenerationException {
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
			generator.writeFieldName("created");
			InstantSerializer.INSTANCE.serialize(status.getCreated(), generator, provider);
		}
		generator.writeNumberField("userId", status.getUserId());
		generator.writeNumberField("chargePointId", status.getChargePointId());
		if ( status.getConnectedTo() != null ) {
			generator.writeStringField("connectedTo", status.getConnectedTo());
		}
		if ( status.getSessionId() != null ) {
			generator.writeStringField("sessionId", status.getSessionId());
		}
		if ( status.getConnectedDate() != null ) {
			generator.writeFieldName("connectedDate");
			InstantSerializer.INSTANCE.serialize(status.getConnectedDate(), generator, provider);
		}
		generator.writeEndObject();
	}

}
