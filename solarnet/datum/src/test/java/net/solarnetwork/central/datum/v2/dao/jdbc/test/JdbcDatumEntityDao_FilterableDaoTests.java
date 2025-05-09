/* ==================================================================
 * JdbcDatumEntityDao_FilterableDaoTests.java - 3/12/2020 11:10:04 am
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
import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.UUID_STRING_ORDER;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listNodeMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.sortedStreamIds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link FilterableDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcDatumEntityDao_FilterableDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@BeforeEach
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
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
						DatumSamples data = new DatumSamples();
						for ( int i = 0; i < count; i++ ) {
							cs.setTimestamp(1, Timestamp.from(ts.toInstant()));
							cs.setLong(2, nodeId);
							cs.setString(3, sourceId);
							cs.setTimestamp(4, received);

							data.putInstantaneousSampleValue(propPrefix + "_i", Math.random() * 1000000);
							data.putAccumulatingSampleValue(propPrefix + "_a", i + 1);

							String jdata = JsonUtils.getJSONString(data, null);
							cs.setString(5, jdata);
							cs.execute();
							log.debug("Inserted datum node {} source {} ts {}", nodeId, sourceId, ts);
							ts = ts.plus(frequency);
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
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(12L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(12));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

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

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		final UUID streamId = metas.keySet().iterator().next();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));

		ObjectDatumStreamMetadata meta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(metas.get(streamId).getObjectId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

	@Test
	public void find_byNodeId() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		final UUID streamId = metas.keySet().iterator().next();
		final ObjectDatumStreamMetadata m = metas.get(streamId);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(m.getObjectId());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));

		ObjectDatumStreamMetadata meta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(metas.get(streamId).getObjectId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

	@Test
	public void find_noneAgg_node() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		final UUID streamId = metas.keySet().iterator().next();
		final ObjectDatumStreamMetadata m = metas.get(streamId);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(m.getObjectId());
		filter.setAggregation(Aggregation.None);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));

		ObjectDatumStreamMetadata meta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(metas.get(streamId).getObjectId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

	@Test
	public void find_noneAgg_nonePartialAgg_node() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		final UUID streamId = metas.keySet().iterator().next();
		final ObjectDatumStreamMetadata m = metas.get(streamId);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(m.getObjectId());
		filter.setAggregation(Aggregation.None);
		filter.setPartialAggregation(Aggregation.None);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		SortedSet<UUID> streamIds = sortedStreamIds(results, UUID_STRING_ORDER);
		assertThat("Result stream IDs count", streamIds, contains(streamId));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));

		ObjectDatumStreamMetadata meta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(metas.get(streamId).getObjectId()));
		Instant ts = start.toInstant();
		for ( int i = 0; i < 4; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp", d.getTimestamp(), not(lessThan(ts)));
			ts = d.getTimestamp();
		}
	}

	@Test
	public void find_paginated_withoutTotalResultCount_first() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 6);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setWithoutTotalResultsCount(true);
		filter.setNodeId(1L);
		filter.setMax(2);
		filter.setOffset(0L);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), nullValue());
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		Instant ts = start.toInstant();
		for ( Datum d : results ) {
			assertThat("Datum timestamp", d.getTimestamp(), equalTo(ts));
			ts = ts.plus(freq);
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
		filter.setOffset(0L);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

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
		filter.setOffset(2L);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(2L));

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
		filter.setOffset(4L);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(4L));

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
		filter.setOffset(6L);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(6L));
	}

	@Test
	public void find_mostRecent_nodes() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		final UUID[] expectedStreamIds = metas.keySet().toArray(new UUID[metas.size()]);
		Arrays.sort(expectedStreamIds, UUID_STRING_ORDER);

		List<UUID> streamIds = new ArrayList<>(sortedStreamIds(results, UUID_STRING_ORDER));
		assertThat("Result stream IDs count", streamIds, contains(expectedStreamIds));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		for ( int i = 0; i < 2; i++ ) {
			Datum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
			Long nodeId = meta.getObjectId();
			assertThat("Node ID", nodeId, equalTo(metas.get(streamIds.get(i)).getObjectId()));
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamIds.get(i)));
			assertThat("Max timestamp", d.getTimestamp(),
					equalTo(nodeId == 1L ? start.plusMinutes(3 * 30).toInstant()
							: start.plusMinutes(3 * 30 + 15).toInstant()));
		}
	}

	@Test
	public void find_mostRecent_source() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("s2");
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		final UUID streamId = metas.entrySet().stream()
				.filter(e -> "s2".equals(e.getValue().getSourceId())).findAny().get().getKey();
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));

		Datum d = datumList.get(0);
		ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
		assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", meta.getObjectId(), equalTo(2L));
		assertThat("Source ID", meta.getSourceId(), equalTo("s2"));
		assertThat("Stream ID ", d.getStreamId(), equalTo(streamId));
		assertThat("Max timestamp", d.getTimestamp(),
				equalTo(start.plusMinutes(3 * 30 + 15).toInstant()));
	}

	@Test
	public void find_mostRecent_sources() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(15), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceIds(new String[] { "s1", "s2" });
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		final UUID[] expectedStreamIds = metas.keySet().toArray(new UUID[metas.size()]);
		Arrays.sort(expectedStreamIds, UUID_STRING_ORDER);

		List<UUID> streamIds = new ArrayList<>(sortedStreamIds(results, UUID_STRING_ORDER));
		assertThat("Result stream IDs count", streamIds, contains(expectedStreamIds));
		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		for ( int i = 0; i < 2; i++ ) {
			Datum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Metadata is for node", meta.getKind(), equalTo(ObjectDatumKind.Node));
			Long nodeId = meta.getObjectId();
			assertThat("Node ID", nodeId, equalTo(metas.get(streamIds.get(i)).getObjectId()));
			assertThat("Stream ID ", d.getStreamId(), equalTo(streamIds.get(i)));
			assertThat("Max timestamp", d.getTimestamp(),
					equalTo(nodeId == 1L ? start.plusMinutes(3 * 30).toInstant()
							: start.plusMinutes(3 * 30 + 15).toInstant()));
		}
	}

	@Test
	public void find_mostRecent_user() {
		// GIVEN
		setupTestUser(1L, "one");
		setupTestUser(2L, "two");
		setupTestLocation();
		setupTestNode(1L, TEST_LOC_ID);
		setupTestNode(2L, TEST_LOC_ID);
		setupTestNode(3L, TEST_LOC_ID);
		setupUserNodeEntity(1L, 1L);
		setupUserNodeEntity(2L, 2L);
		setupUserNodeEntity(3L, 2L);

		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final Duration freq = Duration.ofMinutes(30);
		insertDatum(1L, "s1", "foo", start, freq, 4);
		insertDatum(2L, "s2", "bim", start.plusMinutes(10), freq, 4);
		insertDatum(3L, "s3", "bop", start.plusMinutes(20), freq, 4);

		Map<UUID, ObjectDatumStreamMetadata> metas = listNodeMetadata(jdbcTemplate).stream()
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(2L);
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		Set<UUID> streamIds = metas.entrySet().stream()
				.filter(e -> !e.getValue().getObjectId().equals(1L)).map(Map.Entry::getKey)
				.collect(toSet());
		for ( Datum d : results ) {
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("User stream returned", streamIds.contains(meta.getStreamId()), equalTo(true));
			Instant expectedTs;
			if ( meta.getObjectId().equals(2L) ) {
				expectedTs = start.plusMinutes(3 * 30 + 10).toInstant();
			} else {
				expectedTs = start.plusMinutes(3 * 30 + 20).toInstant();
			}
			assertThat("Max timestamp", d.getTimestamp(), equalTo(expectedTs));
		}
	}

}
