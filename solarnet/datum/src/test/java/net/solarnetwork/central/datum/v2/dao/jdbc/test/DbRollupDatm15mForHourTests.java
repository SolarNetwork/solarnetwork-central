/* ==================================================================
 * DbRollupDatm15mForHourTests.java - 20/11/2026 9:00:00 am
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
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.groups.Tuple.tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;

/**
 * Test cases for the {@code solardatm.rollup_datm_15m_for_hour()} database
 * function, against the hourly rollup of
 * {@code solardatm.rollup_datm_for_time_span()}.
 *
 * <p>
 * Each parameterized test runs on the 24 generated streams of
 * {@code net540-generated-streams.sql}, for every hour of each stream, with the
 * hours taken in a session time zone whose offset is a whole hour, a half hour,
 * or a quarter hour, so the hours start at :00, :30, or :15 past a UTC hour.
 * The other tests check exact values for one hour of a stream of their own.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbRollupDatm15mForHourTests extends BaseDatumJdbcTestSupport {

	private static final String GENERATED_STREAMS = "classpath:net/solarnetwork/central/datum/v2/dao/jdbc/test/net540-generated-streams.sql";

	private static final Instant HOUR = Instant.parse("2020-06-01T10:00:00Z");

	private void setupGeneratedStreams(String zone) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, zone);
		executeSqlScript(GENERATED_STREAMS, false);
	}

	private static long count(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).longValue();
	}

	private UUID setupStream(String namesA) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', 'UTC', TRUE)", String.class);
		final UUID streamId = UUID.randomUUID();
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm_meta (stream_id, node_id, source_id, names_i, names_a)
				VALUES (?::uuid, ?, ?, ARRAY['w'], ?::text[])
				""", streamId.toString(), TEST_NODE_ID, TEST_SOURCE_ID, namesA);
		return streamId;
	}

	private Map<String, Object> hourRow(UUID streamId) {
		return jdbcTemplate.queryForMap("""
				SELECT q.data_i::text AS data_i
					, q.data_a::text AS data_a
					, q.stat_i::text AS stat_i
					, q.read_a::text AS read_a
				FROM solardatm.rollup_datm_15m_for_hour(?::uuid, ?::timestamptz, ?::timestamptz) q
				""", streamId.toString(), Timestamp.from(HOUR), Timestamp.from(HOUR.plus(1, HOURS)));
	}

	private List<Map<String, Object>> slotRows(UUID streamId) {
		return jdbcTemplate.queryForList("""
				SELECT to_char(x.ts_start, 'HH24:MI') AS slot
					, x.data_i::text AS data_i
					, x.data_a::text AS data_a
					, x.stat_i::text AS stat_i
					, x.read_a::text AS read_a
				FROM solardatm.rollup_datm_15m_for_hour(?::uuid, ?::timestamptz, ?::timestamptz) q
					, unnest(q.slots) x
				ORDER BY x.ts_start
				""", streamId.toString(), Timestamp.from(HOUR), Timestamp.from(HOUR.plus(1, HOURS)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "UTC", "Asia/Kolkata", "Asia/Kathmandu" })
	public void hourRowEqualsHourlyRollup(String zone) {
		// GIVEN
		setupGeneratedStreams(zone);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"""
						WITH o AS (
							SELECT h.k, h.hour, q.*
							FROM t_hours h
								, solardatm.rollup_datm_for_time_span(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
						)
						, n AS (
							SELECT h.k, h.hour, q.*
							FROM t_hours h
								, solardatm.rollup_datm_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
						)
						SELECT (SELECT count(DISTINCT k) FROM t_hours) AS streams
							, (SELECT count(*) FROM t_hours) AS hours
							, (SELECT count(*) FROM o) AS hourly_rows
							, (SELECT count(*) FROM n) AS rows
							, (SELECT count(*) FROM o FULL OUTER JOIN n USING (k, hour)
								WHERE o.k IS NULL OR n.k IS NULL
									OR (o.stream_id, o.ts_start, o.data_i, o.data_a, o.data_s, o.data_t, o.stat_i, o.read_a)::text
									IS DISTINCT FROM
									(n.stream_id, n.ts_start, n.data_i, n.data_a, n.data_s, n.data_t, n.stat_i, n.read_a)::text
							) AS differ
						""");

		// THEN
		// @formatter:off
		then(count(result, "streams"))
			.as("All generated streams compared")
			.isEqualTo(24L)
			;
		then(count(result, "hourly_rows"))
			.as("Many hours have an hourly row")
			.isGreaterThan(1000L)
			;
		then(count(result, "hours") - count(result, "hourly_rows"))
			.as("Some hours have no hourly row")
			.isPositive()
			;
		then(count(result, "rows"))
			.as("Same number of hour rows as the hourly rollup")
			.isEqualTo(count(result, "hourly_rows"))
			;
		then(count(result, "differ"))
			.as("No hour row differs from the hourly rollup, as text, and no hour has a row in only one")
			.isZero()
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = { "UTC", "Asia/Kolkata", "Asia/Kathmandu" })
	public void slotsAddUpToHour(String zone) {
		// GIVEN
		setupGeneratedStreams(zone);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap("""
				WITH n AS (
					SELECT h.k, h.sid, h.hour, q.*
					FROM t_hours h
						, solardatm.rollup_datm_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
				)
				, s AS (
					SELECT n.k, n.sid, n.hour, x.*
					FROM n, unnest(n.slots) x
				)
				, p AS (
					SELECT n.k, n.hour, n.data_a, n.stat_i, i.idx
					FROM n, generate_series(1, 2) i(idx)
				)
				-- hour rows with no datum or reset record in their hour
				, nd AS (
					SELECT n.*
					FROM n
					WHERE NOT EXISTS (
							SELECT 1 FROM solardatm.da_datm d
							WHERE d.stream_id = n.sid
								AND d.ts >= n.hour AND d.ts < n.hour + INTERVAL 'PT1H')
						AND NOT EXISTS (
							SELECT 1 FROM solardatm.da_datm_aux a
							WHERE a.stream_id = n.sid
								AND a.ts >= n.hour AND a.ts < n.hour + INTERVAL 'PT1H')
				)
				, sa AS (
					SELECT s.k, s.hour, i.idx
						, sum(s.data_a[i.idx]) AS a
						, sum(s.stat_i[i.idx][1]) AS c
						, min(s.stat_i[i.idx][2]) AS mn
						, max(s.stat_i[i.idx][3]) AS mx
					FROM s, generate_series(1, 2) i(idx)
					GROUP BY s.k, s.hour, i.idx
				)
				SELECT (SELECT count(*) FROM n) AS rows
					, (SELECT count(*) FROM s) AS slot_rows
					, (SELECT count(*) FROM n WHERE n.slots IS NULL) AS rows_without_slots
					, (SELECT count(*) FROM s WHERE s.stream_id <> s.sid) AS slot_stream_wrong
					, (SELECT count(*) FROM s
						WHERE s.ts_start < s.hour OR s.ts_start >= s.hour + INTERVAL 'PT1H'
							OR s.ts_start <> time_bucket(INTERVAL 'PT15M', s.ts_start)
					) AS slot_misplaced
					, (SELECT count(*) - count(DISTINCT (s.k, s.ts_start)) FROM s) AS slot_duplicated
					, (SELECT count(*) FROM n
						WHERE n.slots <> (SELECT array_agg(x ORDER BY x.ts_start) FROM unnest(n.slots) x)
					) AS slots_unordered
					, (SELECT count(*) FROM nd) AS rows_without_datum
					, (SELECT count(*) FROM nd
						WHERE cardinality(nd.slots) = 1
							AND (nd.slots[1]).ts_start = nd.hour + INTERVAL 'PT45M'
					) AS rows_without_datum_last_slot
					, (SELECT count(*) FROM p LEFT OUTER JOIN sa USING (k, hour, idx)
						WHERE COALESCE(p.data_a[p.idx], 0) <> COALESCE(sa.a, 0)
					) AS data_a_differ
					, (SELECT count(*) FROM p LEFT OUTER JOIN sa USING (k, hour, idx)
						WHERE p.stat_i[p.idx][1] IS DISTINCT FROM sa.c
							OR p.stat_i[p.idx][2] IS DISTINCT FROM sa.mn
							OR p.stat_i[p.idx][3] IS DISTINCT FROM sa.mx
					) AS stat_i_differ
					, (SELECT count(*) FROM n
						WHERE n.data_t IS DISTINCT FROM (
							SELECT array_agg(DISTINCT t ORDER BY t)
							FROM s, unnest(s.data_t) t
							WHERE s.k = n.k AND s.hour = n.hour)
					) AS data_t_differ
				""");

		// THEN
		// @formatter:off
		then(count(result, "rows"))
			.as("Many hours have a row")
			.isGreaterThan(1000L)
			;
		then(count(result, "slot_rows"))
			.as("More slot rows than hour rows")
			.isGreaterThan(count(result, "rows"))
			;
		then(count(result, "rows_without_slots"))
			.as("Every hour row has slot rows")
			.isZero()
			;
		then(count(result, "slot_stream_wrong"))
			.as("Every slot row has the hour's stream")
			.isZero()
			;
		then(count(result, "slot_misplaced"))
			.as("Every slot row starts within its hour, on a 15 minute boundary")
			.isZero()
			;
		then(count(result, "slot_duplicated"))
			.as("No slot row is repeated")
			.isZero()
			;
		then(count(result, "slots_unordered"))
			.as("Slot rows are in time order")
			.isZero()
			;
		// the generated streams have hours with a row but no datum or reset record in UTC and
		// Asia/Kolkata hours, none in Asia/Kathmandu ones; hourWithoutDatum() checks one exactly
		then(count(result, "rows_without_datum_last_slot"))
			.as("An hour row without datum has one slot row, the last slot of the hour")
			.isEqualTo(count(result, "rows_without_datum"))
			;
		then(count(result, "data_a_differ"))
			.as("The slots' accumulation of each property adds up to the hour's exactly")
			.isZero()
			;
		then(count(result, "stat_i_differ"))
			.as("The slots' counts add up to the hour's, and their minimum and maximum are the hour's")
			.isZero()
			;
		then(count(result, "data_t_differ"))
			.as("The slots' tags together are the hour's")
			.isZero()
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = { "UTC", "Asia/Kolkata", "Asia/Kathmandu" })
	public void slotsRolledUpEqualHour(String zone) {
		// GIVEN
		setupGeneratedStreams(zone);

		// the slot rows stand in the hourly table, so the function that rolls hours up to days
		// can roll them up to their hours
		int slotCount = jdbcTemplate
				.update("""
						INSERT INTO solardatm.agg_datm_hourly (stream_id, ts_start, data_i, data_a, data_s, data_t, stat_i, read_a)
						SELECT x.stream_id, x.ts_start, x.data_i, x.data_a, x.data_s, x.data_t, x.stat_i, x.read_a
						FROM t_hours h
							, solardatm.rollup_datm_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
							, unnest(q.slots) x
						""");

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"""
						WITH n AS (
							SELECT h.k, h.hour, q.*
							FROM t_hours h
								, solardatm.rollup_datm_15m_for_hour(h.sid, h.hour, h.hour + INTERVAL 'PT1H') q
						)
						, u AS (
							SELECT h.k, h.hour, q.*
							FROM t_hours h
								, solardatm.rollup_agg_data_for_time_span(h.sid, h.hour, h.hour + INTERVAL 'PT1H', 'h') q
						)
						SELECT count(*) AS rows
							, count(*) FILTER (WHERE n.k IS NULL OR u.k IS NULL) AS presence_differ
							, count(*) FILTER (WHERE n.data_a IS DISTINCT FROM u.data_a) AS data_a_differ
							, count(*) FILTER (WHERE n.read_a IS DISTINCT FROM u.read_a) AS read_a_differ
							, count(*) FILTER (WHERE n.stat_i IS DISTINCT FROM u.stat_i) AS stat_i_differ
							, count(*) FILTER (WHERE n.data_t IS DISTINCT FROM u.data_t) AS data_t_differ
							, COALESCE(max(x.rel), 0)::FLOAT8 AS data_i_max_rel_diff
						FROM n
						FULL OUTER JOIN u USING (k, hour)
						LEFT OUTER JOIN LATERAL (
							SELECT max(abs(a - b) / NULLIF(abs(a), 0)) AS rel
							FROM unnest(n.data_i, u.data_i) AS x(a, b)
						) x ON TRUE
						""");

		// THEN
		// the hour's status is the most frequent value of its datum, which the most frequent value
		// of its slots need not be, so data_s is not compared
		// @formatter:off
		then(slotCount)
			.as("Slot rows rolled up")
			.isGreaterThan(1000)
			;
		then(count(result, "rows"))
			.as("Many hours have a row")
			.isGreaterThan(1000L)
			;
		then(count(result, "presence_differ"))
			.as("The slots roll up to a row for exactly the hours that have one")
			.isZero()
			;
		then(count(result, "data_a_differ"))
			.as("The slots roll up to the hour's accumulation")
			.isZero()
			;
		then(count(result, "read_a_differ"))
			.as("The slots roll up to the hour's reading difference, start and end")
			.isZero()
			;
		then(count(result, "stat_i_differ"))
			.as("The slots roll up to the hour's counts, minimums and maximums")
			.isZero()
			;
		then(count(result, "data_t_differ"))
			.as("The slots roll up to the hour's tags")
			.isZero()
			;
		then((Double) result.get("data_i_max_rel_diff"))
			.as("The slots roll up to the hour's averages, within rounding")
			.isLessThan(1e-12)
			;
		// @formatter:on
	}

	@Test
	public void propertyWithoutValueInSlot() {
		// GIVEN
		final UUID streamId = setupStream("{wh,x}");

		// a datum every 5 minutes from 10:00 to 11:00, without wh from 10:30 to 10:40, never with x
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a)
				SELECT ?::uuid, ?::timestamptz + n * INTERVAL 'PT5M', CURRENT_TIMESTAMP
					, ARRAY[100 + n]::numeric[]
					, CASE
						WHEN n BETWEEN 6 AND 8 THEN ARRAY[NULL, NULL]::numeric[]
						ELSE ARRAY[10 * n, NULL]::numeric[]
						END
				FROM generate_series(0, 12) n
				""", streamId.toString(), Timestamp.from(HOUR));

		// WHEN
		Map<String, Object> hour = hourRow(streamId);
		List<Map<String, Object>> slots = slotRows(streamId);

		// THEN
		// the wh accumulation from 10:25 to 10:45 belongs to the slot of the 10:25 datum, as no datum
		// between has wh; the readings follow the datum
		// @formatter:off
		then(hour)
			.as("The hour accumulates wh, and has zero for x")
			.containsEntry("data_a", "{120,0}")
			.containsEntry("read_a", "{{120,0,120},{0,NULL,NULL}}")
			;
		then(slots)
			.as("A slot whose datum lack wh has NULL for it, as another slot of the hour has wh; "
					+ "every slot has zero for x, as no slot of the hour has x")
			.extracting(r -> r.get("slot"), r -> r.get("data_i"), r -> r.get("data_a"),
					r -> r.get("read_a"))
			.containsExactly(
					tuple("10:00", "{101}", "{30,0}", "{{20,0,20},{0,NULL,NULL}}"),
					tuple("10:15", "{104}", "{60,0}", "{{30,20,50},{0,NULL,NULL}}"),
					tuple("10:30", "{107}", "{NULL,0}", "{{0,NULL,NULL},{0,NULL,NULL}}"),
					tuple("10:45", "{110}", "{30,0}", "{{70,50,120},{0,NULL,NULL}}"))
			;
		// @formatter:on
	}

	@Test
	public void hourWithoutDatum() {
		// GIVEN
		final UUID streamId = setupStream("{wh}");

		// no datum in the hour, but one exactly at its end: the hour has a row for the reading from
		// the datum before the hour, which is too long before for any clock accumulation
		jdbcTemplate.update("""
				INSERT INTO solardatm.da_datm (stream_id, ts, received, data_i, data_a)
				VALUES (?::uuid, ?::timestamptz - INTERVAL 'PT2M', CURRENT_TIMESTAMP
						, ARRAY[1]::numeric[], ARRAY[10]::numeric[])
					, (?::uuid, ?::timestamptz + INTERVAL 'PT1H', CURRENT_TIMESTAMP
						, ARRAY[2]::numeric[], ARRAY[50]::numeric[])
				""", streamId.toString(), Timestamp.from(HOUR), streamId.toString(),
				Timestamp.from(HOUR));

		// WHEN
		Map<String, Object> hour = hourRow(streamId);
		List<Map<String, Object>> slots = slotRows(streamId);

		// THEN
		// @formatter:off
		then(hour)
			.as("The hour has no instantaneous values, zero accumulation, and the reading across it")
			.containsEntry("data_i", null)
			.containsEntry("data_a", "{0}")
			.containsEntry("stat_i", null)
			.containsEntry("read_a", "{{40,10,50}}")
			;
		then(slots)
			.as("The hour's one slot row is its last slot, with the hour's values")
			.extracting(r -> r.get("slot"), r -> r.get("data_i"), r -> r.get("data_a"),
					r -> r.get("stat_i"), r -> r.get("read_a"))
			.containsExactly(tuple("10:45", null, "{0}", null, "{{40,10,50}}"))
			;
		// @formatter:on
	}

}
