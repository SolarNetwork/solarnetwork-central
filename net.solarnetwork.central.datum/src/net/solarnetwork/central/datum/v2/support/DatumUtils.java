/* ==================================================================
 * DatumUtils.java - 23/10/2020 1:51:36 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonGenerator;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Utilities for Datum domain classes.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public final class DatumUtils {

	private DatumUtils() {
		// don't construct me
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
