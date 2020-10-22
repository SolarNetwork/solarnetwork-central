/* ==================================================================
 * DatumUtils.java - Feb 13, 2012 2:52:39 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.springframework.util.PathMatcher;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumProperties;
import net.solarnetwork.central.datum.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Utilities for Datum domain classes.
 * 
 * @author matt
 * @version 2.1
 */
public final class DatumUtils {

	// can't construct me
	private DatumUtils() {
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
	 * @since 1.1
	 * @see JsonUtils#getJSONString(Object, String)
	 */
	public static String getJSONString(final Object o, final String defaultValue) {
		return JsonUtils.getJSONString(o, defaultValue);
	}

	/**
	 * Convert a JSON string to an object. This is designed for simple values.
	 * An internal {@link ObjectMapper} will be used, and all floating point
	 * values will be converted to {@link BigDecimal} values to faithfully
	 * represent the data. All exceptions while deserializing the object are
	 * caught and ignored.
	 * 
	 * @param <T>
	 *        the return object type
	 * @param json
	 *        the JSON string to convert
	 * @param clazz
	 *        the type of Object to map the JSON into
	 * @return the object
	 * @since 1.1
	 * @see JsonUtils#getJSONString(Object, String)
	 */
	public static <T> T getObjectFromJSON(final String json, Class<T> clazz) {
		return JsonUtils.getObjectFromJSON(json, clazz);
	}

	/**
	 * Filter a set of node sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 * @since 1.3
	 */
	public static Set<NodeSourcePK> filterNodeSources(Set<NodeSourcePK> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<NodeSourcePK> itr = sources.iterator(); itr.hasNext(); ) {
			NodeSourcePK pk = itr.next();
			if ( !pathMatcher.match(pattern, pk.getSourceId()) ) {
				itr.remove();
			}
		}
		return sources;
	}

