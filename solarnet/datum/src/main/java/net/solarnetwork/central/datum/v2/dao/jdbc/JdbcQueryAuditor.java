/* ==================================================================
 * JdbcAuditor.java - 14/02/2018 10:11:12 AM
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.isTransientException;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * {@link QueryAuditor} implementation that uses JDBC statements to update audit
 * data when datum is queried.
 *
 * <p>
 * This class uses a {@link Clock} to determine the "audit date". The default
 * clock uses an hour-based tick settings so that audit counts are grouped into
 * hour-based time buckets, and thus result in hour-based rows in the database:
 * </p>
 *
 * <pre>
 * <code>
 * Clock.tick(Clock.systemUTC(), Duration.ofHours(1))
 * </code>
 * </pre>
 *
 * <p>
 * This class opens and maintains a single JDBC {@link Connection} in a
 * dedicated thread. All database updates are buffered in memory and then
 * flushed to the database after the configured {@code flushDelay}. If the
 * connection is lost, a new connection will be created. When the service is
 * shut down, any updates not yet flushed are written before it stops.
 * </p>
 *
 * @author matt
 * @version 2.5
 */
public class JdbcQueryAuditor implements QueryAuditor, PingTest, ServiceLifecycleObserver {

	/** The default value for the {@code updateDelay} property. */
	public static final long DEFAULT_UPDATE_DELAY = 100;

	/** The default value for the {@code flushDelay} property. */
	public static final long DEFAULT_FLUSH_DELAY = 10000;

	/** The default value for the {@code statLogUpdateCount} property. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/** The default value for the {@code connectionRecoveryDelay} property. */
	public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

	/**
	 * The default value for the {@code shutdownMaxWait} property.
	 *
	 * @since 2.5
	 */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofSeconds(10);

	/** The default value for the {@code nodeSourceIncrementSql} property. */
	public static final String DEFAULT_NODE_SOURCE_INCREMENT_SQL = "{call solardatm.audit_increment_datum_q_count(?,?,?,?)}";

