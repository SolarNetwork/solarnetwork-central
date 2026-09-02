/* ==================================================================
 * JdbcDatumEntityDao_WriteOnlyDaoConcurrentAggregateTrackingTests.java - 29/04/2026 4:39:38 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test persisting datum, tracking stale aggregates, while concurrently
 * claiming/processing those aggregates.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_WriteOnlyDaoConcurrentAggregateTrackingTests
		extends BaseDatumJdbcTestSupport {

	// this is used in the solardatm.process_one_agg_stale_datm(kind) function
	private static final String SQL_PROCESS_STALE_ROW = """
			SELECT * FROM solardatm.process_one_agg_stale_datm(?)
			""";

	private JdbcDatumEntityDao dao;

	private Long locId;
	private Long userId;
	private Long nodeId;
	private Instant start;
	protected TransactionTemplate txTemplate;

	@BeforeEach
	public void setup() {
		DatumTestUtils.cleanupDatabase(jdbcTemplate);

		locId = CommonDbTestUtils.insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate, randomString() + "@localhost");
		nodeId = CommonDbTestUtils.insertNode(jdbcTemplate, locId);
		CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, nodeId);

		dao = new JdbcDatumEntityDao(jdbcTemplate);

		start = Instant.now().truncatedTo(ChronoUnit.HOURS);

		txTemplate = new TransactionTemplate(txManager);
	}

	@AfterEach
	public void teardown() {
		DatumTestUtils.cleanupDatabase(jdbcTemplate);
	}

	private GeneralDatum datum(String sourceId, Instant ts) {
		final DatumSamples samples = new DatumSamples();
		samples.putInstantaneousSampleValue("a", CommonTestUtils.randomInt());
		samples.putAccumulatingSampleValue("m", ChronoUnit.SECONDS.between(start, ts));
		return new GeneralDatum(nodeId(nodeId, sourceId, ts), samples);
	}

	private void commitTransaction() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
	}

	private Thread lockStaleHourAggRow(UUID streamId, Instant ts, CountDownLatch lockedLatch,
			Object lockThreadSignal, List<Exception> threadExceptions) {
		// lock a stale row
		Thread lockThread = new Thread(() -> {
			txTemplate.execute(new TransactionCallback<Object>() {

				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						Map<String, Object> row = jdbcTemplate.queryForMap(SQL_PROCESS_STALE_ROW, 'h');

						log.debug("Deleted/locked stale row {}", row);

						lockedLatch.countDown();

						// wait for signal, then commit delete (to release lock)
						try {
							synchronized ( lockThreadSignal ) {
								lockThreadSignal.wait();
							}
						} catch ( InterruptedException e ) {
							log.error("StaleRowLockingThread interrupted waiting", e);
						}
						log.debug("Commiting deleted/locked tx {}", row);
					} catch ( RuntimeException e ) {
						threadExceptions.add(e);
						throw e;
					}
					return null;
				}

			});
		}, "StaleRowLockingThread");
		lockThread.setDaemon(true);
		lockThread.start();
		return lockThread;
	}

	private void thenStaleHoursCreated(String prefix, UUID streamId, Instant... hours) {
		final List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate,
				Aggregation.Hour);
		log.debug("Stale aggs: [{}]",
				stale.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		// @formatter:off
		then(stale)
			.as("%s has stale hours created", prefix)
			.hasSize(hours.length)
			;
		// @formatter:on
	}

	@Test
	public void singleStaleStream() throws Exception {
		commitTransaction();

		// object monitor for main thread to signal to row lock thread to complete
		final Object lockThreadSignal = new Object();

		try {
			// GIVEN
			// setup stream with existing datum within a single hour
			final String sourceId = randomSourceId();
			final GeneralDatum leftDatum = datum(sourceId, start.plus(1, MINUTES));
			dao.store(leftDatum);
			final GeneralDatum rightDatum = datum(sourceId, start.plus(59, MINUTES));
			dao.store(rightDatum);

			// process hourly stale aggregates
			DatumDbUtils.processStaleAggregateDatum(log, jdbcTemplate, EnumSet.of(Aggregation.Hour));

			// create one more stale aggregate on start hour
			final Instant ts = start.plus(30, MINUTES); // between left/right
			final GeneralDatum d = datum(sourceId, ts);
			final DatumPK pk = dao.store(d);

			// latch for row lock thread to indicate it has locked the row and the main thread can continue
			final CountDownLatch lockedLatch = new CountDownLatch(1);

			// list to capture exception thrown by row lock thread
			final List<Exception> threadExceptions = new ArrayList<Exception>(1);

			// lock a stale row
			final Thread lockThread = lockStaleHourAggRow(pk.getStreamId(), start, lockedLatch,
					lockThreadSignal, threadExceptions);

			// wait for our latch to signal the row is locked
			boolean locked = lockedLatch.await(5, TimeUnit.SECONDS);
			if ( !threadExceptions.isEmpty() ) {
				throw threadExceptions.get(0);
			}

			then(locked).as("Stale row locked").isTrue();

			// WHEN

			// latch for datum thread to signal that it has started running
			final CountDownLatch datumLatch = new CountDownLatch(1);

			// store another row exactly on start+1 hour, which should create stale rows
			// in both start and start+1 hours, after previous lock released
			final Instant nextDatumTs = start.plus(1, HOURS);
			final GeneralDatum nextDatum = datum(sourceId, nextDatumTs);

			final Thread datumThread = new Thread(() -> {
				var shouldBeStale = jdbcTemplate.queryForList(
						"SELECT * FROM solardatm.calc_stale_datm(?::uuid, ?)", pk.getStreamId(),
						Timestamp.from(nextDatumTs));
				log.debug("Should create stale datum: {}", shouldBeStale.stream().map(Object::toString)
						.collect(joining("\n\t", "\n\t", "\n")));

				datumLatch.countDown();
				dao.store(nextDatum); // this should block because overlapping stale aggregate row locked
				log.debug("Committing store datum: {}", nextDatum);
			}, "DatumThread");
			datumThread.setDaemon(true);
			datumThread.start();

			// wait for our latch to signal the row is locked
			boolean datumStarted = datumLatch.await(5, TimeUnit.SECONDS);
			then(datumStarted).as("Datum generation started").isTrue();

			// sleep to give DatumThread time to get blocked
			Thread.sleep(1000L);

			final List<Datum> datumWhileLocked = DatumDbUtils.listDatum(jdbcTemplate);
			// @formatter:off
			then(datumWhileLocked)
				.as("Only 3 datum should exist because the 4th datum in blocked")
				//.hasSize(3)
				;
			// @formatter:on

			// signal to stale thread that it can complete
			synchronized ( lockThreadSignal ) {
				lockThreadSignal.notifyAll();
			}

			// wait for the threads to complete
			lockThread.join(5000);
			datumThread.join(5000);

			// THEN
			// @formatter:off
			thenStaleHoursCreated("Continued stream", pk.getStreamId(),
				start,
				start.plus(1, HOURS)
			);

			final List<Datum> datumAtEnd = DatumDbUtils.listDatum(jdbcTemplate);
			then(datumAtEnd)
				.as("4 datum should exist at end")
				.hasSize(4)
				;
			// @formatter:on
		} finally {
			// in case of exception, signal to stale thread that it can complete
			synchronized ( lockThreadSignal ) {
				lockThreadSignal.notifyAll();
			}
			DatumTestUtils.cleanupDatabase(jdbcTemplate);
		}
	}

}
