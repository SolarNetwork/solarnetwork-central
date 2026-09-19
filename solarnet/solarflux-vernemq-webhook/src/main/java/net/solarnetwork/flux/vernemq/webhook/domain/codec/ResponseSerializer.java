/* ==================================================================
 * ResponseSerializer.java - 16/01/2026 11:46:13 am
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

package net.solarnetwork.flux.vernemq.webhook.domain.codec;

import net.solarnetwork.flux.vernemq.webhook.domain.Response;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serialize {@link Response} objects.
 * 
 * @author matt
 * @version 1.0
 */
public final class ResponseSerializer extends StdSerializer<Response> {

	/**
	 * Constructor.
	 */
	public ResponseSerializer() {
		super(Response.class);
	}

	@Override
	public void serialize(Response value, JsonGenerator gen, SerializationContext provider)
			throws JacksonException {
		if ( value == null ) {
			gen.writeNull();
			return;
		}
		gen.writeStartObject();

		if ( value.getErrorStatus() != null ) {
			gen.writePOJOProperty("result", value.getErrorStatus());
		} else {
			gen.writeStringProperty("result", value.getStatus().toResultValue());
		}

		if ( value.getModifiers() != null ) {
			gen.writePOJOProperty("modifiers", value.getModifiers());
		}
		if ( value.getTopics() != null ) {
			gen.writePOJOProperty("topics", value.getTopics());
		}

		gen.writeEndObject();
	}

}
