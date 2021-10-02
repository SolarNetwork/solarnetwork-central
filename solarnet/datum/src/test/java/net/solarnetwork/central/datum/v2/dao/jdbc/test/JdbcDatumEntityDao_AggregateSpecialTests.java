/* ==================================================================
 * JdbcDatumEntityDao_AggregateSpecialTests.java - 10/12/2020 8:37:57 pm
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
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.equalTo;
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
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class implementation of
 * "special" aggregate queries like day-of-week and hour-of-day.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_AggregateSpecialTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	protected DatumEntity lastDatum;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				timeZoneId, ObjectDatumKind.Node, nodeId, sourceId, new String[] { "w" },
				new String[] { "wh" }, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	private ObjectDatumStreamFilterResults<Datum, DatumPK> execute(DatumCriteria filter) {
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = dao.findFiltered(filter);
		log.debug("Filter results:\n{}",
				stream(results.spliterator(), false).map(Object::toString).collect(joining("\n")));
		return results;
	}

	@Test
	public void find_dow_nodesAndSources_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(1L, "a", "UTC");
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(2L, "b", "UTC");

		final ZonedDateTime startDate = ZonedDateTime.of(2019, 1, 7, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		long wh = 0;
		for ( int i = 0; i < 14; i++ ) {
			Instant ts = startDate.plusDays(i).toInstant();
			datums.add(new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Day,
					propertiesOf(decimalArray(valueOf((i + 1) * 100)), decimalArray(valueOf(wh)), null,
							null),
					statisticsOf(
							new BigDecimal[][] { decimalArray("600", valueOf((i + i) * 100 - 100),
									valueOf((i + 1) * 100 + 100)) },
							new BigDecimal[][] { decimalArray(valueOf(wh)) })));

			datums.add(new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Day,
					propertiesOf(decimalArray(valueOf((i + 1) * 1000)), decimalArray(valueOf(wh * 10)),
							null, null),
					statisticsOf(
							new BigDecimal[][] { decimalArray("600", valueOf((i + i) * 1000 - 1000),
									valueOf((i + 1) * 1000 + 1000)) },
							new BigDecimal[][] { decimalArray(valueOf(wh * 10)) })));

			wh += 1000;
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setAggregation(Aggregation.DayOfWeek);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(Instant.now());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(14L));
		assertThat("7 day results x2 streams", results.getReturnedResultCount(), equalTo(14));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		final java.time.LocalDate startLocalDate = java.time.LocalDate.of(2001, 1, 1);
		for ( int i = 0; i < 7; i++ ) {
			java.time.LocalDate currLocalDate = startLocalDate.plusDays(i);

			AggregateDatum d = datumList.get(i);
			assertThat("Stream 1 agg", d.getAggregation(), equalTo(Aggregation.DayOfWeek));
			assertThat("Stream 1 DOW " + currLocalDate, d.getId(),
					equalTo(new DatumPK(meta_1.getStreamId(),
							currLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant())));

			// 2nd stream
			d = datumList.get(7 + i);
			assertThat("Stream 2 agg", d.getAggregation(), equalTo(Aggregation.DayOfWeek));
			assertThat("Stream 3 DOW " + currLocalDate, d.getId(),
					equalTo(new DatumPK(meta_2.getStreamId(),
							currLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant())));
		}
	}

	@Test
	public void find_hod_nodesAndSources_sortNodeSourceTime() {
		// GIVEN
		ObjectDatumStreamMetadata meta_1 = testStreamMetadata(1L, "a", "UTC");
		ObjectDatumStreamMetadata meta_2 = testStreamMetadata(2L, "b", "UTC");

		final ZonedDateTime startDate = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AggregateDatum> datums = new ArrayList<>();
		long wh = 0;
		for ( int i = 0; i < 72; i++ ) {
			Instant ts = startDate.plusHours(i).toInstant();
			datums.add(new AggregateDatumEntity(meta_1.getStreamId(), ts, Aggregation.Hour,
					propertiesOf(decimalArray(valueOf((i + 1) * 100)), decimalArray(valueOf(wh)), null,
							null),
					statisticsOf(
							new BigDecimal[][] { decimalArray("60", valueOf((i + i) * 100 - 100),
									valueOf((i + 1) * 100 + 100)) },
							new BigDecimal[][] { decimalArray(valueOf(wh)) })));

			datums.add(new AggregateDatumEntity(meta_2.getStreamId(), ts, Aggregation.Hour,
					propertiesOf(decimalArray(valueOf((i + 1) * 1000)), decimalArray(valueOf(wh * 10)),
							null, null),
					statisticsOf(
							new BigDecimal[][] { decimalArray("60", valueOf((i + i) * 1000 - 1000),
									valueOf((i + 1) * 1000 + 1000)) },
							new BigDecimal[][] { decimalArray(valueOf(wh * 10)) })));

			wh += 100;
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setAggregation(Aggregation.HourOfDay);
		filter.setSorts(sorts("node", "source", "time"));
		filter.setStartDate(Instant.EPOCH);
		filter.setEndDate(Instant.now());
		ObjectDatumStreamFilterResults<Datum, DatumPK> results = execute(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		assertThat("Result total count", results.getTotalResults(), equalTo(48L));
		assertThat("24 hour results x2 streams", results.getReturnedResultCount(), equalTo(48));

		List<AggregateDatum> datumList = stream(results.spliterator(), false)
				.map(AggregateDatum.class::cast).collect(toList());
		final java.time.LocalDateTime startLocalDate = java.time.LocalDate.of(2001, 1, 1).atStartOfDay();
		for ( int i = 0; i < 24; i++ ) {
			java.time.LocalDateTime currLocalDate = startLocalDate.plusHours(i);

			AggregateDatum d = datumList.get(i);
			assertThat("Stream 1 agg " + currLocalDate.toLocalTime(), d.getAggregation(),
					equalTo(Aggregation.HourOfDay));
			assertThat("Stream 1 HOD " + currLocalDate.toLocalTime(), d.getId(),
					equalTo(new DatumPK(meta_1.getStreamId(),
							currLocalDate.atZone(ZoneOffset.UTC).toInstant())));

			// 2nd stream
			d = datumList.get(24 + i);
			assertThat("Stream 2 agg " + currLocalDate.toLocalTime(), d.getAggregation(),
					equalTo(Aggregation.HourOfDay));
			assertThat("Stream 3 DOW " + currLocalDate.toLocalTime(), d.getId(),
					equalTo(new DatumPK(meta_2.getStreamId(),
							currLocalDate.atZone(ZoneOffset.UTC).toInstant())));
		}
	}

}
