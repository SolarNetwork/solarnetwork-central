/* ==================================================================
 * DbProcessStaleAggregateDatumTests.java - 7/11/2020 7:00:42 am
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

import static java.lang.String.valueOf;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.ingestDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAuditDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleFluxDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.processStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for DB stored procedures that process stale aggregate datum.
 * 
 * @author matt
 * @version 1.0
 */
public class DbProcessStaleAggregateDatumTests extends BaseDatumJdbcTestSupport {

	protected TransactionTemplate txTemplate;

	private List<GeneralNodeDatum> loadJson(String resource, int from, int to) throws IOException {
		List<GeneralNodeDatum> datums = loadJsonDatumResource(resource, getClass()).subList(from, to);
		log.debug("Got test data: {}", datums);
		return datums;
	}

	private static void assertStaleAggregateDatum(String prefix, StaleAggregateDatum stale,
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

	private List<AggregateDatum> aggDatum(Aggregation kind) {
		List<AggregateDatum> result = listAggregateDatum(jdbcTemplate, kind);
		log.debug("Got {} data:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private List<StaleFluxDatum> staleFluxDatum(Aggregation kind) {
		List<StaleFluxDatum> result = listStaleFluxDatum(jdbcTemplate, kind);
		log.debug("Got {} stale flux datum:\n{}", kind,
				result.stream().map(Object::toString).collect(Collectors.joining("\n")));
		return result;
	}

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", ObjectDatumKind.Node, nodeId,
				sourceId, new String[] { "x", "y", "z" }, new String[] { "w", "ww" },
				new String[] { "st" });
	}

	private ObjectDatumStreamMetadata loadStream(String resource, Aggregation kind) throws IOException {
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource(resource, getClass(),
				staticProvider(singleton(meta)));
		log.debug("Got test data: {}", datums);
		insertAggregateDatum(log, jdbcTemplate, datums);
		return meta;
	}

	@Before
	public void setup() {
		txTemplate = new TransactionTemplate(txManager);
	}

	@Test
	public void processStaleHour() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-01.txt", 0, 6);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

		// THEN

