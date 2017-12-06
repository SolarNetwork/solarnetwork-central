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
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.agg.StaleGeneralNodeDatumProcessor;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.test.AbstractCentralTest;

/**
 * Test cases for the {@link StaleGeneralNodeDatumProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration("classpath:/net/solarnetwork/central/test/test-tx-context.xml")
public class StaleGeneralNodeDatumProcessorTests extends AbstractCentralTest {

	private static final String TEST_JOB_ID = "Test Stale General Node Datum Processor";

	@Resource
	private DataSource dataSource;

	@Resource
	private PlatformTransactionManager txManager;

	private JdbcTemplate jdbcTemplate;
	private TransactionTemplate txTemplate;

	private TestStaleGeneralNodeDatumProcessor job;

	private final Logger log = LoggerFactory.getLogger(getClass());

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

	@Before
	public void setup() {
		Assert.assertNotNull("DataSource", dataSource);

		jdbcTemplate = new JdbcTemplate(dataSource);
		txTemplate = new TransactionTemplate(txManager);

		job = new TestStaleGeneralNodeDatumProcessor(null, jdbcTemplate);
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumRowCount(10);
		job.setAggregateProcessType("h");
		job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();
	}

	@After
	public void cleanupDatabase() {
		jdbcTemplate.update("DELETE FROM solardatum.da_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_hourly");
	}

	private static final String SQL_INSERT_DATUM = "INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a) "
			+ "VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))";

	private static final long MS_PER_HOUR = 60 * 60 * 1000L;

	private void populateTestData(final int count, final long step, final Long nodeId,
			final String sourceId) {
		final long now = System.currentTimeMillis();
		jdbcTemplate.execute(SQL_INSERT_DATUM, new PreparedStatementCallback<Object>() {

			@Override
			public Object doInPreparedStatement(PreparedStatement stmt)
					throws SQLException, DataAccessException {
				// round to hour ts
				long ts = (System.currentTimeMillis() / MS_PER_HOUR) * MS_PER_HOUR;
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
			populateTestData(11, 10 * 60 * 1000L, (long) i, "test.source");
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
}
