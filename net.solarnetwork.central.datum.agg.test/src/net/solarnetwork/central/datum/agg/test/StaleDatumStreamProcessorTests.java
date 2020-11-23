/* ==================================================================
 * StaleDatumStreamProcessorTests.java - 23/11/2020 6:57:16 am
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

package net.solarnetwork.central.datum.agg.test;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.util.JsonUtils.getJSONString;
import static net.solarnetwork.util.JsonUtils.getObjectFromJSON;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import net.solarnetwork.central.datum.agg.StaleDatumStreamProcessor;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.domain.AggregateUpdatedEventInfo;
import net.solarnetwork.central.datum.domain.DatumAppEvent;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.StaleAggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.InsertAggregateDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectDatum;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.SelectStaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.StaticOptionalServiceCollection;

/**
 * Test cases for the [@link StaleDatumStreamProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StaleDatumStreamProcessorTests extends AggTestSupport {

	private static final String TEST_JOB_ID = "Test Stale Datum Stream Processor";

	private static final String TEST_SOURCE_ID = "test.source";

	private TestStaleGeneralNodeDatumProcessor job;

	private static final class TestStaleGeneralNodeDatumProcessor extends StaleDatumStreamProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
			super(eventAdmin, jdbcOps);
			setExecutorService(Executors.newCachedThreadPool(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleDatumStreamProcessorTask-" + taskThreadCount.incrementAndGet());
				}
			}));
		}

		/**
		 * Provide way to call {@code handleJob} directly in test cases.
		 * 
		 * @return {@code true} if job completed successfully
		 * @throws Exception
		 *         if any error occurs
		 */
		private boolean executeJob() throws Exception {
			Event jobEvent = new Event(SchedulerConstants.TOPIC_JOB_REQUEST,
					Collections.singletonMap(SchedulerConstants.JOB_ID, TEST_JOB_ID));
			return handleJob(jobEvent);
		}

	}

	@Override
	@Before
	public void setup() {
		super.setup();

		job = new TestStaleGeneralNodeDatumProcessor(null, jdbcTemplate);
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumIterations(10);
		job.setAggregateProcessType("h");
		job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();

		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
	}

	private Map<NodeSourcePK, NodeDatumStreamMetadata> populateTestData(final long start,
			final int count, final long step, final Long nodeId, final String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(count);
		long ts = start;
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setCreated(new DateTime(ts));
			d.setNodeId(nodeId);
			d.setSourceId(sourceId);
			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			s.putInstantaneousSampleValue("watts", 125);
			s.putAccumulatingSampleValue("wattHours", 10);
			d.setSamples(s);
			data.add(d);
			ts += step;
		}
		return DatumDbUtils.ingestDatumStream(log, jdbcTemplate, data, TEST_TZ);
	}

	private Map<NodeSourcePK, NodeDatumStreamMetadata> populateTestData() {
		// populate 2 hours worth of 10min samples for 5 nodes
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = new LinkedHashMap<>(2);
		Instant start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).toInstant();
		for ( int i = 1; i <= 5; i++ ) {
			metas.putAll(populateTestData(start.toEpochMilli(), 11, 10 * 60 * 1000L, (long) i,
					TEST_SOURCE_ID));
		}
		return metas;
	}

	private void verifyStaleHourlyRowsAllProcessed() {
		verifyStaleHourlyRowsAllProcessed(10);
	}

	private void verifyStaleHourlyRowsAllProcessed(int expectedHourAggregates) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		List<StaleAggregateDatum> staleRows = jdbcTemplate.query(new SelectStaleAggregateDatum(filter),
				StaleAggregateDatumEntityRowMapper.INSTANCE);
		assertThat("No stale rows", staleRows, hasSize(0));

		List<AggregateDatum> hourRows = jdbcTemplate.query(new SelectDatum(filter),
				AggregateDatumEntityRowMapper.HOUR_INSTANCE);
		assertThat("Agg hour rows", hourRows, hasSize(expectedHourAggregates));
	}

	@Test
	public void runSingleTask() throws Exception {
		populateTestData();
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale hour rows (5 nodes * 2 stale hours)", staleRows, hasSize(10));
		boolean result = job.executeJob();
		assertThat("Completed", result, equalTo(true));
		verifyStaleHourlyRowsAllProcessed();
		assertThat("Thread count", job.taskThreadCount.get(), equalTo(0));
	}

	@Test
	public void runParallelTasks() throws Exception {
		job.setParallelism(4);
		populateTestData();
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale hour rows (5 nodes * 2 stale hours)", staleRows, hasSize(10));
		boolean result = job.executeJob();
		assertThat("Completed", result, equalTo(true));
		verifyStaleHourlyRowsAllProcessed();
	}

	private static final String SQL_LOCK_STALE_ROW = "SELECT * FROM solardatm.agg_stale_datm "
			+ "WHERE agg_kind = ? AND stream_id = ?::uuid AND ts_start = ? FOR UPDATE";

	@Test
	public void runParallelTasksWithLockedRow() throws Exception {
		job.setParallelism(4);
		populateTestData();

		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale hour rows (5 nodes * 2 stale hours)", staleRows, hasSize(10));

		// pick a single stale row to lock in a different thread; this row should be skipped
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
		Assert.assertTrue("Stale row locked", locked);

		try {
			boolean result = job.executeJob();
			Assert.assertTrue("Tasks completed", result);

		} finally {
			synchronized ( lockThreadSignal ) {
				lockThreadSignal.notifyAll();
			}
		}

		// wait for the lock thread to complete
		lockThread.join(5000);

		// only the previously locked stale row should be left
		staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate, Aggregation.Hour);
		assertThat("Only locked row remains", staleRows, hasSize(1));
		assertThat("Locked stale row remains", staleRows.get(0), equalTo(oneStaleRow));
	}

	private void insertAggDatum(AggregateDatumEntity datum) {
		jdbcTemplate.update(new InsertAggregateDatum(datum));
	}

	private void insertStaleAggDatum(StaleAggregateDatumEntity datum) {
		jdbcTemplate.update(
				"INSERT INTO solardatm.agg_stale_datm (ts_start,stream_id,agg_kind,created) VALUES (?,?::uuid,?,?)",
				Timestamp.from(datum.getTimestamp()), datum.getStreamId(), datum.getKind().getKey(),
				new Timestamp(System.currentTimeMillis()));

	}

	@Test
	public void processStaleDailyAggProducesDailyAggRow() throws Exception {
		ZonedDateTime start = ZonedDateTime.of(2018, 7, 24, 15, 0, 0, 0, ZoneId.of(TEST_TZ));
		DatumProperties props = DatumProperties.propertiesOf(new BigDecimal[] { new BigDecimal("10") },
				new BigDecimal[] { new BigDecimal("150") }, null, null);
		DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(new BigDecimal[][] {
				new BigDecimal[] { new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10") } },
				new BigDecimal[][] { new BigDecimal[] { new BigDecimal("0"), new BigDecimal("150"),
						new BigDecimal("150") } });
		UUID streamId = UUID.randomUUID();
		for ( int i = 0; i < 2; i++ ) {
			ZonedDateTime ts = start.plusHours(i);
			insertAggDatum(
					new AggregateDatumEntity(streamId, ts.toInstant(), Aggregation.Hour, props, stats));
		}
		insertStaleAggDatum(new StaleAggregateDatumEntity(streamId,
				start.truncatedTo(ChronoUnit.DAYS).toInstant(), Aggregation.Day, Instant.now()));

		int count = jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con
						.prepareCall("{call solardatm.process_one_agg_stale_datm(?)}");
				return stmt;
			}
		}, new CallableStatementCallback<Integer>() {

			private int processKind(String kind, CallableStatement cs) throws SQLException {
				int processed = 0;
				cs.setString(1, kind);
				while ( cs.execute() ) {
					try (ResultSet rs = cs.getResultSet()) {
						if ( rs.next() ) {
							processed++;
						} else {
							break;
						}
					}
				}
				return processed;
			}

			@Override
			public Integer doInCallableStatement(CallableStatement cs)
					throws SQLException, DataAccessException {
				int processed = processKind("d", cs);
				log.debug("Processed " + processed + " stale daily datum");
				return processed;
			}
		});
		assertThat("Processed stale row count", count, equalTo(1));

		List<AggregateDatum> rows = DatumDbUtils.listAggregateDatum(jdbcTemplate, Aggregation.Day);
		assertThat("Daily agg row count", rows.size(), equalTo(1));

		assertThat("Daily agg ts_start", rows.get(0).getTimestamp(),
				equalTo(start.truncatedTo(ChronoUnit.DAYS).toInstant()));
		assertThat("Daily agg i", rows.get(0).getProperties().getInstantaneous(),
				arrayContaining(new BigDecimal[] { new BigDecimal("10") }));
		assertThat("Daily agg a", rows.get(0).getProperties().getAccumulating(),
				arrayContaining(new BigDecimal[] { new BigDecimal("300") }));
	}

	private static void assertStaleAuditDatum(String prefix, StaleAuditDatum stale,
			StaleAuditDatum expected) {
		assertThat(prefix + " stale audit record kind", stale.getKind(), equalTo(expected.getKind()));
		assertThat(prefix + "stale audit record stream ID", stale.getStreamId(),
				equalTo(expected.getStreamId()));
		assertThat(prefix + " stale audit record timestamp", stale.getTimestamp(),
				equalTo(expected.getTimestamp()));
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

	private List<StaleAuditDatum> staleAuditDatum() {
		List<StaleAuditDatum> rows = DatumDbUtils.listStaleAuditDatum(jdbcTemplate);
		log.debug("Stale audit datum table:\n{}",
				rows.stream().map(Object::toString).collect(joining("\n")));
		return rows;
	}

	@Test
	public void processStaleDailyAggProducesStaleDailyAuditRows() throws Exception {
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 14, 55, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();

		ZonedDateTime dayStart = ts1.with(TemporalAdjusters.firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);

		// reset stale datum to 'h' as if we already processed 'h' rows
		jdbcTemplate.update("DELETE FROM solardatm.aud_stale_datm");
		insertStaleAggDatum(new StaleAggregateDatumEntity(streamId, dayStart.toInstant(),
				Aggregation.Day, Instant.now()));

		// change job to daily agg processing
		job.setAggregateProcessType("d");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		List<StaleAuditDatum> staleDailyAuditRows = staleAuditDatum();
		assertThat("Stale daily audit row count", staleDailyAuditRows, hasSize(3));

		assertStaleAuditDatum("Audit row raw", staleDailyAuditRows.get(0),
				new StaleAuditDatumEntity(streamId, dayStart.toInstant(), Aggregation.None, null));
		assertStaleAuditDatum("Audit row daily", staleDailyAuditRows.get(1),
				new StaleAuditDatumEntity(streamId, dayStart.toInstant(), Aggregation.Day, null));
		assertStaleAuditDatum("Audit row hourly", staleDailyAuditRows.get(2),
				new StaleAuditDatumEntity(streamId, dayStart.toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void processStaleMonthlyAggProducesStaleDailyAuditRow() throws Exception {
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 14, 55, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();

		ZonedDateTime dayStart = ts1.with(TemporalAdjusters.firstDayOfMonth())
				.truncatedTo(ChronoUnit.DAYS);

		// reset stale datum to 'M' as if we already processed 'h' rows
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");
		insertStaleAggDatum(new StaleAggregateDatumEntity(streamId, dayStart.toInstant(),
				Aggregation.Month, Instant.now()));

		// change job to monthly agg processing
		job.setAggregateProcessType(Aggregation.Month.getKey());
		assertThat("Job completed", job.executeJob(), equalTo(true));

		List<StaleAuditDatum> staleDailyAuditRows = staleAuditDatum();
		assertThat("Stale daily audit row count", staleDailyAuditRows, hasSize(1));

		assertStaleAuditDatum("Audit row montlhly", staleDailyAuditRows.get(0),
				new StaleAuditDatumEntity(streamId, dayStart.toInstant(), Aggregation.Month, null));
	}

	@Test
	public void insertDatumAddStaleRow() {
		// GIVEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setAggregation(Aggregation.Hour);
		List<StaleAggregateDatum> staleRows = jdbcTemplate.query(new SelectStaleAggregateDatum(filter),
				StaleAggregateDatumEntityRowMapper.INSTANCE);
		assertThat("No stale rows at start", staleRows.isEmpty(), equalTo(true));

		// WHEN
		ZonedDateTime start = ZonedDateTime.now(ZoneId.of(TEST_TZ)).truncatedTo(ChronoUnit.HOURS);
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = populateTestData(
				start.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// THEN
		staleRows = jdbcTemplate.query(new SelectStaleAggregateDatum(filter),
				StaleAggregateDatumEntityRowMapper.INSTANCE);
		assertThat("Stale row inserted", staleRows, hasSize(1));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				start.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void insertDatumAddStaleRowAfterPrevHour() {
		// given
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 14, 55, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");

		// when
		ZonedDateTime ts2 = ts1.plusMinutes(10); // prev hour
		populateTestData(ts2.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale rows inserted", staleRows, hasSize(2));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
		assertStaleAggregateDatum("2", staleRows.get(1), new StaleAggregateDatumEntity(streamId,
				ts2.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void insertDatumAddStaleRowBeforeNextHour() {
		// given
		ZonedDateTime ts1 = ZonedDateTime.of(2018, 6, 22, 15, 05, 0, 0, ZoneId.of(TEST_TZ));
		Map<NodeSourcePK, NodeDatumStreamMetadata> metas = populateTestData(
				ts1.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		UUID streamId = metas.values().iterator().next().getStreamId();

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solardatm.agg_stale_datm");

		// when
		ZonedDateTime ts2 = ts1.plusMinutes(-10); // prev hour
		populateTestData(ts2.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale rows inserted", staleRows, hasSize(2));
		assertStaleAggregateDatum("1", staleRows.get(0), new StaleAggregateDatumEntity(streamId,
				ts2.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
		assertStaleAggregateDatum("2", staleRows.get(1), new StaleAggregateDatumEntity(streamId,
				ts1.truncatedTo(ChronoUnit.HOURS).toInstant(), Aggregation.Hour, null));
	}

	@Test
	public void processDatumAppEvents() throws Exception {
		// GIVEN
		DatumAppEventAcceptor acceptor = EasyMock.createMock(DatumAppEventAcceptor.class);
		job.setDatumAppEventAcceptors(new StaticOptionalServiceCollection<>(singleton(acceptor)));
		ZonedDateTime start = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		populateTestData(start.toInstant().toEpochMilli(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);
		List<StaleAggregateDatum> staleRows = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		assertThat("Stale hour row created", staleRows.size(), equalTo(1));

		Capture<DatumAppEvent> eventCaptor = new Capture<>();
		acceptor.offerDatumEvent(capture(eventCaptor));

		// WHEN
		replay(acceptor);
		boolean result = job.executeJob();
		job.getExecutorService().shutdown();
		job.getExecutorService().awaitTermination(10, TimeUnit.SECONDS);

		// THEN
		assertThat("Completed", result, equalTo(true));
		verifyStaleHourlyRowsAllProcessed(1);

		DatumAppEvent event = eventCaptor.getValue();
		assertThat("DatumAppEvent published", event, notNullValue());
		assertThat("DatumAppEvent node ID matches", event.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat("DatumAppEvent source ID matches", event.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("DatumAppEvent properties available", event.getEventProperties(), notNullValue());
		assertThat("DatumAppEvent prop agg key", event.getEventProperties(),
				hasEntry("aggregationKey", Aggregation.Hour.getKey()));
		assertThat("DatumAppEvent prop agg timestamp", event.getEventProperties(),
				hasEntry("timestamp", start.toInstant().toEpochMilli()));

		AggregateUpdatedEventInfo info = getObjectFromJSON(
				getJSONString(event.getEventProperties(), null), AggregateUpdatedEventInfo.class);
		assertThat("AggregateUpdatedEventInfo info available", info, notNullValue());
		assertThat("Info aggregation", info.getAggregation(), equalTo(Aggregation.Hour));
		assertThat("Info aggregation", info.getTimeStart(), equalTo(start.toInstant()));

		verify(acceptor);
	}

}
