/* ==================================================================
 * ObjectMapperStreamDatumFilteredResultsProcessor.java - 1/05/2022 5:32:46 pm
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

import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.DATA_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.META_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.RETURNED_RESULT_COUNT_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.STARTING_OFFSET_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.TOTAL_RESULT_COUNT_FIELD_NAME;
import static net.solarnetwork.codec.JsonUtils.writeDecimalArrayValues;
import static net.solarnetwork.codec.JsonUtils.writeStringArrayValues;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.MimeType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.codec.BasicObjectDatumStreamMetadataSerializer;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.ArrayUtils;

/**
 * {@link FilteredResultsProcessor} for encoding overall results into datum
 * stream form.
 * 
 * <p>
 * The overall structure has the metadata followed by the data:
 * </p>
 * 
 * <pre>
 * <code>{
 *   "returnedResultCount" : &lt;count&gt;,
 *   "startingOffset"      : &lt;offset&gt;,
 *   "totalResultCount"    : &lt;count&gt;,
 *   "meta" : [
 *     {
 *       // ObjectDatumStreamMetadata
 *     },
 *     ...
 *   ],
 *   "data" : [
 *     [ // Datum ],
 *     ...
 *   ]
 * }</code>
 * </pre>
 * 
 * <p>
 * For {@link Datum} results, each {@code data} element is an array with the
 * following:
 * </p>
 * 
 * <ol>
 * <li>0-based index of the associated stream metadata object in the
 * {@literal meta} array</li>
 * <li>timestamp, in millisecond epoch form</li>
 * <li>instantaneous property values (elements in order of the {@code meta.i}
 * array)</li>
 * <li>accumulating property values (elements in order of the {@code meta.a}
 * array)</li>
 * <li>status property values (elements in order of the {@code meta.s}
 * array)</li>
 * <li>tags (one element per tag)</li>
 * </ol>
 * 
 * <p>
 * The {@link Datum} structure resembles this:
 * </p>
 * 
 * <pre>
 * <code>[
 *   &lt;meta index&gt;,
 *   &lt;timestamp&gt;,
 *   &lt;i data&gt;,
 *   ...,
 *   &lt;a data&gt;,
 *   ...,
 *   &lt;s data&gt;,
 *   ...,
 *   &lt;tag*&gt;,
 *   ...,
 * ]</code>
 * </pre>
 * 
 * <p>
 * For {@link AggregateDatum} results, each {@code data} element is an array
 * with the following:
 * </p>
 * 
 * <ol>
 * <li>0-based index of the associated stream metadata object in the
 * {@literal meta} array</li>
 * <li>2-element array with the reading starting and ending timestamps, each in
 * millisecond epoch form</li>
 * <li>4-element arrays with the property value, count, minimum, and maximum,
 * for each instantaneous property value (elements in order of the
 * {@code meta.i} array); values may be {@literal null} if no instantaneous
 * property data is available</li>
 * <li>3-element arrays with the accumulating property value, starting value,
 * ending value, for each accumulating property value (elements in order of the
 * {@code meta.a} array)</li>
 * </ol>
 * 
 * <p>
 * The {@link AggregateDatum} structure resembles this:
 * </p>
 * 
 * <pre>
 * <code>[
 *   &lt;meta index&gt;,
 *   [&lt;timestamp start&gt;, &lt;timestamp end&gt;],
 *   [&lt;i val&gt;, &lt;count&gt;, &lt;min&gt;, &lt;max&gt;],
 *   ...,
 *   [&lt;a val&gt;, &lt;start val&gt;, &lt;end val&gt;],
 *   ...,
 * ]</code>
 * </pre>
 * 
 * @author matt
 * @version 1.1
 * @since 1.3
 */
