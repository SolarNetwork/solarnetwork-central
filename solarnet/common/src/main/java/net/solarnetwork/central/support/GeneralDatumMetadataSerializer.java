/* ==================================================================
 * GeneralDatumMetadataSerializer.java - 13/11/2016 12:25:06 PM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * JSON serializer for {@link GeneralDatumMetadata}.
 *
 * @author matt
 * @version 3.0
 */
public class GeneralDatumMetadataSerializer extends StdSerializer<GeneralDatumMetadata> {

	public GeneralDatumMetadataSerializer() {
		super(GeneralDatumMetadata.class);
	}

	@Override
	public void serialize(GeneralDatumMetadata meta, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		JsonUtils.writeMetadata(generator, meta);
	}

}
