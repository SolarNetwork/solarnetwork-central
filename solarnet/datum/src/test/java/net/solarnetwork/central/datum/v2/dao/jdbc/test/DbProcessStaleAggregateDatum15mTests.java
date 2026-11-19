/* ==================================================================
 * DbProcessStaleAggregateDatum15mTests.java - 20/11/2026 9:30:00 am
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

import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertStaleAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the 15 minute rows that the
 * {@code solardatm.process_one_agg_stale_datm()} database function stores when
 * it processes a stale hour.
 *
 * <p>
 * The test stream has one instantaneous property {@code w}, starting at 100 and
 * going up by 1 with each datum, and one accumulating property {@code wh},
 * starting at 0 and going up by 10, with a datum every 5 minutes. So each 15
 * minute slot of an hour has three datum and an accumulation of 30.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAggregateDatum15mTests extends BaseDatumJdbcTestSupport {

	private static final Duration STEP = Duration.ofMinutes(5);

	private UUID setupStream(String zone, ZonedDateTime start, int count) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, zone);
		setupTestLocation(TEST_LOC_ID, zone);
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
				""", streamId.toString(), Timestamp.from(start.toInstant()), STEP.toString(), count);
		return streamId;
	}

	private void markHourStale(UUID streamId, ZonedDateTime hour) {
		insertStaleAggregateDatum(log, jdbcTemplate, singleton(
				new StaleAggregateDatumEntity(streamId, hour.toInstant(), Aggregation.Hour, now())));
	}

	private void processStaleHours() {
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));
	}

	private void setCompleteFrom(ZonedDateTime ts) {
		jdbcTemplate.update("UPDATE solardatm.agg_datm_15m_setting SET complete_from = ?",
				Timestamp.from(ts.toInstant()));
	}

	private Map<Instant, String> slotRowLocations(UUID streamId) {
		final Map<Instant, String> result = new LinkedHashMap<>(8);
		jdbcTemplate.query("""
				SELECT ts_start, ctid::text FROM solardatm.agg_datm_15m
				WHERE stream_id = ?::uuid
				ORDER BY ts_start
				""", rs -> {
			result.put(rs.getTimestamp(1).toInstant(), rs.getString(2));
		}, streamId.toString());
		return result;
	}

	private String hourRowLocation(UUID streamId, ZonedDateTime hour) {
		return jdbcTemplate.queryForObject("""
				SELECT ctid::text FROM solardatm.agg_datm_hourly
				WHERE stream_id = ?::uuid AND ts_start = ?
				""", String.class, streamId.toString(), Timestamp.from(hour.toInstant()));
	}

	private static BigDecimal[][] stats(String... values) {
		return new BigDecimal[][] { decimalArray(values) };
	}

	private static Instant slot(ZonedDateTime hour, int index) {
		return hour.plusMinutes(15L * index).toInstant();
	}

	@ParameterizedTest
	@ValueSource(strings = { "UTC", "Asia/Kolkata", "Asia/Kathmandu" })
	public void slotsWritten(String zone) {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(zone));
		final UUID streamId = setupStream(zone, hour, 15);
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		final List<AggregateDatum> hours = listAggregateDatum(jdbcTemplate, Aggregation.Hour);
		final List<AggregateDatum> slots = listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute);

		// @formatter:off
		then(hours)
			.as("Hour stored")
			.hasSize(1)
			.element(0)
			.as("Hour start")
			.returns(hour.toInstant(), AggregateDatum::getTimestamp)
			.as("Hour accumulation")
			.returns(decimalArray("120"), d -> d.getProperties().getAccumulating())
			.as("Hour reading")
			.returns(stats("120", "0", "120"), d -> d.getStatistics().getAccumulating())
			;
		then(slots)
			.as("Slot rows stored, for the stream")
			.allSatisfy(d -> then(d.getStreamId()).isEqualTo(streamId))
			.as("Every slot of the hour stored, in order")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(slot(hour, 0), slot(hour, 1), slot(hour, 2), slot(hour, 3))
			;
		then(slots)
			.as("Slot averages")
			.extracting(d -> d.getProperties().getInstantaneous())
			.containsExactly(decimalArray("101"), decimalArray("104"), decimalArray("107"),
					decimalArray("110"))
			;
		then(slots)
			.as("Slot counts, minimums and maximums")
			.extracting(d -> d.getStatistics().getInstantaneous())
			.containsExactly(stats("3", "100", "102"), stats("3", "103", "105"),
					stats("3", "106", "108"), stats("3", "109", "111"))
			;
		then(slots)
			.as("Slot accumulation: from each datum to the next, in the slot of the earlier one")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("30"), decimalArray("30"), decimalArray("30"),
					decimalArray("30"))
			;
		then(slots)
			.as("Slot readings: each datum's difference in its own slot, the datum at the end of the hour in the last")
			.extracting(d -> d.getStatistics().getAccumulating())
			.containsExactly(stats("20", "0", "20"), stats("30", "20", "50"),
					stats("30", "50", "80"), stats("40", "80", "120"))
			;
		// @formatter:on
	}

	@Test
	public void unchangedSlotsNotRewritten() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final UUID streamId = setupStream(TEST_TZ, hour, 15);
		markHourStale(streamId, hour);
		processStaleHours();
		final Map<Instant, String> slotLocations = slotRowLocations(streamId);
		final String hourLocation = hourRowLocation(streamId, hour);

		// change the average of the second slot only
		jdbcTemplate.update("""
				UPDATE solardatm.da_datm SET data_i = ARRAY[200]::numeric[]
				WHERE stream_id = ?::uuid AND ts = ?
				""", streamId.toString(), Timestamp.from(hour.plusMinutes(20).toInstant()));
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		final Map<Instant, String> newSlotLocations = slotRowLocations(streamId);
		final List<AggregateDatum> slots = listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute);

		// @formatter:off
		then(newSlotLocations)
			.as("Same slots stored")
			.containsOnlyKeys(slotLocations.keySet())
			.as("Unchanged slot rows not rewritten")
			.containsEntry(slot(hour, 0), slotLocations.get(slot(hour, 0)))
			.containsEntry(slot(hour, 2), slotLocations.get(slot(hour, 2)))
			.containsEntry(slot(hour, 3), slotLocations.get(slot(hour, 3)))
			.as("Changed slot row rewritten")
			.doesNotContainEntry(slot(hour, 1), slotLocations.get(slot(hour, 1)))
			;
		then(slots)
			.as("Changed slot average stored")
			.extracting(d -> d.getProperties().getInstantaneous())
			.containsExactly(decimalArray("101"), decimalArray("136"), decimalArray("107"),
					decimalArray("110"))
			;
		then(hourRowLocation(streamId, hour))
			.as("Hour row rewritten, whether changed or not, as before")
			.isNotEqualTo(hourLocation)
			;
		// @formatter:on
	}

	@Test
	public void unchangedHourSlotsNotRewritten() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final UUID streamId = setupStream(TEST_TZ, hour, 15);
		markHourStale(streamId, hour);
		processStaleHours();
		final Map<Instant, String> slotLocations = slotRowLocations(streamId);
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		// @formatter:off
		then(slotRowLocations(streamId))
			.as("No slot row rewritten when the hour is processed again unchanged")
			.isEqualTo(slotLocations)
			.as("All slots stored")
			.hasSize(4)
			;
		// @formatter:on
	}

	@Test
	public void slotWithoutDatumDeleted() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final UUID streamId = setupStream(TEST_TZ, hour, 15);
		markHourStale(streamId, hour);
		processStaleHours();

		// remove the datum of the second slot
		jdbcTemplate.update("""
				DELETE FROM solardatm.da_datm
				WHERE stream_id = ?::uuid AND ts >= ? AND ts < ?
				""", streamId.toString(), Timestamp.from(slot(hour, 1)), Timestamp.from(slot(hour, 2)));
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		final List<AggregateDatum> slots = listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute);

		// @formatter:off
		then(slots)
			.as("Slot without datum deleted")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(slot(hour, 0), slot(hour, 2), slot(hour, 3))
			;
		then(slots)
			.as("Accumulation across the empty slot goes to the slot of the datum before it")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("60"), decimalArray("30"), decimalArray("30"))
			;
		then(slots)
			.as("Reading difference across the empty slot goes to the slot of the datum after it")
			.extracting(d -> d.getStatistics().getAccumulating())
			.containsExactly(stats("20", "0", "20"), stats("60", "20", "80"),
					stats("40", "80", "120"))
			;
		// @formatter:on
	}

	@Test
	public void hourWithoutRowSlotsDeleted() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final UUID streamId = setupStream(TEST_TZ, hour, 15);
		markHourStale(streamId, hour);
		processStaleHours();

		jdbcTemplate.update("DELETE FROM solardatm.da_datm WHERE stream_id = ?::uuid",
				streamId.toString());
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		// @formatter:off
		then(listAggregateDatum(jdbcTemplate, Aggregation.Hour))
			.as("Hour without datum deleted")
			.isEmpty()
			;
		then(listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute))
			.as("All slots of the hour deleted with it")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void hourBeforeBoundarySlotsUntouched() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final ZonedDateTime nextHour = hour.plusHours(1);
		final UUID streamId = setupStream(TEST_TZ, hour, 27);
		setCompleteFrom(nextHour);

		// a slot row the hour would not have, to show it is neither replaced nor deleted
		jdbcTemplate.update("""
				INSERT INTO solardatm.agg_datm_15m (stream_id, ts_start, data_a)
				VALUES (?::uuid, ?, ARRAY[999]::numeric[])
				""", streamId.toString(), Timestamp.from(slot(hour, 1)));
		markHourStale(streamId, hour);
		markHourStale(streamId, nextHour);

		// WHEN
		processStaleHours();

		// THEN
		final List<AggregateDatum> slots = listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute);

		// @formatter:off
		then(listAggregateDatum(jdbcTemplate, Aggregation.Hour))
			.as("Both hours stored")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(hour.toInstant(), nextHour.toInstant())
			;
		then(slots)
			.as("No slot written for the hour before the boundary, nor its slot row deleted; all slots written for the hour at it")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(slot(hour, 1), slot(nextHour, 0), slot(nextHour, 1), slot(nextHour, 2),
					slot(nextHour, 3))
			;
		then(slots)
			.element(0)
			.as("Slot row of the hour before the boundary unchanged")
			.returns(decimalArray("999"), d -> d.getProperties().getAccumulating())
			;
		// @formatter:on
	}

	@Test
	public void noBoundaryNoSlotsWritten() {
		// GIVEN
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 10, 0, 0, 0, ZoneId.of(TEST_TZ));
		final UUID streamId = setupStream(TEST_TZ, hour, 15);
		jdbcTemplate.update("DELETE FROM solardatm.agg_datm_15m_setting");
		markHourStale(streamId, hour);

		// WHEN
		processStaleHours();

		// THEN
		// @formatter:off
		then(jdbcTemplate.queryForObject("SELECT solardatm.agg_datm_15m_complete_from()", Timestamp.class))
			.as("No boundary")
			.isNull()
			;
		then(listAggregateDatum(jdbcTemplate, Aggregation.Hour))
			.as("Hour stored")
			.hasSize(1)
			;
		then(listAggregateDatum(jdbcTemplate, Aggregation.FifteenMinute))
			.as("No slot written without a boundary")
			.isEmpty()
			;
		// @formatter:on
	}

}
