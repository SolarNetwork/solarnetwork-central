/* ==================================================================
 * MyBatisDatumEntityDaoTests.java - 26/10/2020 7:21:00 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.CallableStatementCallback;
import net.solarnetwork.central.datum.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.mybatis.MyBatisDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for the {@link MyBatisDatumEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumEntityDaoTests extends AbstractMyBatisDaoTestSupport {

	protected MyBatisDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisDatumEntityDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	@Test
	public void storeNew() {
		DatumEntity datum = new DatumEntity(UUID.randomUUID(), Instant.now(), Instant.now(),
				DatumProperties.propertiesOf(
						new BigDecimal[] { new BigDecimal("1.23"), new BigDecimal("2.34") },
						new BigDecimal[] { new BigDecimal("3.45") }, new String[] { "On" }, null));
		DatumPK id = dao.save(datum);
		assertNotNull(id);
		lastDatum = datum;
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
		storeNew();
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
	public void findNodeMetadata() {
		// GIVEN
		final List<UUID> streamIds = new ArrayList<>(3);
		jdbcTemplate.batchUpdate(
				"insert into solardatm.da_datm_meta (stream_id,node_id,source_id,names_i,names_a,names_s) "
						+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						UUID streamId = UUID.randomUUID();
						streamIds.add(streamId);
						ps.setString(1, streamId.toString());
						ps.setLong(2, i + 1);
						ps.setString(3, format("s%d", i + 1));
						ps.setString(4, "{a,b,c}");
						ps.setString(5, "{d,e}");
						ps.setString(6, "{f}");
					}

					@Override
					public int getBatchSize() {
						return 3;
					}
				});

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<NodeDatumStreamMetadata> results = dao.getNodeDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, NodeDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( int i = 0, idx = 1; i < 3; i++, idx++ ) {
			final UUID streamId = streamIds.get(i);
			NodeDatumStreamMetadata meta = metas.get(streamId);
			assertThat("Stream ID " + idx, meta.getStreamId(), equalTo(streamId));
			assertThat("Node ID " + idx, meta.getNodeId(), equalTo((long) idx));
			assertThat("Source ID" + idx, meta.getSourceId(), equalTo(format("s%d", idx)));
			assertThat("Instantaneous property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
					arrayContaining("a", "b", "c"));
			assertThat("Accumulating property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
					arrayContaining("d", "e"));
			assertThat("Status property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Status), arrayContaining("f"));
		}
	}

	@Test
	public void findLocationMetadata() {
		// GIVEN
		final List<UUID> streamIds = new ArrayList<>(3);
		jdbcTemplate.batchUpdate(
				"insert into solardatm.da_loc_datm_meta (stream_id,loc_id,source_id,names_i,names_a,names_s) "
						+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						UUID streamId = UUID.randomUUID();
						streamIds.add(streamId);
						ps.setString(1, streamId.toString());
						ps.setLong(2, i + 1);
						ps.setString(3, format("s%d", i + 1));
						ps.setString(4, "{a,b,c}");
						ps.setString(5, "{d,e}");
						ps.setString(6, "{f}");
					}

					@Override
					public int getBatchSize() {
						return 3;
					}
				});

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<LocationDatumStreamMetadata> results = dao.getLocationDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, LocationDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( int i = 0, idx = 1; i < 3; i++, idx++ ) {
			final UUID streamId = streamIds.get(i);
			LocationDatumStreamMetadata meta = metas.get(streamId);
			assertThat("Stream ID " + idx, meta.getStreamId(), equalTo(streamId));
			assertThat("Node ID " + idx, meta.getLocationId(), equalTo((long) idx));
			assertThat("Source ID" + idx, meta.getSourceId(), equalTo(format("s%d", idx)));
			assertThat("Instantaneous property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
					arrayContaining("a", "b", "c"));
			assertThat("Accumulating property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
					arrayContaining("d", "e"));
			assertThat("Status property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Status), arrayContaining("f"));
		}
	}

	// UUID sorter that matches how Postgres orders UUIDS (byte by byte)
	private static Comparator<UUID> UUID_STRING_ORDER = new UuidByteOrder();

	private static class UuidByteOrder implements Comparator<UUID> {

		@Override
		public int compare(UUID o1, UUID o2) {
			return o1.toString().compareTo(o2.toString());
		}

	}

	private static SortedSet<UUID> sortedStreamIds(DatumStreamFilterResults results) {
		return results.metadataStreamIds().stream().collect(Collectors.toCollection(() -> {
			return new TreeSet<>(UUID_STRING_ORDER);
		}));
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

		SortedSet<UUID> streamIds = sortedStreamIds(results);
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

		Map<UUID, NodeDatumStreamMetadata> metas = StreamSupport
				.stream(dao.getNodeDatumStreamMetadata(new BasicDatumCriteria()).spliterator(), false)
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

		SortedSet<UUID> streamIds = sortedStreamIds(results);
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
