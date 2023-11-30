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

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;

/**
 * JSON serializer for {@link ChargePointActionStatus} objects.
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointActionStatusSerializer extends StdSerializer<ChargePointActionStatus> {

	private static final long serialVersionUID = -7679360882593716740L;

	/** A default instance. */
	public static final StdSerializer<ChargePointActionStatus> INSTANCE = new ChargePointActionStatusSerializer();

	public ChargePointActionStatusSerializer() {
		super(ChargePointActionStatus.class);
	}

	@Override
	public void serialize(ChargePointActionStatus status, JsonGenerator generator,
			SerializerProvider provider) throws IOException, JsonGenerationException {
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
			generator.writeObjectField("created", status.getCreated());
		}
		generator.writeNumberField("userId", status.getUserId());
		generator.writeNumberField("chargePointId", status.getChargePointId());
		generator.writeNumberField("connectorId", status.getConnectorId());
		generator.writeStringField("action", status.getAction());
		generator.writeStringField("messageId", status.getMessageId());
		if ( status.getTimestamp() != null ) {
			generator.writeObjectField("ts", status.getTimestamp());
		}
		generator.writeEndObject();
	}

}
