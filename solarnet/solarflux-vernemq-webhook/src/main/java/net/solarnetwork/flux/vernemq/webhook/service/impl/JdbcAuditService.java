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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.common.dao.jdbc.BaseJdbcDatumIdServiceAuditor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.flux.vernemq.webhook.domain.Actor;
import net.solarnetwork.flux.vernemq.webhook.domain.Message;
import net.solarnetwork.flux.vernemq.webhook.service.AuditService;
import net.solarnetwork.util.StatTracker;

/**
 * A JDBC implementation of {@link AuditService}.
 *
 * <p>
 * This service coalesces message byte counts in memory, per node/source/hour
 * for published messages and per user/hour for delivered messages, and flushes
 * these to the database via a single "writer" thread after a small delay. This
 * design is meant to support better throughput of audit updates. Counts not
 * yet written are written when the service shuts down, but can be lost if the
 * service stops without shutting down or the database is unavailable.
 * </p>
 *
 * <p>
 * Published message counts are keyed by node ID and source ID. Delivered
 * message counts are keyed by user ID, with no source ID.
 * </p>
 *
 * @author matt
 * @version 1.3
 */
public class JdbcAuditService extends BaseJdbcDatumIdServiceAuditor implements AuditService {

	/**
	 * The default value for the {@code statLogUpdateCount} property.
	 */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/**
	 * The default value for the {@code serviceIncrementSql} property.
	 *
	 * @since 1.3
	 */
	public static final String DEFAULT_SERVICE_INCREMENT_SQL = "{call solardatm.audit_increment_mqtt_byte_count(?,?,?,?,?)}";

	/**
	 * The default value for the {@code mqttServiceName} property.
	 */
	public static final String DEFAULT_AUDIT_MQTT_SERVICE_NAME = "flxi";

	/**
	 * The default value for the {@code deliverMqttServiceName} property.
	 *
	 * @since 1.2
	 */
	public static final String DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME = "flxo";

	/**
	 * The default value for the {@code deliverTopicRegex} property.
	 */
	public static final String DEFAULT_DELIVER_TOPIC_REGEX = "user/(\\d+)/.*";

	private volatile String mqttServiceName = DEFAULT_AUDIT_MQTT_SERVICE_NAME;
	private volatile String deliverMqttServiceName = DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME;
	private volatile Pattern deliverTopicRegex = Pattern.compile(DEFAULT_DELIVER_TOPIC_REGEX);

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC DataSource
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public JdbcAuditService(DataSource dataSource) {
		this(dataSource, new ConcurrentHashMap<>(1000, 0.8f, 4),
				Clock.tick(Clock.systemUTC(), Duration.ofHours(1)),
				new StatTracker("JdbcMqttAuditor", null, LoggerFactory.getLogger(JdbcAuditService.class),
						DEFAULT_STAT_LOG_UPDATE_COUNT));
	}

	/**
	 * Constructor.
	 *
	 * @param dataSource
	 *        the JDBC DataSource
	 * @param counters
	 *        the counters map; the map must perform {@code compute()}
	 *        atomically, as {@link ConcurrentHashMap} does
	 * @param clock
	 *        the clock to use; the clock should tick only at the rate that
	 *        counts should be aggregated to, e.g.
	 *        {@code Clock.tick(Clock.systemUTC(), Duration.ofHours(1))}
	 * @param statCounter
	 *        the statistics to track
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 1.3
	 */
	public JdbcAuditService(DataSource dataSource, ConcurrentMap<DatumId, AtomicInteger> counters,
			Clock clock, StatTracker statCounter) {
		super(dataSource, counters, clock, statCounter);
		setServiceIncrementSql(DEFAULT_SERVICE_INCREMENT_SQL);
	}

	@Override
	public void auditPublishMessage(Actor actor, Long nodeId, String sourceId, Message message) {
		if ( nodeId == null || sourceId == null ) {
			// a count without a source ID would be written as a delivered count for a user
			return;
		}
		addServiceCount(DatumId.nodeId(nodeId, sourceId, clock.instant()), payloadLength(message));
	}

