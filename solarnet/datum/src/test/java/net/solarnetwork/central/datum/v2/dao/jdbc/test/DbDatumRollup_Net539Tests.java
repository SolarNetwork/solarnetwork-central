/* ==================================================================
 * DbDatumRollup_Net539Tests.java - 17/11/2026 3:10:44 pm
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.datumResourceToList;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DbDatumRollupTests.rollup;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
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
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
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
 * Test cases for datum rollup validating NET-539: the datum before a slot is
 * looked for only when something in the slot, or the next datum after it, can
 * be differenced against it.
 *
 * <p>
 * The stream in each test declares one accumulating property, {@code val}, so
 * the stream's metadata never lets the search for the prior datum be skipped;
 * whether it is skipped depends only on the datum around the slot.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumRollup_Net539Tests extends BaseDatumJdbcTestSupport {

	/** The hour rolled up by most tests. */
	private static final ZonedDateTime HOUR = ZonedDateTime.of(2023, 2, 6, 18, 0, 0, 0, ZoneOffset.UTC);

	/**
	 * The time of the datum with an accumulating value two days before
	 * {@link #HOUR} in {@code sample-raw-data-12-net539.csv}.
	 */
	private static final Instant PRIOR_TS = Instant.parse("2023-02-04T10:00:00Z");

	/** The read tolerance the hourly rollup uses. */
	private static final String TOLERANCE_READ = "P1Y";

	/**
	 * A row returned by {@code solardatm.find_datm_for_time_slot()}.
	 *
	 * @param ts
	 *        the row timestamp
	 * @param rtype
	 *        the row type: 0 for a datum, 1 for a reset final reading, 2 for a
	 *        reset start reading
	 */
	private record SlotRow(Instant ts, int rtype) {

	}

	private ObjectDatumStreamMetadata load(String resource) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "A", new String[] { "i" }, new String[] { "val" }, null);
		List<Datum> datum = datumResourceToList(getClass(), resource, staticProvider(singleton(meta)));
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		List<Datum> loaded = DatumDbUtils.listDatum(jdbcTemplate);
		log.debug("Loaded datum:\n{}", loaded.stream().map(Object::toString).collect(joining("\n")));

		return meta;
	}

	private void insertReset(ObjectDatumStreamMetadata meta, String ts, String finalValue,
			String startValue) {
		final Instant date = Instant.parse(ts);
		final DatumSamples samplesFinal = new DatumSamples();
		samplesFinal.putAccumulatingSampleValue("val", new BigDecimal(finalValue));
		final DatumSamples samplesStart = new DatumSamples();
		samplesStart.putAccumulatingSampleValue("val", new BigDecimal(startValue));
		final List<DatumAuxiliary> resets = List.of(new DatumAuxiliaryEntity(meta.getStreamId(), date,
				DatumAuxiliaryType.Reset, date, samplesFinal, samplesStart, null, null));
		DatumDbUtils.insertDatumAuxiliary(log, jdbcTemplate, resets);
	}

	private List<AggregateDatum> rollupHour(ObjectDatumStreamMetadata meta, ZonedDateTime hour) {
		final List<AggregateDatum> hourResults = new ArrayList<>();
		rollup(jdbcTemplate, meta.getStreamId(), hour, hour.plusHours(1),
				(datums, metas, sid, results) -> hourResults.addAll(results));
		log.debug("Rolled up hour {}:\n{}", hour,
				hourResults.stream().map(Object::toString).collect(joining("\n")));
		return hourResults;
	}

	private List<AggregateDatum> rollupSlots(ObjectDatumStreamMetadata meta, ZonedDateTime start,
			ZonedDateTime end, Aggregation agg) {
		final List<AggregateDatum> results = jdbcTemplate.query(
				"SELECT *, NULL AS read_a FROM solardatm.rollup_datm_for_time_span_slots(?::uuid,?,?,?) "
						+ "ORDER BY stream_id, ts_start",
				new AggregateDatumEntityRowMapper(agg), meta.getStreamId().toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(end.toInstant()), agg.getLevel());
		log.debug("Rolled up {} slots {} - {}:\n{}", agg.getKey(), start, end,
				results.stream().map(Object::toString).collect(joining("\n")));
		return results;
	}

	private List<SlotRow> findDatumForTimeSlot(ObjectDatumStreamMetadata meta, ZonedDateTime start,
			ZonedDateTime end) {
		final List<SlotRow> rows = jdbcTemplate.query("""
				SELECT ts, rtype
				FROM solardatm.find_datm_for_time_slot(?::uuid, ?, ?, ?::interval)
				ORDER BY ts, rtype
				""", (rs, rowNum) -> new SlotRow(rs.getTimestamp(1).toInstant(), rs.getInt(2)),
				meta.getStreamId().toString(), Timestamp.from(start.toInstant()),
				Timestamp.from(end.toInstant()), TOLERANCE_READ);
		log.debug("Found datum for time slot {} - {}:\n{}", start, end,
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	private static SlotRow datumAt(String ts) {
		return new SlotRow(Instant.parse(ts), 0);
	}

	@Test
	public void noAccumulatingInSlot_lookupReturnsOnlySlotDatum() {
		// GIVEN
		// the last datum with an accumulating value is 2 days before the hour; the datum since then,
		// including the two in the hour, have none
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");

		// WHEN
		final List<SlotRow> rows = findDatumForTimeSlot(meta, HOUR, HOUR.plusHours(1));

		// THEN
		// @formatter:off
		then(rows)
			.as("""
				Only the datum in the hour returned: nothing in the hour can be differenced against \
				the prior datum, so neither it nor the datum between it and the hour are returned\
				""")
			.containsExactly(
				  datumAt("2023-02-06T18:10:00Z")
				, datumAt("2023-02-06T18:40:00Z")
			)
			;
		// @formatter:on
	}

	@Test
	public void noAccumulatingInSlot_rollupHasNoAccumulating() {
		// GIVEN
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");

		// WHEN
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.as("Stream ID matches")
			.returns(meta.getStreamId(), AggregateDatum::getStreamId)
			.as("Timestamp is hour")
			.returns(HOUR.toInstant(), AggregateDatum::getTimestamp)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (4,5)")
					.containsExactly(decimalArray("4.5"))
					;
				then(h.getStatistics().getInstantaneous())
					.as("Instantaneous stats are count, min, max")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("2", "4", "5") })
					;
				then(h.getProperties().getAccumulating())
					.as("""
						No accumulating value, rather than a zero difference to the prior datum's \
						reading carried forward\
						""")
					.isNull()
					;
				then(h.getStatistics().getAccumulating())
					.as("No accumulating reading, rather than the prior datum's reading carried forward")
					.isNull()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void resetInSlot_keepsPriorDatum() {
		// GIVEN
		// no datum in the hour has an accumulating value, but a reset record in it does
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");
		insertReset(meta, "2023-02-06T18:20:00Z", "105", "0");

		// WHEN
		final List<SlotRow> rows = findDatumForTimeSlot(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(rows)
			.as("Prior datum is returned, for the reset's final reading to be differenced against")
			.first()
			.isEqualTo(new SlotRow(PRIOR_TS, 0))
			;
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (4,5)")
					.containsExactly(decimalArray("4.5"))
					;
				then(h.getProperties().getAccumulating())
					.as("Accumulating is 0 as the prior datum is beyond the clock tolerance")
					.containsExactly(decimalArray("0"))
					;
				then(h.getStatistics().getAccumulating())
					.as("""
						Accumulating reading difference is from the prior datum to the reset's final \
						reading (105 - 100), ending at the reset's start reading\
						""")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("5", "100", "0") })
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void resetAtSlotStart_keepsPriorDatum() {
		// GIVEN
		// a reset record exactly at the start of the hour, and an older one before the prior datum
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");
		insertReset(meta, "2023-01-01T00:00:00Z", "50", "0");
		insertReset(meta, "2023-02-06T18:00:00Z", "105", "0");

		// WHEN
		final List<SlotRow> rows = findDatumForTimeSlot(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(rows)
			.as("""
				Reset at start of hour is returned: the prior datum limits the search for resets to \
				after it, so the older reset does not hide this one\
				""")
			.containsExactly(
				  new SlotRow(HOUR.toInstant(), 1)
				, new SlotRow(HOUR.toInstant(), 2)
				, datumAt("2023-02-06T18:10:00Z")
				, datumAt("2023-02-06T18:40:00Z")
			)
			;
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getAccumulating())
					.as("Accumulating is 0 from the reset's start reading")
					.containsExactly(decimalArray("0"))
					;
				then(h.getStatistics().getAccumulating())
					.as("Accumulating reading starts and ends at the reset's start reading")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("0", "0", "0") })
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void resetBeforeSlot_noAccumulatingInSlot_rollupHasNoAccumulating() {
		// GIVEN
		// a reset record between the prior datum and the hour, and none in the hour
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");
		insertReset(meta, "2023-02-06T17:55:00Z", "105", "0");

		// WHEN
		final List<SlotRow> rows = findDatumForTimeSlot(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(rows)
			.as("Only the datum in the hour returned: the reset before the hour is not needed either")
			.containsExactly(
				  datumAt("2023-02-06T18:10:00Z")
				, datumAt("2023-02-06T18:40:00Z")
			)
			;
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (4,5)")
					.containsExactly(decimalArray("4.5"))
					;
				then(h.getProperties().getAccumulating())
					.as("No accumulating value, rather than a zero difference from the reset")
					.isNull()
					;
				then(h.getStatistics().getAccumulating())
					.as("No accumulating reading, rather than the reset's start reading carried forward")
					.isNull()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void emptySlotAfterReset_noRollup() {
		// GIVEN
		// a reset record between the prior datum and an hour with no datum at all
		ObjectDatumStreamMetadata meta = load("sample-raw-data-12-net539.csv");
		insertReset(meta, "2023-02-06T18:50:00Z", "105", "0");

		// WHEN
		final ZonedDateTime hour = HOUR.plusHours(1);
		final List<AggregateDatum> hourResults = rollupHour(meta, hour);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("""
				No aggregate for an hour without datum, rather than one carrying the reset's start \
				reading forward\
				""")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void accumulatingAtSlotEnd_keepsPriorDatum() {
		// GIVEN
		// no datum within the hour has an accumulating value; the one exactly at its end does
		ObjectDatumStreamMetadata meta = load("sample-raw-data-13-net539.csv");

		// WHEN
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (4,5), excluding the datum at the end")
					.containsExactly(decimalArray("4.5"))
					;
				then(h.getProperties().getAccumulating())
					.as("Accumulating is 0 as the prior datum is beyond the clock tolerance")
					.containsExactly(decimalArray("0"))
					;
				then(h.getStatistics().getAccumulating())
					.as("""
						Accumulating reading difference is from the prior datum to the datum at the \
						end of the hour (110 - 100), see NET-469\
						""")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("10", "100", "110") })
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void nextAccumulatingAfterSlot_keepsPriorDatum() {
		// GIVEN
		// no datum in the hour has an accumulating value; the next datum after the hour does, within
		// the 1 hour clock tolerance of the last datum in the hour
		ObjectDatumStreamMetadata meta = load("sample-raw-data-14-net539.csv");

		// WHEN
		final List<SlotRow> rows = findDatumForTimeSlot(meta, HOUR, HOUR.plusHours(1));
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(rows)
			.as("Prior datum and next datum returned, as the clock accumulation between them crosses the hour")
			.hasSize(6)
			.first()
			.isEqualTo(new SlotRow(PRIOR_TS, 0))
			;
		then(rows)
			.as("Next datum returned")
			.last()
			.isEqualTo(datumAt("2023-02-06T19:20:00Z"))
			;
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getAccumulating())
					.as("Accumulating is 0 as the prior datum is beyond the clock tolerance of the next")
					.containsExactly(decimalArray("0"))
					;
				then(h.getStatistics().getAccumulating())
					.as("Accumulating reading is the prior datum's, with no difference")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("0", "100", "100") })
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void slots_priorAndNextStraddleRange_keepsPriorDatum() {
		// GIVEN
		// no datum in the range has an accumulating value; the datum before and after it do, and the
		// range ends part way through the 15 minute slot holding the next datum
		ObjectDatumStreamMetadata meta = load("sample-raw-data-15-net539.csv");

		// WHEN
		final ZonedDateTime end = HOUR.plusMinutes(20);
		final List<AggregateDatum> results = rollupSlots(meta, HOUR, end, Aggregation.FifteenMinute);

		// THEN
		// @formatter:off
		then(results)
			.as("Two slots generated, for the datum in the range")
			.hasSize(2)
			.satisfies(list -> {
				then(list.get(0))
					.as("First slot is start of range")
					.returns(HOUR.toInstant(), AggregateDatum::getTimestamp)
					;
				then(list.get(0).getProperties().getAccumulating())
					.as("First slot has no accumulation")
					.containsExactly(decimalArray("0"))
					;
				then(list.get(1))
					.as("Second slot is 15 minutes after start of range")
					.returns(HOUR.plusMinutes(15).toInstant(), AggregateDatum::getTimestamp)
					;
				then(list.get(1).getProperties().getAccumulating())
					.as("""
						Second slot has the next datum's share of the clock accumulation from the \
						prior datum: (108 - 100) * (18:25 - 18:15) / (18:25 - 17:45)\
						""")
					.containsExactly(decimalArray("2"))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void net538_wayBackDatumWithoutAccumulating_usesPriorDatumBeforeIt() {
		// GIVEN
		// the hour has accumulating values; no datum within the 14 days before it has, nor does the
		// latest datum before those 14 days, but one 5 days before that does
		ObjectDatumStreamMetadata meta = load("sample-raw-data-16-net539.csv");

		// WHEN
		final List<AggregateDatum> hourResults = rollupHour(meta, HOUR);

		// THEN
		// @formatter:off
		then(hourResults)
			.as("One aggregate hour generated")
			.hasSize(1)
			.element(0)
			.satisfies(h -> {
				then(h.getProperties().getInstantaneous())
					.as("Instantaneous is average (5,6)")
					.containsExactly(decimalArray("5.5"))
					;
				then(h.getStatistics().getInstantaneous())
					.as("Instantaneous stats are count, min, max")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("2", "5", "6") })
					;
				then(h.getProperties().getAccumulating())
					.as("Accumulating is difference within hour (115 - 110)")
					.containsExactly(decimalArray("5"))
					;
				then(h.getStatistics().getAccumulating())
					.as("""
						Accumulating reading difference is from the datum before the way-back datum \
						(115 - 100), see NET-538\
						""")
					.isDeepEqualTo(new BigDecimal[][] { decimalArray("15", "100", "115") })
					;
			})
			;
		// @formatter:on
	}

}