public final class ObjectMapperStreamDatumFilteredResultsProcessor
		implements StreamDatumFilteredResultsProcessor {

	/** The success array field name. */
	public static final SerializedString SUCCESS_FIELD_NAME = new SerializedString("success");

	private final JsonGenerator generator;
	private final SerializerProvider provider;
	private final MimeType mimeType;

	private ObjectDatumStreamMetadataProvider metadataProvider;
	private Collection<UUID> streamIds;
	private Map<UUID, Integer> metaIndexMap;
	private int resultIndex = 0;

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 * @param provider
	 *        the provider to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ObjectMapperStreamDatumFilteredResultsProcessor(JsonGenerator generator,
			SerializerProvider provider, MimeType mimeType) {
		super();
		this.generator = requireNonNullArgument(generator, "generator");
		this.provider = requireNonNullArgument(provider, "provider");
		this.mimeType = requireNonNullArgument(mimeType, "mimeType");
		this.generator.enable(Feature.AUTO_CLOSE_TARGET);
	}

	@Override
	public MimeType getMimeType() {
		return mimeType;
	}

	@Override
	public void start(final Long totalResultCount, final Integer startingOffset,
			final Integer expectedResultCount, Map<String, ?> attributes) throws IOException {
		if ( attributes == null || !(attributes
				.get(METADATA_PROVIDER_ATTR) instanceof ObjectDatumStreamMetadataProvider) ) {
			throw new IllegalArgumentException("No metadata provider provided.");
		}
		this.metadataProvider = (ObjectDatumStreamMetadataProvider) attributes
				.get(METADATA_PROVIDER_ATTR);
		this.streamIds = metadataProvider.metadataStreamIds();
		this.metaIndexMap = new HashMap<>(streamIds.size());

		int count = 1 + (expectedResultCount != null ? 1 : 0) + (startingOffset != null ? 1 : 0)
				+ (totalResultCount != null ? 1 : 0)
				+ (streamIds != null && !streamIds.isEmpty() ? 2 : 0);

		generator.writeStartObject(this, count);
		generator.writeFieldName(SUCCESS_FIELD_NAME);
		generator.writeBoolean(true);

		if ( expectedResultCount != null ) {
			generator.writeFieldName(RETURNED_RESULT_COUNT_FIELD_NAME);
			generator.writeNumber(expectedResultCount);
		}
		if ( startingOffset != null ) {
			generator.writeFieldName(STARTING_OFFSET_FIELD_NAME);
			generator.writeNumber(startingOffset);
		}
		if ( totalResultCount != null ) {
			generator.writeFieldName(TOTAL_RESULT_COUNT_FIELD_NAME);
			generator.writeNumber(totalResultCount);
		}

		if ( streamIds != null && !streamIds.isEmpty() ) {
			generator.writeFieldName(META_FIELD_NAME);
			generator.writeStartArray(metadataProvider, streamIds.size());
			int i = 0;
			for ( UUID streamId : streamIds ) {
				metaIndexMap.put(streamId, i);
				BasicObjectDatumStreamMetadataSerializer.INSTANCE
						.serialize(metadataProvider.metadataForStreamId(streamId), generator, provider);
				i++;
			}
			generator.writeEndArray();

			generator.writeFieldName(DATA_FIELD_NAME);
			generator.writeStartArray();
		}
	}

	@Override
	public void handleResultItem(StreamDatum d) throws IOException {
		final ObjectDatumStreamMetadata meta = metadataProvider.metadataForStreamId(d.getStreamId());
		if ( meta == null ) {
			throw new JsonMappingException(generator, String.format(
					"Metadata for stream %s not available for datum %d", d.getStreamId(), resultIndex));
		}
		final String[] iNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
		final String[] aNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
		final String[] sNames = meta.propertyNamesForType(DatumSamplesType.Status);
		final int iLen = (iNames != null ? iNames.length : 0);
		final int aLen = (aNames != null ? aNames.length : 0);
		final int sLen = (sNames != null ? sNames.length : 0);
		final int baseLen = (1 + iLen + aLen + sLen);
		final DatumProperties p = d.getProperties();
		final long ts = (d.getTimestamp() != null ? d.getTimestamp().toEpochMilli() : 0);
		int tLen = (p != null ? p.getTagsLength() : 0);
		int totalLen = 1 + baseLen + tLen;

		generator.writeStartArray(d, totalLen);
		generator.writeNumber(metaIndexMap.get(d.getStreamId()));
		if ( d instanceof AggregateDatum ) {
			AggregateDatum rd = (AggregateDatum) d;
			generator.writeStartArray(d, 2);
			generator.writeNumber(ts);
			if ( rd instanceof ReadingDatum && ((ReadingDatum) rd).getEndTimestamp() != null ) {
				generator.writeNumber(((ReadingDatum) rd).getEndTimestamp().toEpochMilli());
			} else {
				generator.writeNull();
			}
			generator.writeEndArray();

			DatumPropertiesStatistics stats = rd.getStatistics();
			if ( stats != null ) {
				writeAggregateProperty(generator, DatumSamplesType.Instantaneous, iLen,
						p.getInstantaneous(), stats.getInstantaneous());
				writeAggregateProperty(generator, DatumSamplesType.Accumulating, aLen,
						p.getAccumulating(), stats.getAccumulating());
				if ( sLen > 0 || tLen > 0 ) {
					// if status or tags provided try to optimize them away if
					if ( !ArrayUtils.isOnlyNull(p.getStatus()) || tLen > 0 ) {
						writeStringArrayValues(generator, p.getStatus(), sLen);
					}
					writeStringArrayValues(generator, p.getTags(), tLen);
				}
			} else {
				generator.writeNull();
			}
		} else if ( p != null ) {
			generator.writeNumber(ts);
			writeDecimalArrayValues(generator, p.getInstantaneous(), iLen);
			writeDecimalArrayValues(generator, p.getAccumulating(), aLen);
			writeStringArrayValues(generator, p.getStatus(), sLen);
			writeStringArrayValues(generator, p.getTags(), tLen);
		}
		generator.writeEndArray();
		resultIndex++;
	}

	private static void writeAggregateProperty(final JsonGenerator generator,
			final DatumSamplesType type, final int len, final BigDecimal[] values,
			final BigDecimal[][] statValues) throws IOException {
		for ( int i = 0; i < len; i++ ) {
			BigDecimal[] sv = (statValues != null && statValues.length > i ? statValues[i] : null);
			int arrayLen = (sv != null ? sv.length : 0);
			if ( type == DatumSamplesType.Instantaneous && values != null && values.length > i ) {
				arrayLen++;
			}
			if ( arrayLen > 0 ) {
				generator.writeStartArray(sv, arrayLen);
				if ( type == DatumSamplesType.Instantaneous && values != null ) {
					BigDecimal v = values[i];
					if ( v != null ) {
						generator.writeNumber(v);
					} else {
						generator.writeNull();
					}
				}
				if ( sv != null ) {
					for ( BigDecimal v : sv ) {
						if ( v != null ) {
							generator.writeNumber(v);
						} else {
							generator.writeNull();
						}
					}
				}
				generator.writeEndArray();
			} else {
				generator.writeNull();
			}
		}
	}

	@Override
	public void flush() throws IOException {
		generator.flush();
	}

	@Override
	public void close() throws IOException {
		if ( !generator.isClosed() ) {
			if ( streamIds != null && !streamIds.isEmpty() ) {
				generator.writeEndArray();
			}
			generator.writeEndObject();
			generator.flush();
			generator.close();
		}
	}

}
