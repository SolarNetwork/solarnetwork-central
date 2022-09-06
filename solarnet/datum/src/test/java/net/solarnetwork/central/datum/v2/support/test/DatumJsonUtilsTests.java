/* ==================================================================
 * DatumUtilsTests.java - 7/10/2019 3:25:02 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link DatumJsonUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumJsonUtilsTests {

	/** Regex for a line starting with a {@literal #} comment character. */
	public static final Pattern COMMENT = Pattern.compile("\\s*#");

	@Test
	public void writePropertyValues_typical() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34") },
						new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On", "Happy" },
						null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(), equalTo(
				format("[%d,1.23,2.34,123456,\"On\",\"Happy\"]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writePropertyValues_nullValues() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23"), null },
						new BigDecimal[] { null, new BigDecimal("123456") },
						new String[] { "On", null, "Holy" }, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,1.23,null,null,123456,\"On\",null,\"Holy\"]",
						datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writePropertyValues_withTags() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23") },
						new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On" },
						new String[] { "A", "B" }));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(), equalTo(
				format("[%d,1.23,123456,\"On\",\"A\",\"B\"]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writePropertyValues_missingTimestamp() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), null, null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23") },
						new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On" }, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(), equalTo("[null,1.23,123456,\"On\"]"));
	}

	@Test
	public void writePropertyValues_missingInstantaneous() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, propertiesOf(null,
				new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On" }, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,123456,\"On\"]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writePropertyValues_missingAccumulation() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, propertiesOf(
				new BigDecimal[] { new BigDecimal("1.23") }, null, new String[] { "On" }, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,1.23,\"On\"]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writePropertyValues_missingStatus() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23") },
						new BigDecimal[] { new BigDecimal("123456") }, null, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,1.23,123456]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeStreamMetadata_typical() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"tz\":\"UTC\",\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noInstantaneous() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(randomUUID(), "UTC", null,
				new String[] { "four", "five" }, new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"tz\":\"UTC\",\"props\":[\"four\",\"five\",\"six\"],\"class\":{\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noAccumulating() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, null, new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"tz\":\"UTC\",\"props\":[\"one\",\"two\",\"three\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noStatus() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" }, null);
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"tz\":\"UTC\",\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"]}}"));
	}

	@Test
	public void writeStreamMetadata_none() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(randomUUID(), "UTC", null, null,
				null);
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(),
				equalTo("{\"tz\":\"UTC\",\"props\":null,\"class\":null}"));
	}

	@Test
	public void writeStream_typical() throws IOException {
		BasicDatumStreamMetadata metadata = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		UUID streamId = UUID.randomUUID();
		List<Datum> datum = Arrays.asList(
				(Datum) new DatumEntity(streamId, Instant.now().minusSeconds(3), null,
						propertiesOf(
								new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34"),
										new BigDecimal("3.45") },
								new BigDecimal[] { new BigDecimal("456"), new BigDecimal("567") },
								new String[] { "On" }, null)),
				new DatumEntity(
						streamId, Instant.now().minusSeconds(2), null,
						propertiesOf(
								new BigDecimal[] { new BigDecimal("1.234"), new BigDecimal("2.345"),
										new BigDecimal("3.456") },
								new BigDecimal[] { new BigDecimal("4567"), new BigDecimal("5678") },
								new String[] { "Onn" }, new String[] { "TAG" })),
				new DatumEntity(streamId, Instant.now().minusSeconds(1), null,
						propertiesOf(
								new BigDecimal[] { new BigDecimal("1.2345"), new BigDecimal("2.3456"),
										new BigDecimal("3.4567") },
								new BigDecimal[] { new BigDecimal("45678"), new BigDecimal("56789") },
								new String[] { "Onnn" }, null)));

		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStream(generator, streamId, metadata, datum.iterator(), datum.size());
		}
		assertThat("JSON object generated", out.toString(), equalTo("{\"streamId\":\""
				+ streamId.toString()
				+ "\",\"metadata\":{\"tz\":\"UTC\",\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"
				+ format(
						",\"values\":[[%d,1.23,2.34,3.45,456,567,\"On\"],[%d,1.234,2.345,3.456,4567,5678,\"Onn\",\"TAG\"],[%d,1.2345,2.3456,3.4567,45678,56789,\"Onnn\"]]}",
						datum.get(0).getTimestamp().toEpochMilli(),
						datum.get(1).getTimestamp().toEpochMilli(),
						datum.get(2).getTimestamp().toEpochMilli())));

	}

	private static BigDecimal[] arrayOfDecimals(String... values) {
		if ( values == null ) {
			return null;
		}
		BigDecimal[] result = new BigDecimal[values.length];
		for ( int i = 0, len = values.length; i < len; i++ ) {
			result[i] = (values[i] != null ? new BigDecimal(values[i]) : null);
		}
		return result;
	}

	@Test
	public void writeStatisticValues_typical() throws IOException {
		// @formatter:off
		AggregateDatumEntity datum = new AggregateDatumEntity(
				UUID.randomUUID(), Instant.now(), Aggregation.Hour,
				propertiesOf(
						arrayOfDecimals("1.23", "2.34"), 
						arrayOfDecimals("123456"),
						null, null),
				statisticsOf(
						new BigDecimal[][] {
								arrayOfDecimals("1.11", "2.22", "60"),
								arrayOfDecimals("2.22", "3.33", "59") },
						new BigDecimal[][] {
								arrayOfDecimals("0", "123456") }));
		// @formatter:on
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStatisticValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,[1.11,2.22,60],[2.22,3.33,59],[0,123456]]",
						datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeStatisticValues_nullValues() throws IOException {
		// @formatter:off
		AggregateDatumEntity datum = new AggregateDatumEntity(
				UUID.randomUUID(), Instant.now(), Aggregation.Hour,
				propertiesOf(
						arrayOfDecimals("1.23", null), 
						arrayOfDecimals(null, "123456"),
						null, null),
				statisticsOf(
						new BigDecimal[][] {
								arrayOfDecimals("1.11", "2.22", "60"),
								null },
						new BigDecimal[][] {
								null,
								arrayOfDecimals("0", "123456") }));
		// @formatter:on
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStatisticValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,[1.11,2.22,60],null,null,[0,123456]]",
						datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeStatisticValues_missingTimestamp() throws IOException {
		// @formatter:off
		AggregateDatumEntity datum = new AggregateDatumEntity(
				UUID.randomUUID(), null, Aggregation.Hour,
				propertiesOf(
						arrayOfDecimals("1.23"),
						null,
						null, null),
				statisticsOf(
						new BigDecimal[][] {
								arrayOfDecimals("1.11", "2.22", "60") },
						null));
		// @formatter:on
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStatisticValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(), equalTo("[null,[1.11,2.22,60]]"));
	}

	@Test
	public void writeStatisticValues_missingInstantaneous() throws IOException {
		// @formatter:off
		AggregateDatumEntity datum = new AggregateDatumEntity(
				UUID.randomUUID(), Instant.now(), Aggregation.Hour,
				propertiesOf(
						null, 
						arrayOfDecimals("123456"),
						null, null),
				statisticsOf(
						null,
						new BigDecimal[][] {
								arrayOfDecimals("0", "123456") }));
		// @formatter:on
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStatisticValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,[0,123456]]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeStatisticValues_missingAccumulation() throws IOException {
		// @formatter:off
		AggregateDatumEntity datum = new AggregateDatumEntity(
				UUID.randomUUID(), Instant.now(), Aggregation.Hour,
				propertiesOf(
						arrayOfDecimals("1.23"),
						null,
						null, null),
				statisticsOf(
						new BigDecimal[][] {
							arrayOfDecimals("1.11", "2.22", "60") },
						null));
		// @formatter:on
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeStatisticValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,[1.11,2.22,60]]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeAggregateStream_typical() throws IOException {
		BasicDatumStreamMetadata metadata = new BasicDatumStreamMetadata(randomUUID(), "UTC",
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		UUID streamId = UUID.randomUUID();
		// @formatter:off
		List<AggregateDatum> datum = Arrays.asList(
				new AggregateDatumEntity(
						UUID.randomUUID(), Instant.now().minusSeconds(3), Aggregation.Hour,
						propertiesOf(
								arrayOfDecimals("1.23", "2.34", "3.45"),
								arrayOfDecimals("456", "567"),
								new String[] {"On"},
								null),
						statisticsOf(
								new BigDecimal[][] {
									arrayOfDecimals("1.11", "2.22", "60"),
									arrayOfDecimals("2.22", "3.33", "59"),
									arrayOfDecimals("3.33", "4.44", "58"),
									},
								new BigDecimal[][] {
									arrayOfDecimals("0", "456"),
									arrayOfDecimals("0", "567"),
									}
								)),
				new AggregateDatumEntity(
						UUID.randomUUID(), Instant.now().minusSeconds(2), Aggregation.Hour,
						propertiesOf(
								arrayOfDecimals("1.234", "2.345", "3.456"),
								arrayOfDecimals("4567", "5678"),
								new String[] {"Onn"},
								new String[] { "TAG" }),
						statisticsOf(
								new BigDecimal[][] {
									arrayOfDecimals("1.111", "2.222", "600"),
									arrayOfDecimals("2.222", "3.333", "599"),
									arrayOfDecimals("3.333", "4.444", "588"),
									},
								new BigDecimal[][] {
									arrayOfDecimals("456", "5023"),
									arrayOfDecimals("567", "6245"),
									}
								)),
				new AggregateDatumEntity(
						UUID.randomUUID(), Instant.now().minusSeconds(1), Aggregation.Hour,
						propertiesOf(
								arrayOfDecimals("1.2345", "2.3456", "3.4567"),
								arrayOfDecimals("45678", "56789"),
								new String[] {"Onnn"},
								null),
						statisticsOf(
								new BigDecimal[][] {
									arrayOfDecimals("1.1111", "2.2222", "6000"),
									arrayOfDecimals("2.2222", "3.3333", "5999"),
									arrayOfDecimals("3.3333", "4.4444", "5888"),
									},
								new BigDecimal[][] {
									arrayOfDecimals("5023", "50701"),
									arrayOfDecimals("6245", "63034"),
									}
								))
				);
		// @formatter:on

		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumJsonUtils.writeAggregateStream(generator, streamId, metadata, datum.iterator(),
					datum.size());
		}

		// @formatter:off
		assertThat("JSON object generated", out.toString(), equalTo("{\"streamId\":\""
				+ streamId.toString()
				+ "\",\"metadata\":{\"tz\":\"UTC\",\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],"
				+ "\"class\":{\"i\":[\"one\",\"two\",\"three\"],"
				+ "\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"
				+ format(",\"values\":[" 
								+ "[%d,1.23,2.34,3.45,456,567,\"On\"],"
								+ "[%d,[1.11,2.22,60],[2.22,3.33,59],[3.33,4.44,58],[0,456],[0,567]],"
								+ "[%d,1.234,2.345,3.456,4567,5678,\"Onn\",\"TAG\"],"
								+ "[%d,[1.111,2.222,600],[2.222,3.333,599],[3.333,4.444,588],[456,5023],[567,6245]],"
								+ "[%d,1.2345,2.3456,3.4567,45678,56789,\"Onnn\"],"
								+ "[%d,[1.1111,2.2222,6000],[2.2222,3.3333,5999],[3.3333,4.4444,5888],[5023,50701],[6245,63034]]"
								+ "]}",
						datum.get(0).getTimestamp().toEpochMilli(),
						datum.get(0).getTimestamp().toEpochMilli(),
						datum.get(1).getTimestamp().toEpochMilli(),
						datum.get(1).getTimestamp().toEpochMilli(),
						datum.get(2).getTimestamp().toEpochMilli(),
						datum.get(2).getTimestamp().toEpochMilli()
						)));
		// @formatter:on
	}

	private static List<AggregateDatum> loadAggregateDatum(String resource, Class<?> clazz,
			JsonFactory factory, ObjectDatumStreamMetadataProvider streamIdProvider) throws IOException {
		List<AggregateDatum> result = new ArrayList<>();
		int row = 0;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				row++;
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}

				JsonParser parser = factory.createParser(line);
				AggregateDatum d = DatumJsonUtils.parseAggregateDatum(parser, streamIdProvider);
				assertThat(format("Parsed JSON aggregate datum in line %d", row), d, notNullValue());
				result.add(d);
			}
		}
		return result;
	}

	/**
	 * Create a {@link Matcher} for an array of {@link BigDecimal} values.
	 * 
	 * @param nums
	 *        the string numbers, which will be parsed as {@link BigDecimal}
	 *        instances
	 * @return the matcher
	 */
	public static Matcher<BigDecimal[]> arrayContainingDecimals(String... nums) {
		BigDecimal[] vals = new BigDecimal[nums.length];
		for ( int i = 0; i < nums.length; i++ ) {
			vals[i] = new BigDecimal(nums[i]);
		}
		return Matchers.arrayContaining(vals);
	}

	@Test
	public void parseAggregateDatum_mappedNodeSourceId() throws IOException {
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "x", "y" }, new String[] { "w" }, null);
		List<AggregateDatum> list = loadAggregateDatum("test-agg-datum-01.txt", getClass(),
				new JsonFactory(), staticProvider(singleton(meta)));
		assertThat("Parsed agg datum", list, hasSize(1));

		AggregateDatum d = list.get(0);
		assertThat("Stream ID mapped from parsed node + source ID", d.getStreamId(), equalTo(streamId));

		Instant timestamp = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
		assertThat("Timestamp parsed", d.getTimestamp(), equalTo(timestamp));

		assertThat("Aggregation parsed", d.getAggregation(), equalTo(Aggregation.Hour));

		DatumProperties props = d.getProperties();
		assertThat("Datum properties parsed", props, notNullValue());
		assertThat("Agg instantaneous", props.getInstantaneous(), arrayContainingDecimals("1.2", "2.1"));
		assertThat("Agg accumulating", props.getAccumulating(), arrayContainingDecimals("100"));

		DatumPropertiesStatistics stats = d.getStatistics();
		assertThat("Datum statistics parsed", stats, notNullValue());
		assertThat("Stats instantaneous", stats.getInstantaneous(),
				arrayContaining(arrayContainingDecimals(new String[] { "6", "1.2", "1.7" }),
						arrayContainingDecimals(new String[] { "6", "2.1", "7.1" })));
		assertThat("Stats accumulating", stats.getAccumulating(),
				arrayContaining(arrayContainingDecimals(new String[] { "100", "120", "20" })));
	}

}
