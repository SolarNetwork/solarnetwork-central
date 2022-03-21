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

import java.io.IOException;
import java.io.Serializable;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.codec.BasicObjectDatumStreamMetadataField;

/**
 * Serializer for {@link ObjectDatumStreamMetadataId}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class BasicObjectDatumStreamMetadataIdSerializer
		extends StdScalarSerializer<ObjectDatumStreamMetadataId> implements Serializable {

	private static final long serialVersionUID = -5886091573502558556L;

	/** A default instance. */
	public static final BasicObjectDatumStreamMetadataIdSerializer INSTANCE = new BasicObjectDatumStreamMetadataIdSerializer();

	/**
	 * Constructor.
	 */
	public BasicObjectDatumStreamMetadataIdSerializer() {
		super(ObjectDatumStreamMetadataId.class);
	}

	@Override
	public void serialize(ObjectDatumStreamMetadataId meta, JsonGenerator generator,
			SerializerProvider provider) throws IOException, JsonGenerationException {
		generator.writeStartObject(meta, 4);

		BasicObjectDatumStreamMetadataField.StreamId.writeValue(generator, provider, meta.getStreamId());
		BasicObjectDatumStreamMetadataField.ObjectDatumKind.writeValue(generator, provider,
				meta.getKind());
		BasicObjectDatumStreamMetadataField.ObjectId.writeValue(generator, provider, meta.getObjectId());
		BasicObjectDatumStreamMetadataField.SourceId.writeValue(generator, provider, meta.getSourceId());

		generator.writeEndObject();
	}

}
