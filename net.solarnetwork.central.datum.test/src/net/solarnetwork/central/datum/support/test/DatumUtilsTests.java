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

package net.solarnetwork.central.datum.support.test;

import static java.lang.String.format;
import static net.solarnetwork.central.datum.domain.DatumProperties.propertiesOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.solarnetwork.central.datum.domain.BasicDatumStreamMetadata;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumEntity;
import net.solarnetwork.central.datum.support.DatumUtils;

/**
 * Test cases for the {@link DatumUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumUtilsTests {

	@Test
	public void filterSources_startWithSlash() {
		// GIVEN
		Set<String> sources = new LinkedHashSet<>(
				Arrays.asList("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1", "/power/switch/2",
						"/power/switch/3", "/power/switch/grid", "ADAM-4118", "thermistor"));

		// WHEN
		Set<String> result = DatumUtils.filterSources(sources, new AntPathMatcher(), "/**");

		// THEN
		assertThat("Sources filtered", result,
				Matchers.containsInAnyOrder("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1",
						"/power/switch/2", "/power/switch/3", "/power/switch/grid"));
	}

	@Test
	public void filterSources_startWith2Char() {
		// GIVEN
		Set<String> sources = new LinkedHashSet<>(
				Arrays.asList("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1", "/power/switch/2",
						"/power/switch/3", "/power/switch/grid", "ADAM-4118", "thermistor"));

		// WHEN
		Set<String> result = DatumUtils.filterSources(sources, new AntPathMatcher(), "/??/**");

		// THEN
		assertThat("Sources filtered", result,
				Matchers.containsInAnyOrder("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1"));
	}

	@Test
	public void writePropertyValues_typical() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null,
				propertiesOf(new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34") },
						new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On", "Happy" },
						null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writePropertyValuesArray(generator, datum);
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
			DatumUtils.writePropertyValuesArray(generator, datum);
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
			DatumUtils.writePropertyValuesArray(generator, datum);
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
			DatumUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(), equalTo("[null,1.23,123456,\"On\"]"));
	}

	@Test
	public void writePropertyValues_missingInstantaneous() throws IOException {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), null, propertiesOf(null,
				new BigDecimal[] { new BigDecimal("123456") }, new String[] { "On" }, null));
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writePropertyValuesArray(generator, datum);
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
			DatumUtils.writePropertyValuesArray(generator, datum);
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
			DatumUtils.writePropertyValuesArray(generator, datum);
		}
		assertThat("JSON array generated", out.toString(),
				equalTo(format("[%d,1.23,123456]", datum.getTimestamp().toEpochMilli())));
	}

	@Test
	public void writeStreamMetadata_typical() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(
				new String[] { "one", "two", "three" }, new String[] { "four", "five" },
				new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noInstantaneous() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(null,
				new String[] { "four", "five" }, new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"props\":[\"four\",\"five\",\"six\"],\"class\":{\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noAccumulating() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(
				new String[] { "one", "two", "three" }, null, new String[] { "six" });
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"props\":[\"one\",\"two\",\"three\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"s\":[\"six\"]}}"));
	}

	@Test
	public void writeStreamMetadata_noStatus() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(
				new String[] { "one", "two", "three" }, new String[] { "four", "five" }, null);
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo(
				"{\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"]}}"));
	}

	@Test
	public void writeStreamMetadata_none() throws IOException {
		BasicDatumStreamMetadata meta = new BasicDatumStreamMetadata(null, null, null);
		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStreamMetadata(generator, meta);
		}
		assertThat("JSON object generated", out.toString(), equalTo("{\"props\":null,\"class\":null}"));
	}

	@Test
	public void writeStream_typical() throws IOException {
		BasicDatumStreamMetadata metadata = new BasicDatumStreamMetadata(
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
				new DatumEntity(streamId, Instant.now().minusSeconds(2), null,
						propertiesOf(
								new BigDecimal[] { new BigDecimal("1.2345"), new BigDecimal("2.3456"),
										new BigDecimal("3.4567") },
								new BigDecimal[] { new BigDecimal("45678"), new BigDecimal("56789") },
								new String[] { "Onnn" }, null)));

		StringWriter out = new StringWriter();
		try (JsonGenerator generator = JsonFactory.builder().build().createGenerator(out)) {
			DatumUtils.writeStream(generator, streamId, metadata, datum.iterator(), datum.size());
		}
		assertThat("JSON object generated", out.toString(), equalTo("{\"streamId\":\""
				+ streamId.toString()
				+ "\",\"metadata\":{\"props\":[\"one\",\"two\",\"three\",\"four\",\"five\",\"six\"],\"class\":{\"i\":[\"one\",\"two\",\"three\"],\"a\":[\"four\",\"five\"],\"s\":[\"six\"]}}"
				+ format(
						",\"values\":[[%d,1.23,2.34,3.45,456,567,\"On\"],[%d,1.234,2.345,3.456,4567,5678,\"Onn\",\"TAG\"],[%d,1.2345,2.3456,3.4567,45678,56789,\"Onnn\"]]}",
						datum.get(0).getTimestamp().toEpochMilli(),
						datum.get(1).getTimestamp().toEpochMilli(),
						datum.get(2).getTimestamp().toEpochMilli())));

	}

}