	@Override
	public void auditDeliverMessage(Message message) {
		final int byteCount = payloadLength(message);
		if ( byteCount > 0 && message.getTopic() != null ) {
			Matcher m = deliverTopicRegex.matcher(message.getTopic());
			if ( m.matches() ) {
				final String userId = m.group(1);

				if ( userId != null && !userId.isBlank() ) {
					final DatumId key = DatumId.nodeId(Long.valueOf(userId), null, clock.instant());
					log.trace("Message on topic [{}] delivers {} bytes to user {} @ {}",
							message.getTopic(), byteCount, userId, key.getTimestamp());
					addServiceCount(key, byteCount);
				}
			}
		}
	}

	private static int payloadLength(Message message) {
		final byte[] payload = message.getPayload();
		return (payload != null ? payload.length : 0);
	}

	/**
	 * Set the parameters of the {@code serviceIncrementSql} statement, to
	 * write a count.
	 *
	 * <p>
	 * The statement must accept the following parameters:
	 * </p>
	 *
	 * <ol>
	 * <li>string - the MQTT service name: {@code mqttServiceName} for a
	 * published count, or {@code deliverMqttServiceName} for a delivered
	 * count</li>
	 * <li>long - the node ID for a published count, or the user ID for a
	 * delivered count</li>
	 * <li>string - the source ID for a published count, or {@code null} for a
	 * delivered count</li>
	 * <li>timestamp - the audit date</li>
	 * <li>integer - the byte count to add</li>
	 * </ol>
	 */
	@Override
	protected void setServiceIncrementParameters(PreparedStatement stmt, DatumId key, int count)
			throws SQLException {
		final String sourceId = key.getSourceId();
		stmt.setString(1, sourceId != null ? mqttServiceName : deliverMqttServiceName);
		stmt.setObject(2, key.getObjectId());
		stmt.setString(3, sourceId);
		stmt.setTimestamp(4, Timestamp.from(key.getTimestamp()));
		stmt.setInt(5, count);
	}

	@Override
	public String getPingTestName() {
		return "JDBC MQTT Auditor";
	}

	/**
	 * Set the MQTT audit service name to use for publish events.
	 *
	 * @param mqttServiceName
	 *        the service to set; defaults to
	 *        {@link #DEFAULT_AUDIT_MQTT_SERVICE_NAME}
	 */
	public void setMqttServiceName(String mqttServiceName) {
		this.mqttServiceName = requireNonNullArgument(mqttServiceName, "mqttServiceName");
	}

	/**
	 * Set the MQTT audit service name to use for deliver events.
	 *
	 * @param deliverMqttServiceName
	 *        the service to use; defaults to
	 *        {@link #DEFAULT_AUDIT_DELIVER_MQTT_SERVICE_NAME}
	 * @since 1.2
	 */
	public void setDeliverMqttServiceName(String deliverMqttServiceName) {
		this.deliverMqttServiceName = requireNonNullArgument(deliverMqttServiceName,
				"deliverMqttServiceName");
	}

	/**
	 * Get the deliver topic regular expression.
	 *
	 * @return the regular expression; defaults to
	 *         {@link #DEFAULT_DELIVER_TOPIC_REGEX}
	 * @since 1.2
	 */
	public Pattern getDeliverTopicRegex() {
		return deliverTopicRegex;
	}

	/**
	 * Set the deliver topic regular expression.
	 *
	 * <p>
	 * This expression is matched against the deliver request topics, and must
	 * provide the following matching groups:
	 * </p>
	 *
	 * <ol>
	 * <li>user ID</li>
	 * </ol>
	 *
	 * @param deliverTopicRegex
	 *        the regular expression to use
	 * @throws IllegalArgumentException
	 *         if {@code deliverTopicRegex} is {@code null}
	 * @since 1.2
	 */
	public void setDeliverTopicRegex(Pattern deliverTopicRegex) {
		this.deliverTopicRegex = requireNonNullArgument(deliverTopicRegex, "deliverTopicRegex");
	}

}
