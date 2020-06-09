/* ==================================================================
 * StaleGeneralLocationDatumProcessorTests.java - 26/09/2018 11:01:44 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.JsonUtils.getStringMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import net.solarnetwork.central.datum.agg.StaleGeneralLocationDatumProcessor;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link StaleGeneralLocationDatumProcessor} class.
 * 
 * @author matt
 * @version 1.1
 */
public class StaleGeneralLocationDatumProcessorTests extends AggTestSupport {

	private static final String TEST_JOB_ID = "Test Stale General Location Datum Processor";

	private static final String TEST_SOURCE_ID = "test.source";

	private TestStaleGeneralLocationDatumProcessor job;

	private static final class TestStaleGeneralLocationDatumProcessor
			extends StaleGeneralLocationDatumProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleGeneralLocationDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
			super(eventAdmin, jdbcOps);
			setExecutorService(Executors.newCachedThreadPool(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleGeneralLocationDatumTask-" + taskThreadCount.incrementAndGet());
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

		job = new TestStaleGeneralLocationDatumProcessor(null, jdbcTemplate);
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumIterations(10);
		job.setAggregateProcessType("h");
		job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();
	}

	private static final String SQL_INSERT_DATUM = "INSERT INTO solardatum.da_loc_datum(ts, loc_id, source_id, posted, jdata_i, jdata_a) "
			+ "VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))";

	private static final long MS_PER_HOUR = 60 * 60 * 1000L;

	private long populateTestData(final int count, final long step, final Long locationId,
			final String sourceId) {
		// round to hour ts
		final long start = (System.currentTimeMillis() / MS_PER_HOUR) * MS_PER_HOUR;
		populateTestData(start, count, step, locationId, sourceId);
		return start;
	}

	private void populateTestData(final long start, final int count, final long step,
			final Long locationId, final String sourceId) {
		final long now = System.currentTimeMillis();
		jdbcTemplate.execute(SQL_INSERT_DATUM, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement stmt)
					throws SQLException, DataAccessException {
				// round to hour ts
				long ts = start;
				for ( int i = 0; i < count; i++ ) {
					stmt.setTimestamp(1, new Timestamp(ts));
					stmt.setLong(2, locationId);
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
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h'");
		Assert.assertTrue("No stale rows", staleRows.isEmpty());

		List<Map<String, Object>> hourRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_loc_datum_hourly ORDER BY ts_start, loc_id, source_id");
		Assert.assertEquals("Agg hour rows", 10, hourRows.size());
	}

	@Test
	public void runSingleTask() throws Exception {
		populateTestData();
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum");
		Assert.assertEquals("Stale hour rows (5 nodes * 2 stale hours)", 10, staleRows.size());
		boolean result = job.executeJob();
		Assert.assertTrue("Completed", result);
		verifyResults();
		Assert.assertEquals("Thread count", 0, job.taskThreadCount.get());
	}

	@Test
	public void runParallelTasks() throws Exception {
		job.setParallelism(4);
		populateTestData();
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum");
		Assert.assertEquals("Stale hour rows (5 nodes * 2 stale hours)", 10, staleRows.size());
		boolean result = job.executeJob();
		Assert.assertTrue("Completed", result);
		verifyResults();
	}

	private static final String SQL_LOCK_STALE_ROW = "SELECT * FROM solaragg.agg_stale_loc_datum "
			+ "WHERE agg_kind = ? AND loc_id = ? AND ts_start = ? AND source_id = ? FOR UPDATE";

	@Test
	public void runParallelTasksWithLockedRow() throws Exception {
		job.setParallelism(4);
		populateTestData();

		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum");
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
									oneStaleRow.get("loc_id"), oneStaleRow.get("ts_start"),
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
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h'");
		Assert.assertEquals("Only locked row remains", 1, staleRows.size());
		Assert.assertEquals("Locked stale row remains", oneStaleRow, staleRows.get(0));
	}

	private void validateStaleRow(String desc, Map<String, Object> row, DateTime ts, Long locationId,
			String sourceId, String aggKind) {
		assertThat("Stale row " + desc + " ts_start", row,
				hasEntry("ts_start", (Object) new Timestamp(ts.getMillis())));
		assertThat("Stale row " + desc + " loc_id", row, hasEntry("loc_id", (Object) locationId));
		assertThat("Stale row " + desc + " source_id", row, hasEntry("source_id", (Object) sourceId));
		assertThat("Stale row " + desc + " agg_kind", row, hasEntry("agg_kind", (Object) aggKind));
	}

	private void insertAggDatumHourlyRow(long ts, Long locationId, String sourceId,
			Map<String, Object> iData, Map<String, Object> aData, Map<String, Object> jMeta) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_loc_datum_hourly (ts_start,local_date,loc_id,source_id,jdata_i,jdata_a,jmeta)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), locationId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null));
	}

	private List<Map<String, Object>> listAggDatumDailyRows() {
		return jdbcTemplate.queryForList(
				"SELECT loc_id,ts_start,source_id,jdata_i::text as jdata_i,jdata_a::text as jdata_a,jdata_s::text as jdata_s,jmeta::text as jmeta "
						+ "FROM solaragg.agg_loc_datum_daily ORDER BY loc_id,ts_start,source_id");
	}

	@Test
	public void processStaleDailyAggProducesDailyAggRow() throws Exception {
		DateTime start = new DateTime(2018, 7, 24, 15, 0, DateTimeZone.forID(TEST_TZ));
		Map<String, Object> iData = singletonMap("foo", (Object) 10);
		Map<String, Object> aData = singletonMap("bar", (Object) 150);
		Map<String, Object> jMeta = getStringMap("{\"i\":{\"foo\":{\"count\":10}}}");
		for ( int i = 0; i < 2; i++ ) {
			DateTime ts = start.plusHours(i);
			insertAggDatumHourlyRow(ts.getMillis(), TEST_LOC_ID, TEST_SOURCE_ID, iData, aData, jMeta);
		}
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_stale_loc_datum (ts_start,loc_id,source_id,agg_kind) VALUES (?,?,?,?)",
				new Timestamp(start.dayOfMonth().roundFloorCopy().getMillis()), TEST_LOC_ID,
				TEST_SOURCE_ID, "d");

		int count = jdbcTemplate.execute(new CallableStatementCreator() {

			@Override
			public CallableStatement createCallableStatement(Connection con) throws SQLException {
				CallableStatement stmt = con
						.prepareCall("{call solaragg.process_one_agg_stale_loc_datum(?)}");
				return stmt;
			}
		}, new CallableStatementCallback<Integer>() {

			private int processKind(String kind, CallableStatement cs) throws SQLException {
				int processed = 0;
				while ( true ) {
					cs.setString(1, kind);
					if ( cs.execute() ) {
						try (ResultSet rs = cs.getResultSet()) {
							if ( rs.next() ) {
								processed++;
							} else {
								break;
							}
						}
					} else {
						break;
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

		List<Map<String, Object>> rows = listAggDatumDailyRows();
		assertThat("Daily agg row count", rows.size(), equalTo(1));
		assertThat("Daily agg ts_start",
				new DateTime(rows.get(0).get("ts_start"), DateTimeZone.forID(TEST_TZ)),
				equalTo(start.dayOfMonth().roundFloorCopy()));
		assertThat("Daily agg jdata_i", getStringMap((String) rows.get(0).get("jdata_i")),
				equalTo(getStringMap("{\"foo\":10}")));
		assertThat("Daily agg jdata_a", getStringMap((String) rows.get(0).get("jdata_a")),
				equalTo(getStringMap("{\"bar\":300}")));
		assertThat("Daily agg jmeta", getStringMap((String) rows.get(0).get("jmeta")),
				equalTo(getStringMap("{\"i\":{\"foo\":{\"count\":20}}}")));
	}

	@Test
	public void insertDatumAddStaleRow() {
		// given
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h'");
		assertThat("No stale rows at start", staleRows.isEmpty(), equalTo(true));

		// when
		final long startTs = populateTestData(1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		DateTime start = new DateTime(startTs);
		staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h'");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		Map<String, Object> row = staleRows.get(0);
		validateStaleRow("1", row, start.hourOfDay().roundFloorCopy(), TEST_LOC_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void insertDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when
		DateTime ts2 = ts1.plusMinutes(10); // next hour
		populateTestData(ts2.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts2.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void insertDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when
		DateTime ts2 = ts1.plusMinutes(-10); // prev hour
		populateTestData(ts2.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts2.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRow() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when
		int updateCount = jdbcTemplate
				.update("UPDATE solardatum.da_loc_datum SET posted = posted + interval '1 minute'");

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"UPDATE solardatum.da_loc_datum SET posted = posted + interval '1 minute'"
						+ " WHERE ts = ? AND loc_id = ? AND source_id = ?",
				new Timestamp(ts1.plusMinutes(10).getMillis()), TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_LOC_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"UPDATE solardatum.da_loc_datum SET posted = posted + interval '1 minute'"
						+ " WHERE ts = ? AND loc_id = ? AND source_id = ?",
				new Timestamp(ts1.getMillis()), TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_LOC_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRow() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when
		int updateCount = jdbcTemplate.update("DELETE FROM solardatum.da_loc_datum");

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(1));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRowAfterPrevHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when

		// delete 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"DELETE FROM solardatum.da_loc_datum WHERE ts = ? AND loc_id = ? AND source_id = ?",
				new Timestamp(ts1.plusMinutes(10).getMillis()), TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_LOC_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void deleteDatumAddStaleRowBeforeNextHour() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 14, 55);
		populateTestData(ts1.getMillis(), 2, TimeUnit.MINUTES.toMillis(10), TEST_LOC_ID, TEST_SOURCE_ID);

		// clear stale datum row
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when

		// update 2nd datum, in 2nd hour
		int updateCount = jdbcTemplate.update(
				"DELETE FROM solardatum.da_loc_datum WHERE ts = ? AND loc_id = ? AND source_id = ?",
				new Timestamp(ts1.getMillis()), TEST_LOC_ID, TEST_SOURCE_ID);

		// then
		assertThat("Deleted row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale row inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy().plusHours(1),
				TEST_LOC_ID, TEST_SOURCE_ID, "h");
	}

	@Test
	public void updateDatumChangeSourceIdAddStaleRows() {
		// given
		DateTime ts1 = new DateTime(2018, 6, 22, 15, 05);
		populateTestData(ts1.getMillis(), 1, 0, TEST_LOC_ID, TEST_SOURCE_ID);

		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_loc_datum");

		// when
		final String testSourceId2 = "test.source.2";
		int updateCount = jdbcTemplate.update("UPDATE solardatum.da_loc_datum SET source_id = ?",
				testSourceId2);

		// then
		assertThat("Update row count", updateCount, equalTo(1));
		List<Map<String, Object>> staleRows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.agg_stale_loc_datum WHERE agg_kind = 'h' ORDER BY ts_start, loc_id, source_id");
		assertThat("Stale rows inserted", staleRows, hasSize(2));
		validateStaleRow("1", staleRows.get(0), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				TEST_SOURCE_ID, "h");
		validateStaleRow("2", staleRows.get(1), ts1.hourOfDay().roundFloorCopy(), TEST_LOC_ID,
				testSourceId2, "h");
	}

}
