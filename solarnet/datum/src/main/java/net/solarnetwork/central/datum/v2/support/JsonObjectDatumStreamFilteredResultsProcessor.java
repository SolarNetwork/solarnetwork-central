/* ==================================================================
 * JsonObjectDatumStreamFilteredResultsProcessor.java - 1/05/2022 5:32:46 pm
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.codec.BasicObjectDatumStreamMetadataSerializer;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * {@link FilteredResultsProcessor} for encoding {@link Datum} results into
 * datum stream JSON form.
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
 *     [&lt;meta index&gt;, &lt;timestamp&gt;, &lt;i data&gt;..., &lt;a data&gt;..., &lt;s data&gt;..., &lt;tags*&gt;...],
 *     ...
 *   ]
 * }</code>
 * </pre>
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class JsonObjectDatumStreamFilteredResultsProcessor
		implements StreamDatumFilteredResultsProcessor {

	private final JsonGenerator generator;
	private final SerializerProvider provider;

	private ObjectDatumStreamMetadataProvider metadataProvider;
	private Collection<UUID> streamIds;
	private Map<UUID, Integer> metaIndexMap;
	private int resultIndex = 0;

	/**
	 * Constructor.
	 */
	public JsonObjectDatumStreamFilteredResultsProcessor(JsonGenerator generator,
			SerializerProvider provider) {
		super();
		this.generator = requireNonNullArgument(generator, "generator");
		this.provider = requireNonNullArgument(provider, "provider");
		this.generator.enable(Feature.AUTO_CLOSE_TARGET);
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

		int count = (expectedResultCount != null ? 1 : 0) + (startingOffset != null ? 1 : 0)
				+ (totalResultCount != null ? 1 : 0)
				+ (streamIds != null && !streamIds.isEmpty() ? 2 : 0);

		generator.writeStartObject(this, count);

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
		generator.writeNumber(ts);
		if ( p != null ) {
			writeDecimalArrayValues(generator, p.getInstantaneous(), iLen);
			writeDecimalArrayValues(generator, p.getAccumulating(), aLen);
			writeStringArrayValues(generator, p.getStatus(), sLen);
			writeStringArrayValues(generator, p.getTags(), tLen);
		}
		generator.writeEndArray();
		resultIndex++;
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