	/**
	 * A regular expression that matches if a JDBC statement is a
	 * {@link CallableStatement}.
	 */
	public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*}",
			Pattern.CASE_INSENSITIVE);

	/**
	 * The time to wait, in seconds, when checking if the connection is still
	 * usable after an error writing a count.
	 */
	private static final int CONNECTION_VALID_TIMEOUT = 5;

	private static final ThreadLocal<Map<GeneralNodeDatumPK, Integer>> auditResultMap = ThreadLocal
			.withInitial(HashMap::new);

	private static final Logger log = LoggerFactory.getLogger(JdbcQueryAuditor.class);

	private final Clock clock;
	private final DataSource dataSource;
	private final ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> nodeSourceCounters;
	private final StatTracker stats;

	private volatile @Nullable WriterThread writerThread;
	private long updateDelay;
	private long flushDelay;
	private long connectionRecoveryDelay;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;
	private volatile String nodeSourceIncrementSql;

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 */
	public JdbcQueryAuditor(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(64));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param nodeSourceCounters
	 *        the map to use for tracking counts for node datum; the map must
	 *        perform {@code compute()} atomically, as {@link ConcurrentHashMap}
	 *        does
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 */
	public JdbcQueryAuditor(DataSource dataSource,
			ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> nodeSourceCounters) {
		this(Clock.tick(Clock.systemUTC(), Duration.ofHours(1)), dataSource, nodeSourceCounters,
				new StatTracker("QueryAuditor", null, log, 1000));
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use; use an appropriate tick duration for auditing
	 *        date derivation
	 * @param dataSource
	 *        the JDBC data source to use
	 * @param nodeSourceCounters
	 *        the map to use for tracking counts for node datum; the map must
	 *        perform {@code compute()} atomically, as {@link ConcurrentHashMap}
	 *        does
	 * @throws IllegalArgumentException
	 *         if any parameter is {@code null}
	 * @since 2.1
	 */
	public JdbcQueryAuditor(Clock clock, DataSource dataSource,
			ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> nodeSourceCounters,
			StatTracker statCounter) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.dataSource = requireNonNullArgument(dataSource, "dataSource");
		this.nodeSourceCounters = requireNonNullArgument(nodeSourceCounters, "nodeSourceCounters");
		this.stats = requireNonNullArgument(statCounter, "statCounter");
		this.connectionRecoveryDelay = DEFAULT_CONNECTION_RECOVERY_DELAY;
		this.flushDelay = DEFAULT_FLUSH_DELAY;
		this.updateDelay = DEFAULT_UPDATE_DELAY;
		this.nodeSourceIncrementSql = DEFAULT_NODE_SOURCE_INCREMENT_SQL;
		setStatLogUpdateCount(DEFAULT_STAT_LOG_UPDATE_COUNT);
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

	@Override
	public Clock getAuditClock() {
		return clock;
	}

	@Override
	public <T extends FilterMatch<GeneralNodeDatumPK>> void auditNodeDatumFilterResults(
			GeneralNodeDatumFilter filter, FilterResults<T, GeneralNodeDatumPK> results) {
		final int returnedCount = (results != null ? results.getReturnedResultCount() : 0);
		// if no results, no count
		if ( results == null || returnedCount < 1 ) {
			return;
		}

		// configure date to current time; the clock is expected to truncate if desired
		final Instant auditDate = Instant.now(clock);

		final Map<GeneralNodeDatumPK, Integer> resultMap = auditResultMap.get();

		// try shortcut for single node + source
		Long[] nodeIds = filter.getNodeIds();
		String[] sourceIds = filter.getSourceIds();
		if ( nodeIds != null && nodeIds.length == 1 && sourceIds != null && sourceIds.length == 1 ) {
			GeneralNodeDatumPK pk = nodeDatumKey(auditDate, nodeIds[0], sourceIds[0]);
			addNodeSourceCount(pk, returnedCount);
			resultMap.put(pk, resultMap.getOrDefault(pk, 0) + returnedCount);
			return;
		}

		// coalesce counts by key first to simplify inserts into counters
		Map<GeneralNodeDatumPK, Integer> counts = new HashMap<>(returnedCount);
		for ( FilterMatch<GeneralNodeDatumPK> result : results ) {
			GeneralNodeDatumPK id = nonnull(result.getId(), "ID");
			GeneralNodeDatumPK pk = nodeDatumKey(auditDate, nonnull(id.getNodeId(), "nodeId"),
					nonnull(id.getSourceId(), "sourceId"));
			counts.compute(pk, (_, v) -> v == null ? 1 : v + 1);
		}

		// insert counts
		for ( Map.Entry<GeneralNodeDatumPK, Integer> me : counts.entrySet() ) {
			GeneralNodeDatumPK key = me.getKey();
			Integer val = me.getValue();
			addNodeSourceCount(key, val);
			resultMap.put(key, resultMap.getOrDefault(key, 0) + val);
		}
	}

	@Override
	public void addNodeDatumAuditResults(Map<GeneralNodeDatumPK, Integer> results) {
		for ( Map.Entry<GeneralNodeDatumPK, Integer> me : results.entrySet() ) {
			GeneralNodeDatumPK key = me.getKey();
			Integer val = me.getValue();
			addNodeSourceCount(key, val);
		}
	}

	@Override
	public Map<GeneralNodeDatumPK, Integer> currentAuditResults() {
		return auditResultMap.get();
	}

	@Override
	public void resetCurrentAuditResults() {
		auditResultMap.get().clear();
	}

	@Override
	public void auditNodeDatum(Datum datum) {
		if ( datum == null || datum.getKind() != ObjectDatumKind.Node || datum.getSourceId() == null ) {
			return;
		}
		addNodeSourceCount(nodeDatumKey(clock.instant(), nonnull(datum.getObjectId(), "objectId"),
				nonnull(datum.getSourceId(), "sourceId")), 1);
	}

	private static GeneralNodeDatumPK nodeDatumKey(Instant date, Long nodeId, String sourceId) {
		return new GeneralNodeDatumPK(nodeId, date, sourceId);
	}

	private void addNodeSourceCount(GeneralNodeDatumPK key, int count) {
		// increment within compute() so the update is serialized with flushNodeSourceData()
		// removing the counter; otherwise the count could land on a counter that was just removed
		nodeSourceCounters.compute(key, (_, counter) -> {
			if ( counter == null ) {
				return new AtomicInteger(count);
			}
			counter.addAndGet(count);
			return counter;
		});
		stats.increment(JdbcQueryAuditorCount.ResultsAdded);
	}

	private void flushNodeSourceData(PreparedStatement stmt, boolean throttle)
			throws SQLException, InterruptedException {
		stats.increment(JdbcQueryAuditorCount.CountsFlushed);
		for ( GeneralNodeDatumPK key : nodeSourceCounters.keySet() ) {
			// remove the counter, rather than reset it to 0 and remove it on a later flush if it
			// is still 0, so a count added after this goes to a new counter rather than being lost
			final AtomicInteger counter = nodeSourceCounters.remove(key);
			if ( counter == null ) {
				continue;
			}
			final int count = counter.get();
			if ( count < 1 ) {
				stats.increment(JdbcQueryAuditorCount.ZeroCountsCleared, true);
				continue;
			}
			try {
				stmt.setObject(1, key.getNodeId());
				stmt.setString(2, key.getSourceId());
				stmt.setTimestamp(3, Timestamp.from(key.getCreated()));
				stmt.setInt(4, count);
				stmt.execute();
			} catch ( SQLException | RuntimeException e ) {
				stats.increment(JdbcQueryAuditorCount.UpdatesFailed);
				if ( !isTransientException(e) && isConnectionUsable(stmt) ) {
					// the problem is with this count and will not go away, so discard it rather
					// than try it again on every flush, which would block the counts after it
					stats.increment(JdbcQueryAuditorCount.ResultsDiscarded);
					log.error("Discarding query audit count {} for {} that could not be written: {}",
							count, key, e.toString());
					continue;
				}
				// add the count back, to try again after reconnecting
				addNodeSourceCount(key, count);
				stats.increment(JdbcQueryAuditorCount.ResultsReadded);
				throw e;
			}
			// the count has been written now, so it must not be added back if what follows fails,
			// such as being interrupted during the update delay, or it would be written again
			stats.increment(JdbcQueryAuditorCount.UpdatesExecuted);
			if ( throttle && updateDelay > 0 ) {
				Thread.sleep(updateDelay);
			}
		}
	}

	private static boolean isConnectionUsable(PreparedStatement stmt) {
		try {
			return stmt.getConnection().isValid(CONNECTION_VALID_TIMEOUT);
		} catch ( SQLException | RuntimeException e ) {
			// any error checking means it is not usable, so the count is kept to try again
			return false;
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
	private void writeRemainingNodeSourceData(PreparedStatement stmt) throws SQLException {
		if ( nodeSourceCounters.isEmpty() ) {
			return;
		}
		log.info("Writing {} remaining query audit counts", nodeSourceCounters.size());
		try {
			// a few passes, to also write counts added while writing
			for ( int i = 0; i < 3 && !nodeSourceCounters.isEmpty(); i++ ) {
				flushNodeSourceData(stmt, false);
			}
		} catch ( InterruptedException e ) {
			// not thrown without the update delay, but keep the interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Log any counts not written, as an error.
	 */
	private void logUnwrittenNodeSourceData() {
		if ( !nodeSourceCounters.isEmpty() ) {
			log.error("Stopping with {} query audit counts not written: {}", nodeSourceCounters.size(),
					nodeSourceCounters);
		}
	}

	private boolean isCallableStatement(String sql) {
		Matcher m = CALLABLE_STATEMENT_REGEX.matcher(sql);
		return m.matches();
	}

	private class WriterThread extends Thread {

		// keepGoing only ever changes from true to false, so a request to exit cannot be undone
		// when the writer starts a new connection, as resetting a combined flag could
		private volatile boolean keepGoing = true;
		private volatile boolean writeOnExit = false;
		private volatile boolean reconnect = false;
		private boolean started = false;

		// the error stopping the writer from writing, until it next writes successfully
		private volatile @Nullable String writeError;

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
			stats.increment(JdbcQueryAuditorCount.WriterThreadsStarted);
			try {
				while ( keepGoing ) {
					reconnect = false;
					try {
						execute();
					} catch ( SQLException | RuntimeException e ) {
						if ( !keepGoing ) {
							// stopping, so give up rather than try again
							if ( writeOnExit ) {
								log.error("Error writing remaining query audit counts: {}",
										e.toString());
							}
							break;
						}
						writeError = e.toString();
						if ( e instanceof SQLTransientException ) {
							log.warn("Transient SQL exception with query auditing: {}", e.toString());
						} else {
							log.warn("Exception with query auditing: {}", e.getMessage(), e);
						}
						// sleep, then try again
						try {
							Thread.sleep(connectionRecoveryDelay);
						} catch ( InterruptedException e2 ) {
							log.info("Writer thread interrupted: exiting now.");
							keepGoing = false;
						}
					}
				}
				if ( writeOnExit ) {
					logUnwrittenNodeSourceData();
				}
			} finally {
				stats.increment(JdbcQueryAuditorCount.WriterThreadsEnded);
			}
		}

		private void execute() throws SQLException {
			final String sql = nodeSourceIncrementSql;
			try (Connection conn = dataSource.getConnection()) {
				stats.increment(JdbcQueryAuditorCount.ConnectionsCreated);
				conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
				PreparedStatement stmt = isCallableStatement(sql) ? conn.prepareCall(sql)
						: conn.prepareStatement(sql);
				while ( keepGoing && !reconnect ) {
					try {
						if ( Thread.interrupted() ) {
							throw new InterruptedException();
						}
						flushNodeSourceData(stmt, true);
						writeError = null;
						Thread.sleep(flushDelay);
					} catch ( InterruptedException e ) {
						log.info("Writer thread interrupted: exiting now.");
						keepGoing = false;
					}
				}
				if ( writeOnExit ) {
					writeRemainingNodeSourceData(stmt);
				}
			}
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
			t.setName("JdbcQueryAuditorWriter");
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
				logUnwrittenNodeSourceData();
			}
			return;
		}
		t.exit(writeRemaining);
		t.interrupt();
		try {
			if ( !t.join(shutdownMaxWait) ) {
				log.warn("Query audit writer thread {} did not stop within {}", t.getName(),
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
		Map<String, Long> statMap = stats.allCounts();
		if ( t == null || !t.isAlive() ) {
			return new PingTestResult(false,
					(t == null ? "Writer thread missing." : "Writer thread dead."), statMap);
		}
		final String writeError = t.writeError;
		if ( writeError != null ) {
			return new PingTestResult(false, "Writer thread cannot write: " + writeError, statMap);
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
	 * @since 2.5
	 */
	public final void setShutdownMaxWait(@Nullable Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
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
	 * <li>string - the source ID</li>
	 * <li>timestamp - the audit date</li>
	 * <li>integer - the query count</li>
	 * </ol>
	 *
	 * @param sql
	 *        the SQL statement to use; defaults to
	 *        {@link #DEFAULT_NODE_SOURCE_INCREMENT_SQL}
	 */
	public final void setNodeSourceIncrementSql(String sql) {
		if ( sql == null ) {
			throw new IllegalArgumentException("nodeSourceIncrementSql must not be null");
		}
		if ( sql.equals(nodeSourceIncrementSql) ) {
			return;
		}
		this.nodeSourceIncrementSql = sql;
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
	 * @since 1.1
	 */
	public final void setStatLogUpdateCount(int statLogUpdateCount) {
		stats.setLogFrequency(statLogUpdateCount);
	}

}
