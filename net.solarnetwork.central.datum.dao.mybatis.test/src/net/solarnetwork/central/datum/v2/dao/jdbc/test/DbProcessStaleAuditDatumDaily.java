/* ==================================================================
 * DbProcessStaleAuditDatumDaily.java - 9/11/2020 2:39:08 pm
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

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertStaleAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.processStaleAuditDatum;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for DB stored procedures that process stale audit datum daily
 * records.
 * 
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAuditDatumDaily extends BaseDatumJdbcTestSupport {

	private BasicNodeDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), "UTC", nodeId, sourceId,
				new String[] { "x", "y", "z" }, new String[] { "w", "ww" }, new String[] { "st" });
	}

	private List<GeneralNodeDatum> loadJson(String resource) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		return datums;
	}

	private List<AuditDatumEntity> auditDatum(Aggregation kind) {
		List<AuditDatumEntity> result = listAuditDatum(jdbcTemplate, kind);
		log.debug("Got {} audit data:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private static void assertStaleAuditDatum(String prefix, StaleAuditDatum stale,
			StaleAuditDatum expected) {
		assertThat(prefix + " stale audit record kind", stale.getKind(), equalTo(expected.getKind()));
		assertThat(prefix + "stale audit record stream ID", stale.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale audit record timestamp", stale.getTimestamp(),
				equalTo(expected.getTimestamp()));
	}

	private static void assertAuditDatumId(String prefix, AuditDatum datum, AuditDatum expected) {
		assertThat(prefix + " audit record stream ID", datum.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " audit record timestamp", datum.getTimestamp(),
				equalTo(expected.getTimestamp()));
		assertThat(prefix + " audit record kind", datum.getAggregation(),
				equalTo(expected.getAggregation()));
	}

	private List<AuditDatum> insertTestAuditDatumHourly(ZonedDateTime start, UUID streamId) {
		List<AuditDatum> hourlyAudits = new ArrayList<>();
		for ( int i = 0; i < 8; i++ ) {
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			AuditDatumEntity audit = AuditDatumEntity.hourlyAuditDatum(streamId,
					start.plusHours(i * 3).toInstant(), i + 1L, p, q);
			hourlyAudits.add(audit);
		}
		insertAuditDatum(log, jdbcTemplate, hourlyAudits);
		return hourlyAudits;
	}

	private List<AuditDatum> insertTestAuditDatumDaily(ZonedDateTime start, UUID streamId) {
		List<AuditDatum> dailyAudits = new ArrayList<>();
		for ( int i = 0; i < 8; i++ ) {
			long r = (i + 1) * 5000;
			long h = (i + 1) * 720;
			int d = 1; // day present
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			AuditDatumEntity audit = AuditDatumEntity.dailyAuditDatum(streamId,
					start.plusHours(i * 24).toInstant(), r, h, d, p, q);
			dailyAudits.add(audit);
		}
		insertAuditDatum(log, jdbcTemplate, dailyAudits);
		return dailyAudits;
	}

	private List<AuditDatum> insertTestAuditDatumMonthly(ZonedDateTime start, UUID streamId) {
		List<AuditDatum> monthlyAudits = new ArrayList<>();
		for ( int i = 0; i < 8; i++ ) {
			long r = (i + 1) * 5000;
			long h = (i + 1) * 720;
			int d = 1; // day present
			long p = (i + 1) * 2L;
			long q = (i + 1) * 100L;
			AuditDatumEntity audit = AuditDatumEntity.monthlyAuditDatum(streamId,
					start.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(i).toInstant(), r, h, d,
					1, p, q);
			monthlyAudits.add(audit);
		}
		insertAuditDatum(log, jdbcTemplate, monthlyAudits);
		return monthlyAudits;
	}

	@Test
	public void processStaleRaw() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt");
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.None, Instant.now());
		insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.None));

		// THEN

		// should have stored rollup in datum_count column of aud_datm_daily table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Day);
		assertThat("Hour rollup result stored database", result, hasSize(1));

		AuditDatumEntity expected = AuditDatumEntity.dailyAuditDatum(meta.getStreamId(), day.toInstant(),
				7L, null, null, null, null);
		assertAuditDatumId("Daily audit rollup", result.get(0), expected);
		assertThat("Datum count", result.get(0).getDatumCount(), equalTo(expected.getDatumCount()));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAuditDatum> staleRows = listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for Month rollup level", staleRows, hasSize(1));
		assertStaleAuditDatum("Stale Month rollup created", staleRows.get(0), new StaleAuditDatumEntity(
				meta.getStreamId(),
				day.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
				Aggregation.Month, null));
	}

	@Test
	public void processStaleHourly() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-hour-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.Hour, Instant.now());
		insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

		// THEN

		// should have stored rollup in datum_hourly_count column of aud_datm_daily table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Day);
		assertThat("Hour rollup result stored database", result, hasSize(1));

		AuditDatumEntity expected = AuditDatumEntity.dailyAuditDatum(meta.getStreamId(), day.toInstant(),
				null, 8L, null, null, null);
		assertAuditDatumId("Daily audit rollup", result.get(0), expected);
		assertThat("Hourly datum count", result.get(0).getDatumHourlyCount(),
				equalTo(expected.getDatumHourlyCount()));

		// should have deleted stale Hour and inserted stale Month
		List<StaleAuditDatum> staleRows = listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for Month rollup level", staleRows, hasSize(1));
		assertStaleAuditDatum("Stale Month rollup created", staleRows.get(0), new StaleAuditDatumEntity(
				meta.getStreamId(),
				day.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
				Aggregation.Month, null));
	}

	@Test
	public void processStaleDaily() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-day-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<AuditDatum> hourAuditData = insertTestAuditDatumHourly(day, meta.getStreamId());

		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.Day, Instant.now());
		insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN

		// should have stored rollup in datum_daily_pres, prop_count, datum_q_count column of aud_datm_daily table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Day);
		assertThat("Hour rollup result stored database", result, hasSize(1));

		AuditDatumEntity expected = AuditDatumEntity.dailyAuditDatum(meta.getStreamId(), day.toInstant(),
				null, null, 1, hourAuditData.stream().mapToLong(AuditDatum::getDatumPropertyCount).sum(),
				hourAuditData.stream().mapToLong(AuditDatum::getDatumQueryCount).sum());
		assertAuditDatumId("Daily audit rollup", result.get(0), expected);
		assertThat("Daily datum count", result.get(0).getDatumDailyCount(),
				equalTo(expected.getDatumDailyCount()));
		assertThat("Daily datum property count", result.get(0).getDatumPropertyCount(),
				equalTo(expected.getDatumPropertyCount()));
		assertThat("Daily datum query count", result.get(0).getDatumQueryCount(),
				equalTo(expected.getDatumQueryCount()));

		// should have deleted stale Day and inserted stale Month
		List<StaleAuditDatum> staleRows = listStaleAuditDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for Month rollup level", staleRows, hasSize(1));
		assertStaleAuditDatum("Stale Month rollup created", staleRows.get(0), new StaleAuditDatumEntity(
				meta.getStreamId(),
				day.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
				Aggregation.Month, null));
	}

	@Test
	public void processStaleMonthly() throws IOException {
		// GIVEN
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-month-datum-01.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<AuditDatum> dayAuditDatum = insertTestAuditDatumDaily(day, meta.getStreamId());
		List<AuditDatum> monthAuditDatum = insertTestAuditDatumMonthly(
				day.with(TemporalAdjusters.firstDayOfMonth()).plusMonths(1), meta.getStreamId());

		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.Month, Instant.now());
		insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Month));

		// THEN

		// should have stored rollup in datum_count, datum_hourly_count, datum_daily_count, datum_monthly_pres,
		// prop_count, datum_q_count column of aud_datm_monthly table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Month);
		assertThat("Hour rollup result stored database (with existing Month data)", result, hasSize(9));

		AuditDatumEntity expectedMonth = AuditDatumEntity.monthlyAuditDatum(meta.getStreamId(),
				day.toInstant(), dayAuditDatum.stream().mapToLong(AuditDatum::getDatumCount).sum(),
				dayAuditDatum.stream().mapToLong(AuditDatum::getDatumHourlyCount).sum(),
				dayAuditDatum.stream().mapToInt(AuditDatum::getDatumDailyCount).sum(), 1,
				dayAuditDatum.stream().mapToLong(AuditDatum::getDatumPropertyCount).sum(),
				dayAuditDatum.stream().mapToLong(AuditDatum::getDatumQueryCount).sum());
		assertAuditDatumId("Daily audit rollup", result.get(0), expectedMonth);
		assertThat("Month datum count", result.get(0).getDatumCount(),
				equalTo(expectedMonth.getDatumCount()));
		assertThat("Month hourly datum count", result.get(0).getDatumHourlyCount(),
				equalTo(expectedMonth.getDatumHourlyCount()));
		assertThat("Month daily datum count", result.get(0).getDatumDailyCount(),
				equalTo(expectedMonth.getDatumDailyCount()));
		assertThat("Month monthly datum count", result.get(0).getDatumMonthlyCount(),
				equalTo(expectedMonth.getDatumMonthlyCount()));
		assertThat("Month datum property count", result.get(0).getDatumPropertyCount(),
				equalTo(expectedMonth.getDatumPropertyCount()));
		assertThat("Month datum query count", result.get(0).getDatumQueryCount(),
				equalTo(expectedMonth.getDatumQueryCount()));

		// should have deleted stale Month
		List<StaleAuditDatum> staleRows = listStaleAuditDatum(jdbcTemplate);
		assertThat("No stale aggregate records remain", staleRows, hasSize(0));

		// should have calculated accumulative audit data
		List<AuditDatumEntity> resultAcc = auditDatum(Aggregation.RunningTotal);

		ZonedDateTime today = ZonedDateTime.now(day.getZone()).truncatedTo(ChronoUnit.DAYS);
		AuditDatumEntity expectedAcc = AuditDatumEntity.accumulativeAuditDatum(meta.getStreamId(),
				today.toInstant(),
				expectedMonth.getDatumCount()
						+ monthAuditDatum.stream().mapToLong(AuditDatum::getDatumCount).sum(),
				expectedMonth.getDatumHourlyCount()
						+ monthAuditDatum.stream().mapToLong(AuditDatum::getDatumHourlyCount).sum(),
				expectedMonth.getDatumDailyCount()
						+ monthAuditDatum.stream().mapToInt(AuditDatum::getDatumDailyCount).sum(),
				expectedMonth.getDatumMonthlyCount()
						+ monthAuditDatum.stream().mapToInt(AuditDatum::getDatumMonthlyCount).sum());

		assertThat("Accumulative rollup result stored database", resultAcc, hasSize(1));
		assertAuditDatumId("Accumulative audit rollup", resultAcc.get(0), expectedAcc);
		assertThat("Accumulative datum count", resultAcc.get(0).getDatumCount(),
				equalTo(expectedAcc.getDatumCount()));
		assertThat("Accumulative hourly datum count", resultAcc.get(0).getDatumHourlyCount(),
				equalTo(expectedAcc.getDatumHourlyCount()));
		assertThat("Accumulative daily datum count", resultAcc.get(0).getDatumDailyCount(),
				equalTo(expectedAcc.getDatumDailyCount()));
		assertThat("Accumulative monthly datum count", resultAcc.get(0).getDatumMonthlyCount(),
				equalTo(expectedAcc.getDatumMonthlyCount()));
	}
}
