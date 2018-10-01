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

package net.solarnetwork.central.query.audit.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryAuditor;

/**
 * {@link QueryAuditor} implementation that uses JDBC statements to update audit
 * data.
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcQueryAuditor implements QueryAuditor {

	/** The default value for the {@code updateDelay} property. */
	public static final long DEFAULT_UPDATE_DELAY = 100;

	/** The default value for the {@code flushDelay} property. */
	public static final long DEFAULT_FLUSH_DELAY = 10000;

	/** The default value for the {@code statLogUpdateCount} property. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/** The default value for the {@code connecitonRecoveryDelay} property. */
	public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

	/** The default value for the {@code nodeSourceIncrementSql} property. */
	public static final String DEFAULT_NODE_SOURCE_INCREMENT_SQL = "{call solaragg.aud_inc_datum_query_count(?, ?, ?, ?)}";

	/**
	 * A regular expression that matches if a JDBC statement is a
	 * {@link CallableStatement}.
	 */
	public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*\\}",
			Pattern.CASE_INSENSITIVE);

	private static final ThreadLocal<Map<GeneralNodeDatumPK, Integer>> auditResultMap = ThreadLocal
			.withInitial(() -> new HashMap<>());

	private final AtomicLong updateCount;
	private final JdbcOperations jdbcOps;
	private final ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> nodeSourceCounters;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private WriterThread writerThread;
	private long updateDelay;
	private long flushDelay;
	private long connectionRecoveryDelay;
	private String nodeSourceIncrementSql;
	private int statLogUpdateCount;

	/**
	 * Constructor.
	 * 
	 * @param jdbcOperations
	 *        the JDBC accessor to use
	 */
	public JdbcQueryAuditor(JdbcOperations jdbcOperations) {
		this(jdbcOperations, new ConcurrentHashMap<>(64));
	}

	/**
	 * Constructor.
	 * 
	 * @param jdbcOperations
	 *        the JDBC accessor to use
	 * @param nodeSourceCounters
	 *        the map to use for tracking counts for node datum
	 */
	public JdbcQueryAuditor(JdbcOperations jdbcOperations,
			ConcurrentMap<GeneralNodeDatumPK, AtomicInteger> nodeSourceCounters) {
		super();
		this.updateCount = new AtomicLong(0);
		this.jdbcOps = jdbcOperations;
		this.nodeSourceCounters = nodeSourceCounters;
		setConnectionRecoveryDelay(DEFAULT_CONNECTION_RECOVERY_DELAY);
		setFlushDelay(DEFAULT_FLUSH_DELAY);
		setUpdateDelay(DEFAULT_UPDATE_DELAY);
		setNodeSourceIncrementSql(DEFAULT_NODE_SOURCE_INCREMENT_SQL);
		setStatLogUpdateCount(DEFAULT_STAT_LOG_UPDATE_COUNT);
	}

	@Override
	public <T extends FilterMatch<GeneralNodeDatumPK>> void auditNodeDatumFilterResults(
			GeneralNodeDatumFilter filter, FilterResults<T> results) {
		final int returnedCount = (results.getReturnedResultCount() != null
				? results.getReturnedResultCount()
				: 0);
		// if no results, no count
		if ( results == null || returnedCount < 1 ) {
			return;
		}

		// configure date to current hour (floored)
		DateTime hour = new DateTime();
		hour = hour.withTime(hour.getHourOfDay(), 0, 0, 0);

		final Map<GeneralNodeDatumPK, Integer> resultMap = auditResultMap.get();

		// try shortcut for single node + source
		Long[] nodeIds = filter.getNodeIds();
		String[] sourceIds = filter.getSourceIds();
		if ( nodeIds != null && nodeIds.length == 1 && sourceIds != null && sourceIds.length == 1 ) {
			GeneralNodeDatumPK pk = nodeDatumKey(hour, nodeIds[0], sourceIds[0]);
			addNodeSourceCount(pk, returnedCount);
			resultMap.put(pk, resultMap.getOrDefault(pk, 0) + returnedCount);
			return;
		}

		// coalesce counts by key first to simplify inserts into counters
		Map<GeneralNodeDatumPK, Integer> counts = new HashMap<>(returnedCount);
		for ( FilterMatch<GeneralNodeDatumPK> result : results ) {
			@SuppressWarnings("deprecation")
			GeneralNodeDatumPK id = result.getId();
			GeneralNodeDatumPK pk = nodeDatumKey(hour, id.getNodeId(), id.getSourceId());
			counts.compute(pk, (k, v) -> v == null ? 1 : v.intValue() + 1);
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

	private static GeneralNodeDatumPK nodeDatumKey(DateTime date, Long nodeId, String sourceId) {
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK();
		pk.setCreated(date);
		pk.setNodeId(nodeId);
		pk.setSourceId(sourceId);
		return pk;
	}

	private void addNodeSourceCount(GeneralNodeDatumPK key, int count) {
		nodeSourceCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
	}

	private void flushNodeSourceData(PreparedStatement stmt) throws SQLException, InterruptedException {
		for ( Iterator<Map.Entry<GeneralNodeDatumPK, AtomicInteger>> itr = nodeSourceCounters.entrySet()
				.iterator(); itr.hasNext(); ) {
			Map.Entry<GeneralNodeDatumPK, AtomicInteger> me = itr.next();
			GeneralNodeDatumPK key = me.getKey();
			AtomicInteger counter = me.getValue();
			final int count = counter.getAndSet(0);
			if ( count < 1 ) {
				// clean out stale 0 valued counter
				itr.remove();
				continue;
			}
			try {
				stmt.setTimestamp(1, new java.sql.Timestamp(key.getCreated().getMillis()));
				stmt.setLong(2, key.getNodeId());
				stmt.setString(3, key.getSourceId());
				stmt.setInt(4, count);
				stmt.execute();
				long currUpdateCount = updateCount.incrementAndGet();
				if ( statLogUpdateCount > 0 && currUpdateCount % statLogUpdateCount == 0 ) {
					log.info("Updated {} node source query count records", currUpdateCount);
				}
				if ( updateDelay > 0 ) {
					Thread.sleep(updateDelay);
				}
			} catch ( SQLException | InterruptedException e ) {
				addNodeSourceCount(key, count);
				throw e;
			} catch ( Exception e ) {
				addNodeSourceCount(key, count);
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
			while ( keepGoing.get() ) {
				keepGoingWithConnection.set(true);
				synchronized ( this ) {
					started = true;
					this.notifyAll();
				}
				try {
					keepGoing.compareAndSet(true, jdbcOps.execute(new ConnectionCallback<Boolean>() {

						@Override
						public Boolean doInConnection(Connection con)
								throws SQLException, DataAccessException {
							con.setAutoCommit(true); // we want every execution of our loop to commit immediately
							PreparedStatement stmt = isCallableStatement(nodeSourceIncrementSql)
									? con.prepareCall(nodeSourceIncrementSql)
									: con.prepareStatement(nodeSourceIncrementSql);
							do {
								try {
									if ( Thread.interrupted() ) {
										throw new InterruptedException();
									}
									flushNodeSourceData(stmt);
									Thread.sleep(flushDelay);
								} catch ( InterruptedException e ) {
									log.info("Writer thread interrupted: exiting now.");
									return false;
								}
							} while ( keepGoingWithConnection.get() );
							return true;
						}

					}));
				} catch ( DataAccessException e ) {
					log.warn("JDBC exception with query auditing", e);
					// sleep, then try again
					try {
						Thread.sleep(connectionRecoveryDelay);
					} catch ( InterruptedException e2 ) {
						log.info("Writer thread interrupted: exiting now.");
						keepGoing.set(false);
					}
				} catch ( RuntimeException e ) {
					log.warn("Exception with query auditing", e);
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
			writerThread = new WriterThread();
			writerThread.setName("JdbcQueryAuditorWriter");
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
	 * <li>timestamp - the audit date</li>
	 * <li>long - the node ID</li>
	 * <li>string - the source ID</li>
	 * </ol>
	 * 
	 * @param sql
	 *        the SQL statement to use; defaults to
	 *        {@link #DEFAULT_NODE_SOURCE_INCREMENT_SQL}
	 */
	public void setNodeSourceIncrementSql(String sql) {
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
	public void setStatLogUpdateCount(int statLogUpdateCount) {
		this.statLogUpdateCount = statLogUpdateCount;
	}

}
