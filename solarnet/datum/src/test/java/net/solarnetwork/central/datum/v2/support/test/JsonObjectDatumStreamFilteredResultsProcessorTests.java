/* ==================================================================
 * JsonObjectDatumStreamFilteredResultsProcessorTests.java - 1/05/2022 6:02:55 pm
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

package net.solarnetwork.central.datum.v2.support.test;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.BasicObjectDatumStreamDataSet.dataSet;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.v2.support.JsonObjectDatumStreamFilteredResultsProcessor;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamDataSet;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.ByteUtils;

/**
 * Test cases for the {@link JsonObjectDatumStreamFilteredResultsProcessor}
 * class.
 * 
 * @author matt
 * @version 1.0
 */
public class JsonObjectDatumStreamFilteredResultsProcessorTests {

	private ObjectMapper mapper;

	private ObjectMapper createObjectMapper() {
		ObjectMapper m = new ObjectMapper();
		return m;
	}

	@Before
	public void setup() {
		mapper = createObjectMapper();
	}

	private ObjectDatumStreamMetadata nodeMeta(Long nodeId, String sourceId, String[] i, String[] a,
			String[] s) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, sourceId, i, a, s);
	}

	@Test
	public void oneStream() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = nodeMeta(123L, "test/source", new String[] { "a", "b" },
				new String[] { "c" }, new String[] { "d" });
		Instant start = Instant
				.from(LocalDateTime.of(2022, 4, 29, 13, 52).atZone(ZoneId.of("Pacific/Auckland")));

		DatumProperties p1 = new DatumProperties();
		p1.setInstantaneous(decimalArray("1.23", "2.34"));
		p1.setAccumulating(decimalArray("3.45"));
		p1.setStatus(new String[] { "foo" });
		p1.setTags(new String[] { "a" });
		StreamDatum d1 = new BasicStreamDatum(meta.getStreamId(), start, p1);

		DatumProperties p2 = new DatumProperties();
		p2.setInstantaneous(decimalArray("3.21", "4.32"));
		p2.setAccumulating(decimalArray("5.43"));
		p2.setStatus(new String[] { "bar" });
		StreamDatum d2 = new BasicStreamDatum(meta.getStreamId(), start.plusSeconds(1), p2);

		BasicObjectDatumStreamDataSet<StreamDatum> data = dataSet(asList(meta), asList(d1, d2));

		// WHEN
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (JsonObjectDatumStreamFilteredResultsProcessor processor = new JsonObjectDatumStreamFilteredResultsProcessor(
				mapper.createGenerator(out), mapper.getSerializerProviderInstance())) {
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			processor.handleResultItem(d1);
			processor.handleResultItem(d2);
		}

		// THEN
		String json = out.toString(ByteUtils.UTF8);
		assertThat("JSON", json, is(format("{\"meta\":[{\"streamId\":\"%s\",", meta.getStreamId())
				+ "\"zone\":\"Pacific/Auckland\",\"kind\":\"n\",\"objectId\":123,"
				+ "\"sourceId\":\"test/source\",\"i\":[\"a\",\"b\"],\"a\":[\"c\"],\"s\":[\"d\"]}],"
				+ "\"data\":[[0,1651197120000,1.23,2.34,3.45,\"foo\",\"a\"],"
				+ "[0,1651197121000,3.21,4.32,5.43,\"bar\"]]}"));
	}

}
