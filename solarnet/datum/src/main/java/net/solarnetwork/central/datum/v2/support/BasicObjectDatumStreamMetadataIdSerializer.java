/* ==================================================================
 * BasicObjectDatumStreamMetadataIdSerializer.java - 16/03/2022 1:52:26 PM
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

package net.solarnetwork.central.datum.v2.support;

import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.codec.jackson.BasicObjectDatumStreamMetadataField;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer for {@link ObjectDatumStreamMetadataId}.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class BasicObjectDatumStreamMetadataIdSerializer
		extends StdSerializer<ObjectDatumStreamMetadataId> {

	/** A default instance. */
	public static final ValueSerializer<ObjectDatumStreamMetadataId> INSTANCE = new BasicObjectDatumStreamMetadataIdSerializer();

	/**
	 * Constructor.
	 */
	public BasicObjectDatumStreamMetadataIdSerializer() {
		super(ObjectDatumStreamMetadataId.class);
	}

	@Override
	public void serialize(ObjectDatumStreamMetadataId meta, JsonGenerator generator,
			SerializationContext provider) throws JacksonException {
		// @formatter:off
		final int size =
				  (meta.getStreamId() != null ? 1 : 0)
				+ (meta.getKind() != null ? 1 : 0)
				+ (meta.getObjectId() != null ? 1 : 0)
				+ (meta.getSourceId() != null ? 1 : 0)
				;
		// @formatter:on
		generator.writeStartObject(meta, size);

		BasicObjectDatumStreamMetadataField.StreamId.writeValue(generator, provider, meta.getStreamId());
		BasicObjectDatumStreamMetadataField.ObjectDatumKind.writeValue(generator, provider,
				meta.getKind());
		BasicObjectDatumStreamMetadataField.ObjectId.writeValue(generator, provider, meta.getObjectId());
		BasicObjectDatumStreamMetadataField.SourceId.writeValue(generator, provider, meta.getSourceId());

		generator.writeEndObject();
	}

}
