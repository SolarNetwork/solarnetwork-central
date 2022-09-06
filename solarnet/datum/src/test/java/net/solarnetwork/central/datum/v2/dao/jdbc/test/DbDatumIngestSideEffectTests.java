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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static net.solarnetwork.central.datum.v2.dao.AuditDatumEntity.ioAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.ingestDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.ingestDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumAuxiliaryResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.moveDatumAuxiliary;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertStaleAggregateDatum;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test DB functions for datum ingest, that populate "stale" records and update
 * audit records.
 * 
 * @author matt
 * @version 1.1
 */
public class DbDatumIngestSideEffectTests extends BaseDatumJdbcTestSupport {

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

	private List<GeneralNodeDatumAuxiliary> loadAuxJson(String resource) throws IOException {
		List<GeneralNodeDatumAuxiliary> datums = loadJsonDatumAuxiliaryResource(resource, getClass());
		log.debug("Got test data: {}", datums);
		return datums;
	}

	@Test
	public void firstDatum_onHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt", 0, 1);
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for lone datm", auditHourly, hasSize(1));
		assertAuditDatum("1 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 3L, 0L, 0L));
	}

	@Test
	public void firstDatum_midHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 1, 2);
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for lone datm", auditHourly, hasSize(1));
		assertAuditDatum("1 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 3L, 0L, 0L));
	}

	@Test
	public void twoDatum_midHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 1, 3);
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Two datum in same hour", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour.toInstant(), Aggregation.Hour,
						null));

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for two datum in same hour", auditHourly, hasSize(1));
		assertAuditDatum("2 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 2L, 6L, 0L, 0L));
	}

	@Test
	public void twoDatum_updateFirstDatum() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 1, 2);
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate row for datum hour
		final ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record created for lone datm", staleRows, hasSize(1));
		assertStaleAggregateDatum("One datum in hour", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for one datum", auditHourly, hasSize(1));
		assertAuditDatum("1 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 3L, 0L, 0L));

		// WHEN
		// re-store the same datum, but with one new property added
		GeneralNodeDatum updated = datums.get(0);
		updated.getSamples().putAccumulatingSampleValue("just.one.more.after.dinner.mint", 1);
		ingestDatumStream(log, jdbcTemplate, Collections.singleton(updated), "UTC");

		// THEN
		assertThat("One stale aggregate record created for re-ingested datum", staleRows, hasSize(1));
		assertStaleAggregateDatum("Same datum ingested twice produces same salt agg record",
				staleRows.get(0), new StaleAggregateDatumEntity(meta.getStreamId(), hour.toInstant(),
						Aggregation.Hour, null));

		auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Same datum ingested twice updates audit IO data", auditHourly, hasSize(1));
		assertAuditDatum("1 datum re-ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 2L, 7L, 0L, 4L));

	}

	@Test
	public void twoDatum_crossHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 0, 2);
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate rows for 2 datum hours
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Two stale aggregate records created for two datm in adjacent hours", staleRows,
				hasSize(2));

		ZonedDateTime hour1 = ZonedDateTime.of(2020, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("First datum in first hour", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.toInstant(), Aggregation.Hour,
						null));
		assertStaleAggregateDatum("Second datum in second hour", staleRows.get(1),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.plusHours(1).toInstant(),
						Aggregation.Hour, null));

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for two datum", auditHourly, hasSize(1));
		assertAuditDatum("2 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 2L, 6L, 0L, 0L));
	}

	@Test
	public void manyDatum_crossHours() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have inserted "stale" aggregate rows for 3 datum hours
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
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

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("One hourly audit record created for all datum", auditHourly, hasSize(1));
		assertAuditDatum("8 datum ingested", auditHourly.get(0), AuditDatumEntity
				.ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 8L, 24L, 0L, 0L));
	}

	@Test
	public void reset_atHourStart() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-15.txt");
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have no "stale" aggregate rows yet
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate records created yet", staleRows, hasSize(0));

		List<GeneralNodeDatumAuxiliary> auxDatums = loadAuxJson("test-datum-15.txt");
		ingestDatumAuxiliary(log, jdbcTemplate, meta.getStreamId(), auxDatums);

		staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale aggregate records created for reset", staleRows, hasSize(2));

		ZonedDateTime hour1 = ZonedDateTime.of(2020, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Reset in hour before", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour1.toInstant(), Aggregation.Hour,
						null));
		assertStaleAggregateDatum("Reset in hour", staleRows.get(1), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour1.plusHours(1).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void reset_withinHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-16.txt");
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have no "stale" aggregate rows yet
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate records created yet", staleRows, hasSize(0));

		List<GeneralNodeDatumAuxiliary> auxDatums = loadAuxJson("test-datum-16.txt");
		ingestDatumAuxiliary(log, jdbcTemplate, meta.getStreamId(), auxDatums);

		staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale aggregate record created for reset", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Reset in hour", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void reset_atHourEnd() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-17.txt");
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// should have no "stale" aggregate rows yet
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate records created yet", staleRows, hasSize(0));

		List<GeneralNodeDatumAuxiliary> auxDatums = loadAuxJson("test-datum-17.txt");
		ingestDatumAuxiliary(log, jdbcTemplate, meta.getStreamId(), auxDatums);

		staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale aggregate records created for reset", staleRows, hasSize(2));

		ZonedDateTime hour1 = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Reset in hour", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour1.toInstant(), Aggregation.Hour, null));
		assertStaleAggregateDatum("Reset in hour after", staleRows.get(1), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour1.plusHours(1).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void reset_moveWithinHour() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-16.txt");
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<GeneralNodeDatumAuxiliary> auxDatums = loadAuxJson("test-datum-16.txt");
		insertDatumAuxiliary(log, jdbcTemplate, meta.getStreamId(), auxDatums);

		// should have no "stale" aggregate rows yet
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate records created yet", staleRows, hasSize(0));

		// now move reset
		GeneralNodeDatumAuxiliary fromAux = auxDatums.get(0);
		DatumSamples f = new DatumSamples();
		f.putAccumulatingSampleValue("w", new BigDecimal("116"));
		DatumSamples s = new DatumSamples();
		s.putAccumulatingSampleValue("w", new BigDecimal("6"));

		DatumAuxiliaryPK from = new DatumAuxiliaryPK(meta.getStreamId(), fromAux.getCreated(),
				DatumAuxiliaryType.Reset);
		DatumAuxiliary to = new DatumAuxiliaryEntity(from.getStreamId(),
				from.getTimestamp().plusSeconds(60), from.getKind(), null, f, s, fromAux.getNotes(),
				fromAux.getMeta());

		boolean moved = moveDatumAuxiliary(log, jdbcTemplate, from, to);
		assertThat("Reset record moved", moved, equalTo(true));

		staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale aggregate record created for reset", staleRows, hasSize(1));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Reset in hour", staleRows.get(0), new StaleAggregateDatumEntity(
				meta.getStreamId(), hour.toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void reset_moveNodeId() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-16.txt");
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<GeneralNodeDatumAuxiliary> auxDatums = loadAuxJson("test-datum-16.txt");
		insertDatumAuxiliary(log, jdbcTemplate, meta.getStreamId(), auxDatums);

		List<GeneralNodeDatum> datums2 = datums.stream().map(e -> {
			GeneralNodeDatum clone = e.clone();
			clone.setNodeId(2L);
			return clone;
		}).collect(Collectors.toList());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas2 = insertDatumStream(log, jdbcTemplate,
				datums2, "UTC");
		ObjectDatumStreamMetadata meta2 = metas2.values().iterator().next();

		// should have no "stale" aggregate rows yet
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate records created yet", staleRows, hasSize(0));

		// now move reset
		GeneralNodeDatumAuxiliary fromAux = auxDatums.get(0);
		DatumAuxiliaryPK from = new DatumAuxiliaryPK(meta.getStreamId(), fromAux.getCreated(),
				DatumAuxiliaryType.Reset);
		DatumAuxiliary to = new DatumAuxiliaryEntity(meta2.getStreamId(), from.getTimestamp(),
				from.getKind(), null, fromAux.getSamplesFinal(), fromAux.getSamplesStart(),
				fromAux.getNotes(), fromAux.getMeta());

		boolean moved = moveDatumAuxiliary(log, jdbcTemplate, from, to);
		assertThat("Reset record moved", moved, equalTo(true));

		List<DatumAuxiliary> auxList = listDatumAuxiliary(jdbcTemplate);
		log.debug("Datum auxiliary data: {}", auxList);

		staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale aggregate records created for reset", staleRows, hasSize(2));

		Map<UUID, StaleAggregateDatum> streamToStale = staleRows.stream()
				.collect(Collectors.toMap(StaleAggregateDatum::getStreamId, Function.identity()));

		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		assertStaleAggregateDatum("Reset in hour node 1", streamToStale.get(meta.getStreamId()),
				new StaleAggregateDatumEntity(meta.getStreamId(), hour.toInstant(), Aggregation.Hour,
						null));
		assertStaleAggregateDatum("Reset in hour node 2", streamToStale.get(meta2.getStreamId()),
				new StaleAggregateDatumEntity(meta2.getStreamId(), hour.toInstant(), Aggregation.Hour,
						null));
	}

	@Test
	public void audit_oneInstantaneous() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-09.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatum("Audit record counted instantaneous prop", auditHourly.get(0),
				ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 1L, 0L, 0L));
	}

	@Test
	public void audit_oneAccumulating() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-10.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatum("Audit record counted accumulating prop", auditHourly.get(0),
				ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 1L, 0L, 0L));
	}

	@Test
	public void audit_oneStatus() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-11.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatum("Audit record counted status prop", auditHourly.get(0),
				ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 1L, 0L, 0L));
	}

	@Test
	public void audit_oneInstantaneousWithTags() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-13.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatum("Audit record counted instantaneous prop + 2 tags", auditHourly.get(0),
				ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 1L, 3L, 0L, 0L));
	}

	@Test
	public void audit_inconsistentProperties() throws IOException {
		List<GeneralNodeDatum> datums = loadJson("test-datum-14.txt");
		Instant now = Instant.now();
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		List<AuditDatum> auditHourly = listAuditDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("1 hourly audit record created for datum", auditHourly, hasSize(1));
		assertAuditDatum("Audit record counted all props and tags", auditHourly.get(0),
				ioAuditDatum(meta.getStreamId(), now.truncatedTo(ChronoUnit.HOURS), 6L, 19L, 0L, 0L));
	}
}
