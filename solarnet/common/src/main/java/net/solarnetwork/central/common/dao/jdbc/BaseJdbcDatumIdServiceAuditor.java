/* ==================================================================
 * BaseJdbcDatumIdServiceAuditor.java - 29/10/2024 9:59:09 am
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * Base class for {@link DatumId} related service auditors using JDBC.
 *
 * @author matt
 * @version 1.1
 */
public abstract class BaseJdbcDatumIdServiceAuditor implements PingTest, ServiceLifecycleObserver {

	/**
	 * The default value for the {@code updateDelay} property.
	 */
	public static final long DEFAULT_UPDATE_DELAY = 100;

	/**
	 * The default value for the {@code flushDelay} property.
	 */
	public static final long DEFAULT_FLUSH_DELAY = 10000;

	/**
	 * The default value for the {@code connectionRecoveryDelay} property.
	 */
	public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

	/**
	 * The default value for the {@code shutdownMaxWait} property.
	 *
	 * @since 1.1
	 */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofSeconds(10);

	/**
	 * A regular expression that matches if a JDBC statement is a
	 * {@link CallableStatement}.
	 */
	public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*}",
			Pattern.CASE_INSENSITIVE);

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The JDBC data source. */
	protected final DataSource dataSource;

	/**
	 * A temporary cache of service counters.
	 *
	 * <p>
	 * This cache is where service updates are performed. The primary key is a
	 * {@link DatumId} but the actual meaning of the kind, object ID, source ID,
	 * and timestamp components are service dependent. For example a node-based
	 * service might treat the {@code objectId} and {@code sourceId} as node ID
	 * and datum source ID values, while a user-based service might treat the
	 * {@code objectId} as a user ID and the {@code sourceId} as a service name.
	 * </p>
	 */
	protected final ConcurrentMap<DatumId, AtomicInteger> serviceCounters;

	/**
	 * The clock to use.
	 *
	 * <p>
	 * A tick-based clock can be used to group updates into time-based
	 * "buckets".
	 * </p>
	 */
	protected final Clock clock;
	protected final StatTracker statCounter;

	private final String writerThreadName;

	private volatile @Nullable String serviceIncrementSql;

	private volatile @Nullable WriterThread writerThread;
	private long updateDelay;
	private long flushDelay;
	private long connectionRecoveryDelay;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param serviceCounters
	 *        the service counters map; the map must perform {@code compute()}
	 *        atomically, as {@link java.util.concurrent.ConcurrentHashMap} does
	 * @param clock
	 *        the clock to use; a tick-based clock is typical, to align updates
	 *        to time-based "buckets"
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BaseJdbcDatumIdServiceAuditor(DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> serviceCounters, Clock clock,
			StatTracker statCounter) {
		super();
		this.dataSource = requireNonNullArgument(dataSource, "dataSource");
		this.serviceCounters = requireNonNullArgument(serviceCounters, "serviceCounters");
		this.clock = requireNonNullArgument(clock, "clock");
		this.statCounter = requireNonNullArgument(statCounter, "statCounter");
		this.writerThreadName = statCounter.getDisplayName() + "Writer";
		setConnectionRecoveryDelay(DEFAULT_CONNECTION_RECOVERY_DELAY);
		setFlushDelay(DEFAULT_FLUSH_DELAY);
		setUpdateDelay(DEFAULT_UPDATE_DELAY);
	}

	@Override
	public void serviceDidStartup() {
		enableWriting();

	}

	/**
	 * Stop writing, after writing any counts not yet written.
	 *
	 * <p>
	 * The writing thread writes the remaining counts with its own connection
	 * before it stops. If it has no working connection, or writing fails, it
	 * gives up and logs the counts not written as an error.
	 * </p>
	 */
	@Override
	public void serviceDidShutdown() {
		stopWriting(true);
	}

	/**
	 * Add a service count.
	 *
	 * @param key
	 *        the key of the count
	 * @param count
	 *        the count to add
	 */
	protected void addServiceCount(DatumId key, int count) {
		// increment within compute() so the update is serialized with flushServiceData() removing
		// the counter; otherwise the count could land on a counter that was just removed
		serviceCounters.compute(key, (_, counter) -> {
			if ( counter == null ) {
				return new AtomicInteger(count);
			}
			counter.addAndGet(count);
			return counter;
		});
		statCounter.increment(JdbcNodeServiceAuditorCount.ResultsAdded);
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
			log.info("Started JDBC audit writer thread {}", this);
			statCounter.increment(JdbcNodeServiceAuditorCount.WriterThreadsStarted);
			try {
				while ( keepGoing ) {
					reconnect = false;
					try {
						execute();
					} catch ( SQLException | RuntimeException e ) {
						if ( !keepGoing ) {
							// stopping, so give up rather than try again
							if ( writeOnExit ) {
								log.error("Error writing remaining audit counts: {}", e.toString());
							}
							break;
						}
						log.warn("Exception with auditing", e);
						// sleep, then try again
						try {
							Thread.sleep(connectionRecoveryDelay);
						} catch ( InterruptedException e2 ) {
							log.info("Audit writer thread interrupted: exiting now.");
							keepGoing = false;
						}
					}
				}
				if ( writeOnExit ) {
					logUnwrittenServiceData();
				}
			} finally {
				statCounter.increment(JdbcNodeServiceAuditorCount.WriterThreadsEnded);
			}
		}

		private void execute() throws SQLException {
			final String sql = nonnull(serviceIncrementSql, "serviceIncrementSql");
			try (Connection conn = dataSource.getConnection()) {
				statCounter.increment(JdbcNodeServiceAuditorCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = isCallableStatement(sql) ? conn.prepareCall(sql)
						: conn.prepareStatement(sql);
				while ( keepGoing && !reconnect ) {
					try {
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
						flushServiceData(stmt, true);
						Thread.sleep(flushDelay);
					} catch ( InterruptedException e ) {
						log.info("Writer thread interrupted: exiting now.");
						keepGoing = false;
					}
				}
				if ( writeOnExit ) {
					writeRemainingServiceData(stmt);
				}
			}
		}

	}

	private void flushServiceData(PreparedStatement stmt, boolean throttle)
			throws SQLException, InterruptedException {
		statCounter.increment(JdbcNodeServiceAuditorCount.CountsFlushed);
		for ( DatumId key : serviceCounters.keySet() ) {
			// remove the counter, rather than reset it to 0 and remove it on a later flush if it
			// is still 0, so a count added after this goes to a new counter rather than being lost
			final AtomicInteger counter = serviceCounters.remove(key);
			if ( counter == null ) {
				continue;
			}
			final int count = counter.get();
			if ( count < 1 ) {
				statCounter.increment(JdbcNodeServiceAuditorCount.ZeroCountsCleared);
				continue;
			}
			try {
				if ( log.isTraceEnabled() ) {
					log.trace("Incrementing node {} service {} @ {} count by {}", key.getObjectId(),
							key.getSourceId(), key.getTimestamp(), count);
				}
				stmt.setObject(1, key.getObjectId());
				stmt.setString(2, key.getSourceId());
				stmt.setTimestamp(3, Timestamp.from(key.getTimestamp()));
				stmt.setInt(4, count);
				stmt.execute();
			} catch ( SQLException | RuntimeException e ) {
				statCounter.increment(JdbcNodeServiceAuditorCount.UpdatesFailed);
				addServiceCount(key, count);
				statCounter.increment(JdbcNodeServiceAuditorCount.ResultsReadded);
				throw e;
			}
			// the count has been written now, so it must not be added back if what follows fails,
			// such as being interrupted during the update delay, or it would be written again
			statCounter.increment(JdbcNodeServiceAuditorCount.UpdatesExecuted);
			if ( throttle && updateDelay > 0 ) {
				Thread.sleep(updateDelay);
			}
		}
	}

	/**
	 * Write any counts not yet written, without the update delay.
	 *
	 * @param stmt
	 *        the statement to write with
	 * @throws SQLException
	 *         if any error occurs writing
	 */
	private void writeRemainingServiceData(PreparedStatement stmt) throws SQLException {
		if ( serviceCounters.isEmpty() ) {
			return;
		}
		log.info("Writing {} remaining audit counts", serviceCounters.size());
		try {
			// a few passes, to also write counts added while writing
			for ( int i = 0; i < 3 && !serviceCounters.isEmpty(); i++ ) {
				flushServiceData(stmt, false);
			}
		} catch ( InterruptedException e ) {
			// not thrown without the update delay, but keep the interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Log any counts not written, as an error.
	 */
	private void logUnwrittenServiceData() {
		if ( !serviceCounters.isEmpty() ) {
			log.error("Stopping with {} audit counts not written: {}", serviceCounters.size(),
					serviceCounters);
		}
	}

	private boolean isCallableStatement(String sql) {
		Matcher m = CALLABLE_STATEMENT_REGEX.matcher(sql);
		return m.matches();
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
			t.setName(writerThreadName);
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
	 * The writing thread is interrupted, so it stops without waiting for its
	 * next flush. Any counts it has not written remain in memory, for a later
	 * writing thread to write. This waits at most {@code shutdownMaxWait} for
	 * the thread to stop.
	 * </p>
	 */
	public void disableWriting() {
		stopWriting(false);
	}

	/**
	 * Stop the writing thread, and wait for it to stop.
	 *
	 * @param writeRemaining
	 *        {@code true} for the writing thread to write any remaining counts
	 *        before it stops, and log any it cannot write
	 */
	private synchronized void stopWriting(boolean writeRemaining) {
		final WriterThread t = writerThread;
		if ( t == null || !t.isAlive() ) {
			if ( writeRemaining ) {
				// there is no writer to write them
				logUnwrittenServiceData();
			}
			return;
		}
		t.exit(writeRemaining);
		t.interrupt();
		try {
			if ( !t.join(shutdownMaxWait) ) {
				log.warn("Audit writer thread {} did not stop within {}", t.getName(),
						shutdownMaxWait);
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
		return "JDBC Query Auditor";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		final WriterThread t = this.writerThread;
		boolean writerRunning = t != null && t.isAlive();
		Map<String, Long> statMap = statCounter.allCounts();
		if ( !writerRunning ) {
			return new PingTestResult(false,
					(t == null ? "Writer thread missing." : "Writer thread dead."), statMap);
		}
		return new PingTestResult(true, "Writer thread alive.", statMap);
	}

	/**
	 * Set the delay, in milliseconds, between flushing cached audit data.
	 *
	 * @param flushDelay
	 *        the delay, in milliseconds; defaults to
	 *        {@link #DEFAULT_FLUSH_DELAY}
	 * @throws IllegalArgumentException
	 *         if {@code flushDelay} is &lt; 0
	 */
	public final void setFlushDelay(long flushDelay) {
		if ( flushDelay < 0 ) {
			throw new IllegalArgumentException("flushDelay must be >= 0");
		}
		this.flushDelay = flushDelay;
	}

	/**
	 * Set the delay, in milliseconds, to wait after a JDBC connection error
	 * before trying to recover and connect again.
	 *
	 * @param connectionRecoveryDelay
	 *        the delay, in milliseconds; defaults t[
	 *        {@link #DEFAULT_CONNECTION_RECOVERY_DELAY}
	 * @throws IllegalArgumentException
	 *         if {@code connectionRecoveryDelay} is &lt; 0
	 */
	public final void setConnectionRecoveryDelay(long connectionRecoveryDelay) {
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
	 *        the delay, in milliseconds; defaults t[
	 *        {@link #DEFAULT_UPDATE_DELAY}
	 * @throws IllegalArgumentException
	 *         if {@code updateDelay} is &lt; 0
	 */
	public final void setUpdateDelay(long updateDelay) {
		this.updateDelay = updateDelay;
	}

	/**
	 * Set the maximum amount of time to wait for the writing thread to stop,
	 * including writing any remaining counts when the service shuts down.
	 *
	 * @param shutdownMaxWait
	 *        the maximum time to wait; if {@code null} then
	 *        {@link #DEFAULT_SHUTDOWN_MAX_WAIT} will be used
	 * @since 1.1
	 */
	public final void setShutdownMaxWait(@Nullable Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
	}

	/**
	 * The JDBC statement to execute for incrementing a count for a single
	 * {@code DatumId} key.
	 *
	 * <p>
	 * The statement must accept the following parameters:
	 * </p>
	 *
	 * <ol>
	 * <li>long - the object ID</li>
	 * <li>string - the source ID (service name)</li>
	 * <li>timestamp - the audit date</li>
	 * <li>integer - the count to add</li>
	 * </ol>
	 *
	 * @param sql
	 *        the SQL statement to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setServiceIncrementSql(String sql) {
		if ( requireNonNullArgument(sql, "sql").equals(serviceIncrementSql) ) {
			return;
		}
		this.serviceIncrementSql = sql;
		reconnectWriter();
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
	 *        the update count
	 */
	public final void setStatLogUpdateCount(int statLogUpdateCount) {
		statCounter.setLogFrequency(statLogUpdateCount);
	}

}
