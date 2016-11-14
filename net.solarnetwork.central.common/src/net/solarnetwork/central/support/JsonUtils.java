/* ==================================================================
 * JsonUtils.java - 15/05/2015 11:46:24 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Utilities for JSON data.
 * 
 * @author matt
 * @version 1.1
 */
public final class JsonUtils {

	private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.setSerializationInclusion(Include.NON_NULL)
			.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

	// don't construct me
	private JsonUtils() {
		super();
	}

	/**
	 * Convert an object to a JSON string. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and null values will not
	 * be included in the output. All exceptions while serializing the object
	 * are caught and ignored.
	 * 
	 * @param o
	 *        the object to serialize to JSON
	 * @param defaultValue
	 *        a default value to use if {@code o} is <em>null</em> or if any
	 *        error occurs serializing the object to JSON
	 * @return the JSON string
	 */
	public static String getJSONString(final Object o, final String defaultValue) {
		String result = defaultValue;
		if ( o != null ) {
			try {
				return OBJECT_MAPPER.writeValueAsString(o);
			} catch ( Exception e ) {
				LOG.error("Exception marshalling {} to JSON", o, e);
			}
		}
		return result;
	}

	/**
	 * Convert a JSON string to an object. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and all floating point
	 * values will be converted to {@link BigDecimal} values to faithfully
	 * represent the data. All exceptions while deserializing the object are
	 * caught and ignored.
	 * 
	 * @param json
	 *        the JSON string to convert
	 * @param clazz
	 *        the type of Object to map the JSON into
	 * @return the object
	 * @since 1.1
	 */
	public static <T> T getObjectFromJSON(final String json, Class<T> clazz) {
		T result = null;
		if ( json != null ) {
			try {
				result = OBJECT_MAPPER.readValue(json, clazz);
			} catch ( Exception e ) {
				LOG.error("Exception deserialzing json {}", json, e);
			}
		}
		return result;
	}

	/**
	 * Write metadata to a JSON generator.
	 * 
	 * @param generator
	 *        The generator to write to.
	 * @param meta
	 *        The metadata to write.
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	public static void writeMetadata(JsonGenerator generator, GeneralDatumMetadata meta)
			throws IOException {
		if ( meta == null ) {
			return;
		}
		Map<String, Object> m = meta.getM();
		if ( m != null ) {
			generator.writeObjectField("m", m);
		}

		Map<String, Map<String, Object>> pm = meta.getPm();
		if ( pm != null ) {
			generator.writeObjectField("pm", pm);
		}

		Set<String> t = meta.getT();
		if ( t != null ) {
			generator.writeArrayFieldStart("t");
			for ( String tag : t ) {
				generator.writeString(tag);
			}
			generator.writeEndArray();
		}
	}

}
