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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.SimpleSortDescriptor;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class implementation of
 * aggregate queries.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_Aggregate extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a", "UTC");
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, nodeId, sourceId, new String[] { "x", "y" }, new String[] { "w" }, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	@Test
	public void find_hour_streamId_orderDefault() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
	public void find_hour_streamId_orderTimeDesc() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
		assertThat("Total result count", results.getTotalResults(), equalTo(8L));
		assertThat("Offset for last page", results.getStartingOffset(), equalTo(8));
		assertThat("Results count for last page", results.getReturnedResultCount(), equalTo(0));
	}

	@Test
	public void find_hour_streamId_mostRecent() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
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
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
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
		DatumStreamFilterResults results = dao.findFiltered(filter);

		// THEN
		DatumStreamMetadata resultMeta = results.metadataForStream(streamId);
		assertThat("Metadata is for node", resultMeta, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Node ID", ((NodeDatumStreamMetadata) resultMeta).getNodeId(),
				equalTo(meta.getNodeId()));
		assertThat("Result for most recent returned", results.getTotalResults(), equalTo(1L));

		Instant ts = start.plusMonths(6).toInstant();
		Datum d = results.iterator().next();
		assertThat("Aggregate datum returned", d, instanceOf(AggregateDatum.class));
		assertThat("Stream ID", d.getStreamId(), equalTo(streamId));
		assertThat("Timestamp", d.getTimestamp(), equalTo(ts));
	}

}
