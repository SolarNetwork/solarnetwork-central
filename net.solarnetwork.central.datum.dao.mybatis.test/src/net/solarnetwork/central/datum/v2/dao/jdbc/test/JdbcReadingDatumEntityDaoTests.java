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

import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.elementsOf;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcReadingDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcReadingDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcReadingDatumEntityDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcReadingDatumEntityDao dao;

	private Map<NodeSourcePK, NodeDatumStreamMetadata> loadStreamWithAuxiliary(String resource) {
		return loadStreamWithAuxiliary(resource, null, null);
	}

	private Map<NodeSourcePK, NodeDatumStreamMetadata> loadStreamWithAuxiliary(String resource,
			Consumer<GeneralNodeDatum> datumMapper, Consumer<GeneralNodeDatumAuxiliary> auxMapper) {
		List<?> data;
		try {
			data = DatumTestUtils.loadJsonDatumAndAuxiliaryResource(resource, getClass(), datumMapper,
					auxMapper);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		log.debug("Got test data:\n{}", data.stream().map(Object::toString).collect(joining("\n")));
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				DatumTestUtils.insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return meta;
	}

	private FilterResults<ReadingDatum, DatumPK> execute(ReadingDatumCriteria filter) {
		FilterResults<ReadingDatum, DatumPK> results = dao.findDatumReadingFiltered(filter);
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
		dao = new JdbcReadingDatumEntityDao(jdbcTemplate);
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
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = new LinkedHashMap<>(4);
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
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = new LinkedHashMap<>(4);
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
				d.setCreated(d.getCreated().withZoneRetainFields(DateTimeZone.forID(zone.getId())));
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
		assertReading("Node and source", d,
				new ReadingDatumEntity(streamId, start.plusMinutes(9).toInstant(), null,
						start.plusHours(1).minusMinutes(1).toInstant(),
						propertiesOf(null, decimalArray("25"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("25", "105", "130") })));
	}

}
