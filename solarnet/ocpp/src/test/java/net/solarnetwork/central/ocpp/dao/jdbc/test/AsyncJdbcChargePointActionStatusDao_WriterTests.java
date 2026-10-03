/* ==================================================================
 * AsyncJdbcChargePointActionStatusDao_WriterTests.java - 3/10/2026 7:18:50 pm
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

package net.solarnetwork.central.ocpp.dao.jdbc.test;

import static net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount.ResultsAdded;
import static net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount.ResultsRemoved;
import static net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount.ResultsReplaced;
import static net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount.UpdatesExecuted;
import static net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusCount.UpdatesFailed;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.withSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.ocpp.dao.jdbc.AsyncJdbcChargePointActionStatusDao;
import net.solarnetwork.central.ocpp.dao.jdbc.ChargePointActionStatusUpdate;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointIdentifierActionTimestamp;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link AsyncJdbcChargePointActionStatusDao} writer, with
 * a mock JDBC connection.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AsyncJdbcChargePointActionStatusDao_WriterTests {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncJdbcChargePointActionStatusDao_WriterTests.class);

	private static final long RECONNECT_DELAY = 300;
	private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(2);
	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_CHARGER_IDENT = "test.charger";
	private static final String TEST_ACTION = "MeterValues";

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection jdbcConnection;

	@Mock
	private PreparedStatement jdbcStatement;

	@Captor
	private ArgumentCaptor<String> messageIdCaptor;

	private BlockingQueue<ChargePointActionStatusUpdate> queue;
	private StatTracker stats;
	private AsyncJdbcChargePointActionStatusDao dao;
	private Instant nextDate;

	@BeforeEach
	public void setup() {
		queue = new LinkedBlockingQueue<>();
		stats = new StatTracker("ChargePointActionStatusUpdater", "", log, 20);
		dao = new AsyncJdbcChargePointActionStatusDao(dataSource, queue, stats);
		dao.setConnectionRecoveryDelay(RECONNECT_DELAY);
		nextDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
	}

	@AfterEach
	public void teardown() {
		// stop any writer a test left running
		dao.shutdownAndWait(SHUTDOWN_WAIT);
	}

	private void givenWriterConnection() throws SQLException {
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareStatement(UpsertChargePointIdentifierActionTimestamp.sql()))
				.willReturn(jdbcStatement);
	}

	/**
	 * Make the writer wait while it writes its first update, until released.
	 *
	 * @param release
	 *        the latch to wait for
	 * @return a latch counted down once the writer is writing its first update
	 */
	private CountDownLatch givenFirstWriteWaitsFor(CountDownLatch release) throws SQLException {
		final CountDownLatch writing = new CountDownLatch(1);
		given(jdbcStatement.execute()).willAnswer(_ -> {
			if ( writing.getCount() > 0 ) {
				writing.countDown();
				release.await(5, TimeUnit.SECONDS);
			}
			return false;
		});
		return writing;
	}

	/**
	 * Add an update for the test user, with a date one second after the
	 * previous update's.
	 *
	 * @return the date of the update
	 */
	private Instant update(String chargerIdent, int evseId, int connectorId, String action,
			String messageId) {
		final Instant date = nextDate;
		nextDate = nextDate.plusSeconds(1);
		dao.updateActionTimestamp(TEST_USER_ID, chargerIdent, evseId, connectorId, action, messageId,
				date);
		return date;
	}

	/**
	 * Wait for the writer to write every update added that was not replaced.
	 */
	private void awaitAllWritten() throws InterruptedException {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while ( System.nanoTime() < end ) {
			final long waiting = stats.get(ResultsAdded) - stats.get(ResultsReplaced)
					- stats.get(UpdatesExecuted) - stats.get(UpdatesFailed);
			if ( waiting < 1 ) {
				return;
			}
			Thread.sleep(10);
		}
	}

	private static int[] toArray(AtomicIntegerArray array) {
		return IntStream.range(0, array.length()).map(array::get).toArray();
	}

	@Test
	public void updateActionTimestamp_sameAction_latestWritten() throws Exception {
		// GIVEN
		givenWriterConnection();

		// WHEN
		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "m1");
		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "m2");
		final Instant lastDate = update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "m3");

		dao.serviceDidStartup();
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		then(jdbcStatement).should().setString(eq(4), messageIdCaptor.capture());
		then(jdbcStatement).should().setTimestamp(5, Timestamp.from(lastDate));
		then(jdbcStatement).should().execute();

		// @formatter:off
		and.then(messageIdCaptor.getValue())
			.as("Latest update written")
			.isEqualTo("m3")
			;
		and.then(queue)
			.as("Queue emptied")
			.isEmpty()
			;
		and.then(stats)
			.as("Every update added")
			.returns(3L, from(s -> s.get(ResultsAdded)))
			.as("Earlier updates replaced by the latest")
			.returns(2L, from(s -> s.get(ResultsReplaced)))
			.as("Action removed from the queue once")
			.returns(1L, from(s -> s.get(ResultsRemoved)))
			.as("Action written once")
			.returns(1L, from(s -> s.get(UpdatesExecuted)))
			;
		// @formatter:on
	}

	@Test
	public void updateActionTimestamp_differentActions_latestOfEachWrittenInOrderFirstUpdated()
			throws Exception {
		// GIVEN
		givenWriterConnection();

		// WHEN
		// actions differing by connector, action name, EVSE, and charger
		update(TEST_CHARGER_IDENT, 1, 1, TEST_ACTION, "a1");
		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "b1");
		update(TEST_CHARGER_IDENT, 1, 1, TEST_ACTION, "a2");
		update(TEST_CHARGER_IDENT, 1, 1, "StatusNotification", "c1");
		update(TEST_CHARGER_IDENT, 2, 1, TEST_ACTION, "d1");
		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "b2");
		update("other.charger", 1, 1, TEST_ACTION, "e1");
		update(TEST_CHARGER_IDENT, 1, 1, TEST_ACTION, "a3");

		dao.serviceDidStartup();
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		then(jdbcStatement).should(times(5)).setString(eq(4), messageIdCaptor.capture());
		then(jdbcStatement).should(times(5)).execute();

		// @formatter:off
		and.then(messageIdCaptor.getAllValues())
			.as("Latest update of each action written, in the order each action was first updated")
			.containsExactly("a3", "b2", "c1", "d1", "e1")
			;
		and.then(stats)
			.as("Earlier updates replaced by the latest")
			.returns(3L, from(s -> s.get(ResultsReplaced)))
			;
		// @formatter:on
	}

	@Test
	public void updateActionTimestamp_sameActionWhileWriting_writtenAgain() throws Exception {
		// GIVEN
		givenWriterConnection();
		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch writing = givenFirstWriteWaitsFor(release);
		dao.serviceDidStartup();

		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "m1");
		final boolean writerWriting = writing.await(5, TimeUnit.SECONDS);

		// WHEN
		update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "m2");
		release.countDown();
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		then(jdbcStatement).should(times(2)).setString(eq(4), messageIdCaptor.capture());

		// @formatter:off
		and.then(writerWriting)
			.as("Writer writing first update")
			.isTrue()
			;
		and.then(messageIdCaptor.getAllValues())
			.as("Update added while the action was being written is written after it")
			.containsExactly("m1", "m2")
			;
		and.then(stats)
			.as("Update added while the action was being written did not replace anything")
			.returns(0L, from(s -> s.get(ResultsReplaced)))
			;
		// @formatter:on
	}

	@Test
	public void pingTest_replacedUpdatesNotLag() throws Exception {
		// GIVEN
		givenWriterConnection();
		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch writing = givenFirstWriteWaitsFor(release);
		dao.setBufferRemovalLagAlertThreshold(2);
		dao.serviceDidStartup();

		update(TEST_CHARGER_IDENT, 1, 1, TEST_ACTION, "a1");
		final boolean writerWriting = writing.await(5, TimeUnit.SECONDS);

		// WHEN
		for ( int i = 1; i <= 5; i++ ) {
			update(TEST_CHARGER_IDENT, 1, 2, TEST_ACTION, "b" + i);
		}
		final PingTest.Result result = dao.performPingTest();
		release.countDown();
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		then(jdbcStatement).should(times(2)).setString(eq(4), messageIdCaptor.capture());

		// @formatter:off
		and.then(writerWriting)
			.as("Writer writing first update")
			.isTrue()
			;
		and.then(result)
			.as("Ping OK, as only one update is waiting to be written")
			.returns(true, from(PingTest.Result::isSuccess))
			;
		and.then(messageIdCaptor.getAllValues())
			.as("Latest update of each action written")
			.containsExactly("a1", "b5")
			;
		// @formatter:on
	}

	@Test
	public void pingTest_waitingActionsLag() throws Exception {
		// GIVEN
		givenWriterConnection();
		final CountDownLatch release = new CountDownLatch(1);
		final CountDownLatch writing = givenFirstWriteWaitsFor(release);
		dao.setBufferRemovalLagAlertThreshold(2);
		dao.serviceDidStartup();

		update(TEST_CHARGER_IDENT, 1, 1, TEST_ACTION, "a1");
		final boolean writerWriting = writing.await(5, TimeUnit.SECONDS);

		// WHEN
		for ( int i = 2; i <= 4; i++ ) {
			update(TEST_CHARGER_IDENT, 1, i, TEST_ACTION, "c" + i);
		}
		final PingTest.Result result = dao.performPingTest();
		release.countDown();
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		// @formatter:off
		and.then(writerWriting)
			.as("Writer writing first update")
			.isTrue()
			;
		and.then(result)
			.as("Ping fails, as more updates are waiting to be written than the threshold")
			.returns(false, from(PingTest.Result::isSuccess))
			.as("Lag reported")
			.returns("Buffer removal lag 3 > 2", from(PingTest.Result::getMessage))
			;
		// @formatter:on
	}

	@Test
	public void updateActionTimestamp_concurrentUpdatesAndWrites_latestOfEachActionWritten()
			throws Exception {
		// GIVEN
		final int producerCount = 4;
		final int actionsPerProducer = 8;
		final int updatesPerProducer = 5000;
		final int actionCount = producerCount * actionsPerProducer;
		final long updateCount = (long) producerCount * updatesPerProducer;

		// stub-only statement that tracks the sequence of each update written, to avoid recording
		// every invocation; each action is updated by one producer, so its sequence only increases
		final PreparedStatement stmt = mock(PreparedStatement.class, withSettings().stubOnly());
		final AtomicIntegerArray lastWritten = new AtomicIntegerArray(actionCount);
		final AtomicInteger outOfOrder = new AtomicInteger();
		willAnswer(inv -> {
			if ( inv.getArgument(0, Integer.class) == 4 ) {
				// the message ID is "<action index>-<sequence>"
				final String[] msgId = inv.getArgument(1, String.class).split("-");
				final int seq = Integer.parseInt(msgId[1]);
				if ( lastWritten.getAndSet(Integer.parseInt(msgId[0]), seq) >= seq ) {
					outOfOrder.incrementAndGet();
				}
			}
			return null;
		}).given(stmt).setString(anyInt(), anyString());
		given(dataSource.getConnection()).willReturn(jdbcConnection);
		given(jdbcConnection.prepareStatement(UpsertChargePointIdentifierActionTimestamp.sql()))
				.willReturn(stmt);

		dao.setStatLogUpdateCount(0);
		dao.serviceDidStartup();

		// WHEN
		final AtomicIntegerArray lastAdded = new AtomicIntegerArray(actionCount);
		final ExecutorService producers = Executors.newFixedThreadPool(producerCount);
		final boolean producersDone;
		try {
			for ( int p = 0; p < producerCount; p++ ) {
				final int firstAction = p * actionsPerProducer;
				producers.execute(() -> {
					final ThreadLocalRandom rng = ThreadLocalRandom.current();
					for ( int seq = 1; seq <= updatesPerProducer; seq++ ) {
						final int idx = firstAction + rng.nextInt(actionsPerProducer);
						dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, 0, 0,
								"Action" + idx, idx + "-" + seq, Instant.now());
						lastAdded.set(idx, seq);
						if ( rng.nextInt(16) == 0 ) {
							// pause now and then, to vary how updates and writes interleave
							LockSupport.parkNanos(rng.nextLong(50_000));
						}
					}
				});
			}
			producers.shutdown();
			producersDone = producers.awaitTermination(30, TimeUnit.SECONDS);
		} finally {
			producers.shutdownNow();
		}
		awaitAllWritten();
		dao.shutdownAndWait(SHUTDOWN_WAIT);

		// THEN
		// @formatter:off
		and.then(producersDone)
			.as("Producers finished")
			.isTrue()
			;
		and.then(toArray(lastWritten))
			.as("Latest update of each action written")
			.containsExactly(toArray(lastAdded))
			;
		and.then(outOfOrder.get())
			.as("No update written after a later one for the same action")
			.isZero()
			;
		and.then(queue)
			.as("Queue emptied")
			.isEmpty()
			;
		and.then(stats)
			.as("Every update added")
			.returns(updateCount, from(s -> s.get(ResultsAdded)))
			.as("Every update written or replaced")
			.returns(updateCount, from(s -> s.get(UpdatesExecuted) + s.get(ResultsReplaced)))
			;
		// @formatter:on
	}

}
