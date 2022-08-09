/* ==================================================================
 * JsonFilteredResultsProcessor.java - 6/08/2022 9:40:58 am
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

package net.solarnetwork.central.support;

import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.DATA_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.RETURNED_RESULT_COUNT_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.STARTING_OFFSET_FIELD_NAME;
import static net.solarnetwork.codec.BasicObjectDatumStreamDataSetSerializer.TOTAL_RESULT_COUNT_FIELD_NAME;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Map;
import org.springframework.util.MimeType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Basic {@link FilteredResultsProcessor} that serializes using Jackson JSON.
 * 
 * @author matt
 * @version 1.0
 */
public class ObjectMapperFilteredResultsProcessor<R> extends AbstractFilteredResultsProcessor<R> {

	/** The success array field name. */
	public static final SerializedString SUCCESS_FIELD_NAME = new SerializedString("success");

	private final JsonGenerator generator;
	private final SerializerProvider provider;
	private final JsonSerializer<R> serializer;
	private final MimeType mimeType;

	private int resultIndex = 0;

	/**
	 * Constructor.
	 * 
	 * @param generator
	 *        the generator to use
	 * @param provider
	 *        the provider to use
	 * @param the
	 *        serializer to use, or {@literal null} to rely on the
	 *        {@link JsonGenerator}
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code serializer} is {@literal null}
	 */
	public ObjectMapperFilteredResultsProcessor(JsonGenerator generator, SerializerProvider provider,
			MimeType mimeType, JsonSerializer<R> serializer) {
		super();
		this.generator = requireNonNullArgument(generator, "generator");
		this.provider = requireNonNullArgument(provider, "provider");
		this.mimeType = requireNonNullArgument(mimeType, "mimeType");
		this.serializer = serializer;
		this.generator.enable(Feature.AUTO_CLOSE_TARGET);
	}

	@Override
	public MimeType getMimeType() {
		return mimeType;
	}

	@Override
	public void start(final Long totalResultCount, final Integer startingOffset,
			final Integer expectedResultCount, Map<String, ?> attributes) throws IOException {
		int count = 1 + (expectedResultCount != null ? 1 : 0) + (startingOffset != null ? 1 : 0)
				+ (totalResultCount != null ? 1 : 0);

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
	}

	@Override
	public void handleResultItem(R item) throws IOException {
		if ( resultIndex == 0 ) {
			generator.writeFieldName(DATA_FIELD_NAME);
			generator.writeStartArray();
		}
		if ( serializer != null ) {
			serializer.serialize(item, generator, provider);
		} else {
			generator.writeObject(item);
		}
		resultIndex++;
	}

	@Override
	public void flush() throws IOException {
		generator.flush();
	}

	@Override
	public void close() throws IOException {
		if ( !generator.isClosed() ) {
			if ( resultIndex > 0 ) {
				generator.writeEndArray();
			}
			generator.writeEndObject();
			generator.flush();
			generator.close();
		}
	}

}
