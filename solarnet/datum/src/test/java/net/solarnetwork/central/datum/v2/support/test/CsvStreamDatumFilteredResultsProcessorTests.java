/* ==================================================================
 * CsvStreamDatumFilteredResultsProcessorTests.java - 17/04/2023 3:46:02 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.BasicObjectDatumStreamDataSet.dataSet;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.support.CsvStreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamDataSet;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Test cases for the {@link CsvStreamDatumFilteredResultsProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CsvStreamDatumFilteredResultsProcessorTests {

	private ObjectDatumStreamMetadata nodeMeta(Long nodeId, String sourceId, String[] i, String[] a,
			String[] s) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, sourceId, i, a, s);
	}

	@Test
	public void oneStream_datum() throws IOException {
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
		StringWriter out = new StringWriter();
		try (CsvStreamDatumFilteredResultsProcessor processor = new CsvStreamDatumFilteredResultsProcessor(
				out)) {
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			processor.handleResultItem(d1);
			processor.handleResultItem(d2);
		}

		// THEN
		String csv = out.toString();
		assertThat("Datum CSV", csv, is("""
				ts,streamId,objectId,sourceId,a,b,c,d,tags\r
				2022-04-29T01:52:00Z,%1$s,123,test/source,1.23,2.34,3.45,foo,a\r
				2022-04-29T01:52:01Z,%1$s,123,test/source,3.21,4.32,5.43,bar,\r
				""".formatted(meta.getStreamId())));
	}

	@Test
	public void multiStream_datum() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = nodeMeta(123L, "test/source", new String[] { "a", "b" },
				new String[] { "c" }, new String[] { "d" });
		ObjectDatumStreamMetadata meta2 = nodeMeta(123L, "test/source", new String[] { "b", "c" }, null,
				new String[] { "d" });
		ObjectDatumStreamMetadata meta3 = nodeMeta(123L, "test/source", new String[] { "e" },
				new String[] { "f" }, null);
		Instant start = Instant
				.from(LocalDateTime.of(2022, 4, 29, 13, 52).atZone(ZoneId.of("Pacific/Auckland")));

		final List<ObjectDatumStreamMetadata> metas = asList(meta1, meta2, meta3);
		final int datumCount = 2;
		final List<StreamDatum> datum = new ArrayList<>(datumCount * metas.size());

		DatumProperties p;

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("1.1", "1.2"));
		p.setAccumulating(decimalArray("1.3"));
		p.setStatus(new String[] { "foo" });
		p.setTags(new String[] { "t1" });
		datum.add(new BasicStreamDatum(meta1.getStreamId(), start, p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("10.1", "10.2"));
		p.setStatus(new String[] { "bar" });
		p.setTags(new String[] { "t2" });
		datum.add(new BasicStreamDatum(meta2.getStreamId(), start, p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("100.1"));
		p.setAccumulating(decimalArray("100.2"));
		datum.add(new BasicStreamDatum(meta3.getStreamId(), start, p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("2.1", "2.2"));
		p.setAccumulating(decimalArray("2.3"));
		p.setTags(new String[] { "t1.1" });
		datum.add(new BasicStreamDatum(meta1.getStreamId(), start.plusSeconds(1), p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("20.1", "20.2"));
		p.setStatus(new String[] { "bar.1" });
		p.setTags(new String[] { "t2.1" });
		datum.add(new BasicStreamDatum(meta2.getStreamId(), start.plusSeconds(1), p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("200.1"));
		p.setAccumulating(decimalArray("200.2"));
		datum.add(new BasicStreamDatum(meta3.getStreamId(), start.plusSeconds(1), p));

		p = new DatumProperties();
		p.setInstantaneous(decimalArray("300.1"));
		p.setAccumulating(decimalArray("300.2"));
		p.setTags(new String[] { "t3" });
		datum.add(new BasicStreamDatum(meta3.getStreamId(), start.plusSeconds(2), p));

		BasicObjectDatumStreamDataSet<StreamDatum> data = dataSet(metas, datum);

		// WHEN
		StringWriter out = new StringWriter();
		try (CsvStreamDatumFilteredResultsProcessor processor = new CsvStreamDatumFilteredResultsProcessor(
				out)) {
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			for ( StreamDatum d : datum ) {
				processor.handleResultItem(d);
			}
		}

		// THEN
		String csv = out.toString();
		assertThat("Datum CSV", csv, is("""
				ts,streamId,objectId,sourceId,a,b,c,d,e,f,tags\r
				2022-04-29T01:52:00Z,%1$s,123,test/source,1.1,1.2,1.3,foo,,,t1\r
				2022-04-29T01:52:00Z,%2$s,123,test/source,,10.1,10.2,bar,,,t2\r
				2022-04-29T01:52:00Z,%3$s,123,test/source,,,,,100.1,100.2,\r
				2022-04-29T01:52:01Z,%1$s,123,test/source,2.1,2.2,2.3,,,,t1.1\r
				2022-04-29T01:52:01Z,%2$s,123,test/source,,20.1,20.2,bar.1,,,t2.1\r
				2022-04-29T01:52:01Z,%3$s,123,test/source,,,,,200.1,200.2,\r
				2022-04-29T01:52:02Z,%3$s,123,test/source,,,,,300.1,300.2,t3\r
				""".formatted(meta1.getStreamId(), meta2.getStreamId(), meta3.getStreamId())));
	}

	@Test
	public void oneStream_aggregate() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = nodeMeta(123L, "test/source", new String[] { "a", "b" },
				new String[] { "c" }, new String[] { "d" });
		ZonedDateTime start = LocalDateTime.of(2022, 4, 29, 13, 00)
				.atZone(ZoneId.of("Pacific/Auckland"));

		// @formatter:off
		AggregateDatum d1 = new AggregateDatumEntity(meta.getStreamId(), start.toInstant(),
				Aggregation.Hour,
				propertiesOf(
						decimalArray("1.23", "2.34"),
						decimalArray("30"),
						new String[] {"foo"},
						new String[] {"wham", "bam"}),
				statisticsOf(new BigDecimal[][] {
						decimalArray("10", "1.0", "2.0"),
						decimalArray("10", "2.0", "3.0")
					}, new BigDecimal[][] { 
						decimalArray("30", "100", "130")
					}
				)
			);
		// @formatter:on

		BasicObjectDatumStreamDataSet<StreamDatum> data = dataSet(asList(meta), asList(d1));

		// WHEN
		StringWriter out = new StringWriter();
		try (CsvStreamDatumFilteredResultsProcessor processor = new CsvStreamDatumFilteredResultsProcessor(
				out)) {
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			processor.handleResultItem(d1);
		}

		// THEN
		String csv = out.toString();
		assertThat("Aggregate CSV", csv,
				is("""
						ts_start,ts_end,streamId,objectId,sourceId,a,a_count,a_min,a_max,b,b_count,b_min,b_max,c,c_start,c_end,d,tags\r
						2022-04-29T01:00:00Z,,%1$s,123,test/source,1.23,10,1.0,2.0,2.34,10,2.0,3.0,30,100,130,foo,"wham,bam"\r
						"""
						.formatted(meta.getStreamId())));
	}

	@Test
	public void oneStream_reading() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = nodeMeta(123L, "test/source", new String[] { "a", "b" },
				new String[] { "c" }, new String[] { "d" });
		ZonedDateTime start = LocalDateTime.of(2022, 4, 29, 13, 00)
				.atZone(ZoneId.of("Pacific/Auckland"));

		// @formatter:off
		ReadingDatum d1 = new ReadingDatumEntity(meta.getStreamId(), start.toInstant(),
				null, start.plusHours(1).toInstant(),
				propertiesOf(
						decimalArray("1.23", "2.34"),
						decimalArray("30"), null, null),
				statisticsOf(new BigDecimal[][] {
					decimalArray("10", "1.0", "2.0"),
					decimalArray("10", "2.0", "3.0")
				}, new BigDecimal[][] { 
					decimalArray("30", "100", "130")
				}));
		// @formatter:on

		BasicObjectDatumStreamDataSet<StreamDatum> data = dataSet(asList(meta), asList(d1));

		// WHEN
		StringWriter out = new StringWriter();
		try (CsvStreamDatumFilteredResultsProcessor processor = new CsvStreamDatumFilteredResultsProcessor(
				out)) {
			processor.start(null, null, null, singletonMap(METADATA_PROVIDER_ATTR, data));
			processor.handleResultItem(d1);
		}

		// THEN
		String csv = out.toString();
		assertThat("Aggregate CSV", csv,
				is("""
						ts_start,ts_end,streamId,objectId,sourceId,a,a_count,a_min,a_max,b,b_count,b_min,b_max,c,c_start,c_end,d,tags\r
						2022-04-29T01:00:00Z,2022-04-29T02:00:00Z,%1$s,123,test/source,1.23,10,1.0,2.0,2.34,10,2.0,3.0,30,100,130,,\r
						"""
						.formatted(meta.getStreamId())));
	}

}
