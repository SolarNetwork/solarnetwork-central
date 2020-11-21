/* ==================================================================
 * JdbcDatumEntityDao_GenericDaoTests.java - 19/11/2020 5:24:58 pm
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.UUID_STRING_ORDER;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.listNodeMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.sortedStreamIds;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.util.JodaDateUtils;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link GenericDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_GenericDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void saveNew() {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), Instant.now(),
				DatumProperties.propertiesOf(
						new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34") },
						new BigDecimal[] { new BigDecimal("3.45") }, new String[] { "On" }, null));
		DatumPK id = dao.save(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	@Test
	public void store_newStream() throws IOException {
		// GIVEN
		GeneralNodeDatum datum = DatumTestUtils.loadJsonDatumResource("test-datum-01.txt", getClass())
				.get(0);

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, notNullValue());
		assertThat("ID has stream ID", id.getStreamId(), notNullValue());
		assertThat("ID has expected timestamp", id.getTimestamp(),
				equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));

		List<Datum> rows = DatumTestUtils.listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(1));
		assertThat("Datum ID matches returned value", rows.get(0).getId(), equalTo(id));

		List<NodeDatumStreamMetadata> metas = DatumTestUtils.listNodeMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(), equalTo(id.getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getNodeId(), equalTo(1L));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo("a"));
		assertThat("Datum properties", rows.get(0).getProperties(),
				equalTo(propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));
	}

	@Test
	public void store_entireStream() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = DatumTestUtils.loadJsonDatumResource("test-datum-01.txt",
				getClass());

		// WHEN
		List<DatumPK> ids = new ArrayList<>(datums.size());
		for ( GeneralNodeDatum datum : datums ) {
			DatumPK id = dao.store(datum);
			assertThat("ID returned", id, notNullValue());
			assertThat("ID has stream ID", id.getStreamId(), notNullValue());
			assertThat("ID has expected timestamp", id.getTimestamp(),
					equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));
			if ( !ids.isEmpty() ) {
				assertThat("Same stream ID returned for subsequent store", id.getStreamId(),
						equalTo(ids.get(0).getStreamId()));
			}
			ids.add(id);
		}

		// THEN
		List<Datum> rows = DatumTestUtils.listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(datums.size()));
		int i = 0;
		for ( Datum row : rows ) {
			assertThat("Datum ID matches returned value", row.getId(), equalTo(ids.get(i)));
			i++;
		}
		List<NodeDatumStreamMetadata> metas = DatumTestUtils.listNodeMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(),
				equalTo(ids.get(0).getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getNodeId(), equalTo(1L));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo("a"));
	}

	@Test
	public void store_newLocationStream() throws IOException {
		// GIVEN
		GeneralNodeDatum nodeDatum = DatumTestUtils
				.loadJsonDatumResource("test-datum-01.txt", getClass()).get(0);
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(nodeDatum.getCreated());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(nodeDatum.getSourceId());
		GeneralLocationDatumSamples s = new GeneralLocationDatumSamples();
		s.setI(nodeDatum.getSamples().getI());
		s.setA(nodeDatum.getSamples().getA());
		datum.setSamples(s);

		// WHEN
		DatumPK id = dao.store(datum);

		// THEN
		assertThat("ID returned", id, notNullValue());
		assertThat("ID has stream ID", id.getStreamId(), notNullValue());
		assertThat("ID has expected timestamp", id.getTimestamp(),
				equalTo(JodaDateUtils.fromJodaToInstant(datum.getCreated())));

		List<Datum> rows = DatumTestUtils.listDatum(jdbcTemplate);
		assertThat("Datum stored in DB", rows, hasSize(1));
		assertThat("Datum ID matches returned value", rows.get(0).getId(), equalTo(id));
		assertThat("Datum properties", rows.get(0).getProperties(),
				equalTo(propertiesOf(decimalArray("1.2", "2.1"), decimalArray("100"), null, null)));

		List<LocationDatumStreamMetadata> metas = DatumTestUtils.listLocationMetadata(jdbcTemplate);
		assertThat("Stream metadata created", metas, hasSize(1));
		assertThat("Metadata for stream ID", metas.get(0).getStreamId(), equalTo(id.getStreamId()));
		assertThat("Metadata for node ID", metas.get(0).getLocationId(), equalTo(datum.getLocationId()));
		assertThat("Metadata for source ID", metas.get(0).getSourceId(), equalTo(datum.getSourceId()));
	}

	private void assertSame(DatumEntity expected, DatumEntity entity) {
		assertThat("DatumEntity should exist", entity, notNullValue());
		assertThat("Stream ID", expected.getStreamId(), equalTo(entity.getStreamId()));
		assertThat("Timestamp", expected.getTimestamp(), equalTo(entity.getTimestamp()));
		assertThat("Received", expected.getReceived(), equalTo(entity.getReceived()));
		assertThat("Properties", expected.getProperties(), equalTo(entity.getProperties()));
	}

	@Test
	public void getByPrimaryKey() {
		saveNew();
		DatumEntity datum = dao.get(lastDatum.getId());
		assertSame(lastDatum, datum);
	}

	private void insertDatum(Long nodeId, String sourceId, String propPrefix, ZonedDateTime start,
			Duration frequency, int count) {
		jdbcTemplate.execute("{call solardatm.store_datum(?,?,?,?,?)}",
				new CallableStatementCallback<Void>() {

					@Override
					public Void doInCallableStatement(CallableStatement cs)
							throws SQLException, DataAccessException {
						ZonedDateTime ts = start;
						Timestamp received = Timestamp.from(Instant.now());
						GeneralDatumSamples data = new GeneralDatumSamples();
						for ( int i = 0; i < count; i++ ) {
							cs.setTimestamp(1, Timestamp.from(ts.toInstant()));
							cs.setLong(2, nodeId);
							cs.setString(3, sourceId);
							cs.setTimestamp(4, received);

							data.putInstantaneousSampleValue(propPrefix + "_i", Math.random() * 1000000);
							data.putAccumulatingSampleValue(propPrefix + "_a", i + 1);

							String jdata = JsonUtils.getJSONString(data, null);
							cs.setString(5, jdata);
							ts = ts.plus(frequency);
							cs.execute();
						}
						return null;
					}
				});
	}

	@Test
	public void find_multipleStreams() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);
		insertDatum(3L, "s3", "bam", start.plusMinutes(30), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(12L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(12));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, hasSize(3));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(12));

		int streamIdx = 0;
		for ( UUID streamId : streamIds ) {
			Instant ts = start.toInstant();
			for ( int i = 0; i < 4; i++ ) {
				Datum d = datumList.get(streamIdx * 4 + i);
				assertThat("Ordered by stream ID " + streamIdx, d.getStreamId(), equalTo(streamId));
				assertThat("Ordered by timestamp next", d.getTimestamp(), not(lessThan(ts)));
				ts = d.getTimestamp();
			}
			streamIdx++;
		}
	}

	@Test
	public void find_byStreamId() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, NodeDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		final UUID streamId = metas.keySet().iterator().next();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));

		DatumStreamMetadata meta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", meta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) meta).getNodeId(),
				equalTo(metas.get(streamId).getNodeId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

	@Test
	public void find_paginated_withTotalResultCount_first() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 6);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setNodeId(1L);
		filter.setMax(2);
		filter.setOffset(0);
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		Instant ts = start.toInstant();
		for ( Datum d : results ) {
			assertThat("Datum timestamp", d.getTimestamp(), equalTo(ts));
			ts = ts.plus(freq);
		}
	}

	@Test
	public void find_paginated_withTotalResultCount_middle() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 6);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setNodeId(1L);
		filter.setMax(2);
		filter.setOffset(2);
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(2));

		Instant ts = start.plus(freq.multipliedBy(2)).toInstant();
		for ( Datum d : results ) {
			assertThat("Datum timestamp", d.getTimestamp(), equalTo(ts));
			ts = ts.plus(freq);
		}
	}

	@Test
	public void find_paginated_withTotalResultCount_last() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 6);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setNodeId(1L);
		filter.setMax(2);
		filter.setOffset(4);
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(4));

		Instant ts = start.plus(freq.multipliedBy(4)).toInstant();
		for ( Datum d : results ) {
			assertThat("Datum timestamp", d.getTimestamp(), equalTo(ts));
			ts = ts.plus(freq);
		}
	}

	@Test
	public void find_paginated_withTotalResultCount_over() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 6);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(false);
		filter.setNodeId(1L);
		filter.setMax(2);
		filter.setOffset(6);
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(6));
	}

}
