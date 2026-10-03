/* ==================================================================
 * JdbcQueryAuditorTests.java - 15/02/2018 9:28:21 AM
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcQueryAuditor;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcQueryAuditorCount;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcQueryAuditor} class.
 *
 * @author matt
 * @version 2.4
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcQueryAuditorTests {

	private static final Logger log = LoggerFactory.getLogger(JdbcQueryAuditorTests.class);

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_1 = "test.source.1";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> datumCountMap;
	private Clock testClock;
	private StatTracker stats;
	private JdbcQueryAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		stats = new StatTracker("QueryAuditor", "", log, 20);
		auditor = new JdbcQueryAuditor(testClock, dataSource, datumCountMap, stats);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	/**
	 * Wait for the writer to start its first flush, so it writes the counts
	 * added before it started.
	 */
	private void awaitFirstFlush() throws InterruptedException {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ( stats.get(JdbcQueryAuditorCount.CountsFlushed) < 1 && System.nanoTime() < end ) {
			Thread.sleep(10);
		}
	}

	private static GeneralDatum testDatum() {
		return GeneralDatum.nodeDatum(TEST_NODE_ID, TEST_SOURCE_1, Instant.now(), new DatumSamples());
	}

	private void givenWriterConnection() throws Exception {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(jdbcStatement);
	}

	@Test
	public void datumFilterResultsOneNodeAndSourceNoResults() throws Exception {
		// GIVEN
		givenWriterConnection();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results = new BasicFilterResults<>(
				matches, 0L, 0L, 0);

		// WHEN
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).shouldHaveNoInteractions();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("No count added")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void datumFilterResultsOneNodeAndSourceSomeResults() throws Exception {
		// GIVEN
		givenWriterConnection();

		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		filter.setSourceId(TEST_SOURCE_1);

		List<GeneralNodeDatumFilterMatch> matches = new ArrayList<>();
		BasicFilterResults<GeneralNodeDatumFilterMatch, GeneralNodeDatumPK> results = new BasicFilterResults<>(
				matches, 5L, 0L, 3);

		// WHEN
		auditor.auditNodeDatumFilterResults(filter, results);

		auditor.enableWriting();
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 3);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("Counter removed once flushed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditSingleDatum() throws Exception {
		// GIVEN
		givenWriterConnection();

		GeneralDatum datum = GeneralDatum.nodeDatum(TEST_NODE_ID, TEST_SOURCE_1, Instant.now(),
				new DatumSamples());

		// WHEN
		auditor.auditNodeDatum(datum);

		auditor.enableWriting();
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 1);
		then(jdbcStatement).should().execute();
		then(jdbcConnection).should().close();

		// @formatter:off
		and.then(datumCountMap)
			.as("Counter removed once flushed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditNodeDatum_concurrentAddAndFlush_noLostCounts() throws Exception {
		// GIVEN
		final int adderCount = 4;
		final int nodeCount = 256;
		final long runNanos = TimeUnit.SECONDS.toNanos(3);

		// stub-only statement that tallies the flushed counts, to avoid recording every invocation
		final CallableStatement stmt = mock(CallableStatement.class, withSettings().stubOnly());
		final AtomicInteger stmtCount = new AtomicInteger();
		final AtomicLong flushed = new AtomicLong();
		willAnswer(inv -> {
			stmtCount.set(inv.getArgument(1));
			return null;
		}).given(stmt).setInt(eq(4), anyInt());
		given(stmt.execute()).willAnswer(_ -> {
			flushed.addAndGet(stmtCount.get());
			return false;
		});

		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcQueryAuditor.DEFAULT_NODE_SOURCE_INCREMENT_SQL))
				.willReturn(stmt);

		// flush continuously, so the writer keeps removing the counters being added to
		auditor.setFlushDelay(0);

		// do not log statistics for every few of the many counts added
		auditor.setStatLogUpdateCount(Integer.MAX_VALUE);

		// WHEN
		auditor.enableWriting();

		final AtomicLong added = new AtomicLong();
		final long start = System.nanoTime();
		final Thread[] adders = new Thread[adderCount];
		for ( int i = 0; i < adderCount; i++ ) {
			adders[i] = new Thread(() -> {
				final ThreadLocalRandom rnd = ThreadLocalRandom.current();
				while ( System.nanoTime() - start < runNanos ) {
					auditor.auditNodeDatum(GeneralDatum.nodeDatum((long) rnd.nextInt(nodeCount),
							TEST_SOURCE_1, Instant.now(), new DatumSamples()));
					added.incrementAndGet();
					// pause, so most counters hold nothing when the writer reaches them
					LockSupport.parkNanos(rnd.nextInt(20_000));
				}
			}, "Adder-" + i);
			adders[i].start();
		}
		for ( Thread adder : adders ) {
			adder.join();
		}

		// wait for the writer to flush everything added
		final long drainStart = System.nanoTime();
		while ( flushed.get() < added.get()
				&& System.nanoTime() - drainStart < TimeUnit.SECONDS.toNanos(5) ) {
			Thread.sleep(50);
		}
		auditor.disableWriting();

		// THEN
		// @formatter:off
		and.then(flushed.get())
			.as("Every count added is flushed")
			.isEqualTo(added.get())
			;
		and.then(datumCountMap)
			.as("Counters removed once flushed")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void serviceDidShutdown_writesCountsAddedAfterLastFlush() throws Exception {
		// GIVEN
		givenWriterConnection();

		// a long delay, so the writer is waiting for its next flush when the count is added
		auditor.setFlushDelay(TimeUnit.MINUTES.toMillis(1));

		auditor.serviceDidStartup();
		awaitFirstFlush();

		// WHEN
		auditor.auditNodeDatum(testDatum());

		final long start = System.nanoTime();
		auditor.serviceDidShutdown();
		final Duration shutdownTime = Duration.ofNanos(System.nanoTime() - start);

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SOURCE_1);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, 1);
		then(jdbcStatement).should().execute();

		// the count is written with the writer's connection, not another one
		then(dataSource).should().getConnection();

		// @formatter:off
		and.then(shutdownTime)
			.as("Shutdown did not wait for the next flush")
			.isLessThan(Duration.ofSeconds(5))
			;
		and.then(auditor.performPingTest().isSuccess())
			.as("Writer stopped by shutdown")
			.isFalse()
			;
		and.then(datumCountMap)
			.as("Count written at shutdown")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void disableWriting_interruptedDuringUpdateDelay_countNotWrittenAgain() throws Exception {
		// GIVEN
		givenWriterConnection();

		final CountDownLatch executed = new CountDownLatch(1);
		given(jdbcStatement.execute()).willAnswer(_ -> {
			executed.countDown();
			return false;
		});

		// delay long enough that the writer is still sleeping after the update when interrupted
		auditor.setUpdateDelay(TimeUnit.MINUTES.toMillis(1));

		// WHEN
		auditor.auditNodeDatum(testDatum());

		auditor.enableWriting();
		final boolean written = executed.await(5, TimeUnit.SECONDS);

		// interrupt the writer while it sleeps after the update
		auditor.disableWriting();

		// start another writer, which would write the count again if it had been added back
		auditor.enableWriting();
		auditor.disableWriting();

		// THEN
		then(jdbcStatement).should().execute();

		// @formatter:off
		and.then(written)
			.as("Count written")
			.isTrue()
			;
		and.then(datumCountMap)
			.as("Written count not added back when interrupted")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void serviceDidShutdown_writeFails_stopsWriting() throws Exception {
		// GIVEN
		givenWriterConnection();
		given(jdbcStatement.execute()).willThrow(new SQLException("Connection reset", "08006"));

		// a long delay, so the counts are left for the writer to write at shutdown
		auditor.setFlushDelay(TimeUnit.MINUTES.toMillis(1));

		auditor.serviceDidStartup();
		awaitFirstFlush();

		// WHEN
		auditor.auditNodeDatum(testDatum());
		auditor.auditNodeDatum(GeneralDatum.nodeDatum(TEST_NODE_ID - 1, TEST_SOURCE_1, Instant.now(),
				new DatumSamples()));
		auditor.serviceDidShutdown();

		// THEN
		// no more writes are tried after the first fails, and no other connection is used
		then(jdbcStatement).should().execute();
		then(dataSource).should().getConnection();

		// @formatter:off
		and.then(datumCountMap)
			.as("Counts not written once writing failed")
			.hasSize(2)
			;
		// @formatter:on
	}

	@Test
	public void serviceDidShutdown_noConnection_countsNotWritten() throws Exception {
		// GIVEN
		final CountDownLatch connecting = new CountDownLatch(1);
		given(dataSource.getConnection()).willAnswer(_ -> {
			connecting.countDown();
			throw new SQLException("Connection refused", "08001");
		});

		// a long delay, so the writer is waiting to connect again at shutdown
		auditor.setConnectionRecoveryDelay(TimeUnit.MINUTES.toMillis(1));

		auditor.serviceDidStartup();
		final boolean attempted = connecting.await(5, TimeUnit.SECONDS);

		// WHEN
		auditor.auditNodeDatum(testDatum());

		final long start = System.nanoTime();
		auditor.serviceDidShutdown();
		final Duration shutdownTime = Duration.ofNanos(System.nanoTime() - start);

		// THEN
		// no other connection is tried at shutdown
		then(dataSource).should().getConnection();

		// @formatter:off
		and.then(attempted)
			.as("Writer tried to connect")
			.isTrue()
			;
		and.then(shutdownTime)
			.as("Shutdown did not wait to connect again")
			.isLessThan(Duration.ofSeconds(5))
			;
		and.then(datumCountMap)
			.as("Count not written without a connection")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void serviceDidShutdown_noWriter_countsNotWritten() throws Exception {
		// GIVEN
		auditor.auditNodeDatum(testDatum());

		// WHEN
		auditor.serviceDidShutdown();

		// THEN
		then(dataSource).shouldHaveNoInteractions();

		// @formatter:off
		and.then(datumCountMap)
			.as("Count not written without a writer")
			.hasSize(1)
			;
		// @formatter:on
	}

}
