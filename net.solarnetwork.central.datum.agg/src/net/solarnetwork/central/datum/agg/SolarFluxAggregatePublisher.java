/* ==================================================================
 * SolarFluxAggregatePublisher.java - 4/11/2019 3:17:01 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.codec.binary.Hex;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionConfig;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.common.mqtt.MqttStats.MqttStat;
import net.solarnetwork.domain.Identity;

/**
 * Publish aggregate datum to SolarFlux.
 * 
 * @author matt
 * @version 1.3
 * @since 1.7
 */
public class SolarFluxAggregatePublisher extends BaseMqttConnectionService
		implements AggregateDatumProcessor {

	/**
	 * The MQTT topic template for aggregate node data publication.
	 * 
	 * <p>
	 * Accepts the following parameters:
	 * </p>
	 * 
	 * <ol>
	 * <li><b>user ID</b> (long)</li>
	 * <li><b>node ID</b> (long)</li>
	 * <li><b>aggregation code</b> (string)</li>
	 * <li><b>source ID (string)</li>
	 * </ol>
	 */
	public static final String NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE = "user/%d/node/%d/datum/%s/%s";

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnet";

	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the factory to use for {@link MqttConnection} instances
	 * @param objectMapper
	 *        the mapper to use
	 */
	public SolarFluxAggregatePublisher(MqttConnectionFactory connectionFactory,
			ObjectMapper objectMapper) {
		super(connectionFactory, new MqttStats("SolarFluxAggregatePublisher", 500,
				SolarFluxAggregatePublishCountStat.values()));
		this.objectMapper = objectMapper;
		getMqttConfig().setUsername(DEFAULT_MQTT_USERNAME);
		try {
			getMqttConfig().setServerUri(new URI(DEFAULT_MQTT_HOST));
		} catch ( URISyntaxException e ) {
			throw new RuntimeException(e);
		}
	}

	private String topicForDatum(Long userId, Aggregation aggregation,
			Identity<GeneralNodeDatumPK> datum) {
		Long nodeId = datum.getId().getNodeId();
		String sourceId = datum.getId().getSourceId();
		if ( userId == null || nodeId == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return String.format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE, userId, nodeId, aggregation.getKey(),
				sourceId);
	}

	@Override
	public boolean isConfigured() {
		MqttConnection conn = connection();
		return conn != null && conn.isEstablished();
	}

	@Override
	public boolean processStaleAggregateDatum(Long userId, Aggregation aggregation,
			Identity<GeneralNodeDatumPK> datum) {
		String topic = topicForDatum(userId, aggregation, datum);
		if ( topic == null ) {
			return false;
		}
		MqttConnection conn = connection();
		MqttConnectionConfig mqttConfig = getMqttConfig();
		if ( conn != null && conn.isEstablished() ) {
			try {
				JsonNode jsonData = objectMapper.valueToTree(datum);
				byte[] payload = objectMapper.writeValueAsBytes(jsonData);
				if ( log.isDebugEnabled() ) {
					log.debug("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
				}
				if ( log.isTraceEnabled() ) {
					log.trace("Publishing to MQTT topic {}\n{}", topic, Hex.encodeHexString(payload));
				}

				Future<?> f = conn.publish(new BasicMqttMessage(topic, true, getPublishQos(), payload));
				f.get(mqttConfig.getConnectTimeoutSeconds(), TimeUnit.SECONDS);

				MqttStat pubStat = null;
				switch (aggregation) {
					case Hour:
						pubStat = SolarFluxAggregatePublishCountStat.HourlyDatumPublished;
						break;

					case Day:
						pubStat = SolarFluxAggregatePublishCountStat.DailyDatumPublished;
						break;

					case Month:
						pubStat = SolarFluxAggregatePublishCountStat.MonthlyDatumPublished;
						break;

					default:
						// ignore others
				}
				if ( pubStat != null ) {
					getMqttStats().incrementAndGet(pubStat);
				}
				return true;
			} catch ( TimeoutException e ) {
				// don't generate error for timeout; just assume the problem is transient, e.g.
				// network connection lost, and will be resolved eventually
				log.warn("Timeout publishing {} datum {} to SolarFlux @ {}", aggregation, datum,
						mqttConfig.getServerUri());
			} catch ( IOException | InterruptedException | ExecutionException e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.error("Error publishing {} datum {} to SolarFlux @ {}: {}", aggregation, datum,
						mqttConfig.getServerUri(), root.toString(), e);
			}
		} else {
			log.debug("MQTT client not avaialable for publishing SolarFlux {} datum to {}.", aggregation,
					topic);
		}
		return false;
	}

	@Override
	public String getPingTestName() {
		return "SolarFlux Aggregate Publisher";
	}

}
