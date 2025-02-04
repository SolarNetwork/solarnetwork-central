/* ==================================================================
 * Basic.java - 16/03/2022 1:55:26 PM
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
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.codec.BasicObjectDatumStreamMetadataField;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Deserializer for {@link ObjectDatumStreamMetadataId}.
 *
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public class BasicObjectDatumStreamMetadataIdDeserializer
		extends StdScalarDeserializer<ObjectDatumStreamMetadataId> implements Serializable {

	@Serial
	private static final long serialVersionUID = 5772539222468672138L;

	/** A default instance. */
	public static final BasicObjectDatumStreamMetadataIdDeserializer INSTANCE = new BasicObjectDatumStreamMetadataIdDeserializer();

	/**
	 * Constructor.
	 */
	public BasicObjectDatumStreamMetadataIdDeserializer() {
		super(ObjectDatumStreamMetadataId.class);
	}

	@Override
	public ObjectDatumStreamMetadataId deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonToken t = p.currentToken();
		if ( t == JsonToken.VALUE_NULL ) {
			return null;
		} else if ( p.isExpectedStartObjectToken() ) {
			Object[] data = new Object[BasicObjectDatumStreamMetadataField.values().length];
			JsonUtils.parseIndexedFieldsObject(p, ctxt, data,
					BasicObjectDatumStreamMetadataField.FIELD_MAP);
			// @formatter:off
			return new ObjectDatumStreamMetadataId(
					(UUID) data[BasicObjectDatumStreamMetadataField.StreamId.getIndex()],
					(ObjectDatumKind) data[BasicObjectDatumStreamMetadataField.ObjectDatumKind.getIndex()],
					(Long) data[BasicObjectDatumStreamMetadataField.ObjectId.getIndex()],
					(String) data[BasicObjectDatumStreamMetadataField.SourceId.getIndex()]
			);
			// @formattter:on
		}
		throw new JsonParseException(p, "Unable to parse ObjectDatumStreamMetadataId (not an object)");
	}

}
