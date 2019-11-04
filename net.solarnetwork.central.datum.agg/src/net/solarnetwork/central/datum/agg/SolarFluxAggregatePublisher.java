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
import java.util.concurrent.ExecutorService;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.common.mqtt.support.AsyncMqttServiceSupport;
import net.solarnetwork.common.mqtt.support.MqttStats;
import net.solarnetwork.common.mqtt.support.MqttStats.MqttStat;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.OptionalService;

/**
 * Publish aggregate datum to SolarFlux.
 * 
 * @author matt
 * @version 1.0
 * @since 1.7
 */
public class SolarFluxAggregatePublisher extends AsyncMqttServiceSupport
		implements AggregateDatumProcessor {

	/** The MQTT topic template for aggregate node data publication. */
	public static final String NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE = "node/%d/datum/%s/%s";

	/** The default value for the {@code persistencePath} property. */
	public static final String DEFAULT_PERSISTENCE_PATH = "var/mqtt-flux-agg";

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnode";

	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 * 
	 * @param executorService
	 *        task service
	 * @param sslService
	 *        SSL service
	 * @param retryConnect
	 *        {@literal true} to keep retrying to connect to MQTT server
	 * @param objectMapper
	 *        the object mapper
	 */
	public SolarFluxAggregatePublisher(ExecutorService executorService,
			OptionalService<SSLService> sslService, boolean retryConnect, ObjectMapper objectMapper) {
		this(executorService, sslService, retryConnect, null, null, objectMapper);
	}

	/**
	 * Constructor.
	 * 
	 * @param executorService
	 *        task service
	 * @param sslService
	 *        SSL service
	 * @param retryConnect
	 *        {@literal true} to keep retrying to connect to MQTT server
	 * @param serverUri
	 *        MQTT URI to connect to
	 * @param clientId
	 *        the MQTT client ID
	 * @param objectMapper
	 *        the object mapper
	 */
	public SolarFluxAggregatePublisher(ExecutorService executorService,
			OptionalService<SSLService> sslService, boolean retryConnect, String serverUri,
			String clientId, ObjectMapper objectMapper) {
		super(executorService, sslService, retryConnect,
				new MqttStats(serverUri, 500, SolarFluxAggregatePublishCountStat.values()), serverUri,
				clientId);
		this.objectMapper = objectMapper;
		setPublishOnly(true);
		setPersistencePath(DEFAULT_PERSISTENCE_PATH);
	}

	private String topicForDatum(Aggregation aggregation, Identity<GeneralNodeDatumPK> datum) {
		Long nodeId = datum.getId().getNodeId();
		String sourceId = datum.getId().getSourceId();
		if ( nodeId == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		if ( sourceId.startsWith("/") ) {
			sourceId = sourceId.substring(1);
		}
		return String.format(NODE_AGGREGATE_DATUM_TOPIC_TEMPLATE, nodeId, aggregation.getKey(),
				sourceId);
	}

	@Override
	public boolean processStaleAggregateDatum(Aggregation aggregation,
			Identity<GeneralNodeDatumPK> datum) {
		String topic = topicForDatum(aggregation, datum);
		if ( topic == null ) {
			return false;
		}
		IMqttAsyncClient client = client();
		if ( client != null ) {
			try {
				JsonNode jsonData = objectMapper.valueToTree(datum);
				byte[] payload = objectMapper.writeValueAsBytes(jsonData);
				if ( log.isDebugEnabled() ) {
					log.debug("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
				}
				if ( log.isTraceEnabled() ) {
					log.trace("Publishing to MQTT topic {}\n{}", topic, Hex.encodeHexString(payload));
				}

				IMqttDeliveryToken token = client.publish(topic, payload, getPublishQos(), false);
				token.waitForCompletion(getMqttTimeout());

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
					getStats().incrementAndGet(pubStat);
				}
				return true;
			} catch ( MqttException | IOException e ) {
				log.error("Error publishing {} datum {} to SolarFlux @ {}: {}", aggregation, datum,
						client.getServerURI(), e.toString(), e);
			}
		} else {
			log.warn("MQTT client not avaialable for publishing aggregate datum.");
		}
		return false;
	}

}
