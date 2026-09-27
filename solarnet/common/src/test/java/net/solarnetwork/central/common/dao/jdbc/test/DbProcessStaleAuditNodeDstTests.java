/* ==================================================================
 * DbProcessStaleAuditNodeDstTests.java - 27/09/2026 9:58:12 am
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the daylight saving time handling of the
 * {@link solardatm.process_one_aud_stale_node} procedure.
 *
 * <p>
 * The stale row time stamps are local midnight in the node time zone, so a daily
 * rollup window must be one day long <i>in that zone</i>. These tests pin the
 * audit rows to a daylight saving transition day in {@link #TEST_TZ}, where a
 * local day is not 24 hours long, and force the JDBC session time zone to UTC so
 * it differs from the node time zone.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAuditNodeDstTests extends AbstractJUnit5JdbcDaoTestSupport {

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

	private static final String TEST_SERVICE = "test";

	private int executeCall(Aggregation kind) {
		return jdbcTemplate.execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.process_one_aud_stale_node(?)}")) {
					cs.registerOutParameter(1, Types.INTEGER);
					cs.setString(2, kind.getKey());
					cs.execute();
					return cs.getInt(1);
				}
			}
		});
	}

	/**
	 * Set the session time zone for the duration of the test transaction.
	 *
	 * @param zoneId
	 *        the zone ID to use
	 */
	private void setSessionTimeZone(String zoneId) {
		jdbcTemplate.queryForObject("SELECT set_config('TimeZone', ?, TRUE)", String.class, zoneId);
	}

	/**
	 * Verify the premise of these tests: the given local day is not 24 hours
	 * long, and the session time zone differs from the node time zone.
	 *
	 * @param dayStart
	 *        the local midnight the day starts at
	 * @param expectedLength
	 *        the expected length of the local day
	 */
	private void thenTestPremise(ZonedDateTime dayStart, Duration expectedLength) {
		// @formatter:off
		then(Duration.between(dayStart, dayStart.plusDays(1)))
			.as("Test premise: %s is a %d hour day in %s", dayStart.toLocalDate(),
					expectedLength.toHours(), TEST_TZ)
			.isEqualTo(expectedLength)
			;
		then(jdbcTemplate.queryForObject("SHOW TimeZone", String.class))
			.as("Test premise: session time zone differs from the node time zone")
			.isEqualTo("UTC")
			;
		// @formatter:on
	}

	@Test
	public void processDaily_dstOverlapDay() {
		// GIVEN
		setupTestNode();
		setSessionTimeZone("UTC");
		thenTestPremise(DST_OVERLAP_DAY, Duration.ofHours(25));

		final ZonedDateTime nextDayStart = DST_OVERLAP_DAY.plusDays(1);

		// audit the first and last local hours of the 25-hour day, plus the first local hour of the
		// following day; the counts are distinct powers of two so a mis-sized window is obvious
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				DST_OVERLAP_DAY.toInstant(), 1);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				nextDayStart.minusHours(1).toInstant(), 2);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				nextDayStart.toInstant(), 4);

		CommonDbTestUtils.debugStaleAuditNodeServiceTable(log, jdbcTemplate, "stale node services");

		// WHEN
		int[] processedCounts = new int[] { executeCall(Aggregation.Day), executeCall(Aggregation.Day),
				executeCall(Aggregation.Day) };

		// THEN
		List<AuditNodeServiceValue> days = CommonDbTestUtils
				.listAuditNodeServiceValueDaily(jdbcTemplate);
		// @formatter:off
		then(processedCounts)
			.as("Both stale Day records processed, then no more remain")
			.containsExactly(1, 1, 0)
			;
		then(days)
			.as("Both local days rolled up")
			.hasSize(2)
			.satisfiesExactly(day -> then(day)
					.as("Rolled up at the local midnight the 25-hour day starts at")
					.returns(DST_OVERLAP_DAY.toInstant(), from(AuditNodeServiceValue::getTimestamp))
					.as("Final local hour of the 25-hour day included in its rollup (1 + 2)")
					.returns(3L, from(AuditNodeServiceValue::getCount))
				, day -> then(day)
					.as("Rolled up at the following local midnight")
					.returns(nextDayStart.toInstant(), from(AuditNodeServiceValue::getTimestamp))
					.as("Following day counted only in its own rollup")
					.returns(4L, from(AuditNodeServiceValue::getCount))
			)
			;
		// @formatter:on
	}

	@Test
	public void processDaily_dstGapDay() {
		// GIVEN
		setupTestNode();
		setSessionTimeZone("UTC");
		thenTestPremise(DST_GAP_DAY, Duration.ofHours(23));

		final ZonedDateTime nextDayStart = DST_GAP_DAY.plusDays(1);

		// audit the first and last local hours of the 23-hour day, plus the first local hour of the
		// following day; the counts are distinct powers of two so a mis-sized window is obvious
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				DST_GAP_DAY.toInstant(), 1);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				nextDayStart.minusHours(1).toInstant(), 2);
		CommonDbTestUtils.auditNodeService(jdbcTemplate, TEST_NODE_ID, TEST_SERVICE,
				nextDayStart.toInstant(), 4);

		CommonDbTestUtils.debugStaleAuditNodeServiceTable(log, jdbcTemplate, "stale node services");

		// WHEN
		int[] processedCounts = new int[] { executeCall(Aggregation.Day), executeCall(Aggregation.Day),
				executeCall(Aggregation.Day) };

		// THEN
		List<AuditNodeServiceValue> days = CommonDbTestUtils
				.listAuditNodeServiceValueDaily(jdbcTemplate);
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
					.returns(DST_GAP_DAY.toInstant(), from(AuditNodeServiceValue::getTimestamp))
					.as("First local hour of the following day excluded from the 23-hour day (1 + 2)")
					.returns(3L, from(AuditNodeServiceValue::getCount))
				, day -> then(day)
					.as("Rolled up at the following local midnight")
					.returns(nextDayStart.toInstant(), from(AuditNodeServiceValue::getTimestamp))
					.as("Following day counted only in its own rollup")
					.returns(4L, from(AuditNodeServiceValue::getCount))
			)
			;
		// @formatter:on
	}

}
