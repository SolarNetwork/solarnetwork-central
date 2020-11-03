/* ==================================================================
 * DbDatumIngestSideEffectTests.java - 3/11/2020 10:52:40 am
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

import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.ingestDatumStream;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.mybatis.test.DatumTestUtils.staleAggregateDatumStreams;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumHourlyEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test DB functions for datum ingest, that populate "stale" records and update
 * audit records.
 * 
 * @author matt
 * @version 1.0
 */
public class DbDatumIngestSideEffectTests extends BaseDatumJdbcTestSupport {

	private static void assertStaleAggregateDatum(String prefix, StaleAggregateDatumEntity stale,
			StaleAggregateDatumEntity expected) {
		assertThat(prefix + " stale aggregate record kind", stale.getKind(),
				equalTo(expected.getKind()));
		assertThat(prefix + "stale aggregate record stream ID", stale.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale aggregate record timestamp", stale.getTimestamp(),
				equalTo(expected.getTimestamp()));
	}

	private static void assertAuditDatumHourly(String prefix, AuditDatumHourlyEntity audit,
			AuditDatumHourlyEntity expected) {
		assertThat(prefix + " hourly audit record stream ID", audit.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " hourly audit record timestamp", audit.getTimestamp(),
				equalTo(expected.getTimestamp()));
		assertThat(prefix + " hourly audit datum count", audit.getDatumCount(),
				equalTo(expected.getDatumCount()));
		assertThat(prefix + " hourly audit prop count", audit.getPropCount(),
				equalTo(expected.getPropCount()));
		assertThat(prefix + " hourly audit datum query count", audit.getDatumQueryCount(),
				equalTo(expected.getDatumQueryCount()));
	}

	private List<GeneralNodeDatum> loadJson(String resource) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		return datums;
	}

	private List<GeneralNodeDatum> loadJson(String resource, int from, int to) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass()).subList(from, to);
		log.debug("Got test data: {}", datums);
		return datums;
	}

	@Test
	public void firstDatum_onHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt", 0, 1);
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatumEntity> staleRows = staleAggregateDatumStreams(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for lone datm", auditHourly, hasSize(1));
		assertAuditDatumHourly("1 datum ingested", auditHourly.get(0), new AuditDatumHourlyEntity(
				meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 3, 0));
	}

	@Test
	public void firstDatum_midHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 1, 2);
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatumEntity> staleRows = staleAggregateDatumStreams(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for lone datm", auditHourly, hasSize(1));
		assertAuditDatumHourly("1 datum ingested", auditHourly.get(0), new AuditDatumHourlyEntity(
				meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 3, 0));
	}

	@Test
	public void twoDatum_midHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 1, 3);
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatumEntity> staleRows = staleAggregateDatumStreams(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Two datum in same hour", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour.toInstant(), Aggregation.Hour,
						null));

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for two datum in same hour", auditHourly, hasSize(1));
		assertAuditDatumHourly("2 datum ingested", auditHourly.get(0), new AuditDatumHourlyEntity(
				meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 2, 6, 0));
	}

	@Test
	public void twoDatum_crossHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 0, 2);
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate rows for 2 datum hours
		List<StaleAggregateDatumEntity> staleRows = staleAggregateDatumStreams(jdbcTemplate);
		assertThat("Two stale aggregate records created for two datm in adjacent hours", staleRows,
				hasSize(2));

		ZonedDateTime hour1 = ZonedDateTime.of(2020, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum in first hour", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.toInstant(), Aggregation.Hour,
						null));
		assertStaleAggregateDatum("Second datum in second hour", staleRows.get(1),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.plusHours(1).toInstant(),
						Aggregation.Hour, null));

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for two datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("2 datum ingested", auditHourly.get(0), new AuditDatumHourlyEntity(
				meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 2, 6, 0));
	}

	@Test
	public void manyDatum_crossHours() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate rows for 3 datum hours
		List<StaleAggregateDatumEntity> staleRows = staleAggregateDatumStreams(jdbcTemplate);
		assertThat("3 stale aggregate records created for 8 datum in adjacent hours", staleRows,
				hasSize(3));

		ZonedDateTime hour1 = ZonedDateTime.of(2020, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum in first hour", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.toInstant(), Aggregation.Hour,
						null));
		assertStaleAggregateDatum("Middle datum in second hour", staleRows.get(1),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.plusHours(1).toInstant(),
						Aggregation.Hour, null));
		assertStaleAggregateDatum("Last datum in third hour", staleRows.get(2),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.plusHours(2).toInstant(),
						Aggregation.Hour, null));

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("One hourly audit record created for all datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("8 datum ingested", auditHourly.get(0), new AuditDatumHourlyEntity(
				meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 8, 24, 0));
	}

	@Test
	public void audit_oneInstantaneous() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-09.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("Audit record counted instantaneous prop", auditHourly.get(0),
				new AuditDatumHourlyEntity(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 1,
						0));
	}

	@Test
	public void audit_oneAccumulating() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-10.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("Audit record counted accumulating prop", auditHourly.get(0),
				new AuditDatumHourlyEntity(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 1,
						0));
	}

	@Test
	public void audit_oneStatus() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-11.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("Audit record counted status prop", auditHourly.get(0),
				new AuditDatumHourlyEntity(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 1,
						0));
	}

	@Test
	public void audit_oneInstantaneousWithTags() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-13.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("Audit record counted instantaneous prop + 2 tags", auditHourly.get(0),
				new AuditDatumHourlyEntity(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1, 3,
						0));
	}

	@Test
	public void audit_inconsistentProperties() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-14.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums);
		NodeDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatumHourlyEntity> auditHourly = DatumTestUtils.auditDatumHourly(jdbcTemplate);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatumHourly("Audit record counted all props and tags", auditHourly.get(0),
				new AuditDatumHourlyEntity(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 6, 19,
						0));
	}
}
