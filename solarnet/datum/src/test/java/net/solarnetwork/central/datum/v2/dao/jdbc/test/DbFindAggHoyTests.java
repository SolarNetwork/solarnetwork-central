/* ==================================================================
 * DbFindAggDoyTests.java - 5/08/2024 6:19:37 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@literal solardatm.find_agg_hoy} database stored
 * procedure.
 *
 * @author matt
 * @version 1.0
 */
public class DbFindAggHoyTests extends BaseDatumJdbcTestSupport {

	private static final int HOURS_PER_NON_LEAP_YEAR = 8760;
	private static final ZonedDateTime FEB_29 = ZonedDateTime.of(1996, 2, 29, 0, 0, 0, 0,
			ZoneOffset.UTC);

	private List<AggregateDatum> findAggHoy(UUID streamId, Instant start, Instant end) {
		List<Map<String, Object>> meta = jdbcTemplate.queryForList(
				"SELECT * FROM solardatm.find_metadata_for_stream(?::uuid)", streamId.toString());
		log.debug("Metadata for stream:\n{}",
				meta.stream().map(Object::toString).collect(joining("\n")));

		return jdbcTemplate.execute(new ConnectionCallback<List<AggregateDatum>>() {

			@Override
			public List<AggregateDatum> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding agg DOY for stream {} from {} - {}", streamId, start, end);
				List<AggregateDatum> result = new ArrayList<>();
				RowMapper<AggregateDatum> mapper = new AggregateDatumEntityRowMapper(
						Aggregation.HourOfYear);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_agg_datm_hoy(?,?,?)}")) {
					stmt.setObject(1, streamId, Types.OTHER);
					stmt.setTimestamp(2, Timestamp.from(start));
					stmt.setTimestamp(3, Timestamp.from(end));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								AggregateDatum d = mapper.mapRow(rs, ++i);
								result.add(d);
							}
						}
					}
				}
				log.debug("Found agg HOY for stream:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a", "UTC");
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId,
			String timeZoneId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), timeZoneId, ObjectDatumKind.Node,
				nodeId, sourceId, new String[] { "x", "y" }, new String[] { "w" }, null);
	}

	@Test
	public void find_hoy_utc() {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();

		final ZonedDateTime start = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final ZonedDateTime end = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		final List<AggregateDatum> datums = new ArrayList<>(730);

		ZonedDateTime ts = start;
		for ( int hour = 0; ts.isBefore(end); hour++, ts = ts.plusHours(1) ) {
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(hour) },
					new BigDecimal[] { new BigDecimal((hour + 1)) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(6), new BigDecimal(hour - 1),
							new BigDecimal(hour + 1) } },
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(0), new BigDecimal(0),
							new BigDecimal(0) } });

			datums.add(new AggregateDatumEntity(meta.getStreamId(), ts.toInstant(), Aggregation.Hour,
					props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		List<AggregateDatum> results = findAggHoy(meta.getStreamId(), Instant.EPOCH, Instant.now());

		// THEN
		assertThat("365 * 24 hour results for each hour of year", results,
				hasSize(HOURS_PER_NON_LEAP_YEAR));
		ZonedDateTime date = ZonedDateTime.of(1996, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < HOURS_PER_NON_LEAP_YEAR; i++ ) {
			AggregateDatum d = results.get(i);
			double pi = i + HOURS_PER_NON_LEAP_YEAR / 2.0;
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(pi) },
					new BigDecimal[] { new BigDecimal(pi + 1) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(12), new BigDecimal(i - 1),
							new BigDecimal(i + HOURS_PER_NON_LEAP_YEAR + 1) } },
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(pi + 1),
							new BigDecimal(i + 1), new BigDecimal(i + HOURS_PER_NON_LEAP_YEAR + 1) } });

			// skip 29 Feb as data not over leap year
			ZonedDateTime expectedDate = date.plusHours(i);
			if ( expectedDate.compareTo(FEB_29) >= 0 ) {
				expectedDate = expectedDate.plusDays(1);
			}

			assertAggregateDatum("Hour " + i, d, new AggregateDatumEntity(meta.getStreamId(),
					expectedDate.toInstant(), Aggregation.HourOfYear, props, stats));
		}
	}

	@Test
	public void find_hoy_tz() {
		// GIVEN
		// a node in Pacific/Honolulu time zone
		final ZoneId zone = ZoneId.of("Pacific/Honolulu");
		setupTestLocation(1L, zone.getId());
		setupTestNode(1L, 1L);

		// datum stream metadata for node
		final ObjectDatumStreamMetadata meta = testStreamMetadata(1L, "a", zone.getId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		final ZonedDateTime start = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, zone);
		final ZonedDateTime end = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, zone);
		final List<AggregateDatum> datums = new ArrayList<>(730);

		ZonedDateTime ts = start;
		for ( int hour = 0; ts.isBefore(end); hour++, ts = ts.plusHours(1) ) {
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(hour) },
					new BigDecimal[] { new BigDecimal((hour + 1)) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(6), new BigDecimal(hour - 1),
							new BigDecimal(hour + 1) } },
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(0), new BigDecimal(0),
							new BigDecimal(0) } });

			datums.add(new AggregateDatumEntity(meta.getStreamId(), ts.toInstant(), Aggregation.Hour,
					props, stats));
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		List<AggregateDatum> results = findAggHoy(meta.getStreamId(), Instant.EPOCH, Instant.now());

		// THEN
		assertThat("365 * 24 hour results for each hour of year", results,
				hasSize(HOURS_PER_NON_LEAP_YEAR));
		ZonedDateTime date = ZonedDateTime.of(1996, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < HOURS_PER_NON_LEAP_YEAR; i++ ) {
			AggregateDatum d = results.get(i);
			double pi = i + HOURS_PER_NON_LEAP_YEAR / 2.0;
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(pi) },
					new BigDecimal[] { new BigDecimal(pi + 1) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(12), new BigDecimal(i - 1),
							new BigDecimal(i + HOURS_PER_NON_LEAP_YEAR + 1) } },
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(pi + 1),
							new BigDecimal(i + 1), new BigDecimal(i + HOURS_PER_NON_LEAP_YEAR + 1) } });

			// skip 29 Feb as data not over leap year
			ZonedDateTime expectedDate = date.plusHours(i);
			if ( expectedDate.compareTo(FEB_29) >= 0 ) {
				expectedDate = expectedDate.plusDays(1);
			}

			assertAggregateDatum("Hour " + i, d, new AggregateDatumEntity(meta.getStreamId(),
					expectedDate.toInstant(), Aggregation.HourOfYear, props, stats));
		}
	}

}
