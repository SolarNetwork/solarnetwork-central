/* ==================================================================
 * JdbNodeServiceAuditor.java - 21/01/2023 5:06:05 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.NodeServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatCounter;

/**
 * JDBC based implementation of {@link NodeServiceAuditor}.
 * 
 * <p>
 * This service coalesces updates per node/service/hour in memory and flushes
 * these to the database via a single "writer" thread after a small delay. This
 * design is meant to support better throughput of audit updates, but has the
 * potential to drop some count values if the service is restarted.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcNodeServiceAuditor implements NodeServiceAuditor, PingTest, ServiceLifecycleObserver {

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
	 * The default value for the {@code nodeServiceIncrementSql} property.
	 */
	public static final String DEFAULT_NODE_SERVICE_INCREMENT_SQL = "{call solardatm.audit_increment_node_count(?,?,?,?)}";

	/**
	 * A regular expression that matches if a JDBC statement is a
	 * {@link CallableStatement}.
	 */
	public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*\\}",
			Pattern.CASE_INSENSITIVE);

	private static final Logger log = LoggerFactory.getLogger(JdbcNodeServiceAuditor.class);

	private final DataSource dataSource;
	private final ConcurrentMap<DatumId, AtomicInteger> nodeServiceCounters; // DatumId used with sourceId for service
	private final Clock clock;
	private final StatCounter statCounter;

	private String nodeServiceIncrementSql;

	private WriterThread writerThread;
	private long updateDelay;
	private long flushDelay;
	private long connectionRecoveryDelay;

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcNodeServiceAuditor(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4),
				Clock.tick(Clock.systemUTC(), Duration.ofHours(1)), new StatCounter("NodeServiceAuditor",
						"", log, 1000, JdbcNodeServiceAuditorCount.values()));
	}

	/**
	 * Constructor.
	 * 
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param nodeSourceCounters
	 *        the node source counters map
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcNodeServiceAuditor(DataSource dataSource,
			ConcurrentMap<DatumId, AtomicInteger> nodeSourceCounters, Clock clock,
			StatCounter statCounter) {
		super();
		this.dataSource = requireNonNullArgument(dataSource, "dataSource");
		this.nodeServiceCounters = requireNonNullArgument(nodeSourceCounters, "nodeSourceCounters");
		this.clock = requireNonNullArgument(clock, "clock");
		this.statCounter = requireNonNullArgument(statCounter, "statCounter");
		setConnectionRecoveryDelay(DEFAULT_CONNECTION_RECOVERY_DELAY);
		setFlushDelay(DEFAULT_FLUSH_DELAY);
		setUpdateDelay(DEFAULT_UPDATE_DELAY);
		setNodeServiceIncrementSql(DEFAULT_NODE_SERVICE_INCREMENT_SQL);
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
	public Clock getAuditClock() {
		return clock;
	}

	@Override
	public void auditNodeService(Long nodeId, String service, int count) {
		if ( count == 0 ) {
			return;
		}
		addNodeServiceCount(DatumId.nodeId(nodeId, service, clock.instant()), count);

	}

	private void addNodeServiceCount(DatumId key, int count) {
		nodeServiceCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
		statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.ResultsAdded);
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
			statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.WriterThreadsStarted);
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
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.WriterThreadsEnded);
			}
		}

		private Boolean execute() throws SQLException {
			try (Connection conn = dataSource.getConnection()) {
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = isCallableStatement(nodeServiceIncrementSql)
						? conn.prepareCall(nodeServiceIncrementSql)
						: conn.prepareStatement(nodeServiceIncrementSql);
				do {
					try {
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
						flushNodeServiceData(stmt);
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

	private void flushNodeServiceData(PreparedStatement stmt) throws SQLException, InterruptedException {
		statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.CountsFlushed);
		for ( Iterator<Map.Entry<DatumId, AtomicInteger>> itr = nodeServiceCounters.entrySet()
				.iterator(); itr.hasNext(); ) {
			Map.Entry<DatumId, AtomicInteger> me = itr.next();
			DatumId key = me.getKey();
			AtomicInteger counter = me.getValue();
			final int count = counter.getAndSet(0);
			if ( count < 1 ) {
				// clean out stale 0 valued counter
				itr.remove();
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.ZeroCountsCleared);
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
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.UpdatesExecuted);
				if ( updateDelay > 0 ) {
					Thread.sleep(updateDelay);
				}
			} catch ( SQLException | InterruptedException e ) {
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.UpdatesFailed);
				addNodeServiceCount(key, count);
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.ResultsReadded);
				throw e;
			} catch ( Exception e ) {
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.UpdatesFailed);
				addNodeServiceCount(key, count);
				statCounter.incrementAndGet(JdbcNodeServiceAuditorCount.ResultsReadded);
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
			writerThread.setName("JdbcNodeServiceAuditorWriter");
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
		Map<String, Long> statMap = new LinkedHashMap<>(JdbcNodeServiceAuditorCount.values().length);
		for ( JdbcNodeServiceAuditorCount s : JdbcNodeServiceAuditorCount.values() ) {
			statMap.put(s.toString(), statCounter.get(s));
		}
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
	 * The JDBC statement to execute for incrementing a count for a single date,
	 * node, and source.
	 * 
	 * <p>
	 * The statement must accept the following parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li>long - the node ID</li>
	 * <li>string - the service name</li>
	 * <li>timestamp - the audit date</li>
	 * <li>integer - the instruction count to add</li>
	 * </ol>
	 * 
	 * @param sql
	 *        the SQL statement to use; defaults to
	 *        {@link #DEFAULT_NODE_SOURCE_INCREMENT_SQL}
	 */
	public void setNodeServiceIncrementSql(String sql) {
		if ( requireNonNullArgument(sql, "sql").equals(nodeServiceIncrementSql) ) {
			return;
		}
		this.nodeServiceIncrementSql = sql;
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
	 *        the update count; defaults to
	 *        {@link #DEFAULT_STAT_LOG_UPDATE_COUNT}
	 */
	public void setStatLogUpdateCount(int statLogUpdateCount) {
		statCounter.setLogFrequency(statLogUpdateCount);
	}

}
