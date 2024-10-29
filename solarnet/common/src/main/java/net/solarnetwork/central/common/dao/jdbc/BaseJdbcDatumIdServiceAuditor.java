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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
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
 * @version 1.0
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
	 * The default value for the {@code connecitonRecoveryDelay} property.
	 */
	public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

	/**
	 * A regular expression that matches if a JDBC statement is a
	 * {@link CallableStatement}.
	 */
	public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*\\}",
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

	private String serviceIncrementSql;

	private WriterThread writerThread;
	private long updateDelay;
	private long flushDelay;
	private long connectionRecoveryDelay;

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param serviceCounters
	 *        the service counters map
	 * @param clock
	 *        the clock to use; a tick-based clock is typical, to align updates
	 *        to time-based "buckets"
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
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

	@Override
	public void serviceDidShutdown() {
		disableWriting();
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
		serviceCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
		statCounter.increment(JdbcNodeServiceAuditorCount.ResultsAdded);
	}

	private class WriterThread extends Thread {

		private final AtomicBoolean keepGoingWithConnection = new AtomicBoolean(true);
		private final AtomicBoolean keepGoing = new AtomicBoolean(true);
		private boolean started = false;

		public boolean hasStarted() {
			return started;
		}

		public boolean isGoing() {
			return keepGoing.get();
		}

		public void reconnect() {
			keepGoingWithConnection.compareAndSet(true, false);
		}

		public void exit() {
			keepGoing.compareAndSet(true, false);
			keepGoingWithConnection.compareAndSet(true, false);
		}

		@Override
		public void run() {
			log.info("Started JDBC audit writer thread {}", this);
			statCounter.increment(JdbcNodeServiceAuditorCount.WriterThreadsStarted);
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
						log.warn("Exception with auditing", e);
						// sleep, then try again
						try {
							Thread.sleep(connectionRecoveryDelay);
						} catch ( InterruptedException e2 ) {
							log.info("Audit writer thread interrupted: exiting now.");
							keepGoing.set(false);
						}
					}
				}
			} finally {
				statCounter.increment(JdbcNodeServiceAuditorCount.WriterThreadsEnded);
			}
		}

		private Boolean execute() throws SQLException {
			try (Connection conn = dataSource.getConnection()) {
				statCounter.increment(JdbcNodeServiceAuditorCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = isCallableStatement(serviceIncrementSql)
						? conn.prepareCall(serviceIncrementSql)
						: conn.prepareStatement(serviceIncrementSql);
				do {
					try {
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
						flushServiceData(stmt);
						Thread.sleep(flushDelay);
					} catch ( InterruptedException e ) {
						log.info("Writer thread interrupted: exiting now.");
						return false;
					}
				} while ( keepGoingWithConnection.get() );
				return true;
			}
		}

	}

	private void flushServiceData(PreparedStatement stmt) throws SQLException, InterruptedException {
		statCounter.increment(JdbcNodeServiceAuditorCount.CountsFlushed);
		for ( Iterator<Map.Entry<DatumId, AtomicInteger>> itr = serviceCounters.entrySet()
				.iterator(); itr.hasNext(); ) {
			Map.Entry<DatumId, AtomicInteger> me = itr.next();
			DatumId key = me.getKey();
			AtomicInteger counter = me.getValue();
			final int count = counter.getAndSet(0);
			if ( count < 1 ) {
				// clean out stale 0 valued counter
				itr.remove();
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
				statCounter.increment(JdbcNodeServiceAuditorCount.UpdatesExecuted);
				if ( updateDelay > 0 ) {
					Thread.sleep(updateDelay);
				}
			} catch ( SQLException | InterruptedException e ) {
				statCounter.increment(JdbcNodeServiceAuditorCount.UpdatesFailed);
				addServiceCount(key, count);
				statCounter.increment(JdbcNodeServiceAuditorCount.ResultsReadded);
				throw e;
			} catch ( Exception e ) {
				statCounter.increment(JdbcNodeServiceAuditorCount.UpdatesFailed);
				addServiceCount(key, count);
				statCounter.increment(JdbcNodeServiceAuditorCount.ResultsReadded);
				RuntimeException re;
				if ( e instanceof RuntimeException ) {
					re = (RuntimeException) e;
				} else {
					re = new RuntimeException("Exception flushing node source audit data", e);
				}
				throw re;
			}
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
		if ( writerThread != null && writerThread.isGoing() ) {
			writerThread.reconnect();
		}
	}

	/**
	 * Enable writing, and wait until the writing thread is going.
	 */
	public synchronized void enableWriting() {
		if ( writerThread == null || !writerThread.isGoing() ) {
			writerThread = new WriterThread();
			writerThread.setName(writerThreadName);
			synchronized ( writerThread ) {
				writerThread.start();
				while ( !writerThread.hasStarted() ) {
					try {
						writerThread.wait(5000L);
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
					(writerThread == null ? "Writer thread missing." : "Writer thread dead."), statMap);
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
	public void setFlushDelay(long flushDelay) {
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
	 *        the delay, in milliseconds; defaults t[
	 *        {@link #DEFAULT_UPDATE_DELAY}
	 * @throws IllegalArgumentException
	 *         if {@code updateDelay} is &lt; 0
	 */
	public void setUpdateDelay(long updateDelay) {
		this.updateDelay = updateDelay;
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
	 */
	public void setServiceIncrementSql(String sql) {
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
	public void setStatLogUpdateCount(int statLogUpdateCount) {
		statCounter.setLogFrequency(statLogUpdateCount);
	}

}
