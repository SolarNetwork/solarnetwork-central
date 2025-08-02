/* ==================================================================
 * JdbcDatumEntityDao_DatumStreamMetadataDaoTests.java - 19/11/2020 4:41:41 pm
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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.datumRecordCounts;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatumRecordCounts;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatumId.nodeId;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.domain.datum.Aggregation.Hour;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link DatumMaintenanceDao}.
 *
 * @author matt
 * @version 1.1
 */
public class JdbcDatumEntityDao_DatumMaintenanceDaoTests extends BaseDatumJdbcTestSupport {

	private static final long TEST_NODE_ID_ALT = 2L;
	private static final long TEST_LOC_ID_ALT = 2L;
	private static final String TEST_TZ_ALT = "America/New_York";

	private JdbcDatumEntityDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private static DatumProperties newTestProps() {
		return DatumProperties.propertiesOf(decimalArray("1.1", "1.2"), decimalArray("100"), null, null);
	}

	private static DatumEntity newTestDatm(ZonedDateTime timestamp, UUID streamId,
			DatumProperties props) {
		return new DatumEntity(streamId, timestamp.toInstant(), Instant.now(), props);
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), timeZoneId, ObjectDatumKind.Node,
				nodeId, sourceId, new String[] { "x", "y" }, new String[] { "w" }, null);
	}

	@Test
	public void findStaleNoData() {
		// WHEN
		ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).minusDays(1);
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(1).toInstant());

		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(0L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
	}

	@Test
	public void findStale_noMatchingDataByNode() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(123L);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(0L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
	}

	@Test
	public void findStale_noMatchingDataBySource() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId("a");
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(0L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
	}

	@Test
	public void findStale_noMatchingDataByDate() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.minusDays(1).toInstant());
		criteria.setEndDate(start.minusHours(1).toInstant());
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(0L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
	}

	@Test
	public void findStale() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(5));
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void findStale_subset() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(2).toInstant());
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Total result count", results.getTotalResults(), equalTo(2L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void findStale_paginated_first() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		criteria.setMax(2);
		criteria.setOffset(0L);
		criteria.setWithoutTotalResultsCount(false);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0L));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void findStale_paginated_middle() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		criteria.setMax(2);
		criteria.setOffset(2L);
		criteria.setWithoutTotalResultsCount(false);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(2L));
		int i = 2;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void findStale_paginated_end() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		criteria.setMax(2);
		criteria.setOffset(4L);
		criteria.setWithoutTotalResultsCount(false);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(4L));
		int i = 4;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void markStale_noData() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).minusDays(1);
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(1).toInstant());

		// WHEN
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("No rows inserted because no data", count, equalTo(0));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("No hourly agg rows exist", stale, hasSize(0));
	}

	@Test
	public void markStale_noMatchingDataByNode() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(123L);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("No rows inserted because no data matching filter", count, equalTo(0));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("No hourly agg rows exist", stale, hasSize(0));
	}

	@Test
	public void markStale_noMatchingDataBySource() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId("a");
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("No rows inserted because no data matching filter", count, equalTo(0));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("No hourly agg rows exist", stale, hasSize(0));
	}

	@Test
	public void markStale_noMatchingDataByDate() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.minusDays(1).toInstant());
		criteria.setEndDate(start.minusHours(1).toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("No rows inserted because no data matching filter", count, equalTo(0));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("No hourly agg rows exist", stale, hasSize(0));
	}

	@Test
	public void markStale() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("Rows inserted for data matching filter + previous hour", count, equalTo(6));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(6));
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i - 1).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void markStale_subset() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(2).toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("Rows inserted for data matching filter + previous hour", count, equalTo(3));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(3));
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i - 1).toInstant(), Hour, null));
			i++;
		}
	}

	@Test
	public void markStale_multiNodes() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.DAYS)
				.minusDays(1);
		final ZonedDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_1));

		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_2));

		ZonedDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date, meta_1.getStreamId(), props));
			data.add(newTestDatm(date, meta_2.getStreamId(), props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(end.toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("Rows inserted for data matching filter + prev hour/stream", count, equalTo(12));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(12));
		UUID[] streamIds = new UUID[] { meta_1.getStreamId(), meta_2.getStreamId() };
		Arrays.sort(streamIds, DatumDbUtils.UUID_STRING_ORDER);
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			UUID streamId = streamIds[i % 2];
			int hourIdx = i / 2 - 1;
			assertStaleAggregateDatum("stream " + streamId + " stale hour " + hourIdx, stale,
					new StaleAggregateDatumEntity(streamId, start.plusHours(hourIdx).toInstant(), Hour,
							null));
			i++;
		}
	}

	@Test
	public void markStale_multiNodes_multiTimeZones() {
		// GIVEN
		final LocalDateTime start = LocalDateTime.of(2020, 6, 1, 0, 0);
		final LocalDateTime end = start.plusHours(5);
		final DatumProperties props = newTestProps();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_1));

		setupTestLocation(TEST_LOC_ID_ALT, TEST_TZ_ALT);
		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID_ALT);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID,
				TEST_TZ_ALT);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_2));

		LocalDateTime date = start;

		List<Datum> data = new ArrayList<>();
		while ( date.isBefore(end) ) {
			data.add(newTestDatm(date.atZone(ZoneId.of(meta_1.getTimeZoneId())), meta_1.getStreamId(),
					props));
			data.add(newTestDatm(date.atZone(ZoneId.of(meta_2.getTimeZoneId())), meta_2.getStreamId(),
					props));
			date = date.plusMinutes(30);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, data);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setLocalStartDate(start);
		criteria.setLocalEndDate(end);
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("Rows inserted for data matching filter + previous/stream", count, equalTo(12));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Hour);

		// sort by stream, ts
		stales.sort(new Comparator<StaleAggregateDatum>() {

			@Override
			public int compare(StaleAggregateDatum o1, StaleAggregateDatum o2) {
				int result = DatumDbUtils.UUID_STRING_ORDER.compare(o1.getStreamId(), o2.getStreamId());
				if ( result == 0 ) {
					result = o1.getTimestamp().compareTo(o2.getTimestamp());
				}
				return result;
			}
		});

		SortedMap<UUID, ObjectDatumStreamMetadata> metas = new TreeMap<>(DatumDbUtils.UUID_STRING_ORDER);
		metas.put(meta_1.getStreamId(), meta_1);
		metas.put(meta_2.getStreamId(), meta_2);

		assertThat("Hourly agg rows exist", stales, hasSize(12));
		int i = 0, h = -1;
		for ( StaleAggregateDatum stale : stales ) {
			// because order by ts, stream_id and meta_2 tz < meta_1 tz, always stream 2, stream 1
			UUID streamId = (i < 6 ? metas.firstKey() : metas.lastKey());
			assertStaleAggregateDatum("stream " + streamId + " stale hour " + h, stale,
					new StaleAggregateDatumEntity(
							streamId, start.plusHours(h)
									.atZone(ZoneId.of(metas.get(streamId).getTimeZoneId())).toInstant(),
							Hour, null));
			i++;
			if ( h == 4 ) {
				h = -1;
			} else {
				h++;
			}
		}
	}

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> populateTestData(final long start,
			final int count, final long step, final Long nodeId, final String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(count);
		long ts = start;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(Instant.ofEpochMilli(ts));
			d.setNodeId(nodeId);
			d.setSourceId(sourceId);
			DatumSamples s = new DatumSamples();
			s.putInstantaneousSampleValue("watts", 125);
			s.putAccumulatingSampleValue("wattHours", 10);
			d.setSamples(s);
			data.add(d);
			ts += step;
		}
		return DatumDbUtils.ingestDatumStream(log, jdbcTemplate, data, TEST_TZ);
	}

	private Map<NodeSourcePK, ObjectDatumStreamMetadata> createNodeAndSourceData(ZonedDateTime start) {
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
				start.toInstant().toEpochMilli(), 10, 60000, TEST_NODE_ID, TEST_SOURCE_ID);
		metas.putAll(populateTestData(start.toInstant().toEpochMilli(), 10, 60000, TEST_NODE_ID_ALT,
				TEST_SOURCE_ID));
		return metas;
	}

	@Test
	public void findDatumRecordCounts_typical() {
		// GIVEN
		setupTestNode();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID, TEST_SOURCE_ID);
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMonths(1).toLocalDateTime());
		DatumRecordCounts counts = dao.countDatumRecords(filter);

		// THEN
		assertDatumRecordCounts("Counts", counts, datumRecordCounts(null, 6L, 3L, 3, 1));
	}

	@Test
	public void findDatumRecordCounts_partialHours() {
		// GIVEN
		setupTestNode();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID, TEST_SOURCE_ID);
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.toLocalDateTime().plusMinutes(30));
		filter.setLocalEndDate(start.plusDays(2).plusMinutes(30).toLocalDateTime());
		DatumRecordCounts counts = dao.countDatumRecords(filter);

		// THEN
		assertDatumRecordCounts("Counts", counts, datumRecordCounts(null, 4L, 1L, 1, 0));
	}

	@Test
	public void findDatumRecordCounts_partialHours_multiTimeZones() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_1));

		setupTestLocation(TEST_LOC_ID_ALT, TEST_TZ_ALT);
		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID_ALT);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID,
				TEST_TZ_ALT);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_2));

		final LocalDateTime start = LocalDateTime.of(2018, 11, 1, 0, 0, 0, 0);
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i).atZone(ZoneId.of(meta_1.getTimeZoneId()));
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID, TEST_SOURCE_ID);
		}
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i).atZone(ZoneId.of(meta_2.getTimeZoneId()));
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID_ALT, TEST_SOURCE_ID);
		}

		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.plusMinutes(30));
		filter.setLocalEndDate(start.plusDays(2).plusMinutes(30));
		DatumRecordCounts counts = dao.countDatumRecords(filter);

		// THEN
		assertDatumRecordCounts("Counts", counts, datumRecordCounts(null, 8L, 2L, 2, 0));
	}

	@Test
	public void deleteFiltered_typical() {
		// GIVEN
		setupTestNode();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID, TEST_SOURCE_ID);
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.toLocalDateTime().plusMinutes(30));
		filter.setLocalEndDate(start.plusDays(2).plusMinutes(30).toLocalDateTime());
		long result = dao.deleteFiltered(filter);

		// THEN
		assertThat("Raw delete count all but first/last", result, equalTo(4L));

		ZonedDateTime ts = start;
		ZonedDateTime ts3 = start.plusDays(2).plusMinutes(30);

		List<Datum> rawData = DatumDbUtils.listDatum(jdbcTemplate);
		assertThat("Remaining raw count", rawData, hasSize(2));
		assertThat("Raw 1 date", rawData.get(0).getTimestamp(), equalTo(ts.toInstant()));
		assertThat("Raw 2 date", rawData.get(1).getTimestamp(), equalTo(ts3.toInstant()));

		List<AggregateDatum> hourData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Hour);
		assertThat("Remaining hour count", hourData, hasSize(2));
		assertThat("Hour 1 date", hourData.get(0).getTimestamp(), equalTo(ts.toInstant()));
		assertThat("Hour 2 date", hourData.get(1).getTimestamp(),
				equalTo(ts3.truncatedTo(ChronoUnit.HOURS).toInstant()));

		List<AggregateDatum> dayData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Day);
		assertThat("Remaining day count", dayData, hasSize(2));
		assertThat("Day 1 date", dayData.get(0).getTimestamp(), equalTo(ts.toInstant()));
		assertThat("Day 2 date", dayData.get(1).getTimestamp(),
				equalTo(ts3.truncatedTo(ChronoUnit.DAYS).toInstant()));

		List<AggregateDatum> monData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Month);
		assertThat("Remaining month count", monData, hasSize(1));
		assertThat("Month 1 date", monData.get(0).getTimestamp(), equalTo(ts.toInstant()));
	}

	@Test
	public void deleteFiltered_typical_noEffectOtherStream() {
		// GIVEN
		setupTestNode();
		setupTestNode(TEST_NODE_ID_ALT);
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		final Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = createNodeAndSourceData(start);
		Map<UUID, Long> streamToNodeIds = metas.entrySet().stream()
				.collect(toMap(e -> e.getValue().getStreamId(), e -> e.getKey().getNodeId()));
		final int totalRows = 20;

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(TEST_NODE_ID_ALT);
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.plusMinutes(5).toLocalDateTime());
		int result = (int) dao.deleteFiltered(filter);

		// THEN
		assertThat("Delete count", result, equalTo(5));

		List<Datum> rawData = DatumDbUtils.listDatum(jdbcTemplate);
		assertThat("Remaining row count", rawData, hasSize(totalRows - result));

		List<Datum> node1Data = rawData.stream()
				.filter(m -> streamToNodeIds.get(m.getStreamId()).equals(TEST_NODE_ID))
				.collect(toList());
		assertThat("Remaining node 1 count", node1Data, hasSize(totalRows / 2));

		List<Datum> node2Data = rawData.stream()
				.filter(m -> streamToNodeIds.get(m.getStreamId()).equals(TEST_NODE_ID_ALT))
				.collect(toList());
		assertThat("Remaining node 2 count", node2Data, hasSize(totalRows / 2 - result));
	}

	@Test
	public void deleteFiltered_multiTimeZones() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_1));

		setupTestLocation(TEST_LOC_ID_ALT, TEST_TZ_ALT);
		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID_ALT);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID,
				TEST_TZ_ALT);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta_2));

		final LocalDateTime start = LocalDateTime.of(2018, 11, 1, 0, 0, 0, 0);
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i).atZone(ZoneId.of(meta_1.getTimeZoneId()));
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID, TEST_SOURCE_ID);
		}
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i).atZone(ZoneId.of(meta_2.getTimeZoneId()));
			populateTestData(dayStart.toInstant().toEpochMilli(), 2, TimeUnit.MINUTES.toMillis(30),
					TEST_NODE_ID_ALT, TEST_SOURCE_ID);
		}

		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		filter.setSourceId(TEST_SOURCE_ID);
		filter.setLocalStartDate(start.plusMinutes(30));
		filter.setLocalEndDate(start.plusDays(2).plusMinutes(30));
		long result = dao.deleteFiltered(filter);

		// THEN
		assertThat("Raw delete count all but first/last x2 streams", result, equalTo(8L));

		LocalDateTime ts = start;
		LocalDateTime ts3 = start.plusDays(2).plusMinutes(30);
		assertDeletedDatum("1", meta_1, ts, ts3);
		assertDeletedDatum("2", meta_2, ts, ts3);
	}

	private void assertDeletedDatum(String prefix, ObjectDatumStreamMetadata meta, LocalDateTime ts,
			LocalDateTime ts3) {
		List<Datum> rawData = DatumDbUtils.listDatum(jdbcTemplate);

		List<Datum> rawData_1 = rawData.stream().filter(e -> e.getStreamId().equals(meta.getStreamId()))
				.collect(toList());
		assertThat(prefix + " remaining raw count", rawData_1, hasSize(2));
		assertThat(prefix + " raw 1 date", rawData_1.get(0).getTimestamp(),
				equalTo(ts.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));
		assertThat(prefix + " raw 2 date", rawData_1.get(1).getTimestamp(),
				equalTo(ts3.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));

		List<AggregateDatum> hourData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Hour);
		List<AggregateDatum> hourData_1 = hourData.stream()
				.filter(e -> e.getStreamId().equals(meta.getStreamId())).collect(toList());
		assertThat(prefix + " remaining hour count", hourData_1, hasSize(2));
		assertThat(prefix + " hour 1 date", hourData_1.get(0).getTimestamp(),
				equalTo(ts.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));
		assertThat(prefix + " hour 2 date", hourData_1.get(1).getTimestamp(), equalTo(
				ts3.truncatedTo(ChronoUnit.HOURS).atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));

		List<AggregateDatum> dayData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Day);
		List<AggregateDatum> dayData_1 = dayData.stream()
				.filter(e -> e.getStreamId().equals(meta.getStreamId())).collect(toList());
		assertThat(prefix + " remaining day count", dayData_1, hasSize(2));
		assertThat(prefix + " day 1 date", dayData_1.get(0).getTimestamp(),
				equalTo(ts.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));
		assertThat(prefix + " day 2 date", dayData_1.get(1).getTimestamp(), equalTo(
				ts3.truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));

		List<AggregateDatum> monData = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Month);
		List<AggregateDatum> monData_1 = monData.stream()
				.filter(e -> e.getStreamId().equals(meta.getStreamId())).collect(toList());
		assertThat(prefix + " remaining month count", monData_1, hasSize(1));
		assertThat(prefix + " month 1 date", monData_1.get(0).getTimestamp(),
				equalTo(ts.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant()));
	}

	@Test
	public void deleteByIds_nodeSourceIds() {
		// GIVEN
		setupTestUser();
		setupTestNode();
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		final Set<ObjectDatumId> idsToDelete = new LinkedHashSet<>();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			int count = 5;
			int stepMinutes = 30;
			Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
					dayStart.toInstant().toEpochMilli(), count, TimeUnit.MINUTES.toMillis(stepMinutes),
					TEST_NODE_ID, TEST_SOURCE_ID);
			for ( Entry<NodeSourcePK, ObjectDatumStreamMetadata> e : metas.entrySet() ) {
				ObjectDatumStreamMetadata m = e.getValue();
				// add random IDs to delete from stream
				for ( int t = 0, len = RNG.nextInt(count - 1) + 1; t < len; t++ ) {
					idsToDelete.add(nodeId(null, m.getObjectId(), m.getSourceId(),
							dayStart.plusMinutes(RNG.nextInt(count) * 30).toInstant(),
							Aggregation.None));
				}
			}
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		log.debug("Deleting datum: [{}]",
				idsToDelete.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		Set<ObjectDatumId> result = dao.deleteForIds(TEST_USER_ID, idsToDelete);

		// THEN
		// @formatter:off
		then(result)
			.as("All IDs to delete returned")
			.hasSize(idsToDelete.size())
			.allSatisfy(id -> {
				then(id)
					.as("Stream ID populated in result")
					.doesNotReturn(null, ObjectDatumId::getStreamId)
					;
				ObjectDatumId match = idsToDelete.stream().filter(e -> e.isEquivalent(id)).findAny().orElse(null);
				then(match)
					.as("Expected ID deleted")
					.isNotNull()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void deleteByIds_streamIds() {
		// GIVEN
		setupTestUser();
		setupTestNode();
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		final Set<ObjectDatumId> idsToDelete = new LinkedHashSet<>();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			int count = 5;
			int stepMinutes = 30;
			Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
					dayStart.toInstant().toEpochMilli(), count, TimeUnit.MINUTES.toMillis(stepMinutes),
					TEST_NODE_ID, TEST_SOURCE_ID);
			for ( Entry<NodeSourcePK, ObjectDatumStreamMetadata> e : metas.entrySet() ) {
				ObjectDatumStreamMetadata m = e.getValue();
				// add random IDs to delete from stream
				for ( int t = 0, len = RNG.nextInt(count - 1) + 1; t < len; t++ ) {
					idsToDelete.add(nodeId(m.getStreamId(), null, null,
							dayStart.plusMinutes(RNG.nextInt(count) * 30).toInstant(),
							Aggregation.None));
				}
			}
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		List<Map<String, Object>> datumRowsBefore = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		log.debug("Deleting datum: [{}]",
				idsToDelete.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		Set<ObjectDatumId> result = dao.deleteForIds(TEST_USER_ID, idsToDelete);

		// THEN
		List<Map<String, Object>> datumRowsAfter = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		// @formatter:off
		then(result)
			.as("All IDs to delete returned")
			.hasSize(idsToDelete.size())
			.allSatisfy(id -> {
				then(id)
					.as("Object ID populated in result")
					.returns(TEST_NODE_ID, ObjectDatumId::getObjectId)
					.as("Source ID populated in result")
					.returns(TEST_SOURCE_ID, ObjectDatumId::getSourceId)
					;
				ObjectDatumId match = idsToDelete.stream().filter(e -> e.isEquivalent(id)).findAny().orElse(null);
				then(match)
					.as("Expected ID deleted")
					.isNotNull()
					;
			})
			;

		then(datumRowsAfter)
			.as("Rows deleted from database table")
			.hasSize(datumRowsBefore.size() - idsToDelete.size())
			;
		// @formatter:on
	}

	@Test
	public void deleteByIds_streamIds_notAll() {
		// GIVEN
		setupTestUser();
		setupTestNode();
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		final Set<ObjectDatumId> idsToDelete = new LinkedHashSet<>();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			int count = 5;
			int stepMinutes = 30;
			Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
					dayStart.toInstant().toEpochMilli(), count, TimeUnit.MINUTES.toMillis(stepMinutes),
					TEST_NODE_ID, TEST_SOURCE_ID);
			for ( Entry<NodeSourcePK, ObjectDatumStreamMetadata> e : metas.entrySet() ) {
				ObjectDatumStreamMetadata m = e.getValue();
				// add random IDs to delete from stream
				for ( int t = 0, len = RNG.nextInt(count - 1) + 1; t < len; t++ ) {
					idsToDelete.add(nodeId(m.getStreamId(), null, null,
							dayStart.plusMinutes(RNG.nextInt(count) * 30 + (RNG.nextBoolean() ? 1 : 0))
									.toInstant(),
							Aggregation.None));
				}
			}
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		List<Map<String, Object>> datumRowsBefore = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		log.debug("Deleting datum: [{}]",
				idsToDelete.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		Set<ObjectDatumId> result = dao.deleteForIds(TEST_USER_ID, idsToDelete);

		// THEN
		List<Map<String, Object>> datumRowsAfter = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		// can only match on datum whose date is 0 or 30 minutes after the hour
		Set<ObjectDatumId> expectedIdsToDelete = idsToDelete.stream()
				.filter(e -> e.getTimestamp().getEpochSecond() % 1800 == 0).collect(toSet());
		log.debug("Expected deleted datum: [{}]", expectedIdsToDelete.stream().map(Object::toString)
				.collect(joining("\n\t", "\n\t", "\n")));

		// @formatter:off
		then(result)
			.as("All IDs to delete returned")
			.hasSize(expectedIdsToDelete.size())
			.allSatisfy(id -> {
				then(id)
					.as("Object ID populated in result")
					.returns(TEST_NODE_ID, ObjectDatumId::getObjectId)
					.as("Source ID populated in result")
					.returns(TEST_SOURCE_ID, ObjectDatumId::getSourceId)
					;
				ObjectDatumId match = expectedIdsToDelete.stream().filter(e -> e.isEquivalent(id)).findAny().orElse(null);
				then(match)
					.as("Expected ID deleted")
					.isNotNull()
					;
			})
			;

		then(datumRowsAfter)
			.as("Rows deleted from database table")
			.hasSize(datumRowsBefore.size() - expectedIdsToDelete.size())
			;
		// @formatter:on
	}

	@Test
	public void deleteByIds_streamIds_wrongUser() {
		// GIVEN
		setupTestUser();
		setupTestNode();
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		final Set<ObjectDatumId> idsToDelete = new LinkedHashSet<>();
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ));
		for ( int i = 0; i < 3; i++ ) {
			ZonedDateTime dayStart = start.plusDays(i);
			int count = 5;
			int stepMinutes = 30;
			Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = populateTestData(
					dayStart.toInstant().toEpochMilli(), count, TimeUnit.MINUTES.toMillis(stepMinutes),
					TEST_NODE_ID, TEST_SOURCE_ID);
			for ( Entry<NodeSourcePK, ObjectDatumStreamMetadata> e : metas.entrySet() ) {
				ObjectDatumStreamMetadata m = e.getValue();
				// add random IDs to delete from stream
				for ( int t = 0, len = RNG.nextInt(count - 1) + 1; t < len; t++ ) {
					idsToDelete.add(nodeId(m.getStreamId(), null, null,
							dayStart.plusMinutes(RNG.nextInt(count) * 30).toInstant(),
							Aggregation.None));
				}
			}
		}
		DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate);
		log.debug("Raw data:\n{}", DatumDbUtils.listDatum(jdbcTemplate).stream().map(Object::toString)
				.collect(joining("\n")));
		for ( Aggregation agg : EnumSet.of(Hour, Aggregation.Day, Aggregation.Month) ) {
			log.debug(agg + " data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, agg).stream()
					.map(Object::toString).collect(joining("\n")));
		}

		// WHEN
		List<Map<String, Object>> datumRowsBefore = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		log.debug("Deleting datum: [{}]",
				idsToDelete.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		Set<ObjectDatumId> result = dao.deleteForIds(TEST_USER_ID - 1L, idsToDelete);

		// THEN
		List<Map<String, Object>> datumRowsAfter = allTableData(log, jdbcTemplate, "solardatm.da_datm",
				"stream_id,ts");

		// @formatter:off
		then(result)
			.as("No IDs deleted because wrong user")
			.isEmpty()
			;

		then(datumRowsAfter)
			.as("No rows deleted from database table")
			.hasSize(datumRowsBefore.size())
			;
		// @formatter:on
	}
}