	/**
	 * Filter a set of sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 * @since 1.3
	 */
	public static Set<String> filterSources(Set<String> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<String> itr = sources.iterator(); itr.hasNext(); ) {
			String source = itr.next();
			if ( !pathMatcher.match(pattern, source) ) {
				itr.remove();
			}
		}
		return sources;
	}

	private static void writeJsonArrayValues(JsonGenerator generator, BigDecimal[] array)
			throws IOException {
		if ( array == null ) {
			return;
		}
		for ( BigDecimal n : array ) {
			generator.writeNumber(n);
		}
	}

	private static void writeJsonArrayValues(JsonGenerator generator, String[] array)
			throws IOException {
		if ( array == null ) {
			return;
		}
		for ( String s : array ) {
			generator.writeString(s);
		}
	}

	/**
	 * Write datum stream metadata as a JSON object.
	 * 
	 * @param generator
	 *        the generator to write to
	 * @param metadata
	 *        the metadata to write
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.1
	 */
	public static void writeStreamMetadata(JsonGenerator generator, DatumStreamMetadata metadata)
			throws IOException {
		if ( metadata == null ) {
			generator.writeNull();
			return;
		}

		generator.writeStartObject(metadata, 2);

		// props

		generator.writeFieldName("props");
		String[] array = metadata.getPropertyNames();
		if ( array == null || array.length < 1 ) {
			generator.writeNull();
		} else {
			generator.writeStartArray(array, array.length);
			writeJsonArrayValues(generator, array);
			generator.writeEndArray();
		}

		// class

		String[] iProps = metadata.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
		String[] aProps = metadata.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
		String[] sProps = metadata.propertyNamesForType(GeneralDatumSamplesType.Status);

		generator.writeFieldName("class");
		if ( array == null || array.length < 1 ) {
			generator.writeNull();
		} else {
			generator.writeStartObject(null,
					(iProps != null ? 1 : 0) + (aProps != null ? 1 : 0) + (sProps != null ? 1 : 0));
			if ( iProps != null ) {
				generator.writeFieldName("i");
				generator.writeStartArray(iProps, iProps.length);
				writeJsonArrayValues(generator, iProps);
				generator.writeEndArray();
			}
			if ( aProps != null ) {
				generator.writeFieldName("a");
				generator.writeStartArray(aProps, aProps.length);
				writeJsonArrayValues(generator, aProps);
				generator.writeEndArray();
			}
			if ( sProps != null ) {
				generator.writeFieldName("s");
				generator.writeStartArray(sProps, sProps.length);
				writeJsonArrayValues(generator, sProps);
				generator.writeEndArray();
			}
			generator.writeEndObject();
		}

		generator.writeEndObject();
	}

	/**
	 * Write datum property values as a JSON array.
	 * 
	 * <p>
	 * The output array will contain the following elements:
	 * <p>
	 * 
	 * <ul>
	 * <li>The datum timestamp, as a millisecond epoch number value, or a
	 * literal {@literal null}.</li>
	 * <li>All instantaneous property values, as numbers.</li>
	 * <li>All accumulating property values, as numbers.</li>
	 * <li>All status property values, as strings.</li>
	 * <li>All tag values, as strings.</li>
	 * </ul>
	 * 
	 * <p>
	 * If any property array is {@literal null} or empty, no elements will be
	 * contributed to the output JSON array. Any {@literal null} values
	 * <i>within</i> a property array will contribute {@literal null} literals
	 * to the output JSON array.
	 * <p>
	 * 
	 * @param generator
	 *        the generator to write to
	 * @param datum
	 *        the datum to write the sample values for
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 2.1
	 */
	public static void writePropertyValuesArray(JsonGenerator generator, Datum datum)
			throws IOException {
		if ( datum == null ) {
			generator.writeNull();
			return;
		}

		int len = 1; // for timestamp
		DatumProperties props = datum.getProperties();
		if ( props != null ) {
			len += props.getLength();
		}

		generator.writeStartArray(datum, len);

		// write timestamp
		Instant ts = datum.getTimestamp();
		if ( ts != null ) {
			generator.writeNumber(ts.toEpochMilli());
		} else {
			generator.writeNull();
		}

		if ( props != null ) {
			// instantaneous values
			writeJsonArrayValues(generator, props.getInstantaneous());

			// accumulating values
			writeJsonArrayValues(generator, props.getAccumulating());

			// status values
			writeJsonArrayValues(generator, props.getStatus());

			// status values
			writeJsonArrayValues(generator, props.getTags());
		}

		generator.writeEndArray();
	}

	/**
	 * Write a collection of datum as a JSON datum stream.
	 * 
	 * <p>
	 * A datum stream consists of a string identifier, metadata object, and
	 * array of property value arrays. For example here is the JSON for a stream
	 * of 4 properties:
	 * </p>
	 * 
	 * <pre>
	 * <code>{
	 *     "streamId": "5970bf45-a1c9-4862-8bb8-1d687b370a55",
	 *     "metadata": {
	 *         "props": ["cpu_user", "net_bytes_in_eth0", "net_bytes_out_eth0", "alert"],
	 *         "class": {
	 *            "i": ["cpu_user"],
	 *             "a": ["net_bytes_in_eth0", "net_bytes_out_eth0"],
	 *             "s": ["status"]
	 *         }
	 *     },
	 *     "values": [
	 *         [1325376000000, 3, 23445, 123, null],
	 *         [1325376001000, 99, 23423, 243, "W9347"],
	 *         [1325376002000, 4, 33452, 291, null]
	 *     ]
	 * }</code>
	 * </pre>
	 * 
	 * @param generator
	 *        the generator to write to
	 * @param streamId
	 *        the stream ID to write
	 * @param metadata
	 *        the metadata to write
	 * @param datum
	 *        the datum to write
	 * @param datumLength
	 *        the number of elements in the {@code datum} iterator
	 * @throws IOException
	 *         if any IO error occurs
	 * @see #writeStreamMetadata(JsonGenerator, DatumStreamMetadata)
	 * @see #writePropertyValuesArray(JsonGenerator, Datum)
	 * @since 2.1
	 */
	public static void writeStream(JsonGenerator generator, UUID streamId, DatumStreamMetadata metadata,
			Iterator<Datum> datum, int datumLength) throws IOException {
		if ( datum == null ) {
			generator.writeNull();
			return;
		}
		generator.writeStartObject(null, 3);

		generator.writeFieldName("streamId");
		if ( streamId == null ) {
			generator.writeNull();
		} else {
			generator.writeString(streamId.toString());
		}

		generator.writeFieldName("metadata");
		writeStreamMetadata(generator, metadata);

		generator.writeFieldName("values");
		if ( datum == null || datumLength < 1 ) {
			generator.writeNull();
		} else {
			generator.writeStartArray(datum, datumLength);
			while ( datum.hasNext() ) {
				Datum d = datum.next();
				writePropertyValuesArray(generator, d);
			}
			generator.writeEndArray();
		}

		generator.writeEndObject();
	}

}
