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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
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
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), nodeId, sourceId,
				new String[] { "x", "y", "z" }, new String[] { "w", "ww" }, new String[] { "st" });
	}

	private List<GeneralNodeDatum> loadJson(String resource) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		return datums;
	}

	private List<AuditDatumEntity> auditDatum(Aggregation kind) {
		List<AuditDatumEntity> result = DatumTestUtils.auditDatum(jdbcTemplate, kind);
		log.debug("Got {} audit data:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private static void assertStaleAuditDatum(String prefix, StaleAuditDatumEntity stale,
			StaleAuditDatumEntity expected) {
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

	@Test
	public void processStaleRaw() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt");
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.None, Instant.now());
		DatumTestUtils.insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		DatumTestUtils.processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.None));

		// THEN

		// should have stored rollup in datum_count column of aud_datm_daily table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Day);
		assertThat("Hour rollup result stored database", result, hasSize(1));

		AuditDatumEntity expected = AuditDatumEntity.dailyAuditDatum(meta.getStreamId(), day.toInstant(),
				7L, null, null, null, null);
		assertAuditDatumId("Daily audit rollup", result.get(0), expected);
		assertThat("Datum count", result.get(0).getDatumCount(), equalTo(expected.getDatumCount()));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAuditDatumEntity> staleRows = DatumTestUtils.staleAuditDatum(jdbcTemplate);
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
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(
				"test-agg-hour-datum-01.txt", getClass(), staticProvider(singleton(meta)));
		log.debug("Got test data: {}", datums);
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		StaleAuditDatumEntity staleAudit = new StaleAuditDatumEntity(meta.getStreamId(), day.toInstant(),
				Aggregation.Hour, Instant.now());
		DatumTestUtils.insertStaleAuditDatum(log, jdbcTemplate, singleton(staleAudit));

		// WHEN
		DatumTestUtils.processStaleAuditDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

		// THEN

		// should have stored rollup in datum_hourly_count column of aud_datm_daily table
		List<AuditDatumEntity> result = auditDatum(Aggregation.Day);
		assertThat("Hour rollup result stored database", result, hasSize(1));

		AuditDatumEntity expected = AuditDatumEntity.dailyAuditDatum(meta.getStreamId(), day.toInstant(),
				null, 8L, null, null, null);
		assertAuditDatumId("Daily audit rollup", result.get(0), expected);
		assertThat("Hourly datum count", result.get(0).getDatumHourlyCount(),
				equalTo(expected.getDatumHourlyCount()));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAuditDatumEntity> staleRows = DatumTestUtils.staleAuditDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for Month rollup level", staleRows, hasSize(1));
		assertStaleAuditDatum("Stale Month rollup created", staleRows.get(0), new StaleAuditDatumEntity(
				meta.getStreamId(),
				day.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
				Aggregation.Month, null));
	}
}
