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

import java.util.UUID;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.codec.jackson.BasicObjectDatumStreamMetadataField;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Deserializer for {@link ObjectDatumStreamMetadataId}.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class BasicObjectDatumStreamMetadataIdDeserializer
		extends StdDeserializer<ObjectDatumStreamMetadataId> {

	/** A default instance. */
	public static final ValueDeserializer<ObjectDatumStreamMetadataId> INSTANCE = new BasicObjectDatumStreamMetadataIdDeserializer();

	/**
	 * Constructor.
	 */
	public BasicObjectDatumStreamMetadataIdDeserializer() {
		super(ObjectDatumStreamMetadataId.class);
	}

	@Override
	public ObjectDatumStreamMetadataId deserialize(JsonParser p, DeserializationContext ctxt)
			throws JacksonException {
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
		throw MismatchedInputException.from(p, "Unable to parse ObjectDatumStreamMetadataId (not an object)");
	}

}
