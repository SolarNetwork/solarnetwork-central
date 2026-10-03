/* ==================================================================
 * JdbcNodeServiceAuditorTests.java - 21/01/2023 6:15:01 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditor;
import net.solarnetwork.central.common.dao.jdbc.JdbcNodeServiceAuditorCount;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link JdbcNodeServiceAuditor} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class JdbcNodeServiceAuditorTests {

	private static final Logger log = LoggerFactory.getLogger(JdbcNodeServiceAuditorTests.class);

	private static final long FLUSH_DELAY = 300;
	private static final long UPDATE_DELAY = 0;
	private static final long RECONNECT_DELAY = 300;
	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SERVICE_ID = "test";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private CallableStatement jdbcStatement;

	private ConcurrentMap<DatumId, AtomicInteger> datumCountMap;
	private Clock testClock;
	private StatTracker stats;
	private JdbcNodeServiceAuditor auditor;

	@BeforeEach
	public void setup() {
		testClock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.HOURS), ZoneOffset.UTC);
		datumCountMap = new ConcurrentHashMap<>(8);
		stats = new StatTracker("NodeServiceAuditor", "", log, 20);
		auditor = new JdbcNodeServiceAuditor(dataSource, datumCountMap, testClock, stats);
		auditor.setFlushDelay(FLUSH_DELAY);
		auditor.setUpdateDelay(UPDATE_DELAY);
		auditor.setConnectionRecoveryDelay(RECONNECT_DELAY);
	}

	@AfterEach
	public void teardown() {
		// stop any writer a test left running
		auditor.disableWriting();
	}

	private void givenWriterConnection() throws SQLException {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareCall(JdbcNodeServiceAuditor.DEFAULT_NODE_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);
	}

	/**
	 * Wait for the writer to start its first flush, so it writes the counts
	 * added before it started.
	 */
	private void awaitFirstFlush() throws InterruptedException {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ( stats.get(JdbcNodeServiceAuditorCount.CountsFlushed) < 1 && System.nanoTime() < end ) {
			Thread.sleep(10);
		}
	}

	/**
	 * Wait for a statistic to reach a count.
	 */
	private void awaitStat(JdbcNodeServiceAuditorCount stat, long count) throws InterruptedException {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while ( stats.get(stat) < count && System.nanoTime() < end ) {
			Thread.sleep(10);
		}
	}

	/**
	 * Wait for the ping test to give a result, as the writer's state changes.
	 */
	private PingTest.Result awaitPingResult(boolean success) throws Exception {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		PingTest.Result result = auditor.performPingTest();
		while ( result.isSuccess() != success && System.nanoTime() < end ) {
			Thread.sleep(10);
			result = auditor.performPingTest();
		}
		return result;
	}

	@Test
	public void auditNodeService_one() throws Exception {
		// GIVEN
		final int count = 123;

		givenWriterConnection();

		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, count);

		auditor.enableWriting();
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SERVICE_ID);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, count);
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
	public void auditNodeService_concurrentAddAndFlush_noLostCounts() throws Exception {
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
		given(jdbcConnection.prepareCall(JdbcNodeServiceAuditor.DEFAULT_NODE_SERVICE_INCREMENT_SQL))
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
					auditor.auditNodeService((long) rnd.nextInt(nodeCount), TEST_SERVICE_ID, 1);
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
		final int count = 123;

		givenWriterConnection();

		// a long delay, so the writer is waiting for its next flush when the count is added
		auditor.setFlushDelay(TimeUnit.MINUTES.toMillis(1));

		auditor.serviceDidStartup();
		awaitFirstFlush();

		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, count);

		final long start = System.nanoTime();
		auditor.serviceDidShutdown();
		final Duration shutdownTime = Duration.ofNanos(System.nanoTime() - start);

		// THEN
		then(jdbcStatement).should().setObject(1, TEST_NODE_ID);
		then(jdbcStatement).should().setString(2, TEST_SERVICE_ID);
		then(jdbcStatement).should().setTimestamp(3, Timestamp.from(testClock.instant()));
		then(jdbcStatement).should().setInt(4, count);
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
	public void serviceDidShutdown_rightAfterStartup_writesCounts() throws Exception {
		// GIVEN
		final int attempts = 200;
		givenWriterConnection();

		// WHEN
		// shut down as soon as started, often before the writer has connected
		int unwritten = 0;
		for ( int i = 0; i < attempts; i++ ) {
			final ConcurrentMap<DatumId, AtomicInteger> counts = new ConcurrentHashMap<>(8);
			final var a = new JdbcNodeServiceAuditor(dataSource, counts, testClock, stats);
			a.serviceDidStartup();
			a.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 1);
			a.serviceDidShutdown();
			if ( !counts.isEmpty() ) {
				unwritten++;
			}
		}

		// THEN
		then(jdbcStatement).should(times(attempts)).execute();

		// @formatter:off
		and.then(unwritten)
			.as("Count written at every shutdown")
			.isZero()
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
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 123);

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
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 123);
		auditor.auditNodeService(TEST_NODE_ID - 1, TEST_SERVICE_ID, 321);
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
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 123);

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
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 123);

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

	@Test
	public void auditNodeService_writeFailsForOneCount_othersWritten() throws Exception {
		// GIVEN
		final Long badNodeId = -100L;
		final List<Long> goodNodeIds = List.of(1L, 2L, 3L, 4L, 5L);

		givenWriterConnection();

		// the connection stays usable, so the problem is with the count itself
		given(jdbcStatement.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.isValid(anyInt())).willReturn(true);

		// fail to write the bad node's count, as a value too large for the database would
		final AtomicReference<Object> boundNodeId = new AtomicReference<>();
		willAnswer(inv -> {
			boundNodeId.set(inv.getArgument(1));
			return null;
		}).given(jdbcStatement).setObject(eq(1), any());
		given(jdbcStatement.execute()).willAnswer(_ -> {
			if ( badNodeId.equals(boundNodeId.get()) ) {
				throw new SQLException("integer out of range", "22003");
			}
			return false;
		});

		// WHEN
		auditor.auditNodeService(badNodeId, TEST_SERVICE_ID, 1);
		for ( Long nodeId : goodNodeIds ) {
			auditor.auditNodeService(nodeId, TEST_SERVICE_ID, 1);
		}

		auditor.enableWriting();
		awaitFirstFlush();
		auditor.serviceDidShutdown();

		// THEN
		for ( Long nodeId : goodNodeIds ) {
			then(jdbcStatement).should().setObject(1, nodeId);
		}

		// the writer did not give up on its connection
		then(dataSource).should().getConnection();

		// @formatter:off
		and.then(stats.get(JdbcNodeServiceAuditorCount.ResultsDiscarded))
			.as("Bad count discarded")
			.isEqualTo(1L)
			;
		and.then(datumCountMap)
			.as("Bad count not kept to try again")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditNodeService_transientWriteFailure_countWrittenAfterReconnect()
			throws Exception {
		// GIVEN
		final int count = 123;

		givenWriterConnection();

		// fail the first write as a deadlock would, then succeed
		given(jdbcStatement.execute()).willThrow(new SQLException("deadlock detected", "40P01"))
				.willReturn(false);

		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, count);

		auditor.enableWriting();
		awaitStat(JdbcNodeServiceAuditorCount.UpdatesExecuted, 1);
		auditor.serviceDidShutdown();

		// THEN
		// tried again without checking the connection, as the error is transient
		then(jdbcStatement).should(never()).getConnection();
		then(jdbcStatement).should(times(2)).setInt(4, count);
		then(jdbcStatement).should(times(2)).execute();
		then(dataSource).should(times(2)).getConnection();

		// @formatter:off
		and.then(stats.get(JdbcNodeServiceAuditorCount.ResultsDiscarded))
			.as("Count not discarded")
			.isZero()
			;
		and.then(stats.get(JdbcNodeServiceAuditorCount.ResultsAdded))
			.as("Count added back not counted as added again")
			.isEqualTo(1L)
			;
		and.then(stats.get(JdbcNodeServiceAuditorCount.ResultsReadded))
			.as("Count added back counted as re-added")
			.isEqualTo(1L)
			;
		and.then(datumCountMap)
			.as("Count written once tried again")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditNodeService_negativeCount_ignored() {
		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, -5);

		// THEN
		// @formatter:off
		and.then(datumCountMap)
			.as("Negative count ignored")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void auditNodeService_countOverflow_limitedToMaxValue() {
		// WHEN
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, Integer.MAX_VALUE);
		auditor.auditNodeService(TEST_NODE_ID, TEST_SERVICE_ID, 10);

		// THEN
		// @formatter:off
		and.then(datumCountMap.get(DatumId.nodeId(TEST_NODE_ID, TEST_SERVICE_ID, testClock.instant())))
			.as("Total limited to the largest count, rather than overflowing to negative")
			.hasValue(Integer.MAX_VALUE)
			;
		// @formatter:on
	}

	@Test
	public void setUpdateDelay_negative() {
		// @formatter:off
		and.thenThrownBy(() -> auditor.setUpdateDelay(-1))
			.as("Negative delay rejected")
			.isInstanceOf(IllegalArgumentException.class)
			;
		// @formatter:on
	}

	@Test
	public void performPingTest_writerConnectionFails() throws Exception {
		// GIVEN
		final CountDownLatch reconnect = new CountDownLatch(1);
		given(dataSource.getConnection()).willThrow(new SQLException("Connection refused", "08001"))
				.willAnswer(_ -> {
					// connect again only once the failure has been seen
					reconnect.await(5, TimeUnit.SECONDS);
					return jdbcConnection;
				});
		given(jdbcConnection.prepareCall(JdbcNodeServiceAuditor.DEFAULT_NODE_SERVICE_INCREMENT_SQL))
				.willReturn(jdbcStatement);

		// a short delay, so the writer soon tries to connect again
		auditor.setConnectionRecoveryDelay(50);

		// WHEN
		auditor.serviceDidStartup();
		final PingTest.Result failedResult = awaitPingResult(false);
		reconnect.countDown();
		final PingTest.Result recoveredResult = awaitPingResult(true);

		// THEN
		// @formatter:off
		and.then(failedResult.isSuccess())
			.as("Ping fails while the writer cannot connect")
			.isFalse()
			;
		and.then(failedResult.getMessage())
			.as("Ping says why the writer cannot write")
			.contains("Connection refused")
			;
		and.then(recoveredResult.isSuccess())
			.as("Ping passes once the writer has written again")
			.isTrue()
			;
		// @formatter:on
	}

}
