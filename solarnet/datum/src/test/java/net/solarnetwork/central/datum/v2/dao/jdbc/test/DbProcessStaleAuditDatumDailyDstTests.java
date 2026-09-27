/* ==================================================================
 * DbProcessStaleAuditDatumDailyDstTests.java - 27/09/2026 11:14:26 am
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
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertAuditDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.insertStaleAuditDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the daylight saving time handling of the
 * {@link solardatm.process_one_aud_stale_datm} procedure.
 *
 * <p>
 * The stale row time stamps are local midnight in the stream time zone, so a
 * daily rollup window must be one day long <i>in that zone</i>. These tests pin
 * the audit rows to a daylight saving transition day in {@link #TEST_TZ}, where a
 * local day is not 24 hours long, and force the JDBC session time zone to UTC so
 * it differs from the stream time zone.
 * </p>
 *
 * <p>
 * Note the stream time zone is resolved from the node's location rather than from
 * the stream metadata, so these tests need a node in {@link #TEST_TZ}; the other
 * tests in this package use streams on nodes that do not exist, which resolve to
 * UTC and so never see a transition.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAuditDatumDailyDstTests extends BaseDatumJdbcTestSupport {

	/**
	 * The first day of a month that is 25 hours long in {@link #TEST_TZ}, where
	 * daylight saving ends at 03:00 and the clocks go back to 02:00.
	 */
	private static final ZonedDateTime DST_OVERLAP_DAY = ZonedDateTime.of(2029, 4, 1, 0, 0, 0, 0,
			ZoneId.of(TEST_TZ));

	/**
	 * A day that is 23 hours long in {@link #TEST_TZ}, where daylight saving
	 * starts at 02:00 and the clocks go forward to 03:00.
	 */
	private static final ZonedDateTime DST_GAP_DAY = ZonedDateTime.of(2029, 9, 30, 0, 0, 0, 0,
			ZoneId.of(TEST_TZ));

	private int executeCall(Aggregation kind) {
		return jdbcTemplate.execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.process_one_aud_stale_datm(?)}")) {
					cs.registerOutParameter(1, Types.INTEGER);
					cs.setString(2, kind.getKey());
					cs.execute();
					return cs.getInt(1);
				}
			}
		});
	}

	/**
	 * Create a node in {@link #TEST_TZ} and a datum stream on it.
	 *
	 * @return the stream ID
	 */
	private UUID setupTestStreamOnTestZoneNode() {
		setupTestNode();
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata
				.emptyMeta(UUID.randomUUID(), TEST_TZ, Node, TEST_NODE_ID, "a");
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, "UTC");
		return meta.getStreamId();
	}

	/**
	 * Verify the premise of these tests: the given local day is not 24 hours
	 * long, the stream resolves to the node time zone, and the session time zone
	 * differs from it.
	 *
	 * @param streamId
	 *        the stream ID under test
	 * @param dayStart
	 *        the local midnight the day starts at
	 * @param expectedLength
	 *        the expected length of the local day
	 */
	private void thenTestPremise(UUID streamId, ZonedDateTime dayStart, Duration expectedLength) {
		// @formatter:off
		then(Duration.between(dayStart, dayStart.plusDays(1)))
			.as("Test premise: %s is a %d hour day in %s", dayStart.toLocalDate(),
					expectedLength.toHours(), TEST_TZ)
			.isEqualTo(expectedLength)
			;
		then(jdbcTemplate.queryForObject(
				"SELECT time_zone FROM solardatm.find_metadata_for_stream(?::uuid)", String.class,
				streamId.toString()))
			.as("Test premise: stream time zone resolved from the node location")
			.isEqualTo(TEST_TZ)
			;
		then(jdbcTemplate.queryForObject("SHOW TimeZone", String.class))
			.as("Test premise: session time zone differs from the stream time zone")
			.isEqualTo("UTC")
			;
		// @formatter:on
	}

	private static DatumEntity testDatum(UUID streamId, Instant ts) {
		return new DatumEntity(streamId, ts, Instant.now(),
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null));
	}

	private static AggregateDatumEntity testHourlyDatum(UUID streamId, Instant ts) {
		return new AggregateDatumEntity(streamId, ts, Aggregation.Hour,
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null),
				DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] { decimalArray("100") },
						null));
	}

	private static AuditDatumEntity testIoAuditDatum(UUID streamId, Instant ts, long propCount) {
		return AuditDatumEntity.ioAuditDatum(streamId, ts, 1L, propCount, 0L, 0L, 0L);
	}

	private void insertStale(UUID streamId, Instant ts, Aggregation kind) {
		insertStaleAuditDatum(log, jdbcTemplate, singleton(
				(StaleAuditDatum) new StaleAuditDatumEntity(streamId, ts, kind, Instant.now())));
	}

	@Test
	public void processStaleRaw_dstOverlapDay() {
		// GIVEN
		final UUID streamId = setupTestStreamOnTestZoneNode();
		thenTestPremise(streamId, DST_OVERLAP_DAY, Duration.ofHours(25));

		final ZonedDateTime nextDayStart = DST_OVERLAP_DAY.plusDays(1);

		// a raw datum in the first and last local hours of the 25-hour day, plus one in the first
		// local hour of the following day
		insertDatum(log, jdbcTemplate,
				Set.of(testDatum(streamId, DST_OVERLAP_DAY.toInstant()),
						testDatum(streamId, nextDayStart.minusHours(1).toInstant()),
						testDatum(streamId, nextDayStart.toInstant())));
		insertStale(streamId, DST_OVERLAP_DAY.toInstant(), Aggregation.None);

		// WHEN
		int processedCount = executeCall(Aggregation.None);

		// THEN
		List<AuditDatum> days = listAuditDatum(jdbcTemplate, Aggregation.Day);
		// @formatter:off
		then(processedCount)
			.as("Stale raw record processed")
			.isEqualTo(1)
			;
		then(days)
			.as("Only the stale local day rolled up")
			.hasSize(1)
			.satisfiesExactly(day -> then(day)
					.as("Rolled up at the local midnight the 25-hour day starts at")
					.returns(DST_OVERLAP_DAY.toInstant(), from(AuditDatum::getTimestamp))
					.as("Both raw datum in the 25-hour day counted, the following day's excluded")
					.returns(2L, from(AuditDatum::getDatumCount))
			)
			;
		// @formatter:on
	}

	@Test
	public void processStaleHourly_dstOverlapDay() {
		// GIVEN
		final UUID streamId = setupTestStreamOnTestZoneNode();
		thenTestPremise(streamId, DST_OVERLAP_DAY, Duration.ofHours(25));

		final ZonedDateTime nextDayStart = DST_OVERLAP_DAY.plusDays(1);

		// an hourly aggregate in the first and last local hours of the 25-hour day, plus one in the
		// first local hour of the following day
		insertAggregateDatum(log, jdbcTemplate,
				Set.of(testHourlyDatum(streamId, DST_OVERLAP_DAY.toInstant()),
						testHourlyDatum(streamId, nextDayStart.minusHours(1).toInstant()),
						testHourlyDatum(streamId, nextDayStart.toInstant())));
		insertStale(streamId, DST_OVERLAP_DAY.toInstant(), Aggregation.Hour);

		// WHEN
		int processedCount = executeCall(Aggregation.Hour);

		// THEN
		List<AuditDatum> days = listAuditDatum(jdbcTemplate, Aggregation.Day);
		// @formatter:off
		then(processedCount)
			.as("Stale hourly record processed")
			.isEqualTo(1)
			;
		then(days)
			.as("Only the stale local day rolled up")
			.hasSize(1)
			.satisfiesExactly(day -> then(day)
					.as("Rolled up at the local midnight the 25-hour day starts at")
					.returns(DST_OVERLAP_DAY.toInstant(), from(AuditDatum::getTimestamp))
					.as("Both hourly aggregates in the 25-hour day counted, the following day's excluded")
					.returns(2L, from(AuditDatum::getDatumHourlyCount))
			)
			;
		// @formatter:on
	}

	@Test
	public void processStaleDaily_dstOverlapDay() {
		// GIVEN
		final UUID streamId = setupTestStreamOnTestZoneNode();
		thenTestPremise(streamId, DST_OVERLAP_DAY, Duration.ofHours(25));

		final ZonedDateTime nextDayStart = DST_OVERLAP_DAY.plusDays(1);

		// audit the first and last local hours of the 25-hour day, plus the first local hour of the
		// following day; the property counts are distinct powers of two so a mis-sized window is
		// obvious
		insertAuditDatum(log, jdbcTemplate,
				List.of(testIoAuditDatum(streamId, DST_OVERLAP_DAY.toInstant(), 1L),
						testIoAuditDatum(streamId, nextDayStart.minusHours(1).toInstant(), 2L),
						testIoAuditDatum(streamId, nextDayStart.toInstant(), 4L)));
		insertStale(streamId, DST_OVERLAP_DAY.toInstant(), Aggregation.Day);

		// WHEN
		int processedCount = executeCall(Aggregation.Day);

		// THEN
		List<AuditDatum> days = listAuditDatum(jdbcTemplate, Aggregation.Day);
		// @formatter:off
		then(processedCount)
			.as("Stale daily record processed")
			.isEqualTo(1)
			;
		then(days)
			.as("Only the stale local day rolled up")
			.hasSize(1)
			.satisfiesExactly(day -> then(day)
					.as("Rolled up at the local midnight the 25-hour day starts at")
					.returns(DST_OVERLAP_DAY.toInstant(), from(AuditDatum::getTimestamp))
					.as("Final local hour of the 25-hour day included in its rollup (1 + 2)")
					.returns(3L, from(AuditDatum::getDatumPropertyCount))
			)
			;
		// @formatter:on
	}

	@Test
	public void processStaleDaily_dstGapDay() {
		// GIVEN
		final UUID streamId = setupTestStreamOnTestZoneNode();
		thenTestPremise(streamId, DST_GAP_DAY, Duration.ofHours(23));

		final ZonedDateTime nextDayStart = DST_GAP_DAY.plusDays(1);

		// audit the first and last local hours of the 23-hour day, plus the first local hour of the
		// following day; the property counts are distinct powers of two so a mis-sized window is
		// obvious
		insertAuditDatum(log, jdbcTemplate,
				List.of(testIoAuditDatum(streamId, DST_GAP_DAY.toInstant(), 1L),
						testIoAuditDatum(streamId, nextDayStart.minusHours(1).toInstant(), 2L),
						testIoAuditDatum(streamId, nextDayStart.toInstant(), 4L)));
		insertStale(streamId, DST_GAP_DAY.toInstant(), Aggregation.Day);
		insertStale(streamId, nextDayStart.toInstant(), Aggregation.Day);

		// WHEN
		int[] processedCounts = new int[] { executeCall(Aggregation.Day), executeCall(Aggregation.Day),
				executeCall(Aggregation.Day) };

		// THEN
		List<AuditDatum> days = listAuditDatum(jdbcTemplate, Aggregation.Day);
		// @formatter:off
		then(processedCounts)
			.as("Both stale Day records processed, then no more remain")
			.containsExactly(1, 1, 0)
			;
		then(days)
			.as("Both local days rolled up, neither removed by the time zone change cleanup")
			.hasSize(2)
			.satisfiesExactly(day -> then(day)
					.as("Rolled up at the local midnight the 23-hour day starts at")
					.returns(DST_GAP_DAY.toInstant(), from(AuditDatum::getTimestamp))
					.as("First local hour of the following day excluded from the 23-hour day (1 + 2)")
					.returns(3L, from(AuditDatum::getDatumPropertyCount))
				, day -> then(day)
					.as("Rolled up at the following local midnight")
					.returns(nextDayStart.toInstant(), from(AuditDatum::getTimestamp))
					.as("Following day counted only in its own rollup")
					.returns(4L, from(AuditDatum::getDatumPropertyCount))
			)
			;
		// @formatter:on
	}

}
