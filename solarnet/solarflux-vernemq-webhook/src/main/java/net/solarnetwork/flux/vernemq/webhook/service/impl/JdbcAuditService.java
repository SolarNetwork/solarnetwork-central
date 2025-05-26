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

import static java.lang.System.currentTimeMillis;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.solarnetwork.central.support.DelayQueueSet;
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
 * @version 1.2
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
   * The default value for the {@code connectionRecoveryDelay} property.
   */
  public static final long DEFAULT_CONNECTION_RECOVERY_DELAY = 15000;

  /**
   * The default value for the {@code nodeSourceIncrementSql} property.
   */
  public static final String DEFAULT_NODE_SOURCE_INCREMENT_SQL = "{call solardatm.audit_increment_mqtt_byte_count(?,?,?,?,?)}";

  /**
   * The default value for the {@link mqttServiceName} property.
   */
  public static final String DEFAULT_AUDIT_MQTT_SERVICE_NAME = "flxi";

  /**
   * The default value for the {@link mqttServiceName} property.
   *
   * @since 1.2
   */
  public static final String DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME = "flxo";

  /**
   * A regular expression that matches if a JDBC statement is a {@link CallableStatement}.
   */
  public static final Pattern CALLABLE_STATEMENT_REGEX = Pattern.compile("^\\{call\\s.*}",
      Pattern.CASE_INSENSITIVE);

  /**
   * The default value for the {@code deliverTopicRegex} property.
   */
  public static final String DEFAULT_DELIVER_TOPIC_REGEX = "user/(\\d+)/.*";

  // CHECKSTYLE ON: LineLength

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final DataSource dataSource;
  private final Clock clock;
  private final AtomicLong updateCount;

  /**
   * A mapping of keys to count objects.
   */
  private final ConcurrentMap<DelayedKey, AtomicInteger> counters;

  /**
   * A delay queue of keys whose counts have been updated and need to be persisted.
   */
  private final BlockingQueue<DelayedKey> counterQueue;

  private String mqttServiceName;
  private String nodeSourceIncrementSql;
  private int statLogUpdateCount;

  private Pattern deliverTopicRegex = Pattern.compile(DEFAULT_DELIVER_TOPIC_REGEX);
  private String deliverMqttServiceName;

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
    this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4),
        Clock.tick(Clock.systemUTC(), Duration.ofHours(1)));
  }

  /**
   * Constructor.
   *
   * @param dataSource
   *        the JDBC DataSource
   * @param counters
   *        the node source counters map
   * @param clock
   *        the clock to use; the clock should tick only at the rate that counts should be
   *        aggregated to, e.g. {@code Clock.tick(Clock.systemUTC(), Duration.ofHours(1))}
   * @throws IllegalArgumentException
   *         if any argument is {@literal null}
   */
  public JdbcAuditService(DataSource dataSource, ConcurrentMap<DelayedKey, AtomicInteger> counters,
      Clock clock) {
    super();
    this.dataSource = requireNonNullArgument(dataSource, "dataSource");
    this.counters = requireNonNullArgument(counters, "counters");
    this.counterQueue = new DelayQueueSet<>();
    this.clock = requireNonNullArgument(clock, "clock");
    this.updateCount = new AtomicLong();
    setMqttServiceName(DEFAULT_AUDIT_MQTT_SERVICE_NAME);
    setDeliverMqttServiceName(DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME);
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
      final DelayedKey key = key(nodeId, sourceId);
      addCount(key, byteCount);
    }
  }

  @Override
  public void auditDeliverMessage(Message message) {
    final int byteCount = (message.getPayload() != null ? message.getPayload().length : 0);
    if (byteCount > 0 && message.getTopic() != null) {
      Matcher m = deliverTopicRegex.matcher(message.getTopic());
      if (m.matches()) {
        final String userId = m.group(1);

        if (userId != null && !userId.isBlank()) {
          final DelayedKey key = key(Long.valueOf(userId), null);
          log.trace("Message on topic [{}] delivers {} bytes to user {} @ {}", message.getTopic(),
              byteCount, userId, key.timestamp);
          addCount(key, byteCount);
        }
      }
    }
  }

  private DelayedKey key(Long objectId, String sourceId) {
    return new DelayedKey(objectId, sourceId, clock.instant(), currentTimeMillis() + flushDelay);
  }

  private void addCount(DelayedKey key, int count) {
    counters.computeIfAbsent(key, k -> new AtomicInteger(0)).addAndGet(count);
    counterQueue.add(key);
  }

  /**
   * A delayed key.
   *
   * <p>
   * This class is public to support testing, and is otherwise meant to be internal to this class.
   * </p>
   */
  public static final class DelayedKey implements Delayed {

    /** The object ID (user ID or node ID). */
    private final Long objectId;

    /** The source ID, or {@literal null}. */
    private final String sourceId;

    /** The count timestamp (e.g. an hour-truncated date). */
    private final Instant timestamp;

    /** The expiration date, in epoch milliseconds. */
    private final long expires;

    /**
     * Constructor.
     *
     * @param objectId
     *        the object ID
     * @param sourceId
     *        the source ID
     * @param timestamp
     *        the timestamp
     * @param expires
     *        the expiration
     */
    public DelayedKey(Long objectId, String sourceId, Instant timestamp, long expires) {
      super();
      assert objectId != null : "objectId must not be null";
      assert timestamp != null : "timestamp must not be null";
      this.objectId = objectId;
      this.sourceId = sourceId;
      this.timestamp = timestamp;
      this.expires = expires;
    }

    @Override
    public int hashCode() {
      return Objects.hash(objectId, sourceId, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      // not bothering to check instanceof for performance
      DelayedKey other = (DelayedKey) obj;
      return Objects.equals(objectId, other.objectId) && Objects.equals(sourceId, other.sourceId)
          && Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public int compareTo(Delayed o) {
      // not bothering to check instanceof for performance
      DelayedKey other = (DelayedKey) o;
      int result = Long.compare(expires, other.expires);
      if (result == 0) {
        // fall back to sort by object ID when expires are equal
        result = objectId.compareTo(other.objectId);
        if (result == 0) {
          // fall back to source ID when expires and object ID are equal
          if (sourceId != other.sourceId) {
            if (sourceId == null) {
              result = -1;
            } else if (other.sourceId == null) {
              result = 1;
            } else {
              result = sourceId.compareTo(other.sourceId);
            }
          }
        }
      }
      return result;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      long ms = expires - System.currentTimeMillis();
      return unit.convert(ms, TimeUnit.MILLISECONDS);
    }

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
      log.info("Finished JDBC audit writer thread {}", this);
    }

    private boolean execute() throws SQLException {
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
            DelayedKey key = counterQueue.take();
            flushCount(key, stmt);
          } catch (InterruptedException e) {
            log.info("Writer thread interrupted: exiting now.");
            return false;
          }
        } while (keepGoingWithConnection.get());
        return true;
      }
    }

  }

  private void flushCount(DelayedKey key, PreparedStatement stmt)
      throws SQLException, InterruptedException {
    AtomicInteger counter = counters.get(key);
    if (counter == null) {
      return;
    }
    final int count = counter.getAndSet(0);
    if (count < 1) {
      // clean out stale 0 valued counter
      counters.remove(key, counter);
      return;
    }
    try {
      if (log.isTraceEnabled()) {
        if (key.sourceId != null) {
          log.trace("Incrementing node {} source {} @ {} {} byte count by {}", key.objectId,
              key.sourceId, key.timestamp, mqttServiceName, count);
        } else {
          log.trace("Incrementing user {} @ {} {} byte count by {}", key.objectId, key.timestamp,
              deliverMqttServiceName, count);
        }
      }
      stmt.setString(1, key.sourceId != null ? mqttServiceName : deliverMqttServiceName);
      stmt.setObject(2, key.objectId);
      stmt.setString(3, key.sourceId);
      stmt.setTimestamp(4, java.sql.Timestamp.from(key.timestamp));
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
      addCount(key, count);
      throw e;
    } catch (Exception e) {
      addCount(key, count);
      RuntimeException re;
      if (e instanceof RuntimeException runtime) {
        re = runtime;
      } else {
        re = new RuntimeException("Exception flushing node source audit data", e);
      }
      throw re;
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
   * Disable writing and wait for data to be flushed.
   */
  public synchronized void disableWriting(Duration maxWait) {
    disableWriting();
    if (writerThread != null && writerThread.isAlive()) {
      try {
        log.info("Waiting at most {}ms for writer thread to exit...", maxWait.toMillis());
        writerThread.join(maxWait);
      } catch (InterruptedException e) {
        // ignore and continue
      } finally {
        if (writerThread.isAlive()) {
          writerThread.interrupt();
        }
      }
    }
  }

  /**
   * Set the MQTT audit service name to use for publish events.
   *
   * @param mqttServiceName
   *        the service to set; defaults to {@link #DEFAULT_AUDIT_MQTT_SERVICE_NAME}
   */
  public void setMqttServiceName(String mqttServiceName) {
    this.mqttServiceName = requireNonNullArgument(mqttServiceName, "mqttServiceName");
    reconnectWriter();
  }

  /**
   * Set the MQTT audit service name to use for deliver events.
   *
   * @param deliverMqttServiceName
   *        the service to use; defaults to {@link #DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME}
   * @since 1.2
   */
  public void setDeliverMqttServiceName(String deliverMqttServiceName) {
    this.deliverMqttServiceName = requireNonNullArgument(deliverMqttServiceName,
        "deliverMqttServiceName");
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
    requireNonNullArgument(sql, "sql");
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

  /**
   * Get the deliver topic regular expression.
   *
   * @return the regular expression; defaults to {@link #DEFAULT_DELIVER_TOPIC_REGEX}
   * @since 1.2
   */
  public Pattern getDeliverTopicRegex() {
    return deliverTopicRegex;
  }

  /**
   * Set the deliver topic regular expression.
   *
   * <p>
   * This expression is matched against the deliver request topics, and must provide the following
   * matching groups:
   * </p>
   *
   * <ol>
   * <li>user ID</li>
   * </ol>
   *
   * @param deliverTopicRegex
   *        the regular expression to use
   * @throws IllegalArgumentException
   *         if {@code deliverTopicRegex} is {@literal null}
   * @since 1.2
   */
  public void setDeliverTopicRegex(Pattern deliverTopicRegex) {
    this.deliverTopicRegex = requireNonNullArgument(deliverTopicRegex, "deliverTopicRegex");
  }

}
