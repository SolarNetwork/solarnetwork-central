/* ==================================================================
 * DbDeleteDatum15mTests.java - 20/11/2026 10:00:00 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the 15 minute rows deleted by the
 * {@code solardatm.delete_datm(UUID, TIMESTAMP, TIMESTAMP, TEXT)} database
 * function.
 *
 * <p>
 * The test stream has a datum every 5 minutes for 5 hours from 09:00 local
 * time, and every hour of it is processed, so each hour has its slot rows
 * before the delete, which removes the datum from 10:30 to 12:30 local time.
 * Hourly rows start on the hours of the session time zone, as
 * {@code solardatm.calc_stale_datm()} gives them. In the first test that is the
 * stream's time zone; in the second it is UTC, whose hours are not the
 * stream's.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbDeleteDatum15mTests extends BaseDatumJdbcTestSupport {

	private static final Duration STEP = Duration.ofMinutes(5);

	private UUID setupProcessedStream(String sessionZone, String streamZone, ZonedDateTime start,
			int hours) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, sessionZone);
		setupTestLocation(TEST_LOC_ID, streamZone);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		final UUID streamId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm_meta (stream_id, node_id, source_id, names_i, names_a)
				VALUES (?::uuid, ?, ?, ARRAY['w'], ARRAY['wh'])
				""", streamId.toString(), TEST_NODE_ID, TEST_SOURCE_ID);
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a)
				SELECT ?::uuid, ?::timestamptz + n * ?::interval, CURRENT_TIMESTAMP
					, ARRAY[100 + n]::numeric[], ARRAY[10 * n]::numeric[]
				FROM generate_series(0, ? - 1) n
				""", streamId.toString(), Timestamp.from(start.toInstant()), STEP.toString(),
				hours * 12);
		// every hour holding a datum, on the hours of the session time zone
		jdbcTemplate.update("""
				INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)
				SELECT DISTINCT d.stream_id, date_trunc('hour', d.ts), 'h'
				FROM solardatm.da_datm d
				WHERE d.stream_id = ?::uuid
				""", streamId.toString());
		processStaleHours();
		return streamId;
	}

	private void processStaleHours() {
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));
	}

	private long deleteDatum(UUID streamId, ZonedDateTime from, ZonedDateTime to, String zone) {
		return jdbcTemplate.execute((Connection con) -> {
			try (CallableStatement cs = con
					.prepareCall("{? = call solardatm.delete_datm(?, ?, ?, ?)}")) {
				cs.registerOutParameter(1, Types.BIGINT);
				cs.setObject(2, streamId, Types.OTHER);
				cs.setObject(3, from.toLocalDateTime(), Types.TIMESTAMP);
				cs.setObject(4, to.toLocalDateTime(), Types.TIMESTAMP);
				cs.setString(5, zone);
				cs.execute();
				return cs.getLong(1);
			}
		});
	}

	private List<Instant> hourlyRowHours() {
		return listAggregateDatum(jdbcTemplate, Aggregation.Hour).stream()
				.map(AggregateDatum::getTimestamp).toList();
	}

	private Map<Instant, String> slotRowLocations(UUID streamId) {
		final Map<Instant, String> result = new LinkedHashMap<>(32);
		jdbcTemplate.query("""
				SELECT ts_start, ctid::text FROM solardatm.agg_datm_15m
				WHERE stream_id = ?::uuid
				ORDER BY ts_start
				""", rs -> {
			result.put(rs.getTimestamp(1).toInstant(), rs.getString(2));
		}, streamId.toString());
		return result;
	}

	private static List<Instant> slots(ZonedDateTime hour, int... indexes) {
		List<Instant> result = new ArrayList<>(indexes.length);
		for ( int i : indexes ) {
			result.add(hour.plusMinutes(15L * i).toInstant());
		}
		return result;
	}

	private static List<Instant> concat(List<Instant> a, List<Instant> b) {
		List<Instant> result = new ArrayList<>(a);
		result.addAll(b);
		return result;
	}

	@ParameterizedTest
	@ValueSource(strings = { TEST_TZ, "Asia/Kolkata", "Asia/Kathmandu" })
	public void wholeHourSlotsDeletedPartialHoursReprocessed(String zone) {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 9, 0, 0, 0, ZoneId.of(zone));
		final UUID streamId = setupProcessedStream(zone, zone, start, 5);
		final Map<Instant, String> slotLocations = slotRowLocations(streamId);

		final ZonedDateTime h09 = start;
		final ZonedDateTime h10 = start.plusHours(1);
		final ZonedDateTime h11 = start.plusHours(2);
		final ZonedDateTime h12 = start.plusHours(3);
		final ZonedDateTime h13 = start.plusHours(4);

		// WHEN
		final long deleted = deleteDatum(streamId, h10.plusMinutes(30), h12.plusMinutes(30), zone);

		// THEN
		final Map<Instant, String> afterDeleteLocations = slotRowLocations(streamId);
		final Map<Instant, String> partialAndOtherLocations = new LinkedHashMap<>(slotLocations);
		partialAndOtherLocations.keySet().removeAll(slots(h11, 0, 1, 2, 3));

		// @formatter:off
		then(slotLocations)
			.as("Every slot of every hour stored before the delete")
			.hasSize(20)
			;
		then(deleted)
			.as("Datum from 10:30 to 12:30 deleted")
			.isEqualTo(24L)
			;
		then(hourlyRowHours())
			.as("The whole hour deleted, partial hours left for stale processing")
			.containsExactly(h09.toInstant(), h10.toInstant(), h12.toInstant(), h13.toInstant())
			;
		then(afterDeleteLocations)
			.as("The slots of the whole hour deleted, all others left as they were")
			.isEqualTo(partialAndOtherLocations)
			;
		then(listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour))
			.as("The partial hours marked stale")
			.extracting(StaleAggregateDatum::getTimestamp)
			.contains(h10.toInstant(), h12.toInstant())
			;
		// @formatter:on

		// WHEN
		processStaleHours();

		// THEN
		final Long differ = jdbcTemplate.queryForObject(
				"""
						WITH x AS (
							SELECT x.*
							FROM generate_series(?::timestamptz, ?::timestamptz, INTERVAL 'PT1H') AS h(hour)
							INNER JOIN LATERAL solardatm.rollup_datm_15m_for_hour(?::uuid, h.hour, h.hour + INTERVAL 'PT1H') q ON TRUE
							INNER JOIN LATERAL unnest(q.slots) x ON TRUE
							WHERE h.hour <> ?::timestamptz
						)
						, s AS (
							SELECT * FROM solardatm.agg_datm_15m WHERE stream_id = ?::uuid
						)
						SELECT count(*)
						FROM x FULL OUTER JOIN s ON s.ts_start = x.ts_start
						WHERE (s.*)::text IS DISTINCT FROM (x.*)::text
						""",
				Long.class, Timestamp.from(h09.toInstant()), Timestamp.from(h13.toInstant()),
				streamId.toString(), Timestamp.from(h11.toInstant()), streamId.toString());

		// @formatter:off
		then(listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute))
			.as("Partial hours reprocessed: only the slots with datum left in them, or a reading")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactlyElementsOf(concat(concat(concat(
					slots(h09, 0, 1, 2, 3),
					slots(h10, 0, 1)),
					slots(h12, 2, 3)),
					slots(h13, 0, 1, 2, 3)))
			;
		then(differ)
			.as("Stored slots are those computed for their hours, except the deleted hour, which has none")
			.isZero()
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = { "Asia/Kolkata", "Asia/Kathmandu" })
	public void slotsDeletedWithHourlyRowsOnSessionZoneHours(String zone) {
		// GIVEN
		final ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 9, 0, 0, 0, ZoneId.of(zone));
		final UUID streamId = setupProcessedStream("UTC", zone, start, 5);
		final List<Instant> hoursBefore = hourlyRowHours();
		final Map<Instant, String> slotLocations = slotRowLocations(streamId);

		// the hourly rows deleted are those starting from the stream's 11:00 to before its 12:00
		final Instant wholeFrom = start.plusHours(2).toInstant();
		final Instant wholeTo = start.plusHours(3).toInstant();
		final List<Instant> deletedHours = hoursBefore.stream()
				.filter(h -> !h.isBefore(wholeFrom) && h.isBefore(wholeTo)).toList();
		final List<Instant> keptHours = new ArrayList<>(hoursBefore);
		keptHours.removeAll(deletedHours);
		final Map<Instant, String> keptLocations = new LinkedHashMap<>(slotLocations);
		keptLocations.keySet().removeIf(slot -> deletedHours.contains(slot.truncatedTo(HOURS)));

		// WHEN
		final long deleted = deleteDatum(streamId, start.plusHours(1).plusMinutes(30),
				start.plusHours(3).plusMinutes(30), zone);

		// THEN
		jdbcTemplate.update("""
				CREATE TEMPORARY TABLE t_stale ON COMMIT DROP AS
				SELECT ts_start AS hour FROM solardatm.agg_stale_datm
				WHERE stream_id = ?::uuid AND agg_kind = 'h'
				""", streamId.toString());
		final Set<Instant> staleHours = listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour).stream()
				.map(StaleAggregateDatum::getTimestamp).collect(toSet());

		// @formatter:off
		then(slotLocations)
			.as("Every slot of every hour stored before the delete")
			.hasSize(20)
			;
		then(deletedHours)
			.as("One UTC hour starts within the stream's whole hours")
			.containsExactly(Instant.parse("2020-06-01T06:00:00Z"))
			;
		then(deleted)
			.as("Datum from 10:30 to 12:30 deleted")
			.isEqualTo(24L)
			;
		then(hourlyRowHours())
			.as("Only that hourly row deleted")
			.isEqualTo(keptHours)
			;
		then(slotRowLocations(streamId))
			.as("The slots of that hour deleted, all others left as they were")
			.isEqualTo(keptLocations)
			;
		then(staleHours)
			.as("Hours marked stale")
			.isNotEmpty()
			;
		// @formatter:on

		// WHEN
		processStaleHours();

		// THEN
		// in Asia/Kathmandu the 05:00 UTC hour loses all its datum but is neither deleted nor marked
		// stale, so it keeps its hourly row, and with it its slots
		final Map<Instant, String> unvisitedLocations = new LinkedHashMap<>(keptLocations);
		unvisitedLocations.keySet().removeIf(slot -> staleHours.contains(slot.truncatedTo(HOURS)));
		final Map<Instant, String> afterLocations = slotRowLocations(streamId);
		afterLocations.keySet().removeIf(slot -> staleHours.contains(slot.truncatedTo(HOURS)));
		final Long differ = jdbcTemplate.queryForObject(
				"""
						WITH x AS (
							SELECT x.*
							FROM t_stale h
							INNER JOIN LATERAL solardatm.rollup_datm_15m_for_hour(?::uuid, h.hour, h.hour + INTERVAL 'PT1H') q ON TRUE
							INNER JOIN LATERAL unnest(q.slots) x ON TRUE
						)
						, s AS (
							SELECT s.*
							FROM solardatm.agg_datm_15m s
							INNER JOIN t_stale h ON s.ts_start >= h.hour AND s.ts_start < h.hour + INTERVAL 'PT1H'
							WHERE s.stream_id = ?::uuid
						)
						SELECT count(*)
						FROM x FULL OUTER JOIN s ON s.ts_start = x.ts_start
						WHERE (s.*)::text IS DISTINCT FROM (x.*)::text
						""",
				Long.class, streamId.toString(), streamId.toString());

		// @formatter:off
		then(afterLocations)
			.as("The slots of hours not marked stale left as they were")
			.isEqualTo(unvisitedLocations)
			;
		then(differ)
			.as("The hours marked stale have the slots computed for them")
			.isZero()
			;
		// @formatter:on
	}

}
