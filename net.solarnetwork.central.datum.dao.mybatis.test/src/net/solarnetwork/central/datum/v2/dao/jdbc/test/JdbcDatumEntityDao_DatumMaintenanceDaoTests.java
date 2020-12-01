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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertStaleAggregateDatum;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StreamKindPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link DatumMaintenanceDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_DatumMaintenanceDaoTests extends BaseDatumJdbcTestSupport {

	private static final long TEST_NODE_ID_ALT = 2L;
	private static final long TEST_LOC_ID_ALT = 2L;
	private static final String TEST_TZ_ALT = "America/New_York";

	private JdbcDatumEntityDao dao;

	@Before
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
	public void findFilteredNoData() {
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
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
	}

	@Test
	public void findFiltered_noMatchingDataByNode() {
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
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
	}

	@Test
	public void findFiltered_noMatchingDataBySource() {
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
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
	}

	@Test
	public void findFiltered_noMatchingDataByDate() {
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
		criteria.setEndDate(start.toInstant());
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(0));
		assertThat("Total result count", results.getTotalResults(), equalTo(0L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
	}

	@Test
	public void findFiltered() {
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
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
			i++;
		}
	}

	@Test
	public void findFiltered_subset() {
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
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
			i++;
		}
	}

	@Test
	public void findFiltered_paginated_first() {
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
		criteria.setOffset(0);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		int i = 0;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
			i++;
		}
	}

	@Test
	public void findFiltered_paginated_middle() {
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
		criteria.setOffset(2);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(2));
		int i = 2;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
			i++;
		}
	}

	@Test
	public void findFiltered_paginated_end() {
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
		criteria.setOffset(4);
		dao.markDatumAggregatesStale(criteria);

		// WHEN
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<StaleAggregateDatum, StreamKindPK> results = dao.findStaleAggregateDatum(criteria);

		// THEN
		assertThat("Results available", results, notNullValue());
		assertThat("Total result count", results.getTotalResults(), equalTo(5L));
		assertThat("Returned result count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(4));
		int i = 4;
		for ( StaleAggregateDatum stale : results ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
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
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
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
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
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
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
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
		criteria.setEndDate(start.toInstant());
		int count = dao.markDatumAggregatesStale(criteria);

		// THEN
		assertThat("No rows inserted because no data matching filter", count, equalTo(0));
		List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
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
		assertThat("Rows inserted for data matching filter", count, equalTo(5));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(5));
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
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
		assertThat("Rows inserted for data matching filter", count, equalTo(2));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(2));
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			assertStaleAggregateDatum("stale hour " + i, stale, new StaleAggregateDatumEntity(
					meta.getStreamId(), start.plusHours(i).toInstant(), Aggregation.Hour, null));
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
		assertThat("Rows inserted for data matching filter", count, equalTo(10));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		assertThat("Hourly agg rows exist", stales, hasSize(10));
		UUID[] streamIds = new UUID[] { meta_1.getStreamId(), meta_2.getStreamId() };
		Arrays.sort(streamIds, DatumDbUtils.UUID_STRING_ORDER);
		int i = 0;
		for ( StaleAggregateDatum stale : stales ) {
			UUID streamId = streamIds[i % 2];
			assertStaleAggregateDatum("stream " + streamId + " stale hour " + i / 2, stale,
					new StaleAggregateDatumEntity(streamId, start.plusHours(i / 2).toInstant(),
							Aggregation.Hour, null));
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
		assertThat("Rows inserted for data matching filter", count, equalTo(10));
		List<StaleAggregateDatum> stales = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);

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

		assertThat("Hourly agg rows exist", stales, hasSize(10));
		int i = 0, h = 0;
		for ( StaleAggregateDatum stale : stales ) {
			// because order by ts, stream_id and meta_2 tz < meta_1 tz, always stream 2, stream 1
			UUID streamId = (i < 5 ? metas.firstKey() : metas.lastKey());
			assertStaleAggregateDatum("stream " + streamId + " stale hour " + h, stale,
					new StaleAggregateDatumEntity(
							streamId, start.plusHours(h)
									.atZone(ZoneId.of(metas.get(streamId).getTimeZoneId())).toInstant(),
							Aggregation.Hour, null));
			i++;
			if ( h == 4 ) {
				h = 0;
			} else {
				h++;
			}
		}
	}

}
