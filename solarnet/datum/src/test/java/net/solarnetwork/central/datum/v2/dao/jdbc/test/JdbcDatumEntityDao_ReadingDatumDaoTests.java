/* ==================================================================
 * JdbcReadingDatumEntityDaoTests.java - 17/11/2020 7:28:08 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumAndAuxiliaryResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.readingWith;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumDao;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link ReadingDatumDao} implementation within the
 * {@link JdbcDatumEntityDao} class.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcDatumEntityDao_ReadingDatumDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> loadStreamWithAuxiliary(String resource) {
		return loadStreamWithAuxiliary(resource, null, null);
	}

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> loadStreamWithAuxiliary(String resource,
			Consumer<GeneralNodeDatum> datumMapper, Consumer<GeneralNodeDatumAuxiliary> auxMapper) {
		List<?> data;
		try {
			data = loadJsonDatumAndAuxiliaryResource(resource, getClass(), datumMapper, auxMapper);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		log.debug("Got test data:\n{}", data.stream().map(Object::toString).collect(joining("\n")));
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return meta;
	}

	private ObjectDatumStreamFilterResults<ReadingDatum, DatumPK> execute(ReadingDatumCriteria filter) {
		ObjectDatumStreamFilterResults<ReadingDatum, DatumPK> results = dao
				.findDatumReadingFiltered(filter);
		if ( results.getReturnedResultCount() > 0 ) {
			log.debug("Got {} ReadingDatum results:\n{}", results.getReturnedResultCount(),
					stream(results.spliterator(), false).map(Object::toString).collect(joining("\n")));
		}
		return results;
	}

	private static void assertReading(String prefix, ReadingDatum result, ReadingDatum expected) {
		DatumTestUtils.assertAggregateDatum(prefix, result, expected);
		assertThat(prefix + " end timestamp", result.getEndTimestamp(),
				equalTo(expected.getEndTimestamp()));
	}

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void diff_nodeAndSource_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = dao.findDatumReadingFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(0L));
	}

	@Test
	public void diff_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Node and source", d,
				new ReadingDatumEntity(streamId, start.minusMinutes(1).toInstant(), null,
						start.plusHours(1).minusMinutes(1).toInstant(),
						propertiesOf(null, decimalArray("30"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("30", "100", "130") })));
	}

	@Test
	public void diff_nodeAndSource_localDates() {
		// GIVEN
		final ZoneId tz = ZoneId.of("America/Chicago");
		setupTestLocation(1L, tz.getId());
		setupTestNode(1L, 1L);
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setLocalStartDate(start.withZoneSameInstant(tz).toLocalDateTime());
		filter.setLocalEndDate(start.plusHours(1).withZoneSameInstant(tz).toLocalDateTime());
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Node and source", d,
				new ReadingDatumEntity(streamId, start.minusMinutes(1).toInstant(), null,
						start.plusHours(1).minusMinutes(1).toInstant(),
						propertiesOf(null, decimalArray("30"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("30", "100", "130") })));
	}

	@Test
	public void diff_nodesAndSources_orderByNodeSourceTime() {
		// GIVEN
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>(4);
		for ( int i = 0; i < 5; i++ ) {
			final Long nodeId = (long) (i + 1);
			final String sourceId = Character.toString((char) ('a' + i));
			metas.putAll(loadStreamWithAuxiliary("test-datum-02.txt", d -> {
				d.setNodeId(nodeId);
				d.setSourceId(sourceId);
			}, null));
		}
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(metas.keySet().stream().map(NodeSourcePK::getNodeId).toArray(Long[]::new));
		filter.setSourceIds(
				metas.keySet().stream().map(NodeSourcePK::getSourceId).toArray(String[]::new));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setSorts(sorts("node", "source", "time"));
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo((long) metas.size()));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(metas.size()));

		int i = 0;
		for ( ReadingDatum d : results ) {
			final Long nodeId = (long) (i + 1);
			final String sourceId = Character.toString((char) ('a' + i));
			UUID streamId = metas.get(new NodeSourcePK(nodeId, sourceId)).getStreamId();
			assertReading("Node and source " + i, d, new ReadingDatumEntity(streamId,
					start.minusMinutes(1).toInstant(), null,
					start.plusHours(1).minusMinutes(1).toInstant(),
					propertiesOf(null, decimalArray("30"), null, null),
					statisticsOf(null, new BigDecimal[][] { decimalArray("30", "100", "130") })));
			i++;
		}
	}

	@Test
	public void diff_nodesAndSources_localDates_orderByNodeSourceTime() {
		// GIVEN
		List<ZoneId> zones = Arrays.asList(ZoneId.of("Pacific/Auckland"), ZoneId.of("Asia/Kolkata"),
				ZoneId.of("Europe/Paris"), ZoneId.of("UTC"), ZoneId.of("America/Montreal"));
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = new LinkedHashMap<>(4);
		for ( int i = 0; i < 5; i++ ) {
			final long id = i + 1;
			final Long nodeId = id;
			final String sourceId = Character.toString((char) ('a' + i));
			final ZoneId zone = zones.get(i);
			setupTestLocation(id, zone.getId());
			setupTestNode(id, id);
			metas.putAll(loadStreamWithAuxiliary("test-datum-02.txt", d -> {
				d.setNodeId(nodeId);
				d.setSourceId(sourceId);
				ZonedDateTime dt = d.getCreated().atZone(ZoneId.of("Pacific/Auckland"))
						.withZoneSameLocal(zone);
				d.setCreated(dt.toInstant());
			}, null));
		}

		LocalDateTime start = LocalDateTime.of(2020, 6, 2, 0, 0, 0, 0);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.Difference);
		filter.setNodeIds(metas.keySet().stream().map(NodeSourcePK::getNodeId).toArray(Long[]::new));
		filter.setSourceIds(
				metas.keySet().stream().map(NodeSourcePK::getSourceId).toArray(String[]::new));
		filter.setLocalStartDate(start);
		filter.setLocalEndDate(start.plusHours(1));
		filter.setSorts(sorts("node", "source", "time"));
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo((long) metas.size()));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(metas.size()));

		int i = 0;
		for ( ReadingDatum d : results ) {
			final Long nodeId = (long) (i + 1);
			final String sourceId = Character.toString((char) ('a' + i));
			UUID streamId = metas.get(new NodeSourcePK(nodeId, sourceId)).getStreamId();
			assertReading("Node and source " + i, d, new ReadingDatumEntity(streamId,
					start.minusMinutes(1).atZone(zones.get(i)).toInstant(), null,
					start.plusHours(1).minusMinutes(1).atZone(zones.get(i)).toInstant(),
					propertiesOf(null, decimalArray("30"), null, null),
					statisticsOf(null, new BigDecimal[][] { decimalArray("30", "100", "130") })));
			i++;
		}
	}

	@Test
	public void diffNear_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.NearestDifference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setTimeTolerance(Period.ofDays(7));
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Node and source", d, readingWith(streamId, null, start.minusMinutes(1),
				start.plusHours(1).minusMinutes(1), decimalArray("30", "100", "130")));
	}

	@Test
	public void diffAt_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.CalculatedAtDifference);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setTimeTolerance(Period.ofDays(7));
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Difference at calculated", d,
				readingWith(streamId, null, start, start.plusHours(1), decimalArray("30"),
						new BigDecimal[][] { decimalArray("30.0", "100.5", "130.5") }));
	}

	@Test
	public void diffAt_nodesAndSources_localDates() {
		// GIVEN
		setupTestLocation(1L, "UTC");
		setupTestNode(1L, 1L);
		setupTestLocation(2L, "America/Los_Angeles");
		setupTestNode(2L, 2L);
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"America/Los_Angeles", ObjectDatumKind.Node, 2L, "b", new String[] { "x" },
				new String[] { "xh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final LocalDateTime start = LocalDateTime.of(2020, 6, 1, 12, 0, 0, 0);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 2; i++ ) {
			Instant ts = start.plusMinutes(i * 5).atZone(ZoneId.of(meta_1.getTimeZoneId())).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 3) },
					new BigDecimal[] { new BigDecimal(i * 33) }, null, null);
			datums.add(new DatumEntity(meta_1.getStreamId(), ts, now, props));

			ts = start.plusMinutes(i * 5).atZone(ZoneId.of(meta_2.getTimeZoneId())).toInstant();
			props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 9) },
					new BigDecimal[] { new BigDecimal(i * 99) }, null, null);
			datums.add(new DatumEntity(meta_2.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.CalculatedAtDifference);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setLocalStartDate(start.plusMinutes(1)); // 1/5 of slot time
		filter.setLocalEndDate(start.plusMinutes(4)); // 4/5 of slot time
		filter.setSorts(sorts("node", "source"));

		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(2L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));

		List<ReadingDatum> datumList = stream(results.spliterator(), false).collect(toList());
		ReadingDatum d = datumList.get(0);
		assertReading("CalcualtedAt reading diff for stream 1 @ local time", d,
				new ReadingDatumEntity(meta_1.getStreamId(),
						filter.getLocalStartDate().atZone(ZoneId.of(meta_1.getTimeZoneId())).toInstant(),
						null,
						filter.getLocalEndDate().atZone(ZoneId.of(meta_1.getTimeZoneId())).toInstant(),
						propertiesOf(decimalArray("1.5"), decimalArray("19.8"), null, null), null));

		d = datumList.get(1);
		assertReading("CalcualtedAt reading diff for stream 2 @ local time", d,
				new ReadingDatumEntity(meta_2.getStreamId(),
						filter.getLocalStartDate().atZone(ZoneId.of(meta_2.getTimeZoneId())).toInstant(),
						null,
						filter.getLocalEndDate().atZone(ZoneId.of(meta_2.getTimeZoneId())).toInstant(),
						propertiesOf(decimalArray("4.5"), decimalArray("59.4"), null, null), null));
	}

	@Test
	public void diffWithin_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-02.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.DifferenceWithin);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("Node and source", d, readingWith(streamId, null, start.plusMinutes(9),
				start.plusHours(1).minusMinutes(1), decimalArray("25", "105", "130")));
	}

	@Test
	public void calcAt_nodeAndSource() {
		// GIVEN
		UUID streamId = loadStreamWithAuxiliary("test-datum-01.txt").values().iterator().next()
				.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.CalculatedAt);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.plusMinutes(8).toInstant());
		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("CalcualtedAt reading has props without stats", d,
				new ReadingDatumEntity(streamId, filter.getStartDate(), null, null,
						propertiesOf(decimalArray("1.28", "2.9"), decimalArray("104"), null, null),
						null));
	}

	@Test
	public void calcAt_nodeAndSource_noDifference() {
		// GIVEN
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta));

		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 2; i++ ) {
			Instant ts = start.plusMinutes(i * 2).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal("3") },
					new BigDecimal[] { new BigDecimal("33") }, null, null);
			datums.add(new DatumEntity(meta.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.CalculatedAt);
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setStartDate(start.plusMinutes(1).toInstant());

		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(1L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));

		ReadingDatum d = results.iterator().next();
		assertReading("CalcualtedAt reading between no difference", d,
				new ReadingDatumEntity(meta.getStreamId(), filter.getStartDate(), null, null,
						propertiesOf(decimalArray("3"), decimalArray("33"), null, null), null));
	}

	@Test
	public void calcAt_nodesAndSources_localDates() {
		// GIVEN
		setupTestLocation(1L, "UTC");
		setupTestNode(1L, 1L);
		setupTestLocation(2L, "America/Los_Angeles");
		setupTestNode(2L, 2L);
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"America/Los_Angeles", ObjectDatumKind.Node, 2L, "b", new String[] { "x" },
				new String[] { "xh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final LocalDateTime start = LocalDateTime.of(2020, 6, 1, 12, 0, 0, 0);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 2; i++ ) {
			Instant ts = start.plusMinutes(i * 5).atZone(ZoneId.of(meta_1.getTimeZoneId())).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 3) },
					new BigDecimal[] { new BigDecimal(i * 33) }, null, null);
			datums.add(new DatumEntity(meta_1.getStreamId(), ts, now, props));

			ts = start.plusMinutes(i * 5).atZone(ZoneId.of(meta_2.getTimeZoneId())).toInstant();
			props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 9) },
					new BigDecimal[] { new BigDecimal(i * 99) }, null, null);
			datums.add(new DatumEntity(meta_2.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setReadingType(DatumReadingType.CalculatedAt);
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setLocalStartDate(start.plusMinutes(2)); // 2/5 of slot time
		filter.setSorts(sorts("node", "source"));

		FilterResults<ReadingDatum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Total result populated", results.getTotalResults(), equalTo(2L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));

		List<ReadingDatum> datumList = stream(results.spliterator(), false).collect(toList());
		ReadingDatum d = datumList.get(0);
		assertReading("CalcualtedAt reading for stream 1 @ local time", d,
				new ReadingDatumEntity(meta_1.getStreamId(),
						filter.getLocalStartDate().atZone(ZoneId.of(meta_1.getTimeZoneId())).toInstant(),
						null, null, propertiesOf(decimalArray("1.2"), decimalArray("13.2"), null, null),
						null));

		d = datumList.get(1);
		assertReading("CalcualtedAt reading for stream 2 @ local time", d,
				new ReadingDatumEntity(meta_2.getStreamId(),
						filter.getLocalStartDate().atZone(ZoneId.of(meta_2.getTimeZoneId())).toInstant(),
						null, null, propertiesOf(decimalArray("3.6"), decimalArray("39.6"), null, null),
						null));
	}

}