		// should have stored rollup in Hour table
		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<AggregateDatum> result = aggDatum(Aggregation.Hour);
		assertThat("Hour rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Hour rollup", result.get(0), new AggregateDatumEntity(meta.getStreamId(),
				hour.toInstant(), Aggregation.Hour, null, null));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for next rollup level", staleRows, hasSize(1));
		assertStaleAggregateDatum("Day rollup created", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(),
						hour.truncatedTo(ChronoUnit.DAYS).toInstant(), Aggregation.Day, null));

		// should not have created stale SolarFlux Hour because not current hour
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Hour);
		assertThat("No stale flux record created for time in past", staleFluxRows, hasSize(0));
	}

	private static final String SQL_LOCK_STALE_ROW = "SELECT * FROM solardatm.agg_stale_datm "
			+ "WHERE agg_kind = ? AND stream_id = ?::uuid AND ts_start = ? FOR UPDATE";

	@Test
	public void processStaleHour_lockedRow() throws Exception {
		TestTransaction.end();
		try {
			processStaleHour_lockedRow_impl();
		} finally {
			DatumTestUtils.cleanupDatabase(jdbcTemplate);
		}
	}

	private void processStaleHour_lockedRow_impl() throws Exception {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJson("test-datum-02.txt", 0, 6);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> metas = ingestDatumStream(log, jdbcTemplate, datums,
				"UTC");
		ObjectDatumStreamMetadata meta = metas.values().iterator().next();

		// pick a single stale row to lock in a different thread; this row should be skipped
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		final StaleAggregateDatum oneStaleRow = staleRows.iterator().next();

		// latch for row lock thread to indicate it has locked the row and the main thread can continue
		final CountDownLatch lockedLatch = new CountDownLatch(1);

		// list to capture exception thrown by row lock thread
		final List<Exception> threadExceptions = new ArrayList<Exception>(1);

		// object monitor for main thread to signal to row lock thread to complete
		final Object lockThreadSignal = new Object();

		// lock a stale row
		Thread lockThread = new Thread(new Runnable() {

			@Override
			public void run() {
				txTemplate.execute(new TransactionCallback<Object>() {

					@Override
					public Object doInTransaction(TransactionStatus status) {
						try {
							Map<String, Object> row = jdbcTemplate.queryForMap(SQL_LOCK_STALE_ROW, 'h',
									oneStaleRow.getStreamId(),
									Timestamp.from(oneStaleRow.getTimestamp()));

							log.debug("Locked stale row {}", row);

							lockedLatch.countDown();

							// wait
							try {
								synchronized ( lockThreadSignal ) {
									lockThreadSignal.wait();
								}
							} catch ( InterruptedException e ) {
								log.error("StaleRowLockingThread interrupted waiting", e);
							}
						} catch ( RuntimeException e ) {
							threadExceptions.add(e);
							throw e;
						}
						return null;
					}

				});
			}

		}, "StaleRowLockingThread");
		lockThread.setDaemon(true);
		lockThread.start();

		// wait for our latch
		boolean locked = lockedLatch.await(5, TimeUnit.SECONDS);
		if ( !threadExceptions.isEmpty() ) {
			throw threadExceptions.get(0);
		}
		assertThat("Stale row locked", locked, equalTo(true));

		// WHEN
		try {
			processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));
		} finally {
			synchronized ( lockThreadSignal ) {
				lockThreadSignal.notifyAll();
			}
		}

		// wait for the lock thread to complete
		lockThread.join(5000);

		// THEN

		// should have stored rollup in Hour table
		ZonedDateTime hour = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<AggregateDatum> result = aggDatum(Aggregation.Hour);
		assertThat("Hour rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Hour rollup", result.get(0), new AggregateDatumEntity(meta.getStreamId(),
				hour.toInstant(), Aggregation.Hour, null, null));

		staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Only locked row remains", staleRows, hasSize(1));
		assertThat("Locked stale row remains", staleRows.get(0), equalTo(oneStaleRow));

		// should have deleted stale Hour and inserted stale Day
		staleRows = listStaleAggregateDatum(jdbcTemplate, Aggregation.Day);
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
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime hour = now.truncatedTo(ChronoUnit.HOURS);
		DatumEntity d = new DatumEntity(UUID.randomUUID(), now.toInstant(), Instant.now(),
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null));
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(d.getStreamId(), "UTC",
				ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		DatumDbUtils.insertDatum(log, jdbcTemplate, Collections.singleton(d));
		insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(d.getStreamId(),
						hour.toInstant(), Aggregation.Hour, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

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
		ObjectDatumStreamMetadata meta = loadStream("test-agg-hour-datum-01.txt", Aggregation.Hour);
		ZonedDateTime day = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						day.toInstant(), Aggregation.Day, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN

		// should have stored rollup in Day table
		List<AggregateDatum> result = aggDatum(Aggregation.Day);
		assertThat("Day rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Day rollup", result.get(0), new AggregateDatumEntity(meta.getStreamId(),
				day.toInstant(), Aggregation.Day, null, null));

		// should have deleted stale Hour and inserted stale Day
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("One stale aggregate record remains for next rollup level", staleRows, hasSize(1));
		assertStaleAggregateDatum("Month rollup created", staleRows.get(0),
				new StaleAggregateDatumEntity(meta.getStreamId(),
						day.with(firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant(),
						Aggregation.Month, null));

		// should have inserted None, Hour, and Day stale audit records
		List<StaleAuditDatum> staleAuditRows = listStaleAuditDatum(jdbcTemplate);
		Set<Aggregation> staleAuditKinds = staleAuditRows.stream().map(StaleAuditDatum::getKind)
				.collect(Collectors.toSet());
		assertThat("Raw, Hour, and Day stale audit rows created when process stale Hour agg",
				staleAuditKinds,
				containsInAnyOrder(Aggregation.None, Aggregation.Hour, Aggregation.Day));
		for ( StaleAuditDatum a : staleAuditRows ) {
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
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime day = now.truncatedTo(ChronoUnit.DAYS);
		AggregateDatumEntity agg = new AggregateDatumEntity(UUID.randomUUID(), day.toInstant(),
				Aggregation.Hour,
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null),
				DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] { decimalArray("100") },
						null));
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(agg.getStreamId(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		insertAggregateDatum(log, jdbcTemplate, Collections.singleton(agg));
		insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(agg.getStreamId(),
						day.toInstant(), Aggregation.Day, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

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
		ObjectDatumStreamMetadata meta = loadStream("test-agg-day-datum-01.txt", Aggregation.Day);
		ZonedDateTime month = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						month.toInstant(), Aggregation.Month, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Month));

		// THEN

		// should have stored rollup in Month table
		List<AggregateDatum> result = aggDatum(Aggregation.Month);
		assertThat("Month rollup result stored database", result, hasSize(1));
		assertAggregateDatumId("Month rollup", result.get(0), new AggregateDatumEntity(
				meta.getStreamId(), month.toInstant(), Aggregation.Month, null, null));

		// should have deleted stale Month
		List<StaleAggregateDatum> staleRows = listStaleAggregateDatum(jdbcTemplate);
		assertThat("No stale aggregate record remains because no more rollup levels", staleRows,
				hasSize(0));

		// should have inserted Month stale audit records
		List<StaleAuditDatum> staleAuditRows = listStaleAuditDatum(jdbcTemplate);
		Set<Aggregation> staleAuditKinds = staleAuditRows.stream().map(StaleAuditDatum::getKind)
				.collect(Collectors.toSet());
		assertThat("Raw, Hour, and Day stale audit rows created when process stale Hour agg",
				staleAuditKinds, containsInAnyOrder(Aggregation.Month));
		for ( StaleAuditDatum a : staleAuditRows ) {
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
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime month = now.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS);
		AggregateDatumEntity agg = new AggregateDatumEntity(UUID.randomUUID(), month.toInstant(),
				Aggregation.Day,
				DatumProperties.propertiesOf(decimalArray("1.1"), decimalArray("2.1"), null, null),
				DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] { decimalArray("100") },
						null));
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(agg.getStreamId(),
				"UTC", ObjectDatumKind.Node, 1L, "a");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		insertAggregateDatum(log, jdbcTemplate, Collections.singleton(agg));
		insertStaleAggregateDatum(log, jdbcTemplate,
				singleton((StaleAggregateDatum) new StaleAggregateDatumEntity(agg.getStreamId(),
						month.toInstant(), Aggregation.Month, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Month));

		// THEN

		// should have created stale SolarFlux Hour because current day
		List<StaleFluxDatum> staleFluxRows = staleFluxDatum(Aggregation.Month);
		assertThat("One stale flux record created", staleFluxRows, hasSize(1));
		assertThat("Stale flux for same stream", staleFluxRows.get(0).getStreamId(),
				equalTo(agg.getStreamId()));
	}

	@Test
	public void processStaleDay_dstStart() {
		// GIVEN
		ZoneId zone = ZoneId.of("America/New_York");
		setupTestLocation(1L, zone.getId());
		setupTestNode(1L, 1L);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				zone.getId(), ObjectDatumKind.Node, 1L, "a", null, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta));

		// populate 100Wh hourly data from D-1 to D+1, where D is a DST boundary day
		final ZonedDateTime start = ZonedDateTime.of(2019, 3, 9, 0, 0, 0, 0, zone);
		final ZonedDateTime end = ZonedDateTime.of(2019, 3, 12, 0, 0, 0, 0, zone);

		List<AggregateDatum> datums = new ArrayList<>();
		ZonedDateTime date = start.withZoneSameInstant(ZoneOffset.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			DatumProperties props = propertiesOf(null, new BigDecimal[] { new BigDecimal(wh) }, null,
					null);
			DatumPropertiesStatistics stats = statisticsOf(null,
					new BigDecimal[][] { decimalArray(valueOf(100), valueOf(wh), valueOf(wh + 100)) });
			datums.add(new AggregateDatumEntity(meta.getStreamId(), date.toInstant(), Aggregation.Hour,
					props, stats));
			date = date.plusHours(1);
			wh += 100L;
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		insertStaleAggregateDatum(log, jdbcTemplate,
				asList((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						start.toInstant(), Aggregation.Day, Instant.now()),
						new StaleAggregateDatumEntity(meta.getStreamId(), start.plusDays(1).toInstant(),
								Aggregation.Day, Instant.now()),
						new StaleAggregateDatumEntity(meta.getStreamId(), start.plusDays(2).toInstant(),
								Aggregation.Day, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN
		List<AggregateDatum> daily = aggDatum(Aggregation.Day);
		assertThat("Daily agg found", daily, hasSize(3));

		assertThat("Agg pre DST day data_a", daily.get(0).getProperties().getAccumulating(),
				arrayContaining(decimalArray("27600")));
		assertThat("Agg pre DST day read_a", daily.get(0).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2400", "0", "2400")));

		assertThat("Agg DST day data_a", daily.get(1).getProperties().getAccumulating(),
				arrayContaining(decimalArray("80500")));
		assertThat("Agg DST day read_a", daily.get(1).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2300", "2400", "4700")));

		assertThat("Agg post DST day data_a", daily.get(2).getProperties().getAccumulating(),
				arrayContaining(decimalArray("140400")));
		assertThat("Agg post DST day read_a", daily.get(2).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2400", "4700", "7100")));
	}

	@Test
	public void processStaleDay_dstEnd() {
		// GIVEN
		ZoneId zone = ZoneId.of("America/New_York");
		setupTestLocation(1L, zone.getId());
		setupTestNode(1L, 1L);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				zone.getId(), ObjectDatumKind.Node, 1L, "a", null, new String[] { "wh" }, null, null);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta));

		// populate 100Wh hourly data from D-1 to D+1, where D is a DST boundary day
		final ZonedDateTime start = ZonedDateTime.of(2018, 11, 3, 0, 0, 0, 0, zone);
		final ZonedDateTime end = ZonedDateTime.of(2018, 11, 6, 0, 0, 0, 0, zone);

		List<AggregateDatum> datums = new ArrayList<>();
		ZonedDateTime date = start.withZoneSameInstant(ZoneOffset.UTC);
		long wh = 0;
		while ( !date.isAfter(end) ) {
			DatumProperties props = propertiesOf(null, new BigDecimal[] { new BigDecimal(wh) }, null,
					null);
			DatumPropertiesStatistics stats = statisticsOf(null,
					new BigDecimal[][] { decimalArray(valueOf(100), valueOf(wh), valueOf(wh + 100)) });
			datums.add(new AggregateDatumEntity(meta.getStreamId(), date.toInstant(), Aggregation.Hour,
					props, stats));
			date = date.plusHours(1);
			wh += 100L;
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);
		insertStaleAggregateDatum(log, jdbcTemplate,
				asList((StaleAggregateDatum) new StaleAggregateDatumEntity(meta.getStreamId(),
						start.toInstant(), Aggregation.Day, Instant.now()),
						new StaleAggregateDatumEntity(meta.getStreamId(), start.plusDays(1).toInstant(),
								Aggregation.Day, Instant.now()),
						new StaleAggregateDatumEntity(meta.getStreamId(), start.plusDays(2).toInstant(),
								Aggregation.Day, Instant.now())));

		// WHEN
		processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Day));

		// THEN
		List<AggregateDatum> daily = aggDatum(Aggregation.Day);
		assertThat("Daily agg found", daily, hasSize(3));

		assertThat("Agg pre DST day data_a", daily.get(0).getProperties().getAccumulating(),
				arrayContaining(decimalArray("27600")));
		assertThat("Agg pre DST day read_a", daily.get(0).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2400", "0", "2400")));

		assertThat("Agg DST day data_a", daily.get(1).getProperties().getAccumulating(),
				arrayContaining(decimalArray("90000")));
		assertThat("Agg DST day read_a", daily.get(1).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2500", "2400", "4900")));

		assertThat("Agg post DST day data_a", daily.get(2).getProperties().getAccumulating(),
				arrayContaining(decimalArray("145200")));
		assertThat("Agg post DST day read_a", daily.get(2).getStatistics().getAccumulating()[0],
				arrayContaining(decimalArray("2400", "4900", "7300")));
	}

}
