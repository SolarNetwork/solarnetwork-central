/* ========================================================================
 * Copyright 2021 SolarNetwork Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================================================================
 */

package net.solarnetwork.flux.vernemq.webhook.service.impl;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.service.AuditService;

/**
 * A JDBC implementation of {@link AuditService}.
 * 
 * <p>
 * This service coalesces updates per node/source/hour in memory and flushes these to the database
 * via a single "writer" thread. This design is meant to support better throughput of audit updates,
 * but has the potential to drop some count values if the service is restarted.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class JdbcAuditService implements AuditService {

  // CHECKSTYLE OFF: LineLength

  /**
   * The default value for the {@code updateDelay} property.
   */
  public static final long DEFAULT_UPDATE_DELAY = 100;

  /**
   * The default value for the {@code flushDelay} property.
   */
  public static final long DEFAULT_FLUSH_DELAY = 10000;

  /**
   * The default value for the {@code statLogUpdateCount} property.
   */
  public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

  /**
   * The default value for the {@code connecitonRecoveryDelay} property.
   */
  public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

  /**
   * The default value for the {@code nodeSourceIncrementSql} property.
   */
  public static final String DEFAULT_NODE_SOURCE_INCREMENT_SQL = "{call solardatm.audit_increment_mqtt_publish_byte_count(?,?,?,?,?)}";

  /**
   * The default value for the {@link mqttServiceName} property.
   */
  public static final String DEFAULT_AUDIT_MQTT_SERVICE_NAME = "solarflux";

  /**
   * A regular expression that matches if a JDBC statement is a {@link CallableStatement}.
   */
  public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*\\}",
      Pattern.CASE_INSENSITIVE);

  // CHECKSTYLE ON: LineLength

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final DataSource dataSource;
  private final ConcurrentMap<DatumId, AtomicInteger> nodeSourceCounters;
  private final Clock clock;
  private final AtomicLong updateCount;

  private String mqttServiceName;
  private String nodeSourceIncrementSql;
  private int statLogUpdateCount;

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
  public JdbcAuditService(DataSource dataSource) {
    this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4), Clock.systemUTC());
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
  public JdbcAuditService(DataSource dataSource,
      ConcurrentMap<DatumId, AtomicInteger> nodeSourceCounters, Clock clock) {
    super();
    if (dataSource == null) {
      throw new IllegalArgumentException("The dataSource argument must not be null.");
    }
    this.dataSource = dataSource;
    if (nodeSourceCounters == null) {
      throw new IllegalArgumentException("The nodeSourceCounters argument must not be null.");
    }
    this.nodeSourceCounters = nodeSourceCounters;
    if (clock == null) {
      throw new IllegalArgumentException("The clock argument must not be null.");
    }
    this.clock = clock;
    this.updateCount = new AtomicLong();
    setMqttServiceName(DEFAULT_AUDIT_MQTT_SERVICE_NAME);
    setConnectionRecoveryDelay(DEFAULT_CONNECTION_RECOVERY_DELAY);
    setFlushDelay(DEFAULT_FLUSH_DELAY);
    setUpdateDelay(DEFAULT_UPDATE_DELAY);
    setNodeSourceIncrementSql(DEFAULT_NODE_SOURCE_INCREMENT_SQL);
    setStatLogUpdateCount(DEFAULT_STAT_LOG_UPDATE_COUNT);
  }

  @Override
  public void auditPublishMessage(Actor actor, Long nodeId, String sourceId, Message message) {
    final int byteCount = (message.getPayload() != null ? message.getPayload().length : 0);
    if (byteCount > 0) {
      addNodeSourceCount(
          DatumId.nodeId(nodeId, sourceId, clock.instant().truncatedTo(ChronoUnit.HOURS)),
          byteCount);
    }
  }

  private void addNodeSourceCount(DatumId key, int count) {
    nodeSourceCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
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
      while (keepGoing.get()) {
        keepGoingWithConnection.set(true);
        synchronized (this) {
          started = true;
          this.notifyAll();
        }
        try {
          keepGoing.compareAndSet(true, execute());
        } catch (SQLException | RuntimeException e) {
          log.warn("Exception with auditing", e);
          // sleep, then try again
          try {
            Thread.sleep(connectionRecoveryDelay);
          } catch (InterruptedException e2) {
            log.info("Audit writer thread interrupted: exiting now.");
            keepGoing.set(false);
          }
        }
      }
    }

    private Boolean execute() throws SQLException {
      try (Connection conn = dataSource.getConnection()) {
        conn.setAutoCommit(true); // we want every execution of our loop to commit immediately
        PreparedStatement stmt = isCallableStatement(nodeSourceIncrementSql)
            ? conn.prepareCall(nodeSourceIncrementSql)
            : conn.prepareStatement(nodeSourceIncrementSql);
        do {
          try {
            if (Thread.interrupted()) {
              throw new InterruptedException();
            }
            flushNodeSourceData(stmt);
            Thread.sleep(flushDelay);
          } catch (InterruptedException e) {
            log.info("Writer thread interrupted: exiting now.");
            return false;
          }
        } while (keepGoingWithConnection.get());
        return true;
      }
    }

  }

  private void flushNodeSourceData(PreparedStatement stmt)
      throws SQLException, InterruptedException {
    for (Iterator<Map.Entry<DatumId, AtomicInteger>> itr = nodeSourceCounters.entrySet()
        .iterator(); itr.hasNext();) {
      Map.Entry<DatumId, AtomicInteger> me = itr.next();
      DatumId key = me.getKey();
      AtomicInteger counter = me.getValue();
      final int count = counter.getAndSet(0);
      if (count < 1) {
        // clean out stale 0 valued counter
        itr.remove();
        continue;
      }
      try {
        if (log.isTraceEnabled()) {
          log.trace("Incrementing node {} source {} @ {} byte count by {}", key.getObjectId(),
              key.getSourceId(), key.getTimestamp(), count);
        }
        stmt.setString(1, mqttServiceName);
        stmt.setObject(2, key.getObjectId());
        stmt.setString(3, key.getSourceId());
        stmt.setTimestamp(4, new java.sql.Timestamp(key.getTimestamp().toEpochMilli()));
        stmt.setInt(5, count);
        stmt.execute();
        long currUpdateCount = updateCount.incrementAndGet();
        if (statLogUpdateCount > 0 && currUpdateCount % statLogUpdateCount == 0) {
          log.info("Updated {} node source byte count records", currUpdateCount);
        }
        if (updateDelay > 0) {
          Thread.sleep(updateDelay);
        }
      } catch (SQLException | InterruptedException e) {
        addNodeSourceCount(key, count);
        throw e;
      } catch (Exception e) {
        addNodeSourceCount(key, count);
        RuntimeException re;
        if (e instanceof RuntimeException) {
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
   * Cause the writing thread to re-connect to the database with a new connection.
   */
  public synchronized void reconnectWriter() {
    if (writerThread != null && writerThread.isGoing()) {
      writerThread.reconnect();
    }
  }

  /**
   * Enable writing, and wait until the writing thread is going.
   */
  public synchronized void enableWriting() {
    if (writerThread == null || !writerThread.isGoing()) {
      writerThread = new WriterThread();
      writerThread.setName("JdbcMqttAuditorWriter");
      synchronized (writerThread) {
        writerThread.start();
        while (!writerThread.hasStarted()) {
          try {
            writerThread.wait(5000L);
          } catch (InterruptedException e) {
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
    if (writerThread != null) {
      writerThread.exit();
    }
  }

  /**
   * Set the MQTT audit service name to use.
   * 
   * @param mqttServiceName
   *        the service to set; defaults to {@link #DEFAULT_AUDIT_MQTT_SERVICE_NAME}
   */
  public void setMqttServiceName(String mqttServiceName) {
    if (mqttServiceName == null) {
      throw new IllegalArgumentException("The mqttServiceName argument must not be null.");
    }
    this.mqttServiceName = mqttServiceName;
    reconnectWriter();
  }

  /**
   * Set the delay, in milliseconds, between flushing cached audit data.
   * 
   * @param flushDelay
   *        the delay, in milliseconds; defaults to {@link #DEFAULT_FLUSH_DELAY}
   * @throws IllegalArgumentException
   *         if {@code flushDelay} is &lt; 0
   */
  public void setFlushDelay(long flushDelay) {
    if (flushDelay < 0) {
      throw new IllegalArgumentException("flushDelay must be >= 0");
    }
    this.flushDelay = flushDelay;
  }

  /**
   * Set the delay, in milliseconds, to wait after a JDBC connection error before trying to recover
   * and connect again.
   * 
   * @param connectionRecoveryDelay
   *        the delay, in milliseconds; defaults t[ {@link #DEFAULT_CONNECTION_RECOVERY_DELAY}
   * @throws IllegalArgumentException
   *         if {@code connectionRecoveryDelay} is &lt; 0
   */
  public void setConnectionRecoveryDelay(long connectionRecoveryDelay) {
    if (connectionRecoveryDelay < 0) {
      throw new IllegalArgumentException("connectionRecoveryDelay must be >= 0");
    }
    this.connectionRecoveryDelay = connectionRecoveryDelay;
  }

  /**
   * Set the delay, in milliseconds, to wait after executing JDBC statements within a loop before
   * executing another statement.
   * 
   * @param updateDelay
   *        the delay, in milliseconds; defaults t[ {@link #DEFAULT_UPDATE_DELAY}
   * @throws IllegalArgumentException
   *         if {@code updateDelay} is &lt; 0
   */
  public void setUpdateDelay(long updateDelay) {
    this.updateDelay = updateDelay;
  }

  /**
   * The JDBC statement to execute for incrementing a count for a single date, node, and source.
   * 
   * <p>
   * The statement must accept the following parameters:
   * </p>
   * 
   * <ol>
   * <li>string - the MQTT service name</li>
   * <li>long - the node ID</li>
   * <li>string - the source ID</li>
   * <li>timestamp - the audit date</li>
   * <li>integer - the query count</li>
   * </ol>
   * 
   * @param sql
   *        the SQL statement to use; defaults to {@link #DEFAULT_NODE_SOURCE_INCREMENT_SQL}
   */
  public void setNodeSourceIncrementSql(String sql) {
    if (sql == null) {
      throw new IllegalArgumentException("nodeSourceIncrementSql must not be null");
    }
    if (sql.equals(nodeSourceIncrementSql)) {
      return;
    }
    this.nodeSourceIncrementSql = sql;
    reconnectWriter();
  }

  /**
   * Set the statistic log update count.
   * 
   * <p>
   * Setting this to something greater than {@literal 0} will cause {@literal INFO} level statistic
   * log entries to be emitted every {@code statLogUpdateCount} records have been updated in the
   * database.
   * </p>
   * 
   * @param statLogUpdateCount
   *        the update count; defaults to {@link #DEFAULT_STAT_LOG_UPDATE_COUNT}
   * @since 1.1
   */
  public void setStatLogUpdateCount(int statLogUpdateCount) {
    this.statLogUpdateCount = statLogUpdateCount;
  }

}
