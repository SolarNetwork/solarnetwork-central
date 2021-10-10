/* ==================================================================
 * JdbcDatumEntityDao_VirtualStreamTests.java - 10/12/2020 1:41:07 pm
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.virtualStreamId;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.CombiningType;
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
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link CombiningType} style aggregate queries in
 * {@link JdbcDatumEntityDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_VirtualStreamTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private ObjectDatumStreamFilterResults<Datum, DatumPK> execute(DatumCriteria filter) {
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);
		log.debug("Filter results:\n{}",
				stream(results.spliterator(), false).map(Object::toString).collect(joining("\n")));
		return results;
	}

	@Test
	public void find_virtual_15min_sum_combineNode() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 13; i++ ) {
			Instant ts = start.plusMinutes(i * 5).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			datums.add(new DatumEntity(meta_1.getStreamId(), ts, now, props));
			datums.add(new DatumEntity(meta_2.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(1).toInstant());
		criteria.setAggregation(Aggregation.FifteenMinute);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(4));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "a")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusMinutes(i * 15).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("a"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf((i * 3 + 1) * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray("30")));
		}
	}

	@Test
	public void find_virtual_15min_sum_combineSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 13; i++ ) {
			Instant ts = start.plusMinutes(i * 5).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			datums.add(new DatumEntity(meta_1.getStreamId(), ts, now, props));
			datums.add(new DatumEntity(meta_2.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(1).toInstant());
		criteria.setAggregation(Aggregation.FifteenMinute);
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(4));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(1L, "V")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusMinutes(i * 15).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(1L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf((i * 3 + 1) * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray("30")));
		}
	}

	@Test
	public void find_virtual_15min_sum_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		List<Datum> datums = new ArrayList<>();
		final Instant now = Instant.now();
		for ( int i = 0; i < 13; i++ ) {
			Instant ts = start.plusMinutes(i * 5).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			datums.add(new DatumEntity(meta_1.getStreamId(), ts, now, props));
			datums.add(new DatumEntity(meta_2.getStreamId(), ts, now, props));
		}
		DatumDbUtils.insertDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(1).toInstant());
		criteria.setAggregation(Aggregation.FifteenMinute);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(4L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(4));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(4));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusMinutes(i * 15).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf((i * 3 + 1) * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray("30")));
		}
	}

	@Test
	public void find_virtual_hour_sum_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusHours(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Hour, props, stats));
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Hour, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusHours(5).toInstant());
		criteria.setAggregation(Aggregation.Hour);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusHours(i).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf(i * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 5 * 2))));
			assertThat("Virtual reading Wh from combined streams",
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf(33 * 2), null, null)));
		}
	}

	@Test
	public void find_virtual_day_sum_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusDays(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Day, props, stats));
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Day, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(5).toInstant());
		criteria.setAggregation(Aggregation.Day);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusDays(i).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf(i * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 5 * 2))));
			assertThat("Virtual reading Wh from combined streams",
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf(33 * 2), null, null)));
		}
	}

	@Test
	public void find_virtual_day_avg_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusDays(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Day, props, stats));

			props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 10) },
					new BigDecimal[] { new BigDecimal(i * 50) }, null, null);
			stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 100)) },
					new BigDecimal[][] {
							decimalArray("330", valueOf(330 * i), valueOf(330 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Day, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(5).toInstant());
		criteria.setAggregation(Aggregation.Day);
		criteria.setCombiningType(CombiningType.Average);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time " + i, d.getTimestamp(),
					equalTo(start.plusDays(i).toInstant()));
			assertThat("Result node ID is virutal " + i, meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal " + i, meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams " + i,
					d.getProperties().getInstantaneous()[0].setScale(1),
					equalTo(new BigDecimal((i + i * 10) / 2.0).setScale(1)));
			assertThat("Virtual Wh from combined streams " + i,
					d.getProperties().getAccumulating()[0].setScale(1),
					equalTo(new BigDecimal((i * 5 + i * 50) / 2.0).setScale(1)));
			assertThat("Virtual reading Wh from combined streams " + i,
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf("181.5"), null, null)));
		}
	}

	@Test
	public void find_virtual_day_sub_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusDays(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Day, props, stats));

			props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 10) },
					new BigDecimal[] { new BigDecimal(i * 50) }, null, null);
			stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 100)) },
					new BigDecimal[][] {
							decimalArray("330", valueOf(330 * i), valueOf(330 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Day, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(5).toInstant());
		criteria.setAggregation(Aggregation.Day);
		criteria.setCombiningType(CombiningType.Difference);
		criteria.setObjectIdMaps(new String[] { "10:1,2" }); // note order
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time " + i, d.getTimestamp(),
					equalTo(start.plusDays(i).toInstant()));
			assertThat("Result node ID is virutal " + i, meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal " + i, meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams " + i, d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf(i - i * 10))));
			assertThat("Virtual Wh from combined streams " + i, d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 5 - i * 50))));
			assertThat("Virtual reading Wh from combined streams " + i,
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf("-297"), null, null)));
		}
	}

	@Test
	public void find_virtual_day_sub_combineNodeSource_reversed() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusDays(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Day, props, stats));

			props = propertiesOf(new BigDecimal[] { new BigDecimal(i * 10) },
					new BigDecimal[] { new BigDecimal(i * 50) }, null, null);
			stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 100)) },
					new BigDecimal[][] {
							decimalArray("330", valueOf(330 * i), valueOf(330 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Day, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusDays(5).toInstant());
		criteria.setAggregation(Aggregation.Day);
		criteria.setCombiningType(CombiningType.Difference);
		criteria.setObjectIdMaps(new String[] { "10:2,1" }); // note order
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time " + i, d.getTimestamp(),
					equalTo(start.plusDays(i).toInstant()));
			assertThat("Result node ID is virutal " + i, meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal " + i, meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams " + i, d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf((i * 10 - i)))));
			assertThat("Virtual Wh from combined streams " + i, d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 50 - i * 5))));
			assertThat("Virtual reading Wh from combined streams " + i,
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf("297"), null, null)));
		}
	}

	@Test
	public void find_virtual_month_sum_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 5; i++ ) {
			Instant ts = start.plusMonths(i).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Month, props, stats));
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Month, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusMonths(5).toInstant());
		criteria.setAggregation(Aggregation.Month);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID", d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time", d.getTimestamp(),
					equalTo(start.plusMonths(i).toInstant()));
			assertThat("Result node ID is virutal", meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal", meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams", d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf(i * 2))));
			assertThat("Virtual Wh from combined streams", d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 5 * 2))));
			assertThat("Virtual reading Wh from combined streams",
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf(33 * 2), null, null)));
		}
	}

	@Test
	public void find_virtual_year_sum_combineNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "w" }, new String[] { "wh" }, null, null);
		ObjectDatumStreamMetadata meta_2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "w" }, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta_1, meta_2));

		final ZonedDateTime start = ZonedDateTime.of(2014, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// insert 2months over 5 years
		List<AggregateDatum> datums = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			Instant ts = start.plusMonths(i * 6).toInstant();
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i) },
					new BigDecimal[] { new BigDecimal(i * 5) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { decimalArray("6", valueOf(i - 10), valueOf(i + 10)) },
					new BigDecimal[][] { decimalArray("33", valueOf(33 * i), valueOf(33 * (i + 1))) });
			datums.add(
					new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Month, props, stats));
			datums.add(
					new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Month, props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(new Long[] { 1L, 2L });
		criteria.setSourceIds(new String[] { "a", "b" });
		criteria.setStartDate(start.toInstant());
		criteria.setEndDate(start.plusYears(5).toInstant());
		criteria.setAggregation(Aggregation.Year);
		criteria.setObjectIdMaps(new String[] { "10:1,2" });
		criteria.setSourceIdMaps(new String[] { "V:a,b" });

		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(criteria);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(5L));
		assertThat("Starting offset", results.getStartingOffset(), equalTo(0));
		assertThat("Returned 1 virtual node x 1 virtual source x 4 time buckets",
				results.getReturnedResultCount(), equalTo(5));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		assertThat("Result list size matches", datumList, hasSize(5));
		for ( int i = 0; i < 4; i++ ) {
			AggregateDatum d = datumList.get(i);
			ObjectDatumStreamMetadata meta = results.metadataForStreamId(d.getStreamId());
			assertThat("Virtual stream ID " + i, d.getStreamId(), equalTo(virtualStreamId(10L, "V")));
			assertThat("Result date is grouped by time " + i, d.getTimestamp(),
					equalTo(start.plusYears(i).toInstant()));
			assertThat("Result node ID is virutal " + i, meta.getObjectId(), equalTo(10L));
			assertThat("Result source ID is virutal " + i, meta.getSourceId(), equalTo("V"));
			assertThat("Virtual W from combined streams " + i, d.getProperties().getInstantaneous(),
					arrayContaining(decimalArray(valueOf(1 + i * 4))));
			assertThat("Virtual Wh from combined streams " + i, d.getProperties().getAccumulating(),
					arrayContaining(decimalArray(valueOf(i * 40 + 10))));
			assertThat("Virtual reading Wh from combined streams " + i,
					d.getStatistics().getAccumulating()[0],
					arrayContaining(decimalArray(valueOf(33 * 2 * 2), null, null)));
		}
	}

}
