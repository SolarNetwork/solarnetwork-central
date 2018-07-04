/* ==================================================================
 * StaleAuditDataProcessorTests.java - 3/07/2018 11:13:16 AM
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
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
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import net.solarnetwork.central.datum.agg.StaleAuditDataProcessor;
import net.solarnetwork.central.scheduler.SchedulerConstants;
import net.solarnetwork.central.test.AbstractCentralTest;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link StaleAuditDataProcessor} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration("classpath:/net/solarnetwork/central/test/test-tx-context.xml")
public class StaleAuditDataProcessorTests extends AbstractCentralTest {

	private static final String TEST_JOB_ID = "Test Stale Audit Datum Processor";

	private static final String TEST_SOURCE_ID = "test.source";

	@Resource
	private DataSource dataSource;

	@Resource
	private PlatformTransactionManager txManager;

	private JdbcTemplate jdbcTemplate;
	private TransactionTemplate txTemplate;

	private TestStaleAuditDataProcessor job;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final class TestStaleAuditDataProcessor extends StaleAuditDataProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestStaleAuditDataProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
			super(eventAdmin, jdbcOps);
			setExecutorService(Executors.newCachedThreadPool(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleAuditDataProcessorTask-" + taskThreadCount.incrementAndGet());
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

		job = new TestStaleAuditDataProcessor(null, jdbcTemplate);
		job.setJobGroup("Test");
		job.setJobId(TEST_JOB_ID);
		job.setMaximumRowCount(10);
		job.setTierProcessType("r");
		//job.setMaximumWaitMs(15 * 1000L);

		cleanupDatabase();
	}

	//@After
	public void cleanupDatabase() {
		jdbcTemplate.update("DELETE FROM solardatum.da_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_stale_datum");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.agg_datum_monthly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_hourly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_daily");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_monthly");
		jdbcTemplate.update("DELETE FROM solaragg.aud_datum_daily_stale");
	}

	private static final String SQL_INSERT_DATUM = "INSERT INTO solardatum.da_datum(ts, node_id, source_id, posted, jdata_i, jdata_a) "
			+ "VALUES (?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))";

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

	private void insertAuditDatumDailyStaleRow(String kind, long ts, Long nodeId, String sourceId) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_daily_stale (aud_kind,ts_start,node_id,source_id) VALUES (?,?,?,?)",
				kind, new Timestamp(ts), nodeId, sourceId);
	}

	private void insertAuditDatumDailyRow(long ts, Long nodeId, String sourceId, Integer rawCount,
			Integer hourCount, Boolean dailyPresent, Long propInCount, Long datumOutCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_daily (ts_start,node_id,source_id,datum_count,datum_hourly_count,datum_daily_pres,prop_count,datum_q_count) VALUES (?,?,?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, hourCount, dailyPresent, propInCount,
				datumOutCount);
	}

	private void insertAuditDatumMonthlyRow(long ts, Long nodeId, String sourceId, Long rawCount,
			Long hourCount, Long dailyCount, Boolean monthPresent, Long propInCount,
			Long datumOutCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_monthly (ts_start,node_id,source_id,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres,prop_count,datum_q_count) VALUES (?,?,?,?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, rawCount, hourCount, dailyCount, monthPresent,
				propInCount, datumOutCount);
	}

	private List<Map<String, Object>> listAuditDatumDailyStaleRows() {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.aud_datum_daily_stale ORDER BY aud_kind,ts_start,node_id,source_id");
	}

	private List<Map<String, Object>> listAuditDatumDailyRows() {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.aud_datum_daily ORDER BY ts_start,node_id,source_id");
	}

	private List<Map<String, Object>> listAuditDatumMonthlyRows() {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.aud_datum_monthly ORDER BY ts_start,node_id,source_id");
	}

	private void assertAuditDatumDailyRow(String desc, Map<String, Object> row, long ts, Long nodeId,
			String sourceId, Integer rawCount, Integer hourCount, Boolean dailyPresent) {
		assertThat(desc + " not null", row, notNullValue());
		assertThat(desc + " data", row,
				allOf(hasEntry("ts_start", (Object) new Timestamp(ts)),
						hasEntry("node_id", (Object) nodeId), hasEntry("source_id", (Object) sourceId),
						hasEntry("datum_count", (Object) rawCount),
						hasEntry("datum_hourly_count", (Object) hourCount),
						hasEntry("datum_daily_pres", (Object) dailyPresent)));
	}

	@SuppressWarnings("unchecked")
	private void assertAuditDatumDailyRow(String desc, Map<String, Object> row, long ts, Long nodeId,
			String sourceId, Integer rawCount, Integer hourCount, Boolean dailyPresent, Long propInCount,
			Long datumOutCount) {
		assertThat(desc + " not null", row, notNullValue());
		assertThat(desc + " data", row,
				allOf(hasEntry("ts_start", (Object) new Timestamp(ts)),
						hasEntry("node_id", (Object) nodeId), hasEntry("source_id", (Object) sourceId),
						hasEntry("datum_count", (Object) rawCount),
						hasEntry("datum_hourly_count", (Object) hourCount),
						hasEntry("datum_daily_pres", (Object) dailyPresent),
						hasEntry("prop_count", (Object) propInCount),
						hasEntry("datum_q_count", (Object) datumOutCount)));
	}

	@SuppressWarnings("unchecked")
	private void assertAuditDatumMonthlyRow(String desc, Map<String, Object> row, long ts, Long nodeId,
			String sourceId, Integer rawCount, Integer hourCount, Integer dailyCount,
			Boolean monthPresent, Long propInCount, Long datumOutCount) {
		assertThat(desc + " not null", row, notNullValue());
		assertThat(desc + " data", row,
				allOf(hasEntry("ts_start", (Object) new Timestamp(ts)),
						hasEntry("node_id", (Object) nodeId), hasEntry("source_id", (Object) sourceId),
						hasEntry("datum_count", (Object) rawCount),
						hasEntry("datum_hourly_count", (Object) hourCount),
						hasEntry("datum_daily_count", (Object) dailyCount),
						hasEntry("datum_monthly_pres", (Object) monthPresent),
						hasEntry("prop_count", (Object) propInCount),
						hasEntry("datum_q_count", (Object) datumOutCount)));
	}

	private void insertAuditDatumHourlyRow(long ts, Long nodeId, String sourceId, Integer datumCount,
			Integer propCount, Integer datumQueryCount) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.aud_datum_hourly (ts_start,node_id,source_id,datum_count,prop_count,datum_q_count)"
						+ " VALUES (?,?,?,?,?,?)",
				new Timestamp(ts), nodeId, sourceId, datumCount, propCount, datumQueryCount);
	}

	private void insertAggDatumHourlyRow(long ts, Long nodeId, String sourceId,
			Map<String, Object> iData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_hourly (ts_start,local_date,node_id,source_id,jdata_i)"
						+ " VALUES (?,?,?,?,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null));
	}

	private void insertAggDatumDailyRow(long ts, Long nodeId, String sourceId,
			Map<String, Object> iData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_daily (ts_start,local_date,node_id,source_id,jdata_i)"
						+ " VALUES (?,?,?,?,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null));
	}

	private void insertAggDatumMonthlyRow(long ts, Long nodeId, String sourceId,
			Map<String, Object> iData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_monthly (ts_start,local_date,node_id,source_id,jdata_i)"
						+ " VALUES (?,?,?,?,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null));
	}

	@Test
	public void processStaleRawRow() throws Exception {
		// given
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();
		populateTestData(start.getMillis(), 2, TimeUnit.MINUTES.toMillis(1), TEST_NODE_ID,
				TEST_SOURCE_ID);
		insertAuditDatumDailyStaleRow("r", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("r");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 2, 0, false);
	}

	@Test
	public void processStaleRawRowUpdate() throws Exception {
		// given
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();
		populateTestData(start.getMillis(), 2, TimeUnit.MINUTES.toMillis(1), TEST_NODE_ID,
				TEST_SOURCE_ID);
		insertAuditDatumDailyStaleRow("r", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// insert existing row with counts that we'll ignore or update
		insertAuditDatumDailyRow(start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, 10, 20, false, 40L,
				50L);

		// when
		job.setTierProcessType("r");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 2, 20, false, 40L, 50L);
	}

	@Test
	public void processStaleHourlyRowWithAggRows() throws Exception {
		// given
		final Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();

		// insert some day rows to drive hourly count value
		for ( int i = 0; i < 3; i++ ) {
			insertAggDatumHourlyRow(start.getMillis() + TimeUnit.MINUTES.toMillis(i), TEST_NODE_ID,
					TEST_SOURCE_ID, testData);

			// insert some other node data to verify just the node we're interested in comes back
			insertAggDatumHourlyRow(start.getMillis() + TimeUnit.MINUTES.toMillis(i), TEST_NODE_ID,
					"not.this.source", testData);
		}

		insertAuditDatumDailyStaleRow("h", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("h");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 0, 3, false);
	}

	@Test
	public void processStaleHourlyRowWithAggRowsUpdate() throws Exception {
		// given
		final Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();

		// insert existing row with counts that we'll ignore or update
		insertAuditDatumDailyRow(start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, 10, 20, false, 40L,
				50L);

		// insert some hour rows to drive hourly count value
		for ( int i = 0; i < 3; i++ ) {
			insertAggDatumHourlyRow(start.hourOfDay().addToCopy(i).getMillis(), TEST_NODE_ID,
					TEST_SOURCE_ID, testData);

			// insert some other node data to verify just the node we're interested in comes back
			insertAggDatumHourlyRow(start.hourOfDay().addToCopy(i).getMillis(), TEST_NODE_ID,
					"not.this.source", testData);
		}

		insertAuditDatumDailyStaleRow("h", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("h");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 10, 3, false, 40L, 50L);
	}

	@Test
	public void processStaleDailyRowWithAggRows() throws Exception {
		// given
		Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);

		final DateTime start = new DateTime(DateTimeZone.UTC).dayOfMonth().roundFloorCopy();

		// insert 5 hour rows to drive audit data
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 5; i++ ) {
			long ts = start.getMillis() + TimeUnit.HOURS.toMillis(i);
			insertAuditDatumHourlyRow(ts, TEST_NODE_ID, TEST_SOURCE_ID, 0, i, i + 10);
			propCount += i;
			datumQueryCount += i + 10;
		}

		// insert a couple of day rows to drive day count value
		for ( int i = 0; i < 2; i++ ) {
			insertAggDatumDailyRow(start.getMillis() + TimeUnit.DAYS.toMillis(i), TEST_NODE_ID,
					TEST_SOURCE_ID, testData);
		}

		insertAuditDatumDailyStaleRow("d", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("d");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 0, 0, true, propCount, datumQueryCount);
	}

	@Test
	public void processStaleDailyRowWithAggRowsUpdate() throws Exception {
		// given
		Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);

		final DateTime start = new DateTime(DateTimeZone.UTC).dayOfMonth().roundFloorCopy();

		// insert 5 hour rows to drive audit data
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 5; i++ ) {
			long ts = start.getMillis() + TimeUnit.HOURS.toMillis(i);
			insertAuditDatumHourlyRow(ts, TEST_NODE_ID, TEST_SOURCE_ID, 0, i, i + 10);
			propCount += i;
			datumQueryCount += i + 10;
		}

		// insert a couple of day rows to drive day count value
		for ( int i = 0; i < 2; i++ ) {
			insertAggDatumDailyRow(start.getMillis() + TimeUnit.DAYS.toMillis(i), TEST_NODE_ID,
					TEST_SOURCE_ID, testData);
		}

		// insert existing row with counts that we'll ignore or update
		insertAuditDatumDailyRow(start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, 10, 20, false, 40L,
				50L);

		insertAuditDatumDailyStaleRow("d", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("d");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumDailyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, 10, 20, true, propCount, datumQueryCount);
	}

	@Test
	public void processStaleMonthlyRowWithAggRows() throws Exception {
		// given
		Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);

		final DateTime start = new DateTime(DateTimeZone.UTC).dayOfMonth().roundFloorCopy();

		// insert 5 daily rows to drive audit data
		int rawCount = 0;
		int hourCount = 0;
		int dailyCount = 0;
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 5; i++ ) {
			long ts = start.getMillis() + TimeUnit.HOURS.toMillis(i);
			int r = i, h = i + 10;
			long p = i + 1000, q = i + 10000;
			insertAuditDatumDailyRow(ts, TEST_NODE_ID, TEST_SOURCE_ID, r, h, true, p, q);
			rawCount += r;
			hourCount += h;
			dailyCount++;
			propCount += p;
			datumQueryCount += q;
		}

		// insert a couple of month rows to drive day count value
		for ( int i = 0; i < 7; i++ ) {
			insertAggDatumMonthlyRow(start.getMillis() + TimeUnit.DAYS.toMillis(i), TEST_NODE_ID,
					TEST_SOURCE_ID, testData);
		}

		// insert existing row with counts that we'll ignore or update
		insertAuditDatumMonthlyRow(start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, 10L, 20L, 30L, false,
				50L, 60L);

		insertAuditDatumDailyStaleRow("m", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("m");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumMonthlyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumMonthlyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, rawCount, hourCount, dailyCount, true, propCount, datumQueryCount);
	}

	@Test
	public void processStaleMonthlyRowWithAggRowsUpdate() throws Exception {
		// given
		Map<String, Object> testData = Collections.singletonMap("foo", (Object) 1);

		final DateTime start = new DateTime(DateTimeZone.UTC).dayOfMonth().roundFloorCopy();

		// insert 5 daily rows to drive audit data
		int rawCount = 0;
		int hourCount = 0;
		int dailyCount = 0;
		long propCount = 0;
		long datumQueryCount = 0;
		for ( int i = 0; i < 5; i++ ) {
			long ts = start.getMillis() + TimeUnit.HOURS.toMillis(i);
			int r = i, h = i + 10;
			long p = i + 1000, q = i + 10000;
			insertAuditDatumDailyRow(ts, TEST_NODE_ID, TEST_SOURCE_ID, r, h, true, p, q);
			rawCount += r;
			hourCount += h;
			dailyCount++;
			propCount += p;
			datumQueryCount += q;
		}

		// insert a month row to drive month present value
		insertAggDatumMonthlyRow(start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, testData);

		insertAuditDatumDailyStaleRow("m", start.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID);

		// when
		job.setTierProcessType("m");
		assertThat("Job completed", job.executeJob(), equalTo(true));

		// then
		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumMonthlyRows();
		assertThat("Audit table row count", rows, hasSize(1));
		assertAuditDatumMonthlyRow("Audit row", rows.get(0), start.getMillis(), TEST_NODE_ID,
				TEST_SOURCE_ID, rawCount, hourCount, dailyCount, true, propCount, datumQueryCount);
	}

	@Test
	public void runParallelTasks() throws Exception {
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();

		// 5 nodes posting 8 samples 12 hours apart (2 per day, 4 days)
		for ( long nodeId = TEST_NODE_ID; nodeId > TEST_NODE_ID - 5; nodeId-- ) {
			populateTestData(start.getMillis(), 8, TimeUnit.HOURS.toMillis(12), nodeId, TEST_SOURCE_ID);
		}

		// populate 4 stale daily audit rows for raw data
		final DateTime startDay = start.dayOfMonth().roundFloorCopy();
		for ( long ts = startDay.getMillis(); ts < startDay.getMillis()
				+ TimeUnit.DAYS.toMillis(4); ts += TimeUnit.DAYS.toMillis(1) ) {
			for ( long nodeId = TEST_NODE_ID; nodeId > TEST_NODE_ID - 5; nodeId-- ) {
				insertAuditDatumDailyStaleRow("r", ts, nodeId, TEST_SOURCE_ID);
			}
		}

		assertThat("Stale audit row count", jdbcTemplate.queryForObject(
				"SELECT count(*) FROM solaragg.aud_datum_daily_stale", Integer.class), equalTo(20));

		job.setTierProcessType("r");
		job.setTaskCount(4);
		job.setMaximumRowCount(20);
		assertThat("Job completed", job.executeJob(), equalTo(true));

		List<Map<String, Object>> rows = listAuditDatumDailyStaleRows();
		assertThat("Audit stale table empty", rows, hasSize(0));

		rows = listAuditDatumDailyRows();
		assertThat("Audit table row count", rows, hasSize(20));
		int i = 0;
		for ( long ts = startDay.getMillis(); ts < startDay.getMillis()
				+ TimeUnit.DAYS.toMillis(4); ts += TimeUnit.DAYS.toMillis(1) ) {
			for ( long nodeId = TEST_NODE_ID - 4; nodeId <= TEST_NODE_ID; nodeId++ ) {
				assertAuditDatumDailyRow("Audit row " + (i + 1), rows.get(i), ts, nodeId, TEST_SOURCE_ID,
						2, 0, false);
				i++;
			}
		}
	}

	private static final String SQL_LOCK_STALE_ROW = "SELECT * FROM solaragg.aud_datum_daily_stale "
			+ "WHERE aud_kind = ? AND node_id = ? AND ts_start = ? AND source_id = ? FOR UPDATE";

	@Test
	public void runParallelTasksWithLockedRow() throws Exception {
		final DateTime start = new DateTime(DateTimeZone.UTC).hourOfDay().roundFloorCopy();

		// 5 nodes posting 8 samples 12 hours apart (2 per day, 4 days)
		for ( long nodeId = TEST_NODE_ID; nodeId > TEST_NODE_ID - 5; nodeId-- ) {
			populateTestData(start.getMillis(), 8, TimeUnit.HOURS.toMillis(12), nodeId, TEST_SOURCE_ID);
		}

		// populate 4 stale daily audit rows for raw data
		final DateTime startDay = start.dayOfMonth().roundFloorCopy();
		for ( long ts = startDay.getMillis(); ts < startDay.getMillis()
				+ TimeUnit.DAYS.toMillis(4); ts += TimeUnit.DAYS.toMillis(1) ) {
			for ( long nodeId = TEST_NODE_ID; nodeId > TEST_NODE_ID - 5; nodeId-- ) {
				insertAuditDatumDailyStaleRow("r", ts, nodeId, TEST_SOURCE_ID);
			}
		}

		List<Map<String, Object>> staleRows = listAuditDatumDailyStaleRows();
		assertThat("Stale audit row count", staleRows, hasSize(20));

		// pick a single stale row to lock in a different thread; this row should be skipped
		final Map<String, Object> oneStaleRow = staleRows.get(0);

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
							Map<String, Object> row = jdbcTemplate.queryForMap(SQL_LOCK_STALE_ROW,
									oneStaleRow.get("aud_kind"), oneStaleRow.get("node_id"),
									oneStaleRow.get("ts_start"), oneStaleRow.get("source_id"));

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
			job.setTierProcessType("r");
			job.setTaskCount(4);
			job.setMaximumRowCount(20);
			assertThat("Job completed", job.executeJob(), equalTo(true));
		} finally {
			synchronized ( lockThreadSignal ) {
				lockThreadSignal.notifyAll();
			}
		}

		// wait for the lock thread to complete
		lockThread.join(5000);

		// only the previously locked stale row should be left
		staleRows = listAuditDatumDailyStaleRows();
		assertThat("Only locked row remains", staleRows, hasSize(1));
		Assert.assertEquals("Locked stale row remains", oneStaleRow, staleRows.get(0));
	}

}
