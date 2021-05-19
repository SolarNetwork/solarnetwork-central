/* ==================================================================
 * JdbcDatumEntityDao_Aggregate.java - 24/11/2020 2:59:31 pm
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

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.remapStream;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.toGeneralNodeDatum;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class implementation of
 * aggregate queries.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_AggregateTests extends BaseDatumJdbcTestSupport {

	private static final Long TEST_NODE_ID_ALT = 123L;
	private static final String TEST_SOURCE_ID_ALT = "s2";
	private static final Long TEST_LOC_ID_ALT = -879L;
	private static final String TEST_TZ_ALT = "America/New_York";

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a", "UTC");
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, ObjectDatumKind.Node, nodeId, sourceId, new String[] { "x", "y" },
				new String[] { "w" }, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	private ObjectDatumStreamMetadata testStreamMetadata_simple() {
		return testStreamMetadata_simple(1L, "a", "UTC");
	}

	private ObjectDatumStreamMetadata testStreamMetadata_simple(Long nodeId, String sourceId,
			String timeZoneId) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, ObjectDatumKind.Node, nodeId, sourceId, new String[] { "x" },
				new String[] { "w" }, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	private void insertDatum(Aggregation agg, UUID streamId, ZonedDateTime start,
			TemporalAmount frequency, int count) {
		List<AggregateDatum> data = new ArrayList<>(count);
		ZonedDateTime ts = start;
		for ( int i = 0; i < count; i++ ) {
			AggregateDatum d = new AggregateDatumEntity(streamId, ts.toInstant(), agg,
					DatumProperties.propertiesOf(
							new BigDecimal[] { BigDecimal.valueOf(Math.random() * 1000000) },
							new BigDecimal[] { BigDecimal.valueOf(i + 1) }, null, null),
					DatumPropertiesStatistics.statisticsOf(
							new BigDecimal[][] { decimalArray("6", "0", "100") }, new BigDecimal[][] {
									decimalArray("10", String.valueOf(i), String.valueOf(i + 10)) }));
			data.add(d);
			ts = ts.plus(frequency);
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, data);
	}

	private ObjectDatumStreamFilterResults<Datum, DatumPK> execute(DatumCriteria filter) {
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);
		log.debug("Filter results:\n{}",
				stream(results.spliterator(), false).map(Object::toString).collect(joining("\n")));
		return results;
	}

	@Test
	public void find_hour_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.plusHours(1).toInstant());
		filter.setEndDate(start.plusHours(12).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Results from start to end returned", results.getTotalResults(), equalTo(3L));

		Instant ts = start.plusHours(3).toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_reading_hour_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.plusHours(1).toInstant());
		filter.setEndDate(start.plusHours(12).toInstant());
		filter.setReadingType(DatumReadingType.Difference);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Results from start to end returned", results.getTotalResults(), equalTo(3L));

		Instant ts = start.plusHours(3).toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Reading datum returned " + i, d, instanceOf(ReadingDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_hour_streamId_orderTimeDesc() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.plusHours(1).toInstant());
		filter.setEndDate(start.plusHours(12).toInstant());
		filter.setSorts(singletonList(new SimpleSortDescriptor("time", true)));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Results from start to end returned", results.getTotalResults(), equalTo(3L));

		Instant ts = start.plusHours(9).toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.minusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_hour_paginated_withTotalResultCount_first() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());
		filter.setMax(3);
		filter.setWithoutTotalResultsCount(false);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Total result count", results.getTotalResults(), equalTo(8L));
		assertThat("Offset for first page", results.getStartingOffset(), equalTo(0));
		assertThat("Results count for first page", results.getReturnedResultCount(), equalTo(3));

		Instant ts = start.toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_hour_paginated_withTotalResultCount_middle() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());
		filter.setMax(3);
		filter.setOffset(3);
		filter.setWithoutTotalResultsCount(false);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Total result count", results.getTotalResults(), equalTo(8L));
		assertThat("Offset for middle page", results.getStartingOffset(), equalTo(3));
		assertThat("Results count for middle page", results.getReturnedResultCount(), equalTo(3));

		Instant ts = start.plusHours(9).toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_hour_paginated_withTotalResultCount_end() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());
		filter.setMax(3);
		filter.setOffset(6);
		filter.setWithoutTotalResultsCount(false);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Total result count", results.getTotalResults(), equalTo(8L));
		assertThat("Offset for last page", results.getStartingOffset(), equalTo(6));
		assertThat("Results count for last page", results.getReturnedResultCount(), equalTo(2));

		Instant ts = start.plusHours(18).toInstant();
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts));
			ts = ts.plusSeconds(TimeUnit.HOURS.toSeconds(3));
			i++;
		}
	}

	@Test
	public void find_hour_paginated_withTotalResultCount_over() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(24).toInstant());
		filter.setMax(3);
		filter.setOffset(8);
		filter.setWithoutTotalResultsCount(false);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Total result count", results.getTotalResults(), equalTo(8L));
		assertThat("Offset for last page", results.getStartingOffset(), equalTo(8));
		assertThat("Results count for last page", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void find_hour_streamId_mostRecent() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		filter.setStreamId(streamId);
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Result for most recent returned", results.getTotalResults(), equalTo(1L));

		Instant ts = start.plusHours(21).toInstant();
		Datum d = results.iterator().next();
		assertThat("Aggregate datum returned", d, instanceOf(AggregateDatum.class));
		assertThat("Stream ID", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp", d.getTimestamp(), equalTo(ts));
	}

	@Test
	public void find_day_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-day-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setStreamId(streamId);
		filter.setStartDate(start.plusDays(1).toInstant());
		filter.setEndDate(start.plusDays(19).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Results from start to end returned", results.getTotalResults(), equalTo(3L));

		ZonedDateTime ts = start.plusDays(4);
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts.toInstant()));
			ts = ts.plusDays(5);
			i++;
		}
	}

	@Test
	public void find_day_streamId_mostRecent() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-day-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Day);
		filter.setStreamId(streamId);
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Result for most recent returned", results.getTotalResults(), equalTo(1L));

		Instant ts = start.plusDays(29).toInstant();
		Datum d = results.iterator().next();
		assertThat("Aggregate datum returned", d, instanceOf(AggregateDatum.class));
		assertThat("Stream ID", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp", d.getTimestamp(), equalTo(ts));
	}

	@Test
	public void find_month_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-month-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setStreamId(streamId);
		filter.setStartDate(start.plusMonths(1).toInstant());
		filter.setEndDate(start.plusMonths(4).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Results from start to end returned", results.getTotalResults(), equalTo(3L));

		ZonedDateTime ts = start.plusMonths(1);
		int i = 1;
		for ( Datum d : results ) {
			assertThat("Aggregate datum returned " + i, d, instanceOf(AggregateDatum.class));
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(streamId));
			assertThat("Ordered by timestamp " + i, d.getTimestamp(), equalTo(ts.toInstant()));
			ts = ts.plusMonths(1);
			i++;
		}
	}

	@Test
	public void find_month_streamId_mostRecent() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-month-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Month);
		filter.setStreamId(streamId);
		filter.setMostRecent(true);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Result for most recent returned", results.getTotalResults(), equalTo(1L));

		Instant ts = start.plusMonths(6).toInstant();
		Datum d = results.iterator().next();
		assertThat("Aggregate datum returned", d, instanceOf(AggregateDatum.class));
		assertThat("Stream ID", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp", d.getTimestamp(), equalTo(ts));
	}

	@Test
	public void find_year_streamId_orderDefault() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-month-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Year);
		filter.setStreamId(streamId);
		filter.setLocalStartDate(start.toLocalDateTime());
		filter.setLocalEndDate(start.toLocalDateTime().plusYears(1));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		ObjectDatumStreamMetadata resultMeta = results.metadataForStreamId(streamId);
		assertThat("Metadata is for node", resultMeta.getKind(), equalTo(ObjectDatumKind.Node));
		assertThat("Node ID", resultMeta.getObjectId(), equalTo(meta.getObjectId()));
		assertThat("Result for year", results.getTotalResults(), equalTo(1L));
		assertThat("Result for year", results.getReturnedResultCount(), equalTo(1));

		Datum d = results.iterator().next();
		assertThat("Aggregate datum returned", d, instanceOf(AggregateDatum.class));
		DatumTestUtils.assertAggregateDatum("Year", (AggregateDatum) d,
				new AggregateDatumEntity(streamId, start.toInstant(), Aggregation.Year,
						propertiesOf(decimalArray("1.5", "5.1"), decimalArray("2800"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("42", "1.1", "3.7"),
										decimalArray("42", "2.0", "7.7") },
								new BigDecimal[][] { decimalArray("721", "100", "821") })));
	}

	@Test
	public void find_year_multipleYears_localTimeZones() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata fileMeta = testStreamMetadata();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, TEST_TZ);
		List<AggregateDatum> datums_1 = loadJsonAggregateDatumResource("test-agg-month-datum-03.txt",
				getClass(), staticProvider(singleton(fileMeta)), e -> {
					// map data to stream 1 (with dates adjusted)
					return new AggregateDatumEntity(meta_1.getStreamId(),
							LocalDateTime.ofInstant(e.getTimestamp(), ZoneOffset.UTC)
									.atZone(ZoneId.of(TEST_TZ)).toInstant(),
							e.getAggregation(), e.getProperties(), e.getStatistics());
				});

		setupTestLocation(TEST_LOC_ID_ALT, TEST_TZ_ALT);
		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID_ALT);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID_ALT,
				TEST_TZ_ALT);
		List<AggregateDatum> datums_2 = loadJsonAggregateDatumResource("test-agg-month-datum-03.txt",
				getClass(), staticProvider(singleton(fileMeta)), e -> {
					// map data to stream 2 (with dates adjusted)
					return new AggregateDatumEntity(meta_2.getStreamId(),
							LocalDateTime.ofInstant(e.getTimestamp(), ZoneOffset.UTC)
									.atZone(ZoneId.of(TEST_TZ_ALT)).toInstant(),
							e.getAggregation(), e.getProperties(), e.getStatistics());
				});
		insertAggregateDatum(log, jdbcTemplate,
				concat(datums_1.stream(), datums_2.stream()).collect(toList()));

		log.debug("Month raw data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Month)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		LocalDateTime start = LocalDateTime.of(2019, 1, 1, 0, 0);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID_ALT });
		filter.setLocalStartDate(start);
		filter.setLocalEndDate(filter.getLocalStartDate().plusYears(2));
		filter.setAggregation(Aggregation.Year);
		filter.setSorts(sorts("time", "node"));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results available", results, notNullValue());
		assertThat("2x years per stream returned", results.getTotalResults(), equalTo(4L));
		assertThat("2x years per stream returned", results.getReturnedResultCount(), equalTo(4));

		List<AggregateDatum> data_1 = StreamSupport.stream(results.spliterator(), false)
				.filter(e -> meta_1.getStreamId().equals(e.getStreamId()))
				.map(AggregateDatum.class::cast).collect(toList());

		List<AggregateDatum> data_2 = StreamSupport.stream(results.spliterator(), false)
				.filter(e -> meta_2.getStreamId().equals(e.getStreamId()))
				.map(AggregateDatum.class::cast).collect(toList());

		assertYearStream("Stream 1", data_1, meta_1, start);
		assertYearStream("Stream 1", data_2, meta_2, start);
	}

	private void assertYearStream(String prefix, List<AggregateDatum> data,
			ObjectDatumStreamMetadata meta, LocalDateTime start) {
		assertThat(prefix + " 2x years for stream 1", data, hasSize(2));
		assertAggregateDatum(prefix + " year 1", data.get(0),
				new AggregateDatumEntity(meta.getStreamId(),
						start.atZone(ZoneId.of(meta.getTimeZoneId())).toInstant(), Aggregation.Year,
						propertiesOf(decimalArray("1.3", "3.1"), decimalArray("600"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("18", "1.1", "3.3"),
										decimalArray("18", "2.0", "7.3") },
								new BigDecimal[][] { decimalArray("303", "100", "403") })));
		assertAggregateDatum(prefix + " year 2", data.get(1),
				new AggregateDatumEntity(meta.getStreamId(),
						start.plusYears(1).atZone(ZoneId.of(meta.getTimeZoneId())).toInstant(),
						Aggregation.Year,
						propertiesOf(decimalArray("1.65", "6.6"), decimalArray("2200"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("24", "1.4", "3.7"),
										decimalArray("24", "2.3", "7.7") },
								new BigDecimal[][] { decimalArray("418", "403", "821") })));
	}

	@Test
	public void find_year_multipleYears_combine_sum() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata fileMeta = testStreamMetadata();

		setupTestLocation(TEST_LOC_ID, "UTC");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(TEST_NODE_ID, TEST_SOURCE_ID, "UTC");
		List<AggregateDatum> datums_1 = loadJsonAggregateDatumResource("test-agg-month-datum-03.txt",
				getClass(), staticProvider(singleton(fileMeta)), e -> {
					// map data to stream 1 (with dates adjusted)
					return new AggregateDatumEntity(meta_1.getStreamId(), e.getTimestamp(),
							e.getAggregation(), e.getProperties(), e.getStatistics());
				});

		setupTestNode(TEST_NODE_ID_ALT, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(TEST_NODE_ID_ALT, TEST_SOURCE_ID_ALT,
				"UTC");
		List<AggregateDatum> datums_2 = loadJsonAggregateDatumResource("test-agg-month-datum-03.txt",
				getClass(), staticProvider(singleton(fileMeta)), e -> {
					// map data to stream 2 (with dates adjusted)
					return new AggregateDatumEntity(meta_2.getStreamId(), e.getTimestamp(),
							e.getAggregation(), e.getProperties(), e.getStatistics());
				});
		insertAggregateDatum(log, jdbcTemplate,
				concat(datums_1.stream(), datums_2.stream()).collect(toList()));

		log.debug("Month raw data:\n{}", DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Month)
				.stream().map(Object::toString).collect(joining("\n")));

		// WHEN
		LocalDateTime start = LocalDateTime.of(2019, 1, 1, 0, 0);
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_ALT });
		filter.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID_ALT });
		filter.setLocalStartDate(start);
		filter.setLocalEndDate(filter.getLocalStartDate().plusYears(2));
		filter.setAggregation(Aggregation.Year);
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "2:-1,123" });
		filter.setSourceIdMaps(new String[] { "V:test.source,s2" });
		filter.setSorts(sorts("time", "node"));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results available", results, notNullValue());
		assertThat("2x years per combined streams", results.getTotalResults(), equalTo(2L));
		assertThat("2x years per combined streams", results.getReturnedResultCount(), equalTo(2));

		List<AggregateDatum> data = StreamSupport.stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());

		assertThat("2x years for combined virtual stream", data, hasSize(2));
		UUID virtualStreamId = DatumUtils.virtualStreamId(2L, "V");
		assertAggregateDatum("Year 1", data.get(0),
				new AggregateDatumEntity(virtualStreamId, start.atZone(ZoneOffset.UTC).toInstant(),
						Aggregation.Year,
						propertiesOf(decimalArray("2.6", "6.2"), decimalArray("1200"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("606", null, null) })));
		assertAggregateDatum("Year 2", data.get(1),
				new AggregateDatumEntity(virtualStreamId,
						start.plusYears(1).atZone(ZoneOffset.UTC).toInstant(), Aggregation.Year,
						propertiesOf(decimalArray("3.3", "13.2"), decimalArray("4400"), null, null),
						statisticsOf(null, new BigDecimal[][] { decimalArray("836", null, null) })));

	}

	@Test
	public void find_hour_nodeAndSource_absoluteDates() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		ObjectDatumStreamMetadata meta_2 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 2L, "b");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Duration freq = Duration.ofHours(3);
		insertDatum(Aggregation.Hour, meta_1.getStreamId(), start, freq, 4);
		insertDatum(Aggregation.Hour, meta_2.getStreamId(), start.plusHours(1), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(2L);
		filter.setSourceId("b");
		filter.setAggregation(Aggregation.Hour);
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(7).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		for ( int i = 0; i < 2; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(meta_2.getStreamId()));
			assertThat("Timestamp", d.getTimestamp(), equalTo(start.plusHours(i * 3 + 1).toInstant()));
		}
	}

	@Test
	public void find_hour_source_mostRecent() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		ObjectDatumStreamMetadata meta_2 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 2L, "b");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Duration freq = Duration.ofHours(3);
		insertDatum(Aggregation.Hour, meta_1.getStreamId(), start, freq, 4);
		insertDatum(Aggregation.Hour, meta_2.getStreamId(), start.plusHours(1), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("b");
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Hour);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));

		Datum d = datumList.get(0);
		assertThat("Stream ID ", d.getStreamId(), equalTo(meta_2.getStreamId()));
		assertThat("Max timestamp", d.getTimestamp(), equalTo(start.plusHours(3 * 3 + 1).toInstant()));
	}

	@Test
	public void find_hour_nodeAndSource_mostRecent_fewerDatumPropsThanMetadataProps() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a",
				new String[] { "watts", "current", "voltage", "frequency", "realPower", "powerFactor",
						"phaseVoltage", "apparentPower", "reactivePower", "tou", "cost" },
				new String[] { "wattHours", "cost" }, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1));

		// @formatter:off
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		AggregateDatum h1 = new AggregateDatumEntity(meta_1.getStreamId(), start.toInstant(), Aggregation.Hour,
				DatumProperties.propertiesOf(
						decimalArray("6528.083333333","2.03884575","230.905399167","49.969080083","6011.75","0.999999953","13.063502917","6011.75","1.333333333","11"),
						decimalArray("1192.249933333","13.114749267"),
						new String[] {"_v2"},
						null),
				DatumPropertiesStatistics.statisticsOf(
						new BigDecimal[][] { 
							decimalArray("12","833","17305"),
							decimalArray("12","-33.023814","56.901104"),
							decimalArray("12","225.10013","234.99686"),
							decimalArray("12","49.50017","50.49986"),
							decimalArray("12","3485","10181"),
							decimalArray("12","0.99999991","0.99999999"),
							decimalArray("12","-298.8894","330.6257"),
							decimalArray("12","3485","10181"),
							decimalArray("12","0","4"),
							decimalArray("12","11","11"),
							}, 
						new BigDecimal[][] { 
							decimalArray("1195","5983591","5984786"),
							decimalArray("13.145","-182.25348","-169.10848"),
						}));
		// @formatter:on
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, asList(h1));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setSourceId("a");
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Hour);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));

		Datum d = datumList.get(0);
		assertThat("Stream ID ", d.getStreamId(), equalTo(meta_1.getStreamId()));
		assertThat("Timestamp", d.getTimestamp(), equalTo(h1.getTimestamp()));

		List<ReportingGeneralNodeDatumMatch> data = stream(results.spliterator(), false)
				.map(e -> toGeneralNodeDatum(e, results.metadataForStreamId(e.getStreamId())))
				.collect(toList());
		ReportingGeneralNodeDatumMatch m = data.get(0);
		Map<String, ?> sample = m.getSampleData();
		assertThat("Watts min", sample, hasEntry("watts_min", new BigDecimal("833")));
		assertThat("Watts max", sample, hasEntry("watts_max", new BigDecimal("17305")));
	}

	@Test
	public void find_hour_nodeAndSource_localDates_combinedSum() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Duration freq = Duration.ofHours(3);
		insertDatum(Aggregation.Hour, meta_1.getStreamId(), start, freq, 4);
		insertDatum(Aggregation.Hour, meta_2.getStreamId(), start, freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setAggregation(Aggregation.Hour);
		filter.setLocalStartDate(start.plusHours(3).toLocalDateTime());
		filter.setLocalEndDate(start.plusHours(9).toLocalDateTime());
		filter.setCombiningType(CombiningType.Sum);
		filter.setObjectIdMaps(new String[] { "0:1,2" });
		filter.setSourceIdMaps(new String[] { "V:a,b" });
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		UUID vStreamId = DatumUtils.virtualStreamId(0L, "V");
		for ( int i = 0; i < 2; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(vStreamId));
			assertThat("Timestamp", d.getTimestamp(), equalTo(start.plusHours(i * 3 + 3).toInstant()));
			assertThat("Accumulation is sum", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(String.valueOf((i + 2) * 2))));
		}
	}

	@Test
	public void find_hour_nodeAndSource_localDates_combinedDiff() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Duration freq = Duration.ofHours(3);
		insertDatum(Aggregation.Hour, meta_1.getStreamId(), start, freq, 4);

		// start 2nd stream at offset so Diff combine doesn't result in 0
		insertDatum(Aggregation.Hour, meta_2.getStreamId(), start.plusHours(3), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSourceIds(new String[] { "a", "b" });
		filter.setAggregation(Aggregation.Hour);
		filter.setLocalStartDate(start.plusHours(3).toLocalDateTime());
		filter.setLocalEndDate(start.plusHours(9).toLocalDateTime());
		filter.setCombiningType(CombiningType.Difference);
		filter.setObjectIdMaps(new String[] { "0:1,2" });
		filter.setSourceIdMaps(new String[] { "V:a,b" });
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		UUID vStreamId = DatumUtils.virtualStreamId(0L, "V");
		for ( int i = 0; i < 2; i++ ) {
			Datum d = datumList.get(i);
			assertThat("Stream ID ", d.getStreamId(), equalTo(vStreamId));
			assertThat("Timestamp", d.getTimestamp(), equalTo(start.plusHours(i * 3 + 3).toInstant()));
			assertThat("Accumulation is diff", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(String.valueOf((i + 2) - (i + 1)))));
		}
	}

	@Test
	public void find_day_source_mostRecent() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		ObjectDatumStreamMetadata meta_2 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 2L, "b");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Duration freq = Duration.ofHours(36);
		insertDatum(Aggregation.Day, meta_1.getStreamId(), start, freq, 4);
		insertDatum(Aggregation.Day, meta_2.getStreamId(), start.plusHours(24), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("b");
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Day);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));

		Datum d = datumList.get(0);
		assertThat("Stream ID ", d.getStreamId(), equalTo(meta_2.getStreamId()));
		assertThat("Max timestamp", d.getTimestamp(), equalTo(start.plusHours(3 * 36 + 24).toInstant()));
	}

	@Test
	public void find_month_source_mostRecent() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		ObjectDatumStreamMetadata meta_2 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 2L, "b");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Period freq = Period.ofMonths(3);
		insertDatum(Aggregation.Month, meta_1.getStreamId(), start, freq, 4);
		insertDatum(Aggregation.Month, meta_2.getStreamId(), start.plusMonths(1), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setSourceId("b");
		filter.setMostRecent(true);
		filter.setAggregation(Aggregation.Month);
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<Datum> datumList = StreamSupport.stream(results.spliterator(), false).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));

		Datum d = datumList.get(0);
		assertThat("Stream ID ", d.getStreamId(), equalTo(meta_2.getStreamId()));
		assertThat("Max timestamp", d.getTimestamp(), equalTo(start.plusMonths(3 * 3 + 1).toInstant()));
	}

	@Test
	public void find_runningTotal_node_absoluteDates() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata_simple();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-runtot-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setStartDate(LocalDate.of(2017, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
		filter.setEndDate(LocalDateTime.of(2017, 4, 4, 3, 0).toInstant(ZoneOffset.UTC));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = StreamSupport.stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));
		DatumTestUtils.assertAggregateDatum("Running total", datumList.get(0),
				new AggregateDatumEntity(streamId, null, Aggregation.RunningTotal,
						propertiesOf(decimalArray("1.310469799"), decimalArray("945"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("13410", "1.1", "3.3") },
								new BigDecimal[][] { decimalArray("936", "100", "1036") })));
	}

	@Test
	public void find_runningTotal_node_absoluteDates_subset() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata_simple();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-runtot-datum-03.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(LocalDateTime.of(2017, 2, 3, 3, 0).toInstant(ZoneOffset.UTC));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(1L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = StreamSupport.stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(1));
		DatumTestUtils.assertAggregateDatum("Running total", datumList.get(0),
				new AggregateDatumEntity(streamId, null, Aggregation.RunningTotal,
						propertiesOf(decimalArray("1.203891051"), decimalArray("202"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("4626", "1.1", "3.3") },
								new BigDecimal[][] { decimalArray("201", "100", "403") })));
	}

	@Test
	public void find_runningTotal_nodes_sortNode() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata_simple(1L, "a", "UTC");
		List<AggregateDatum> datums_1 = loadJsonAggregateDatumResource("test-agg-runtot-datum-01.txt",
				getClass(), staticProvider(singleton(meta_1)));
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata_simple(2L, "a", "UTC");
		List<AggregateDatum> datums_2 = loadJsonAggregateDatumResource("test-agg-runtot-datum-02.txt",
				getClass(), staticProvider(singleton(meta_1)), remapStream(meta_2.getStreamId()));
		insertAggregateDatum(log, jdbcTemplate,
				concat(datums_1.stream(), datums_2.stream()).collect(toList()));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { meta_1.getObjectId(), meta_2.getObjectId() });
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setSorts(sorts("node"));
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = StreamSupport.stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		// stream 1 (order by node)
		AggregateDatum d = datumList.get(0);
		DatumTestUtils.assertAggregateDatum("Running total", d,
				new AggregateDatumEntity(meta_1.getStreamId(), null, Aggregation.RunningTotal,
						propertiesOf(decimalArray("1.310469799"), decimalArray("945"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("13410", "1.1", "3.3") },
								new BigDecimal[][] { decimalArray("936", "100", "1036") })));
		// stream 2
		d = datumList.get(1);
		DatumTestUtils.assertAggregateDatum("Running total", d,
				new AggregateDatumEntity(meta_2.getStreamId(), null, Aggregation.RunningTotal,
						propertiesOf(decimalArray("10.314496644"), decimalArray("9450"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("13410", "10.1", "30.3") },
								new BigDecimal[][] { decimalArray("9360", "1000", "10360") })));
	}

	@Test
	public void find_runningTotal_nodes_absoluteDates_sortNodeSource() {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		ObjectDatumStreamMetadata meta_1 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		ObjectDatumStreamMetadata meta_2 = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 2L, "b");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));
		final Period freq = Period.ofMonths(3);
		insertDatum(Aggregation.Month, meta_1.getStreamId(), start, freq, 4);

		// give stream 2 double the data
		insertDatum(Aggregation.Month, meta_2.getStreamId(), start.minusYears(1), freq, 4);
		insertDatum(Aggregation.Month, meta_2.getStreamId(), start.plusMonths(1), freq, 4);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setAggregation(Aggregation.RunningTotal);
		filter.setSorts(sorts("node", "source"));
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(start.plusYears(2).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(2L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(2));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = StreamSupport.stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(2));

		List<ObjectDatumStreamMetadata> metas = asList(meta_1, meta_2);
		for ( int i = 0; i < 2; i++ ) {
			AggregateDatum d = datumList.get(i);
			assertThat("Stream ID " + i, d.getStreamId(), equalTo(metas.get(i).getStreamId()));
			assertThat("Agg type " + i, d.getAggregation(), equalTo(Aggregation.RunningTotal));
			assertThat("Running total timestamp is 'now'",
					d.getTimestamp().truncatedTo(ChronoUnit.MINUTES),
					equalTo(Instant.now().truncatedTo(ChronoUnit.MINUTES)));
			assertThat("Agg total", d.getProperties().getAccumulating(),
					arrayContaining(i == 0 ? decimalArray("10") : decimalArray("20")));
			assertThat("Instantaneous stats", d.getStatistics().getInstantaneous()[0], arrayContaining(
					i == 0 ? decimalArray("24", "0", "100") : decimalArray("48", "0", "100")));
			assertThat("Accumulating stats", d.getStatistics().getAccumulating()[0], arrayContaining(
					i == 0 ? decimalArray("40", "0", "13") : decimalArray("80", "0", "13")));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void find_1min() {
		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setAggregation(Aggregation.Minute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(Instant.now().truncatedTo(ChronoUnit.HOURS));
		filter.setEndDate(filter.getStartDate().plusSeconds(3600));
		execute(filter);
	}

	@Test
	public void find_5min_5minData_nodeSource_absoluteDates_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// populate 12 5 minute, 10 Wh segments, for a total of 110 Wh in 55 minutes
		List<Datum> datums = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 12; i++ ) {
			DatumEntity d = new DatumEntity(meta.getStreamId(), start.plusMinutes(i * 5).toInstant(),
					Instant.now(), DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal(i) },
							new BigDecimal[] { new BigDecimal(i * 5) }, null, null));
			datums.add(d);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.FiveMinute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(12L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(12));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(12));
		for ( int i = 0; i < datumList.size(); i++ ) {
			AggregateDatumEntity expected = new AggregateDatumEntity(meta.getStreamId(),
					start.plusMinutes(i * 5).toInstant(), Aggregation.FiveMinute,
					propertiesOf(decimalArray(valueOf(i)), decimalArray(i == 11 ? "0" : "5"), null,
							null),
					statisticsOf(new BigDecimal[][] { decimalArray("1", valueOf(i), valueOf(i)) },
							null));
			assertAggregateDatum("Result " + i, datumList.get(i), expected);
		}
	}

	@Test
	public void find_5min_10minData_nodeSource_absoluteDates_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// populate 7 10 minute, 10 Wh segments, for a total of 120 Wh in 60 minutes
		List<Datum> datums = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 7; i++ ) {
			DatumEntity d = new DatumEntity(meta.getStreamId(), start.plusMinutes(i * 10).toInstant(),
					Instant.now(), DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal(i) },
							new BigDecimal[] { new BigDecimal(i * 10) }, null, null));
			datums.add(d);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.FiveMinute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(6));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(6));
		for ( int i = 0; i < datumList.size(); i++ ) {
			AggregateDatumEntity expected = new AggregateDatumEntity(meta.getStreamId(),
					start.plusMinutes(i * 10).toInstant(), Aggregation.FiveMinute,
					propertiesOf(decimalArray(valueOf(i)), decimalArray("10"), null, null), statisticsOf(
							new BigDecimal[][] { decimalArray("1", valueOf(i), valueOf(i)) }, null));
			assertAggregateDatum("5min result per 10min data " + i, datumList.get(i), expected);
		}
	}

	@Test
	public void find_10min_1minData_nodeSource_absoluteDates_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// populate 61 1-minute, 2 Wh segments, for a total of 120 Wh in 60 minutes
		List<Datum> datums = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 61; i++ ) {
			DatumEntity d = new DatumEntity(meta.getStreamId(), start.plusMinutes(i).toInstant(),
					Instant.now(), DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal(i) },
							new BigDecimal[] { new BigDecimal(i * 2) }, null, null));
			datums.add(d);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.TenMinute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(6L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(6));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(6));
		for ( int i = 0; i < datumList.size(); i++ ) {
			AggregateDatumEntity expected = new AggregateDatumEntity(meta.getStreamId(),
					start.plusMinutes(i * 10).toInstant(), Aggregation.TenMinute,
					propertiesOf(decimalArray(valueOf(4.5 + i * 10)), decimalArray("20"), null, null),
					statisticsOf(new BigDecimal[][] {
							decimalArray("10", valueOf(i * 10), valueOf(i * 10 + 9)) }, null));
			DatumTestUtils.assertAggregateDatum("Result " + i, datumList.get(i), expected);
		}
	}

	@Test
	public void find_5min_paginated_withTotalResultCount() {
		// GIVEN
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// populate 61 1-minute, 2 Wh segments, for a total of 120 Wh in 60 minutes
		List<Datum> datums = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 61; i++ ) {
			DatumEntity d = new DatumEntity(meta.getStreamId(), start.plusMinutes(i).toInstant(),
					Instant.now(), DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal(i) },
							new BigDecimal[] { new BigDecimal(i * 2) }, null, null));
			datums.add(d);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.FiveMinute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		filter.setMax(5);
		filter.setWithoutTotalResultsCount(false);

		for ( int i = 0; i < 3; i++ ) {
			filter.setOffset(i * 5);
			ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

			assertThat("Results returned " + i, results, notNullValue());
			assertThat("Result total count " + i, results.getTotalResults(), equalTo(12L));
			assertThat("Returned count " + i, results.getReturnedResultCount(), equalTo(i == 2 ? 2 : 5));
			assertThat("Starting offset " + i, results.getStartingOffset(), equalTo(i * 5));

			List<AggregateDatum> datumList = stream(results.spliterator(), false)
					.map(AggregateDatum.class::cast).collect(toList());
			for ( int j = 0; j < datumList.size(); j++ ) {
				assertThat(String.format("Page %d result %d timestamp", i, j),
						datumList.get(j).getTimestamp(),
						equalTo(start.plusMinutes((i * 5 * 5) + (j * 5)).toInstant()));
			}
		}
	}

	@Test
	public void find_15min_1minData_nodeSource_absoluteDates_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// populate 61 1-minute, 2 Wh segments, for a total of 120 Wh in 60 minutes
		List<Datum> datums = new ArrayList<>();
		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 61; i++ ) {
			DatumEntity d = new DatumEntity(meta.getStreamId(), start.plusMinutes(i).toInstant(),
					Instant.now(), DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal(i) },
							new BigDecimal[] { new BigDecimal(i * 2) }, null, null));
			datums.add(d);
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(meta.getObjectId());
		filter.setAggregation(Aggregation.FifteenMinute);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(start.toInstant());
		filter.setEndDate(start.plusHours(1).toInstant());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Returned count", results.getReturnedResultCount(), equalTo(4));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));
		for ( int i = 0; i < datumList.size(); i++ ) {
			AggregateDatumEntity expected = new AggregateDatumEntity(meta.getStreamId(),
					start.plusMinutes(i * 15).toInstant(), Aggregation.TenMinute,
					propertiesOf(decimalArray(valueOf(7 + i * 15)), decimalArray("30"), null, null),
					statisticsOf(new BigDecimal[][] {
							decimalArray("15", valueOf(i * 15), valueOf(i * 15 + 14)) }, null));
			DatumTestUtils.assertAggregateDatum("Result " + i, datumList.get(i), expected);
		}
	}

}
