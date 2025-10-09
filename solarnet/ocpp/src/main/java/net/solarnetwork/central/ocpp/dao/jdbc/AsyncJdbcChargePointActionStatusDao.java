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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
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
 * @author matt
 * @version 1.1
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

	private static final Logger log = LoggerFactory.getLogger(AsyncJdbcChargePointActionStatusDao.class);

	private final DataSource dataSource;
	private final BlockingQueue<ChargePointActionStatusUpdate> statuses;
	private final StatTracker stats;

	private WriterThread writerThread;
	private long updateDelay;
	private long connectionRecoveryDelay;
	private int bufferRemovalLagAlertThreshold;

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
	 *         if any parameter is {@literal null}
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
	 *        the map to use for tracking status updates
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
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
	 *        the map to use for tracking status updates
	 * @param stats
	 *        the statistics counter
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 * @since 1.1
	 */
	public AsyncJdbcChargePointActionStatusDao(DataSource dataSource,
			BlockingQueue<ChargePointActionStatusUpdate> statuses, StatTracker stats) {
		super();
		this.dataSource = requireNonNullArgument(dataSource, "dataSource");
		this.statuses = requireNonNullArgument(statuses, "statuses");
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

	@Override
	public void serviceDidShutdown() {
		disableWriting();
	}

	@Override
	public void updateActionTimestamp(Long userId, String chargePointIdentifier, Integer evseId,
			Integer connectorId, String action, String messageId, Instant date) {
		ChargePointActionStatusUpdate upd = new ChargePointActionStatusUpdate(userId,
				chargePointIdentifier, evseId, connectorId, action, messageId, date);
		if ( statuses.offer(upd) ) {
			stats.increment(AsyncJdbcChargePointActionStatusCount.ResultsAdded);
		}
	}

	private class WriterThread extends Thread {

		private final AtomicBoolean keepGoingWithConnection = new AtomicBoolean(true);
		private final AtomicBoolean keepGoing = new AtomicBoolean(true);
		private boolean started = false;

		private boolean hasStarted() {
			return started;
		}

		private boolean isGoing() {
			return keepGoing.get();
		}

		private void reconnect() {
			keepGoingWithConnection.compareAndSet(true, false);
		}

		private void exit() {
			keepGoing.compareAndSet(true, false);
			keepGoingWithConnection.compareAndSet(true, false);
		}

		@Override
		public void run() {
			stats.increment(AsyncJdbcChargePointActionStatusCount.WriterThreadsStarted);
			try {
				while ( keepGoing.get() ) {
					keepGoingWithConnection.set(true);
					synchronized ( this ) {
						started = true;
						this.notifyAll();
					}
					try {
						keepGoing.compareAndSet(true, execute());
					} catch ( SQLException | RuntimeException e ) {
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
							log.info("Writer thread interrupted: exiting now.");
							keepGoing.set(false);
						}
					}
				}
			} finally {
				stats.increment(AsyncJdbcChargePointActionStatusCount.WriterThreadsEnded);
			}
		}

		private PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
			var sql = UpsertChargePointIdentifierActionTimestamp.sql();
			return conn.prepareStatement(sql);
		}

		private Boolean execute() throws SQLException {
			try (Connection conn = dataSource.getConnection()) {
				stats.increment(AsyncJdbcChargePointActionStatusCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = createPreparedStatement(conn);
				do {
					try {
						flushUpdates(stmt);
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
					} catch ( InterruptedException e ) {
						log.info("Writer thread interrupted: exiting now.");
						return false;
					}
				} while ( keepGoingWithConnection.get() );
				return true;
			}
		}

		private void flushUpdates(PreparedStatement stmt) throws SQLException, InterruptedException {
			while ( keepGoingWithConnection.get() ) {
				var upd = statuses.take();
				stats.increment(AsyncJdbcChargePointActionStatusCount.ResultsRemoved);
				try {
					UpsertChargePointIdentifierActionTimestamp.prepareStatement(stmt, upd.getUserId(),
							upd.getChargePointIdentifier(), upd.getEvseId(), upd.getConnectorId(),
							upd.getAction(), upd.getMessageId(), upd.getDate());
					stmt.execute();
					stats.increment(AsyncJdbcChargePointActionStatusCount.UpdatesExecuted);
				} catch ( SQLException e ) {
					stats.increment(AsyncJdbcChargePointActionStatusCount.UpdatesFailed);
					throw e;
				} catch ( Exception e ) {
					stats.increment(AsyncJdbcChargePointActionStatusCount.UpdatesFailed);
					RuntimeException re;
					if ( e instanceof RuntimeException runtime ) {
						re = runtime;
					} else {
						re = new RuntimeException("Exception flushing OCPP charge point action status",
								e);
					}
					throw re;
				}
				if ( updateDelay > 0 ) {
					Thread.sleep(updateDelay);
				}
			}
		}
	}

	/**
	 * Cause the writing thread to re-connect to the database with a new
	 * connection.
	 */
	public synchronized void reconnectWriter() {
		if ( writerThread != null && writerThread.isGoing() ) {
			writerThread.reconnect();
		}
	}

	/**
	 * Enable writing, and wait until the writing thread is going.
	 */
	public synchronized void enableWriting() {
		if ( writerThread == null || !writerThread.isGoing() ) {
			WriterThread t = new WriterThread();
			t.setName("OcppChargePointActionStatusUpdater");
			this.writerThread = t;
			synchronized ( t ) {
				t.start();
				while ( !t.hasStarted() ) {
					try {
						t.wait(5000L);
					} catch ( InterruptedException e ) {
						// ignore
					}
				}
			}
		}
	}

	/**
	 * Disable writing.
	 */
	public synchronized void disableWriting() {
		if ( writerThread != null ) {
			writerThread.exit();
			writerThread.interrupt();
		}
	}

	/**
	 * Disable writing, waiting for the writer thread to exit.
	 */
	public synchronized void shutdownAndWait(Duration max) {
		disableWriting();
		if ( writerThread != null && writerThread.isAlive() ) {
			try {
				writerThread.join(max);
			} catch ( InterruptedException e ) {
				// ignore and continue
			}
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
		// verify buffer removals does not lag additions
		final long addCount = statMap
				.getOrDefault(AsyncJdbcChargePointActionStatusCount.ResultsAdded.name(), 0L);
		final long removeLag = addCount
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
					(writerThread == null ? "Writer thread missing." : "Writer thread dead."), statMap);
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
	 * This threshold represents the <i>difference</i> between the
	 * {@link AsyncJdbcChargePointActionStatusCount#ResultsAdded} and
	 * {@link AsyncJdbcChargePointActionStatusCount#ResultsRemoved} statistics.
	 * If the {@code ResultsRemoved} count lags behind {@code ResultsAdded} it
	 * means updates are not getting persisted fast enough. Passing this
	 * threshold will trigger a failure {@link PingTest} result in
	 * {@link #performPingTest()}.
	 * </p>
	 *
	 * @param bufferRemovalLagAlertThreshold
	 *        the threshold to set
	 */
	public void setBufferRemovalLagAlertThreshold(int bufferRemovalLagAlertThreshold) {
		this.bufferRemovalLagAlertThreshold = bufferRemovalLagAlertThreshold;
	}
}
