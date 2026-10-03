/* ==================================================================
 * AsyncJdbcChargePointActionStatusDao.java - 15/05/2024 7:39:21 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.jdbc;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusUpdateDao;
import net.solarnetwork.central.ocpp.dao.jdbc.sql.UpsertChargePointIdentifierActionTimestamp;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * Asynchronous JDBC {@link ChargePointActionStatusUpdateDao} implementation.
 *
 * <p>
 * A single writing thread writes updates as soon as it can. Only the latest
 * update of a charge point action needs to be written, so an update replaces
 * any update of the same action not written yet. Actions are written in the
 * order they were first updated.
 * </p>
 *
 * @author matt
 * @version 1.2
 */
public class AsyncJdbcChargePointActionStatusDao
		implements ChargePointActionStatusUpdateDao, ServiceLifecycleObserver, PingTest {

	/** The default value for the {@code updateDelay} property. */
	public static final long DEFAULT_UPDATE_DELAY = 0;

	/** The default value for the {@code flushDelay} property. */
	public static final long DEFAULT_FLUSH_DELAY = 10000;

	/** The default value for the {@code statLogUpdateCount} property. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/** The default value for the {@code connectionRecoveryDelay} property. */
	public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 5000;

	/** The {@code bufferRemovalLagAlertThreshold} default value. */
	public static final int DEFAULT_BUFFER_REMOVAL_LAG_ALERT_THRESHOLD = 500;

	/**
	 * The default value for the {@code shutdownMaxWait} property.
	 *
	 * @since 1.2
	 */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofSeconds(10);

	private static final Logger log = LoggerFactory.getLogger(AsyncJdbcChargePointActionStatusDao.class);

	private final DataSource dataSource;
	private final BlockingQueue<ChargePointActionStatusUpdate> statuses;
	private final ConcurrentMap<ChargePointActionStatusUpdate, ChargePointActionStatusUpdate> latestStatuses;
	private final StatTracker stats;

	private volatile @Nullable WriterThread writerThread;
	private volatile long updateDelay;
	private volatile long connectionRecoveryDelay;
	private volatile int bufferRemovalLagAlertThreshold;
	private volatile Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;

	/**
	 * Constructor.
	 *
	 * <p>
	 * A {@link LinkedBlockingQueue} will be used.
	 * </p>
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 */
	public AsyncJdbcChargePointActionStatusDao(DataSource dataSource) {
		this(dataSource, new LinkedBlockingQueue<>());
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param statuses
	 *        the queue of charge point actions waiting to be written, in the
	 *        order they were first updated
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 */
	public AsyncJdbcChargePointActionStatusDao(DataSource dataSource,
			BlockingQueue<ChargePointActionStatusUpdate> statuses) {
		this(dataSource, statuses, new StatTracker("ChargePointActionStatusUpdater", null, log,
				DEFAULT_STAT_LOG_UPDATE_COUNT));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param statuses
	 *        the queue of charge point actions waiting to be written, in the
	 *        order they were first updated
	 * @param stats
	 *        the statistics counter
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 * @since 1.1
	 */
	public AsyncJdbcChargePointActionStatusDao(DataSource dataSource,
			BlockingQueue<ChargePointActionStatusUpdate> statuses, StatTracker stats) {
		super();
		this.dataSource = requireNonNullArgument(dataSource, "dataSource");
		this.statuses = requireNonNullArgument(statuses, "statuses");
		this.latestStatuses = new ConcurrentHashMap<>();
		this.stats = requireNonNullArgument(stats, "stats");
		setConnectionRecoveryDelay(DEFAULT_CONNECTION_RECOVERY_DELAY);
		setUpdateDelay(DEFAULT_UPDATE_DELAY);
		setStatLogUpdateCount(DEFAULT_STAT_LOG_UPDATE_COUNT);
		setBufferRemovalLagAlertThreshold(DEFAULT_BUFFER_REMOVAL_LAG_ALERT_THRESHOLD);
	}

	@Override
	public void serviceDidStartup() {
		enableWriting();

	}

	/**
	 * Stop writing, after writing any updates waiting to be written.
	 *
	 * <p>
	 * The writing thread writes the waiting updates with its own connection
	 * before it stops. If it has no working connection, or writing fails, it
	 * gives up and logs the updates not written as an error. This waits at most
	 * {@code shutdownMaxWait} for the thread to stop.
	 * </p>
	 */
	@Override
	public void serviceDidShutdown() {
		stopWriting(true, shutdownMaxWait);
	}

	@Override
	public void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer evseId,
			Integer connectorId, String action, String messageId, Instant date) {
		// an update is its own key, as its equality ignores the message ID and date
		final ChargePointActionStatusUpdate upd = new ChargePointActionStatusUpdate(userId,
				chargePointIdentifier, evseId, connectorId, action, messageId, date);
		// queue the action only if it has no update waiting, so it is queued at most once; the
		// writer removes the update when it takes the action from the queue, so an update added
		// after that queues the action again
		if ( latestStatuses.put(upd, upd) != null ) {
			stats.increment(AsyncJdbcChargePointActionStatusCount.ResultsReplaced);
		} else if ( !statuses.offer(upd) ) {
			// the queue is full, so drop the update, letting a later one queue the action again
			latestStatuses.remove(upd);
			return;
		}
		stats.increment(AsyncJdbcChargePointActionStatusCount.ResultsAdded);
	}

	private class WriterThread extends Thread {

		// keepGoing only ever changes from true to false, so a request to exit cannot be undone
		// when the writer starts a new connection, as resetting a combined flag could
		private volatile boolean keepGoing = true;
		private volatile boolean writeOnExit = false;
		private volatile boolean reconnect = false;
		private boolean started = false;

		private boolean hasStarted() {
			return started;
		}

		private boolean isGoing() {
			return keepGoing;
		}

		private void reconnect() {
			reconnect = true;
		}

		private void exit(boolean writeRemaining) {
			// set before keepGoing, so the writer sees it once it sees it is to exit; never
			// cleared, so a later request to just stop cannot cancel writing what remains
			if ( writeRemaining ) {
				writeOnExit = true;
			}
			keepGoing = false;
		}

		@Override
		public void run() {
			// signal before anything that could fail, so enableWriting() is not left waiting
			synchronized ( this ) {
				started = true;
				this.notifyAll();
			}
			stats.increment(AsyncJdbcChargePointActionStatusCount.WriterThreadsStarted);
			try {
				// go at least once, so updates waiting when asked to stop before connecting are written
				do {
					reconnect = false;
					try {
						execute();
					} catch ( SQLException | RuntimeException e ) {
						if ( !keepGoing ) {
							// stopping, so give up rather than try again
							if ( writeOnExit ) {
								log.error("Error writing remaining OCPP charge point action statuses: {}",
										e.toString());
							}
							break;
						}
						if ( e instanceof SQLTransientException ) {
							log.warn("Transient SQL exception with OCPP charge point action status: {}",
									e.toString());
						} else {
							log.warn("Exception with OCPP charge point action status: {}",
									e.getMessage(), e);
						}
						// sleep, then try again
						try {
							Thread.sleep(connectionRecoveryDelay);
						} catch ( InterruptedException e2 ) {
							log.info(
									"Writer thread interrupted during connection recovery: exiting now.");
							keepGoing = false;
						}
					}
				} while ( keepGoing );
				if ( writeOnExit ) {
					logUnwrittenUpdates();
				}
			} finally {
				stats.increment(AsyncJdbcChargePointActionStatusCount.WriterThreadsEnded);
			}
		}

		private PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
			var sql = UpsertChargePointIdentifierActionTimestamp.sql();
			return conn.prepareStatement(sql);
		}

		private void execute() throws SQLException {
			if ( !keepGoing ) {
				// asked to stop before connecting
				if ( !writeOnExit || statuses.isEmpty() ) {
					return;
				}
				// clear the interrupt that asked, so it cannot stop connecting to write what remains
				Thread.interrupted();
			}
			try (Connection conn = dataSource.getConnection()) {
				stats.increment(AsyncJdbcChargePointActionStatusCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = createPreparedStatement(conn);
				try {
					while ( keepGoing && !reconnect ) {
						writeUpdate(stmt, statuses.take(), true);
					}
				} catch ( InterruptedException e ) {
					log.info("Writer thread interrupted: exiting now.");
					keepGoing = false;
				}
				if ( writeOnExit ) {
					writeRemainingUpdates(stmt);
				}
			}
		}

	}

	/**
	 * Write the latest update of a charge point action.
	 *
	 * @param stmt
	 *        the statement to write with
	 * @param key
	 *        the action to write, as taken from the queue
	 * @param throttle
	 *        {@code true} to wait for the update delay after writing
	 * @throws SQLException
	 *         if any error occurs writing
	 * @throws InterruptedException
	 *         if interrupted during the update delay
	 */
	private void writeUpdate(PreparedStatement stmt, ChargePointActionStatusUpdate key,
			boolean throttle) throws SQLException, InterruptedException {
		stats.increment(AsyncJdbcChargePointActionStatusCount.ResultsRemoved);
		// remove the update before writing it, so an update added from now on queues the action again
		final var upd = latestStatuses.remove(key);
		if ( upd == null ) {
			return;
		}
		try {
			UpsertChargePointIdentifierActionTimestamp.prepareStatement(stmt, upd.getUserId(),
					upd.getChargePointIdentifier(), upd.getEvseId(), upd.getConnectorId(),
					upd.getAction(), upd.getMessageId(), upd.getDate());
			stmt.execute();
			stats.increment(AsyncJdbcChargePointActionStatusCount.UpdatesExecuted);
		} catch ( SQLException | RuntimeException e ) {
			stats.increment(AsyncJdbcChargePointActionStatusCount.UpdatesFailed);
			throw e;
		}
		if ( throttle && updateDelay > 0 ) {
			Thread.sleep(updateDelay);
		}
	}

	/**
	 * Write any updates waiting to be written, without the update delay.
	 *
	 * @param stmt
	 *        the statement to write with
	 * @throws SQLException
	 *         if any error occurs writing
	 */
	private void writeRemainingUpdates(PreparedStatement stmt) throws SQLException {
		// clear the interrupt asking the writer to stop, so it cannot disturb writing what remains
		Thread.interrupted();
		if ( statuses.isEmpty() ) {
			return;
		}
		log.info("Writing {} remaining OCPP charge point action statuses", statuses.size());
		try {
			// a few passes, to also write updates added while writing, but not wait on updates
			// that keep coming
			for ( int i = 0; i < 3 && !statuses.isEmpty(); i++ ) {
				for ( int n = statuses.size(); n > 0; n-- ) {
					final var key = statuses.poll();
					if ( key == null ) {
						break;
					}
					writeUpdate(stmt, key, false);
				}
			}
		} catch ( InterruptedException e ) {
			// not thrown without the update delay, but keep the interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Log any updates not written, as an error.
	 */
	private void logUnwrittenUpdates() {
		if ( !latestStatuses.isEmpty() ) {
			log.error("Stopping with {} OCPP charge point action statuses not written: {}",
					latestStatuses.size(), latestStatuses.values());
		}
	}

	/**
	 * Cause the writing thread to re-connect to the database with a new
	 * connection.
	 */
	public synchronized void reconnectWriter() {
		final WriterThread t = writerThread;
		if ( t != null && t.isGoing() ) {
			t.reconnect();
		}
	}

	/**
	 * Enable writing, and wait until the writing thread is going.
	 */
	public synchronized void enableWriting() {
		final WriterThread curr = writerThread;
		if ( curr == null || !curr.isGoing() ) {
			WriterThread t = new WriterThread();
			t.setName("OcppChargePointActionStatusUpdater");
			this.writerThread = t;
			boolean interrupted = false;
			synchronized ( t ) {
				t.start();
				// stop waiting if the thread ends without signalling that it started
				while ( !t.hasStarted() && t.isAlive() ) {
					try {
						t.wait(5000L);
					} catch ( InterruptedException e ) {
						interrupted = true;
					}
				}
			}
			if ( interrupted ) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Disable writing, and wait for the writing thread to stop.
	 *
	 * <p>
	 * The writing thread is interrupted, so it stops without waiting for
	 * another update. Any updates it has not written remain in memory, for a
	 * later writing thread to write. This waits at most {@code shutdownMaxWait}
	 * for the thread to stop.
	 * </p>
	 */
	public void disableWriting() {
		stopWriting(false, shutdownMaxWait);
	}

	/**
	 * Stop writing, after writing any updates waiting to be written, and wait
	 * for the writing thread to stop.
	 *
	 * <p>
	 * This is like {@link #serviceDidShutdown()}, but waits at most {@code max}
	 * rather than {@code shutdownMaxWait} for the thread to stop.
	 * </p>
	 *
	 * @param max
	 *        the maximum time to wait for the writing thread to stop
	 */
	public void shutdownAndWait(Duration max) {
		stopWriting(true, max);
	}

	/**
	 * Stop the writing thread, and wait for it to stop.
	 *
	 * @param writeRemaining
	 *        {@code true} for the writing thread to write any updates waiting
	 *        to be written before it stops, and log any it cannot write
	 * @param maxWait
	 *        the maximum time to wait for the writing thread to stop
	 */
	private synchronized void stopWriting(boolean writeRemaining, Duration maxWait) {
		final WriterThread t = writerThread;
		if ( t == null || !t.isAlive() ) {
			if ( writeRemaining ) {
				// there is no writer to write them
				logUnwrittenUpdates();
			}
			return;
		}
		t.exit(writeRemaining);
		t.interrupt();
		try {
			if ( !t.join(maxWait) ) {
				log.warn("OCPP charge point action status writer thread {} did not stop within {}",
						t.getName(), maxWait);
			}
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public String getPingTestId() {
		return getClass().getName();
	}

	@Override
	public String getPingTestName() {
		return "JDBC OCPP Charge Point Action Status Updater";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		final Map<String, Long> statMap = stats.allCounts();
		// verify buffer removals does not lag additions; replaced updates are never removed
		final long addCount = statMap
				.getOrDefault(AsyncJdbcChargePointActionStatusCount.ResultsAdded.name(), 0L);
		final long removeLag = addCount
				- statMap.getOrDefault(AsyncJdbcChargePointActionStatusCount.ResultsReplaced.name(),
						0L)
				- statMap.getOrDefault(AsyncJdbcChargePointActionStatusCount.ResultsRemoved.name(), 0L);
		final WriterThread t = this.writerThread;
		final boolean writerRunning = t != null && t.isAlive();
		if ( removeLag > bufferRemovalLagAlertThreshold ) {
			return new PingTestResult(false,
					format("Buffer removal lag %d > %d", removeLag, bufferRemovalLagAlertThreshold),
					statMap);
		}
		if ( !writerRunning ) {
			return new PingTestResult(false,
					(t == null ? "Writer thread missing." : "Writer thread dead."), statMap);
		}
		return new PingTestResult(true, format("Processed %d updates; lag %d.", addCount, removeLag),
				statMap);
	}

	/**
	 * Set the delay, in milliseconds, to wait after a JDBC connection error
	 * before trying to recover and connect again.
	 *
	 * @param connectionRecoveryDelay
	 *        the delay, in milliseconds; defaults to
	 *        {@link #DEFAULT_CONNECTION_RECOVERY_DELAY}
	 * @throws IllegalArgumentException
	 *         if {@code connectionRecoveryDelay} is &lt; 0
	 */
	public void setConnectionRecoveryDelay(long connectionRecoveryDelay) {
		if ( connectionRecoveryDelay < 0 ) {
			throw new IllegalArgumentException("connectionRecoveryDelay must be >= 0");
		}
		this.connectionRecoveryDelay = connectionRecoveryDelay;
	}

	/**
	 * Set the delay, in milliseconds, to wait after executing JDBC statements
	 * within a loop before executing another statement.
	 *
	 * @param updateDelay
	 *        the delay, in milliseconds; set to 0 for no delay; defaults to
	 *        {@link #DEFAULT_UPDATE_DELAY}
	 */
	public void setUpdateDelay(long updateDelay) {
		this.updateDelay = updateDelay;
	}

	/**
	 * Set the statistic log update count.
	 *
	 * <p>
	 * Setting this to something greater than {@literal 0} will cause
	 * {@literal INFO} level statistic log entries to be emitted every
	 * {@code statLogUpdateCount} records have been updated in the database.
	 * </p>
	 *
	 * @param statLogUpdateCount
	 *        the update count; defaults to
	 *        {@link #DEFAULT_STAT_LOG_UPDATE_COUNT}
	 */
	public void setStatLogUpdateCount(int statLogUpdateCount) {
		stats.setLogFrequency(statLogUpdateCount);
	}

	/**
	 * Get the datum cache removal alert threshold.
	 *
	 * @return the threshold
	 */
	public int getBufferRemovalLagAlertThreshold() {
		return bufferRemovalLagAlertThreshold;
	}

	/**
	 * Set the datum cache removal alert threshold.
	 *
	 * <p>
	 * This threshold represents the number of updates waiting to be written:
	 * the {@link AsyncJdbcChargePointActionStatusCount#ResultsAdded} statistic
	 * less the {@link AsyncJdbcChargePointActionStatusCount#ResultsReplaced}
	 * and {@link AsyncJdbcChargePointActionStatusCount#ResultsRemoved}
	 * statistics. If the {@code ResultsRemoved} count lags behind it means
	 * updates are not getting persisted fast enough. Passing this threshold
	 * will trigger a failure {@link PingTest} result in
	 * {@link #performPingTest()}.
	 * </p>
	 *
	 * @param bufferRemovalLagAlertThreshold
	 *        the threshold to set
	 */
	public void setBufferRemovalLagAlertThreshold(int bufferRemovalLagAlertThreshold) {
		this.bufferRemovalLagAlertThreshold = bufferRemovalLagAlertThreshold;
	}

	/**
	 * Set the maximum amount of time to wait for the writing thread to stop,
	 * including writing any remaining updates when the service shuts down.
	 *
	 * @param shutdownMaxWait
	 *        the maximum time to wait; if {@code null} then
	 *        {@link #DEFAULT_SHUTDOWN_MAX_WAIT} will be used
	 * @since 1.2
	 */
	public void setShutdownMaxWait(@Nullable Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
	}
}
