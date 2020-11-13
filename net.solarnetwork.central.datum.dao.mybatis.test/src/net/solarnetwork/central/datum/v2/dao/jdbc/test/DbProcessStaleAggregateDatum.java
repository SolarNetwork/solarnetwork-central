/* ==================================================================
 * DbProcessStaleAggregateDatum.java - 7/11/2020 7:00:42 am
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

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.decimalArray;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.ingestDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for DB stored procedures that process stale aggregate datum.
 * 
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAggregateDatum extends BaseDatumJdbcTestSupport {

	private List<GeneralNodeDatum> loadJson(String resource, int from, int to) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass()).subList(from, to);
		log.debug("Got test data: {}", datums);
		return datums;
	}

	private static void assertStaleAggregateDatum(String prefix, StaleAggregateDatumEntity stale,
			StaleAggregateDatumEntity expected) {
		assertThat(prefix + " stale aggregate record kind", stale.getKind(),
				equalTo(expected.getKind()));
		assertThat(prefix + "stale aggregate record stream ID", stale.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale aggregate record timestamp", stale.getTimestamp(),
				equalTo(expected.getTimestamp()));
	}

	private static void assertAggregateDatumId(String prefix, AggregateDatum datum,
			AggregateDatum expected) {
		assertThat(prefix + " aggregate record stream ID", datum.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " aggregate record timestamp", datum.getTimestamp(),
				equalTo(expected.getTimestamp()));
		assertThat(prefix + " aggregate record kind", datum.getAggregation(),
				equalTo(expected.getAggregation()));
	}

	private List<AggregateDatumEntity> aggDatum(Aggregation kind) {
		List<AggregateDatumEntity> result = DatumTestUtils.listAggregateDatum(jdbcTemplate, kind);
		log.debug("Got {} data:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private List<StaleFluxDatum> staleFluxDatum(Aggregation kind) {
		List<StaleFluxDatum> result = DatumTestUtils.listStaleFluxDatum(jdbcTemplate, kind);
		log.debug("Got {} stale flux datum:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private BasicNodeDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), nodeId, sourceId,
				new String[] { "x", "y", "z" }, new String[] { "w", "ww" }, new String[] { "st" });
	}

	private BasicNodeDatumStreamMetadata loadStream(String resource, Aggregation kind)
			throws IOException {
		BasicNodeDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = DatumTestUtils.loadJsonAggregateDatumResource(resource, getClass(),
				staticProvider(singleton(meta)));
		log.debug("Got test data: {}", datums);
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		return meta;
	}

	@Test
	public void processStaleHour() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt", 0, 6);
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

		// THEN

		// should have stored rollup in Hour table
		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<AggregateDatumEntity> result = aggDatum(Aggregation.Hour);
		assertThat("Hour rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Hour rollup", result.get(0), new AggregateDatumEntity(meta.getStreamId(),
				hour.toInstant(), Aggregation.Hour, null, null));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAggregateDatumEntity> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for next rollup level", staleRows, hasSize(1));
		assertStaleAggregateDatum("Day rollup created", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(),
						hour.truncatedTo(ChronoUnit.DAYS).toInstant(), Aggregation.Day, null));

		// should not have created stale SolarFlux Hour because not current hour
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Hour);
		assertThat("No stale flux record created for time in past", staleFluxRows, hasSize(0));
	}

	@Test
	public void processStaleHour_currDayToFlux() throws IOException {
		// GIVEN
		ZonedDateTime hour = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
		DatumEntity d = new DatumEntity(UUID.randomUUID(), hour.toInstant(), Instant.now(),
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null));
		DatumTestUtils.insertDatum(log, jdbcTemplate, Collections.singleton(d));
		DatumTestUtils.insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(d.getStreamId(),
						hour.toInstant(), Aggregation.Hour, Instant.now())));

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

		// THEN

		// should have created stale SolarFlux Hour because current hour
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Hour);
		assertThat("One stale flux record created", staleFluxRows, hasSize(1));
		assertThat("Stale flux for same stream", staleFluxRows.get(0).getStreamId(),
				equalTo(d.getStreamId()));
	}

	@Test
	public void processStaleDay() throws IOException {
		// GIVEN
		NodeDatumStreamMetadata meta = loadStream("test-agg-hour-datum-01.txt", Aggregation.Hour);
		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		DatumTestUtils.insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						day.toInstant(), Aggregation.Day, Instant.now())));

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN

		// should have stored rollup in Day table
		List<AggregateDatumEntity> result = aggDatum(Aggregation.Day);
		assertThat("Day rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Day rollup", result.get(0), new AggregateDatumEntity(meta.getStreamId(),
				day.toInstant(), Aggregation.Day, null, null));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAggregateDatumEntity> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for next rollup level", staleRows, hasSize(1));
		assertStaleAggregateDatum("Month rollup created", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(),
						day.with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Month, null));

		// should have inserted None, Hour, and Day stale audit records
		List<StaleAuditDatumEntity> staleAuditRows = DatumTestUtils.listStaleAuditDatum(jdbcTemplate);
		Set<Aggregation> staleAuditKinds = staleAuditRows.stream().map(StaleAuditDatumEntity::getKind)
				.collect(Collectors.toSet());
		assertThat("Raw, Hour, and Day stale audit rows created when process stale Hour agg",
				staleAuditKinds,
				containsInAnyOrder(Aggregation.None, Aggregation.Hour, Aggregation.Day));
		for ( StaleAuditDatumEntity a : staleAuditRows ) {
			assertThat("Stale audit for processed stream", a.getStreamId(), equalTo(meta.getStreamId()));
			assertThat("Stale audit timestamp is start of day", a.getTimestamp(),
					equalTo(day.toInstant()));
		}

		// should not have created stale SolarFlux Day because not current day
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Day);
		assertThat("No stale flux record created for time in past", staleFluxRows, hasSize(0));
	}

	@Test
	public void processStaleDay_currDayToFlux() throws IOException {
		// GIVEN
		ZonedDateTime day = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
		AggregateDatumEntity agg = new AggregateDatumEntity(UUID.randomUUID(), day.toInstant(),
				Aggregation.Hour,
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null),
				DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] { decimalArray("100") },
						null));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, Collections.singleton(agg));
		DatumTestUtils.insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(agg.getStreamId(),
						day.toInstant(), Aggregation.Day, Instant.now())));

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN

		// should have created stale SolarFlux Hour because current day
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Day);
		assertThat("One stale flux record created", staleFluxRows, hasSize(1));
		assertThat("Stale flux for same stream", staleFluxRows.get(0).getStreamId(),
				equalTo(agg.getStreamId()));
	}

	@Test
	public void processStaleMonth() throws IOException {
		// GIVEN
		NodeDatumStreamMetadata meta = loadStream("test-agg-day-datum-01.txt", Aggregation.Day);
		ZonedDateTime month = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		DatumTestUtils.insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						month.toInstant(), Aggregation.Month, Instant.now())));

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Month));

		// THEN

		// should have stored rollup in Month table
		List<AggregateDatumEntity> result = aggDatum(Aggregation.Month);
		assertThat("Month rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Month rollup", result.get(0), new AggregateDatumEntity(
				meta.getStreamId(), month.toInstant(), Aggregation.Month, null, null));

		// should have deleted stale Month
		List<StaleAggregateDatumEntity> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate record remains because no more rollup levels", staleRows,
				hasSize(0));

		// should have inserted Month stale audit records
		List<StaleAuditDatumEntity> staleAuditRows = DatumTestUtils.listStaleAuditDatum(jdbcTemplate);
		Set<Aggregation> staleAuditKinds = staleAuditRows.stream().map(StaleAuditDatumEntity::getKind)
				.collect(Collectors.toSet());
		assertThat("Raw, Hour, and Day stale audit rows created when process stale Hour agg",
				staleAuditKinds, containsInAnyOrder(Aggregation.Month));
		for ( StaleAuditDatumEntity a : staleAuditRows ) {
			assertThat("Stale audit for processed stream", a.getStreamId(), equalTo(meta.getStreamId()));
			assertThat("Stale audit timestamp is start of day", a.getTimestamp(),
					equalTo(month.toInstant()));
		}

		// should not have created stale SolarFlux Month because not current day
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Month);
		assertThat("No stale flux record created for time in past", staleFluxRows, hasSize(0));
	}

	@Test
	public void processStaleMonth_currDayToFlux() throws IOException {
		// GIVEN
		ZonedDateTime month = ZonedDateTime.now(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);
		AggregateDatumEntity agg = new AggregateDatumEntity(UUID.randomUUID(), month.toInstant(),
				Aggregation.Day,
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null),
				DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] { decimalArray("100") },
						null));
		DatumTestUtils.insertAggregateDatum(log, jdbcTemplate, Collections.singleton(agg));
		DatumTestUtils.insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(agg.getStreamId(),
						month.toInstant(), Aggregation.Month, Instant.now())));

		// WHEN
		DatumTestUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Month));

		// THEN

		// should have created stale SolarFlux Hour because current day
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Month);
		assertThat("One stale flux record created", staleFluxRows, hasSize(1));
		assertThat("Stale flux for same stream", staleFluxRows.get(0).getStreamId(),
				equalTo(agg.getStreamId()));
	}

}
