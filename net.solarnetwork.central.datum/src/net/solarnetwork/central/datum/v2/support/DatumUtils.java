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

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
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

	private static void writeJsonArrayValues(JsonGenerator generator, BigDecimal[][] arrayOfArrays)
			throws IOException {
		if ( arrayOfArrays == null ) {
			return;
		}
		for ( BigDecimal[] array : arrayOfArrays ) {
			if ( array == null ) {
				generator.writeNull();
			} else {
				generator.writeStartArray(array, array.length);
				writeJsonArrayValues(generator, array);
				generator.writeEndArray();
			}
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
	 *             "i": ["cpu_user"],
	 *             "a": ["net_bytes_in_eth0", "net_bytes_out_eth0"],
	 *             "s": ["alert"]
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

	/**
	 * Write aggregate datum statistic values as a JSON array.
	 * 
	 * <p>
	 * The output array will contain the following elements:
	 * <p>
	 * 
	 * <ul>
	 * <li>The datum timestamp, as a millisecond epoch number value, or a
	 * literal {@literal null}.</li>
	 * <li>All instantaneous property statistics, as arrays of {@code [min, max,
	 * count]} numbers.</li>
	 * <li>All accumulating property values, as arrays of {@code [start, end]}
	 * numbers.</li>
	 * </ul>
	 * 
	 * <p>
	 * If any property array is {@literal null} or empty, no elements will be
	 * contributed to the output JSON array. Any {@literal null} values
	 * <i>within</i> a property array will contribute {@literal null} literals
	 * to the output JSON array.
	 * <p>
	 * 
	 * <p>
	 * For example here is the JSON for a stream with one instantaneous and two
	 * accumulating:
	 * </p>
	 * 
	 * <pre>
	 * <code>[1325376000000, [0,10,60], [0,23445], [0,123]]</code>
	 * </pre>
	 * 
	 * @param generator
	 *        the generator to write to
	 * @param datum
	 *        the datum to write the sample values for
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public static void writeStatisticValuesArray(JsonGenerator generator, AggregateDatum datum)
			throws IOException {
		if ( datum == null ) {
			generator.writeNull();
			return;
		}

		int len = 1; // for timestamp
		DatumPropertiesStatistics stats = datum.getStatistics();
		if ( stats != null ) {
			len += stats.getLength();
		}

		generator.writeStartArray(datum, len);

		// write timestamp
		Instant ts = datum.getTimestamp();
		if ( ts != null ) {
			generator.writeNumber(ts.toEpochMilli());
		} else {
			generator.writeNull();
		}

		if ( stats != null ) {
			// instantaneous values
			writeJsonArrayValues(generator, stats.getInstantaneous());

			// accumulating values
			writeJsonArrayValues(generator, stats.getAccumulating());
		}

		generator.writeEndArray();
	}

	/**
	 * Write a collection of aggregate datum as a JSON datum stream.
	 * 
	 * <p>
	 * An aggregate datum stream consists of a string identifier, metadata
	 * object, and array of property value and statistic array pairs. For
	 * example here is the JSON for a stream of 4 properties:
	 * </p>
	 * 
	 * <pre>
	 * <code>{
	 *     "streamId": "5970bf45-a1c9-4862-8bb8-1d687b370a55",
	 *     "metadata": {
	 *         "props": ["cpu_user", "net_bytes_in_eth0", "net_bytes_out_eth0", "alert"],
	 *         "class": {
	 *             "i": ["cpu_user"],
	 *             "a": ["net_bytes_in_eth0", "net_bytes_out_eth0"],
	 *             "s": ["alert"]
	 *         }
	 *     },
	 *     "values": [
	 *         [1325376000000, 3, 23445, 123, null],
	 *         [1325376000000, [0,10,60], [0,23445], [0,123]],
	 *         [1325376001000, 99, 23423, 243, "W9347"],
	 *         [1325376001000, [0,10,60], [0,23445], [0,123]],
	 *         [1325376002000, 4, 33452, 291, null],
	 *         [1325376002000, [0,10,60], [0,23445], [0,123]]
	 *     ]
	 * }</code>
	 * </pre>
	 * 
	 * <p>
	 * The {@code values} array will thus contain {@code 2 × count(datum)}
	 * elements: one array from
	 * {@link #writePropertyValuesArray(JsonGenerator, Datum)} followed by
	 * another array from
	 * {@link #writeStatisticValuesArray(JsonGenerator, AggregateDatum)} for
	 * each datum. The timestamp value will be identical for each pair.
	 * </p>
	 * 
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
	 * @see #writeStatisticValuesArray(JsonGenerator, AggregateDatum)
	 */
	public static void writeAggregateStream(JsonGenerator generator, UUID streamId,
			DatumStreamMetadata metadata, Iterator<AggregateDatum> datum, int datumLength)
			throws IOException {
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
			generator.writeStartArray(datum, datumLength * 2);
			while ( datum.hasNext() ) {
				AggregateDatum d = datum.next();
				writePropertyValuesArray(generator, d);
				writeStatisticValuesArray(generator, d);
			}
			generator.writeEndArray();
		}

		generator.writeEndObject();
	}

	public static AggregateDatum parseAggregateDatum(JsonParser parser,
			ObjectDatumStreamMetadataProvider metadataProvider) throws IOException {
		// read up to the next object end
		UUID streamId = null;
		Instant timestamp = null;
		Aggregation kind = null;

		Long objectId = null;
		String sourceId = null;

		ObjectDatumStreamMetadata meta = null;
		DatumProperties props = null;
		DatumPropertiesStatistics stats = null;

		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				parser.nextToken();
				switch (parser.getCurrentName()) {
					case "kind":
						kind = Aggregation.forKey(parser.getText());
						break;

					case "locationId":
					case "nodeId":
						objectId = parser.getLongValue();
						if ( sourceId != null ) {
							meta = metadataProvider.metadataForObjectSource(objectId, sourceId);
						}
						break;

					case "sourceId":
						sourceId = parser.getText();
						if ( objectId != null ) {
							meta = metadataProvider.metadataForObjectSource(objectId, sourceId);
						}
						break;

					case "streamId":
						streamId = UUID.fromString(parser.getText());
						meta = metadataProvider.metadataForStreamId(streamId);
						break;

					case "ts":
						if ( parser.getCurrentToken() == JsonToken.VALUE_NUMBER_INT ) {
							// parse as millisecond epoch value
							timestamp = Instant.ofEpochMilli(parser.getLongValue());
						} else {
							// parse as ISO 8601
							timestamp = ISO_INSTANT.parse(parser.getText(), Instant::from);
						}
						break;

					case "samples":
						if ( parser.getCurrentToken() == JsonToken.START_OBJECT ) {
							props = parseDatumSamples(parser, meta);
						}
						break;

					case "stats":
						if ( parser.getCurrentToken() == JsonToken.START_OBJECT ) {
							stats = parseDatumSamplesStatistics(parser, meta);
						}
						break;

				}
			}
		}

		// TODO
		return new AggregateDatumEntity(meta != null ? meta.getStreamId() : streamId, timestamp, kind,
				props, stats);
	}

	public static DatumProperties parseDatumSamples(JsonParser parser, ObjectDatumStreamMetadata meta)
			throws IOException {
		BigDecimal[] instantaneous = null;
		BigDecimal[] accumulating = null;
		String[] status = null;
		String[] tags = null;
		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				parser.nextToken();
				switch (parser.getCurrentName()) {
					case "i":
						instantaneous = decimalArrayForSamplesType(parser, meta,
								GeneralDatumSamplesType.Instantaneous);
						break;

					case "a":
						accumulating = decimalArrayForSamplesType(parser, meta,
								GeneralDatumSamplesType.Accumulating);
						break;

					case "s":
						status = stringArrayForSamplesType(parser, meta, GeneralDatumSamplesType.Status);
						break;

					case "t":
						tags = stringArrayForSamplesType(parser, meta, GeneralDatumSamplesType.Tag);
						break;
				}
			}
		}
		return DatumProperties.propertiesOf(instantaneous, accumulating, status, tags);
	}

	public static DatumPropertiesStatistics parseDatumSamplesStatistics(JsonParser parser,
			ObjectDatumStreamMetadata meta) throws IOException {
		BigDecimal[][] instantaneous = null;
		BigDecimal[][] accumulating = null;
		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				parser.nextToken();
				switch (parser.getCurrentName()) {
					case "i":
						instantaneous = decimalArrayOfArraysForSamplesType(parser, meta,
								GeneralDatumSamplesType.Instantaneous);
						break;

					case "ra":
						accumulating = decimalArrayOfArraysForSamplesType(parser, meta,
								GeneralDatumSamplesType.Accumulating);
						break;
				}
			}
		}
		return DatumPropertiesStatistics.statisticsOf(instantaneous, accumulating);
	}

	private static BigDecimal[] decimalArrayForSamplesType(JsonParser parser,
			ObjectDatumStreamMetadata meta, GeneralDatumSamplesType type) throws IOException {
		BigDecimal[] result = null;
		if ( parser.getCurrentToken() == JsonToken.START_OBJECT ) {
			Map<String, BigDecimal> map = parseNumberPropertiesObject(parser);
			String[] names = meta.propertyNamesForType(type);
			if ( names != null ) {
				result = new BigDecimal[names.length];
				for ( int i = 0; i < names.length; i++ ) {
					result[i] = map.get(names[i]);
				}
			}
		}
		return result;
	}

	private static String[] stringArrayForSamplesType(JsonParser parser, ObjectDatumStreamMetadata meta,
			GeneralDatumSamplesType type) throws IOException {
		String[] result = null;
		if ( type == GeneralDatumSamplesType.Tag ) {
			if ( parser.getCurrentToken() == JsonToken.START_ARRAY ) {
				List<String> tags = new ArrayList<>(4);
				while ( parser.getCurrentToken() != JsonToken.END_ARRAY ) {
					parser.nextToken();
					if ( parser.getCurrentToken() == JsonToken.VALUE_STRING ) {
						tags.add(parser.getText());
					}
				}
				if ( !tags.isEmpty() ) {
					result = tags.toArray(new String[tags.size()]);
				}
			}
		} else if ( parser.getCurrentToken() == JsonToken.START_OBJECT ) {
			Map<String, String> map = parseStringPropertiesObject(parser);
			String[] names = meta.propertyNamesForType(type);
			if ( names != null ) {
				result = new String[names.length];
				for ( int i = 0; i < names.length; i++ ) {
					result[i] = map.get(names[i]);
				}
			}
		}
		return result;
	}

	private static BigDecimal[][] decimalArrayOfArraysForSamplesType(JsonParser parser,
			ObjectDatumStreamMetadata meta, GeneralDatumSamplesType type) throws IOException {
		BigDecimal[][] result = null;
		if ( parser.getCurrentToken() == JsonToken.START_OBJECT ) {
			Map<String, BigDecimal[]> map = parseNumberStatisticsObject(parser);
			String[] names = meta.propertyNamesForType(type);
			if ( names != null ) {
				result = new BigDecimal[names.length][];
				for ( int i = 0; i < names.length; i++ ) {
					result[i] = map.get(names[i]);
				}
			}
		}
		return result;
	}

	public static Map<String, BigDecimal> parseNumberPropertiesObject(JsonParser parser)
			throws IOException {
		Map<String, BigDecimal> map = new LinkedHashMap<>(8);
		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				t = parser.nextToken();
				if ( t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT ) {
					BigDecimal d = parser.getDecimalValue();
					if ( d != null ) {
						map.put(parser.getCurrentName(), d);
					}
				}
			}
		}
		return map;
	}

	public static Map<String, String> parseStringPropertiesObject(JsonParser parser) throws IOException {
		Map<String, String> map = new LinkedHashMap<>(8);
		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				t = parser.nextToken();
				String v = parser.getText();
				if ( v != null ) {
					map.put(parser.getCurrentName(), v);
				}
			}
		}
		return map;
	}

	public static Map<String, BigDecimal[]> parseNumberStatisticsObject(JsonParser parser)
			throws IOException {
		Map<String, BigDecimal[]> map = new LinkedHashMap<>(8);
		while ( parser.nextToken() != JsonToken.END_OBJECT ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.FIELD_NAME ) {
				String name = parser.getCurrentName();
				t = parser.nextToken();
				if ( t == JsonToken.START_ARRAY ) {
					BigDecimal[] d = parseDecimalArray(parser);
					if ( d != null ) {
						map.put(name, d);
					}
				}
			}
		}
		return map;
	}

	public static BigDecimal[] parseDecimalArray(JsonParser parser) throws IOException {
		List<BigDecimal> list = new ArrayList<>(3);
		while ( parser.nextToken() != JsonToken.END_ARRAY ) {
			JsonToken t = parser.currentToken();
			if ( t == null ) {
				return null;
			}
			if ( t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT ) {
				BigDecimal d = parser.getDecimalValue();
				if ( d != null ) {
					list.add(d);
				}
			}
		}
		return list.toArray(new BigDecimal[list.size()]);
	}

}
