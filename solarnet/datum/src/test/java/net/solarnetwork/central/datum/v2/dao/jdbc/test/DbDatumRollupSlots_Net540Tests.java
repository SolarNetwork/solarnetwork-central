/* ==================================================================
 * DbDatumRollupSlots_Net540Tests.java - 26/11/2026 8:26:50 am
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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.rollup;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the minute level slot rollups validating NET-540: the clock
 * accumulation of each hour is allocated to the slots of that hour as
 * {@code solardatm.rollup_datm_15m_for_hour()} allocates it, so the slots of an
 * hour add up to the hour's rollup.
 *
 * <p>
 * The stream in each test declares one instantaneous property, {@code i}, and
 * one accumulating property, {@code wh}. Every datum has {@code i} of 1. The
 * slots are 10 minutes long.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumRollupSlots_Net540Tests extends BaseDatumJdbcTestSupport {

	/** The hour rolled up by most tests. */
	private static final ZonedDateTime HOUR = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);

	private ObjectDatumStreamMetadata givenStream() {
		final ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "A", new String[] { "i" }, new String[] { "wh" }, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	private void givenDatum(ObjectDatumStreamMetadata meta, String ts, @Nullable String wh) {
		final Instant date = Instant.parse(ts);
		final List<Datum> datum = List.of(new DatumEntity(meta.getStreamId(), date, date,
				propertiesOf(decimalArray("1"), wh != null ? decimalArray(wh) : null, null, null)));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);
	}

	private void givenReset(ObjectDatumStreamMetadata meta, String ts, String finalValue,
			String startValue) {
		final Instant date = Instant.parse(ts);
		final DatumSamples samplesFinal = new DatumSamples();
		samplesFinal.putAccumulatingSampleValue("wh", new BigDecimal(finalValue));
		final DatumSamples samplesStart = new DatumSamples();
		samplesStart.putAccumulatingSampleValue("wh", new BigDecimal(startValue));
		final List<DatumAuxiliary> resets = List.of(new DatumAuxiliaryEntity(meta.getStreamId(), date,
				DatumAuxiliaryType.Reset, date, samplesFinal, samplesStart, null, null));
		DatumDbUtils.insertDatumAuxiliary(log, jdbcTemplate, resets);
	}

	private List<AggregateDatum> rollupSlots(ObjectDatumStreamMetadata meta, ZonedDateTime start,
			ZonedDateTime end) {
		final Aggregation agg = Aggregation.TenMinute;
		final List<AggregateDatum> results = jdbcTemplate.query(
				"SELECT *, NULL AS read_a FROM solardatm.rollup_datm_for_time_span_slots(?::uuid,?,?,?) "
						+ "ORDER BY stream_id, ts_start",
				new AggregateDatumEntityRowMapper(agg), meta.getStreamId().toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(end.toInstant()), agg.getLevel());
		log.debug("Rolled up {} slots {} - {}:\n{}", agg.getKey(), start, end,
				results.stream().map(Object::toString).collect(joining("\n")));
		return results;
	}

	private List<AggregateDatum> rollupHours(ObjectDatumStreamMetadata meta, ZonedDateTime start,
			ZonedDateTime end) {
		final List<AggregateDatum> hourResults = new ArrayList<>();
		for ( ZonedDateTime hour = start; hour.isBefore(end); hour = hour.plusHours(1) ) {
			rollup(jdbcTemplate, meta.getStreamId(), hour, hour.plusHours(1),
					(datums, metas, sid, results) -> hourResults.addAll(results));
		}
		log.debug("Rolled up hours {} - {}:\n{}", start, end,
				hourResults.stream().map(Object::toString).collect(joining("\n")));
		return hourResults;
	}

	@Test
	public void gapLongerThanTolerance_noAccumulation() {
		// GIVEN
		// two datum 110 minutes apart, longer than the 1 hour clock tolerance
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:05:00Z", "100");
		givenDatum(meta, "2020-06-01T13:55:00Z", "210");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, HOUR.plusHours(2));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR, HOUR.plusHours(2));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot for each datum")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.toInstant(), HOUR.plusMinutes(110).toInstant())
			;
		then(results)
			.as("Neither slot has accumulation, as the gap between the datum exceeds the tolerance")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("0"), decimalArray("0"))
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("0"), decimalArray("0"))
			;
		// @formatter:on
	}

	@Test
	public void gapAcrossHour_splitAtHour() {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:50:00Z", "100");
		givenDatum(meta, "2020-06-01T13:10:00Z", "120");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, HOUR.plusHours(2));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR, HOUR.plusHours(2));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot for each datum")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.plusMinutes(50).toInstant(), HOUR.plusMinutes(70).toInstant())
			;
		then(results)
			.as("""
				The difference is split at the hour: (120 - 100) * (13:00 - 12:50) / (13:10 - 12:50) \
				in the slot of the datum before the hour, and the rest in the slot of the first datum \
				in the hour after\
				""")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("10"), decimalArray("10"))
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("10"), decimalArray("10"))
			;
		// @formatter:on
	}

	@Test
	public void gapAcrossHour_rangeFromHour_sameSlot() {
		// GIVEN
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:50:00Z", "100");
		givenDatum(meta, "2020-06-01T13:10:00Z", "120");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR.plusHours(1), HOUR.plusHours(2));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot, for the datum in the range")
			.hasSize(1)
			.element(0)
			.as("Slot is of the datum in the range")
			.returns(HOUR.plusMinutes(70).toInstant(), AggregateDatum::getTimestamp)
			.extracting(d -> d.getProperties().getAccumulating())
			.as("""
				Slot has the hour's share of the difference from the datum before the range, as when \
				the range starts before that datum: (120 - 100) * (13:10 - 13:00) / (13:10 - 12:50)\
				""")
			.isEqualTo(decimalArray("10"))
			;
		// @formatter:on
	}

	@Test
	public void datumWithoutAccumulatingInSlot_keepsShareOfDifference() {
		// GIVEN
		// the last datum in the first slot has no accumulating value
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:02:00Z", "100");
		givenDatum(meta, "2020-06-01T12:07:00Z", null);
		givenDatum(meta, "2020-06-01T12:12:00Z", "110");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR, HOUR.plusHours(1));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot for each slot with datum")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.toInstant(), HOUR.plusMinutes(10).toInstant())
			;
		then(results)
			.as("""
				First slot has its share of the difference to the datum in the next slot, past the \
				datum without an accumulating value: (110 - 100) * (12:10 - 12:02) / (12:12 - 12:02)\
				""")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("8"), decimalArray("2"))
			;
		then(hours)
			.as("Slots add up to the hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("10"))
			;
		// @formatter:on
	}

	@Test
	public void resetInSlot_startToNextDatumAcrossSlot_keepsShare() {
		// GIVEN
		// the reset's start reading is differenced against a datum in the next slot
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:02:00Z", "100");
		givenReset(meta, "2020-06-01T12:05:00Z", "105", "0");
		givenDatum(meta, "2020-06-01T12:15:00Z", "10");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR, HOUR.plusHours(1));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot for each slot with datum")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.toInstant(), HOUR.plusMinutes(10).toInstant())
			;
		then(results)
			.as("""
				First slot has the difference to the reset's final reading, and its share of the \
				difference from the reset's start reading: \
				(105 - 100) + (10 - 0) * (12:10 - 12:05) / (12:15 - 12:05)\
				""")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("10"), decimalArray("5"))
			;
		then(hours)
			.as("Slots add up to the hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("15"))
			;
		// @formatter:on
	}

	@Test
	public void resetAtDatumTime_noAccumulationBetweenThem() {
		// GIVEN
		// a reset at the same time as a datum, with a final reading different from the datum's
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:02:00Z", "100");
		givenDatum(meta, "2020-06-01T12:05:00Z", "105");
		givenReset(meta, "2020-06-01T12:05:00Z", "107", "0");
		givenDatum(meta, "2020-06-01T12:08:00Z", "10");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR, HOUR.plusHours(1));

		// THEN
		// @formatter:off
		then(results)
			.as("One slot, for the datum in the slot")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.toInstant())
			;
		then(results)
			.as("""
				No accumulation between the datum and the reset's final reading at the same time: \
				(105 - 100) + (10 - 0)\
				""")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("15"))
			;
		then(hours)
			.as("Slot adds up to the hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("15"))
			;
		// @formatter:on
	}

	@Test
	public void firstDatumAtHour_rowInLastSlotOfHourBefore() {
		// GIVEN
		// no datum before the hour; the first is exactly at the start of the hour
		final ObjectDatumStreamMetadata meta = givenStream();
		givenDatum(meta, "2020-06-01T12:00:00Z", "100");
		givenDatum(meta, "2020-06-01T12:10:00Z", "110");

		// WHEN
		final List<AggregateDatum> results = rollupSlots(meta, HOUR.minusHours(1), HOUR.plusHours(1));
		final List<AggregateDatum> hours = rollupHours(meta, HOUR.minusHours(1), HOUR.plusHours(1));

		// THEN
		// @formatter:off
		then(results)
			.as("""
				The hour before the datum has a row in its last slot, as it has an hourly rollup, \
				then one slot for each datum\
				""")
			.extracting(AggregateDatum::getTimestamp)
			.containsExactly(HOUR.minusMinutes(10).toInstant(), HOUR.toInstant(),
					HOUR.plusMinutes(10).toInstant())
			;
		then(results)
			.as("The hour before has no accumulation; the hour has (110 - 100) in its first slot")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("0"), decimalArray("10"), decimalArray("0"))
			;
		then(results)
			.element(0)
			.as("The row of the hour before the datum has no instantaneous values")
			.extracting(d -> d.getProperties().getInstantaneous())
			.isNull()
			;
		then(hours)
			.as("Slots add up to each hour's rollup")
			.extracting(d -> d.getProperties().getAccumulating())
			.containsExactly(decimalArray("0"), decimalArray("10"))
			;
		// @formatter:on
	}

}
