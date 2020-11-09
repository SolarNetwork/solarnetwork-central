/* ==================================================================
 * DbAuditDatumRollupTests.java - 8/11/2020 6:40:06 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;

/**
 * Test cases for the database audit rollup stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbAuditDatumRollupTests extends BaseDatumJdbcTestSupport {

	private BasicNodeDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), nodeId, sourceId,
				new String[] { "x", "y", "z" }, new String[] { "w", "ww" }, new String[] { "st" });
	}

	@Test
	public void calcRaw() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums);
		UUID streamId = meta.values().iterator().next().getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_raw(?::uuid,?,?)", streamId.toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(start.plusHours(24).toInstant()));
		assertThat("Row returned with result columns", result.keySet(),
				containsInAnyOrder("stream_id", "ts_start", "datum_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp matches", result.get("ts_start"),
				equalTo(Timestamp.from(start.toInstant())));
		assertThat("Datum count within hour matches", result.get("datum_count"), equalTo(7));
	}

	@Test
	public void calcHourly() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-hour-datum-01.txt", getClass(), staticProvider(singleton(meta)));
		log.debug("Got test data: {}", datums);
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_hourly(?::uuid,?,?)", streamId.toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(start.plusHours(24).toInstant()));
		assertThat("Row returned with result columns", result.keySet(),
				containsInAnyOrder("stream_id", "ts_start", "datum_hourly_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp matches", result.get("ts_start"),
				equalTo(Timestamp.from(start.toInstant())));
		assertThat("Hourly datum count within hour matches", result.get("datum_hourly_count"),
				equalTo(8));
	}

	@Test
	public void calcDaily() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-day-datum-01.txt", getClass(), staticProvider(singleton(meta)));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AuditDatum> hourlyAudits = new ArrayList<>();
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 8; i++ ) {
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			propCount += p;
			datumQueryCount += q;
			AuditDatumEntity audit = AuditDatumEntity.hourlyAuditDatum(streamId,
					start.plusHours(i * 3).toInstant(), i + 1L, p, q);
			hourlyAudits.add(audit);
		}
		DatumTestUtils.insertAuditDatum(log, jdbcTemplate, hourlyAudits);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_daily(?::uuid,?,?)", streamId.toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(start.plusHours(24).toInstant()));
		assertThat("Row returned with result columns", result.keySet(), containsInAnyOrder("stream_id",
				"ts_start", "datum_daily_pres", "prop_count", "datum_q_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp matches", result.get("ts_start"),
				equalTo(Timestamp.from(start.toInstant())));
		assertThat("Daily datum present flag matches", result.get("datum_daily_pres"), equalTo(true));
		assertThat("Datum property count matches", result.get("prop_count"), equalTo(propCount));
		assertThat("Datum query count matches", result.get("datum_q_count"), equalTo(datumQueryCount));
	}

	@Test
	public void calcMonthly() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-month-datum-01.txt", getClass(), staticProvider(singleton(meta)));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AuditDatum> dailyAudits = new ArrayList<>();
		int datumCount = 0;
		int datumHourlyCount = 0;
		int datumDailyCount = 0;
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 8; i++ ) {
			long r = (i + 1) * 5000;
			long h = (i + 1) * 720;
			int d = 1; // day present
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			datumCount += r;
			datumHourlyCount += h;
			datumDailyCount += d;
			propCount += p;
			datumQueryCount += q;
			AuditDatumEntity audit = AuditDatumEntity.dailyAuditDatum(streamId,
					start.plusHours(i * 24).toInstant(), r, h, d, p, q);
			dailyAudits.add(audit);
		}
		DatumTestUtils.insertAuditDatum(log, jdbcTemplate, dailyAudits);

		// WHEN
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_monthly(?::uuid,?,?)", streamId.toString(),
				Timestamp.from(start.toInstant()), Timestamp.from(
						start.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(1).toInstant()));
		assertThat("Row returned with result columns", result.keySet(),
				containsInAnyOrder("stream_id", "ts_start", "datum_count", "datum_hourly_count",
						"datum_daily_count", "datum_monthly_pres", "prop_count", "datum_q_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp matches", result.get("ts_start"),
				equalTo(Timestamp.from(start.toInstant())));
		assertThat("Raw datum count matches", result.get("datum_count"), equalTo(datumCount));
		assertThat("Hourly datum count matches", result.get("datum_hourly_count"),
				equalTo(datumHourlyCount));
		assertThat("Daily datum count matches", result.get("datum_daily_count"),
				equalTo(datumDailyCount));
		assertThat("Monthly datum present flag matches", result.get("datum_monthly_pres"),
				equalTo(true));
		assertThat("Datum property count matches", result.get("prop_count"), equalTo(propCount));
		assertThat("Datum query count matches", result.get("datum_q_count"), equalTo(datumQueryCount));
	}

	@Test
	public void calcRunningTotal() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-month-datum-01.txt", getClass(), staticProvider(singleton(meta)));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		List<AuditDatum> monthlyAudits = new ArrayList<>();
		int datumCount = 0;
		int datumHourlyCount = 0;
		int datumDailyCount = 0;
		for ( int i = 0; i < 8; i++ ) {
			long r = (i + 1) * 5000;
			long h = (i + 1) * 720;
			int d = 1; // day present
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			datumCount += r;
			datumHourlyCount += h;
			datumDailyCount += d;
			AuditDatumEntity audit = AuditDatumEntity.monthlyAuditDatum(streamId,
					start.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(i).toInstant(), r, h, d,
					1, p, q);
			monthlyAudits.add(audit);
		}
		DatumTestUtils.insertAuditDatum(log, jdbcTemplate, monthlyAudits);

		// WHEN
		ZonedDateTime currDay = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_acc(?::uuid)", streamId.toString());
		assertThat("Row returned with result columns", result.keySet(),
				containsInAnyOrder("stream_id", "ts_start", "datum_count", "datum_hourly_count",
						"datum_daily_count", "datum_monthly_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp is start of current day", result.get("ts_start"),
				equalTo(Timestamp.from(currDay.toInstant())));
		assertThat("Raw datum count matches", result.get("datum_count"), equalTo(datumCount));
		assertThat("Hourly datum count matches", result.get("datum_hourly_count"),
				equalTo(datumHourlyCount));
		assertThat("Daily datum count matches", result.get("datum_daily_count"),
				equalTo(datumDailyCount));
		assertThat("Monthly datum count matches", result.get("datum_monthly_count"),
				equalTo(monthlyAudits.size()));
	}

	private static final String TEST_TZ_ALT = "America/Los_Angeles";

	@Test
	public void calcRunningTotal_tz() throws IOException {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, TEST_TZ_ALT);
		setupTestNode(1L, TEST_LOC_ID);
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-month-datum-02.txt", getClass(), staticProvider(singleton(meta)));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneId.of(TEST_TZ_ALT));

		List<AuditDatum> monthlyAudits = new ArrayList<>();
		int datumCount = 0;
		int datumHourlyCount = 0;
		int datumDailyCount = 0;
		for ( int i = 0; i < 8; i++ ) {
			long r = (i + 1) * 5000;
			long h = (i + 1) * 720;
			int d = 1; // day present
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			datumCount += r;
			datumHourlyCount += h;
			datumDailyCount += d;
			AuditDatumEntity audit = AuditDatumEntity.monthlyAuditDatum(streamId,
					start.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(i).toInstant(), r, h, d,
					1, p, q);
			monthlyAudits.add(audit);
		}
		DatumTestUtils.insertAuditDatum(log, jdbcTemplate, monthlyAudits);

		// WHEN
		ZonedDateTime currDay = ZonedDateTime.now(start.getZone()).truncatedTo(ChronoUnit.DAYS);
		Map<String, Object> result = jdbcTemplate.queryForMap(
				"SELECT * FROM solardatm.calc_audit_datm_acc(?::uuid)", streamId.toString());
		assertThat("Row returned with result columns", result.keySet(),
				containsInAnyOrder("stream_id", "ts_start", "datum_count", "datum_hourly_count",
						"datum_daily_count", "datum_monthly_count"));
		assertThat("Stream ID matches", result.get("stream_id"), equalTo(streamId));
		assertThat("Timestamp is start of current day", result.get("ts_start"),
				equalTo(Timestamp.from(currDay.toInstant())));
		assertThat("Raw datum count matches", result.get("datum_count"), equalTo(datumCount));
		assertThat("Hourly datum count matches", result.get("datum_hourly_count"),
				equalTo(datumHourlyCount));
		assertThat("Daily datum count matches", result.get("datum_daily_count"),
				equalTo(datumDailyCount));
		assertThat("Monthly datum count matches", result.get("datum_monthly_count"),
				equalTo(monthlyAudits.size()));
	}
}
