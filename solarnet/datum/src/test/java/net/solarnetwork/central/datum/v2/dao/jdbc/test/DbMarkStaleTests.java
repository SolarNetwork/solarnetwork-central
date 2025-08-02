/* ==================================================================
 * DbMarkStaleTests.java - 12/11/2020 3:39:54 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static net.solarnetwork.central.test.CommonDbTestUtils.allTableData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the "mark stale" stored procedures.
 *
 * @author matt
 * @version 1.1
 */
public class DbMarkStaleTests extends BaseDatumJdbcTestSupport {

	public void callMarkStaleDateRange(UUID streamId, Instant from, Instant to) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.mark_stale_datm_hours(?,?,?)}")) {
					log.debug("Marking stream {} stale from {} to {}", streamId, from, to);
					stmt.setObject(1, streamId, Types.OTHER);
					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					stmt.execute();
					return null;
				}
			}
		});
	}

	@Test
	public void markStale_dateRange_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		callMarkStaleDateRange(UUID.randomUUID(), start.toInstant(), Instant.now());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange_noMatchingStream() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		insertDatumStream(log, jdbcTemplate, datums, "UTC");

		// WHEN
		callMarkStaleDateRange(UUID.randomUUID(), start.toInstant(), Instant.now());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange_noMatchingDates() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = meta.values().iterator().next().getStreamId();

		// WHEN
		callMarkStaleDateRange(streamId, start.minusYears(1).toInstant(), start.toInstant());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = meta.values().iterator().next().getStreamId();

		// WHEN
		callMarkStaleDateRange(streamId, start.toInstant(), start.plusYears(1).toInstant());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("All matching hours marked stale, plus previous hour", stales, hasSize(3));
		Set<Instant> staleDates = stales.stream().map(StaleAggregateDatum::getTimestamp)
				.collect(Collectors.toSet());
		assertThat("Stale hours from data", staleDates, containsInAnyOrder(
				start.minusHours(1).toInstant(), start.toInstant(), start.plusHours(1).toInstant()));
	}

	@Test
	public void markStale_hourRemoved() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-41.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID streamId = meta.values().iterator().next().getStreamId();

		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");

		// generate hourly aggs
		callMarkStaleDateRange(streamId, start.toInstant(), start.plusYears(1).toInstant());
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");

		allTableData(log, jdbcTemplate, "solardatm.agg_datm_hourly", "stream_id,ts_start");

		int deletedRows = jdbcTemplate.update(
				"DELETE FROM solardatm.da_datm WHERE stream_id = ?::uuid AND ts >= ? AND ts < ?",
				streamId.toString(), Timestamp.from(start.plusHours(1).toInstant()),
				Timestamp.from(start.plusHours(2).toInstant()));
		log.info("Deleted {} rows", deletedRows);
		allTableData(log, jdbcTemplate, "solardatm.da_datm", "stream_id,ts");
		allTableData(log, jdbcTemplate, "solardatm.agg_stale_datm", "stream_id,ts_start");

		// WHEN
		callMarkStaleDateRange(streamId, start.toInstant(), start.plusYears(1).toInstant());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		Set<Instant> staleDates = stales.stream().map(StaleAggregateDatum::getTimestamp)
				.collect(Collectors.toSet());
		assertThat("All matching hours marked stale (including removed hour and previous hour)",
				staleDates, containsInAnyOrder(start.minusHours(1).toInstant(), start.toInstant(),
						start.plusHours(1).toInstant(), start.plusHours(2).toInstant()));
	}
}
