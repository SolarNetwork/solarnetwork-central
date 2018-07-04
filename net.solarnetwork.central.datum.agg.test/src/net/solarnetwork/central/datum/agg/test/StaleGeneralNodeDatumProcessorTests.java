/* ==================================================================
 * StaleGeneralNodeDatumProcessorTests.java - 30/03/2017 7:01:48 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import net.solarnetwork.central.datum.agg.StaleGeneralNodeDatumProcessor;
import net.solarnetwork.central.scheduler.SchedulerConstants;

/**
 * Test cases for the {@link StaleGeneralNodeDatumProcessor} class.
 * 
 * @author matt
 * @version 1.1
 */
public class StaleGeneralNodeDatumProcessorTests extends AggTestSupport {

	private static final String TEST_JOB_ID = "Test Stale General Node Datum Processor";

	private static final String TEST_SOURCE_ID = "test.source";

	private TestStaleGeneralNodeDatumProcessor job;

	private static final class TestStaleGeneralNodeDatumProcessor
			extends StaleGeneralNodeDatumProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
			super(eventAdmin, jdbcOps);
			setExecutorService(Executors.newCachedThreadPool(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleGeneralNodeDatumTask-" + taskThreadCount.incrementAndGet());
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
		job.setMaximumRowCount(10);
		job.setAggregateProcessType("h");
		job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();
	}

	private static final String SQL_INSERT_DATUM = "INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a) "
			+ "VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))";

	private static final String SQL_SELECT_AUD_DATUM_DAILY_STALE = "SELECT * FROM solaragg.aud_datum_daily_stale ORDER BY aud_kind, ts_start, node_id, source_id";

	private static final long MS_PER_HOUR = 60 * 60 * 1000L;

	private long populateTestData(final int count, final long step, final Long nodeId,
			final String sourceId) {
		// round to hour ts
		final long start = (System.currentTimeMillis() / MS_PER_HOUR) * MS_PER_HOUR;
		populateTestData(start, count, step, nodeId, sourceId);
		return start;
	}

	private void populateTestData(final long start, final int count, final long step, final Long nodeId,
			final String sourceId) {
		final long now = System.currentTimeMillis();
		jdbcTemplate.execute(SQL_INSERT_DATUM, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement stmt)
					throws SQLException, DataAccessException {
				// round to hour ts
				long ts = start;
				for ( int i = 0; i < count; i++ ) {
					stmt.setTimestamp(1, new Timestamp(ts));
					stmt.setLong(2, nodeId);
					stmt.setString(3, sourceId);
					stmt.setTimestamp(4, new Timestamp(now));
					stmt.setString(5, "{\"watts\":125}");
					stmt.setString(6, "{\"wattHours\":10}");
					stmt.executeUpdate();
					ts += step;
				}
				return null;
			}
		});
	}

	private void populateTestData() {
		// populate 2 hours worth of 10min samples for 5 nodes
		for ( int i = 1; i <= 5; i++ ) {
			populateTestData(11, 10 * 60 * 1000L, (long) i, TEST_SOURCE_ID);
		}
	}

	private void verifyResults() {
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h'");
		Assert.assertTrue("No stale rows", staleRows.isEmpty());

		List<Map<String, Object>> hourRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_datum_hourly ORDER BY ts_start, node_id, source_id");
		Assert.assertEquals("Agg hour rows", 10, hourRows.size());
	}

	@Test
	public void runSingleTask() throws Exception {
		populateTestData();
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum");
		Assert.assertEquals("Stale hour rows (5 nodes * 2 stale hours)", 10, staleRows.size());
		boolean result = job.executeJob();
		Assert.assertTrue("Completed", result);
		verifyResults();
		Assert.assertEquals("Thread count", 0, job.taskThreadCount.get());
	}

	@Test
	public void runParallelTasks() throws Exception {
		job.setTaskCount(4);
		populateTestData();
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum");
		Assert.assertEquals("Stale hour rows (5 nodes * 2 stale hours)", 10, staleRows.size());
		boolean result = job.executeJob();
		Assert.assertTrue("Completed", result);
		verifyResults();
	}

	private static final String SQL_LOCK_STALE_ROW = "SELECT * FROM solaragg.agg_stale_datum "
			+ "WHERE agg_kind = ? AND node_id = ? AND ts_start = ? AND source_id = ? FOR UPDATE";

	@Test
	public void runParallelTasksWithLockedRow() throws Exception {
		job.setTaskCount(4);
		populateTestData();

		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum");
		Assert.assertEquals("Stale hour rows (5 nodes * 2 stale hours)", 10, staleRows.size());

		// pick a single stale row to lock in a different thread; this row should be skipped
		final Map<String, Object> oneStaleRow = staleRows.iterator().next();

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
									oneStaleRow.get("node_id"), oneStaleRow.get("ts_start"),
									oneStaleRow.get("source_id"));

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
		staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h'");
		Assert.assertEquals("Only locked row remains", 1, staleRows.size());
		Assert.assertEquals("Locked stale row remains", oneStaleRow, staleRows.get(0));
	}

	private void validateStaleRow(String desc, Map<String, Object> row, DateTime ts, Long nodeId,
			String sourceId, String aggKind) {
		assertThat("Stale row " + desc + " ts_start", row,
				hasEntry("ts_start", (Object) new Timestamp(ts.getMillis())));
		assertThat("Stale row " + desc + " node_id", row, hasEntry("node_id", (Object) nodeId));
		assertThat("Stale row " + desc + " source_id", row, hasEntry("source_id", (Object) sourceId));
		assertThat("Stale row " + desc + " agg_kind", row, hasEntry("agg_kind", (Object) aggKind));
	}

	@Test
	public void processStaleDailyAggProducesStaleDailyAuditRows() throws Exception {
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55, DateTimeZone.UTC);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		Timestamp dayStart = new Timestamp(ts1.dayOfMonth().roundFloorCopy().getMillis());

		// reset stale datum to 'h' as if we already processed 'h' rows
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_stale_datum (ts_start,node_id,source_id,agg_kind) VALUES (?,?,?,?)",
				dayStart, TEST_NODE_ID, TEST_SOURCE_ID, "d");

		// change job to daily agg processing
		job.setAggregateProcessType("d");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		List<Map<String, Object>> staleDailyAuditRows = jdbcTemplate
				.queryForList(SQL_SELECT_AUD_DATUM_DAILY_STALE);
		assertThat("Stale daily audit row count", staleDailyAuditRows, hasSize(3));

		assertThat("Audit row daily", staleDailyAuditRows.get(0), allOf(
				hasEntry("ts_start", (Object) dayStart), hasEntry("node_id", (Object) TEST_NODE_ID),
				hasEntry("source_id", (Object) TEST_SOURCE_ID), hasEntry("aud_kind", (Object) "d")));

		assertThat("Audit row hourly", staleDailyAuditRows.get(1), allOf(
				hasEntry("ts_start", (Object) dayStart), hasEntry("node_id", (Object) TEST_NODE_ID),
				hasEntry("source_id", (Object) TEST_SOURCE_ID), hasEntry("aud_kind", (Object) "h")));

		assertThat("Audit row raw", staleDailyAuditRows.get(2), allOf(
				hasEntry("ts_start", (Object) dayStart), hasEntry("node_id", (Object) TEST_NODE_ID),
				hasEntry("source_id", (Object) TEST_SOURCE_ID), hasEntry("aud_kind", (Object) "r")));
	}

	@Test
	public void processStaleMonthlyAggProducesStaleDailyAuditRow() throws Exception {
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55, DateTimeZone.UTC);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		Timestamp dayStart = new Timestamp(ts1.monthOfYear().roundFloorCopy().getMillis());

		// reset stale datum to 'h' as if we already processed 'h' rows
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_stale_datum (ts_start,node_id,source_id,agg_kind) VALUES (?,?,?,?)",
				dayStart, TEST_NODE_ID, TEST_SOURCE_ID, "m");

		// change job to monthly agg processing
		job.setAggregateProcessType("m");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		List<Map<String, Object>> staleDailyAuditRows = jdbcTemplate
				.queryForList(SQL_SELECT_AUD_DATUM_DAILY_STALE);
		assertThat("Stale daily audit row count", staleDailyAuditRows, hasSize(1));

		assertThat("Audit row monthly", staleDailyAuditRows.get(0), allOf(
				hasEntry("ts_start", (Object) dayStart), hasEntry("node_id", (Object) TEST_NODE_ID),
				hasEntry("source_id", (Object) TEST_SOURCE_ID), hasEntry("aud_kind", (Object) "m")));
	}

	@Test
	public void insertDatumAddStaleRow() {
		// given
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h'");
		assertThat("No stale rows at start", staleRows.isEmpty(), equalTo(true));

		// when
		final long startTs = populateTestData(1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		DateTime start = new DateTime(startTs);
		staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h'");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		Map<String, Object> row = staleRows.get(0);
		validateStaleRow("1", row, start.hourOfDay().roundFloorCopy(), TEST_NODE_ID, TEST_SOURCE_ID,
				"h");
	}

	@Test
	public void insertDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when
		DateTime ts2 = ts1.plusMinutes(10); // next hour
		populateTestData(ts2.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts2.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void insertDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when
		DateTime ts2 = ts1.plusMinutes(-10); // prev hour
		populateTestData(ts2.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts2.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRow() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when
		int updateCount = jdbcTemplate
				.update("UPDATE solardatum.da_datum SET posted = posted + interval '1 minute'");

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"UPDATE solardatum.da_datum SET posted = posted + interval '1 minute'"
						+ " WHERE ts = ? AND node_id = ? AND source_id = ?",
				new Timestamp(ts1.plusMinutes(10).getMillis()), TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_NODE_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"UPDATE solardatum.da_datum SET posted = posted + interval '1 minute'"
						+ " WHERE ts = ? AND node_id = ? AND source_id = ?",
				new Timestamp(ts1.getMillis()), TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_NODE_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRow() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_NODE_ID, TEST_SOURCE_ID);

		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when
		int updateCount = jdbcTemplate.update("DELETE FROM solardatum.da_datum");

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when

		// delete 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"DELETE FROM solardatum.da_datum WHERE ts = ? AND node_id = ? AND source_id = ?",
				new Timestamp(ts1.plusMinutes(10).getMillis()), TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_NODE_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_NODE_ID,
				TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"DELETE FROM solardatum.da_datum WHERE ts = ? AND node_id = ? AND source_id = ?",
				new Timestamp(ts1.getMillis()), TEST_NODE_ID, TEST_SOURCE_ID);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_datum WHERE agg_kind = 'h' ORDER BY ts_start, node_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_NODE_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_NODE_ID, TEST_SOURCE_ID, "h");
	}
}
