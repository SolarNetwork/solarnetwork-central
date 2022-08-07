/* ==================================================================
 * SolarFluxPublisher.java - 7/08/2022 4:07:42 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttQos;

/**
 * Basic service to publish objects to SolarFlux.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarFluxPublisher<T> implements Function<T, Future<?>>, MqttConnectionObserver {

	/** The default value for the {@code mqttHost} property. */
	public static final String DEFAULT_MQTT_HOST = "mqtts://influx.solarnetwork.net:8884";

	/** The default value for the {@code mqttUsername} property. */
	public static final String DEFAULT_MQTT_USERNAME = "solarnet-pub";

	private static final Logger log = LoggerFactory.getLogger(SolarFluxPublisher.class);

	private final AtomicReference<MqttConnection> mqttConnection = new AtomicReference<>();
	private final String name;
	private final ObjectMapper objectMapper;
	private final Function<T, String> topicFn;
	private final boolean retained;
	private final MqttQos publishQos;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the MQTT connection factory
	 * @param mqttStats
	 *        the stats to use
	 * @param name
	 *        the display name to use
	 * @param objectMapper
	 *        the mapper for JSON
	 * @param topicFn
	 *        the function to generate the MQTT topic for a given object
	 * @param retained
	 *        {@literal true} to publish each message as retained
	 * @param publishQos
	 *        the publish QoS
	 */
	public SolarFluxPublisher(String name, ObjectMapper objectMapper, Function<T, String> topicFn,
			boolean retained, MqttQos publishQos) {
		this.name = requireNonNullArgument(name, "name");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.topicFn = requireNonNullArgument(topicFn, "topicFn");
		this.retained = retained;
		this.publishQos = requireNonNullArgument(publishQos, "publishQos");
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		log.info("MQTT connection lost for {} publisher", name);
		mqttConnection.compareAndSet(connection, null);
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		log.info("MQTT connection established for {} publisher.", name);
		mqttConnection.set(connection);
	}

	@Override
	public Future<?> apply(T item) {
		if ( item == null ) {
			return CompletableFuture.completedFuture(null);
		}
		String topic = topicFn.apply(item);
		if ( topic == null ) {
			return CompletableFuture.completedFuture(null);
		}

		MqttConnection conn = mqttConnection.get();
		if ( conn == null || !conn.isEstablished() ) {
			log.debug("MQTT client not avaialable for publishing [{}] to SolarFlux", item);
			return CompletableFuture
					.failedFuture(new RemoteServiceException("Not connected to SolarFlux"));
		}

		try {
			byte[] payload = objectMapper.writeValueAsBytes(item);
			if ( log.isDebugEnabled() ) {
				JsonNode jsonData = objectMapper.valueToTree(item);
				log.debug("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
			}
			if ( log.isTraceEnabled() ) {
				log.trace("Publishing to MQTT topic {}\n{}", topic,
						Base64.getEncoder().encodeToString(payload));
			}
			return conn.publish(new BasicMqttMessage(topic, retained, publishQos, payload));
		} catch ( IOException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error publishing {} to SolarFlux topic {}: {}", item, topic, root.toString(), e);
			return CompletableFuture.failedFuture(e);
		}
	}

}
