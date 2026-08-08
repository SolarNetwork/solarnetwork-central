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
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MessageSizeLimitExceeded;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.service.RemoteServiceException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Basic service to publish objects to SolarFlux.
 * 
 * @author matt
 * @version 2.0
 */
public class MqttJsonPublisher<T> extends BaseMqttConnectionObserver implements Function<T, Future<?>> {

	/** The {@code errorTimeout} property default value. */
	public static final Duration DEFAULT_ERROR_TIMEOUT = Duration.ZERO;

	private final ObjectMapper objectMapper;
	private final Function<T, @Nullable String> topicFn;
	private final @Nullable BiFunction<T, Throwable, @Nullable String> errorTopicFn;
	private final @Nullable BiFunction<T, Throwable, ? extends @Nullable Object> errorItemFn;

	private @Nullable Duration errorTimeout = DEFAULT_ERROR_TIMEOUT;

	// cache this because toMillis() is relatively slow
	private long errorTimeoutMs = DEFAULT_ERROR_TIMEOUT.toMillis();

	/**
	 * Constructor.
	 * 
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public MqttJsonPublisher(String name, ObjectMapper objectMapper,
			Function<T, @Nullable String> topicFn, boolean retained, MqttQos publishQos) {
		this(name, objectMapper, topicFn, retained, publishQos, null, null);
	}

	/**
	 * Constructor.
	 * 
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
	 * @param errorTopicFn
	 *        optional function to resolve an error topic name when publishing
	 *        immediately fails
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public MqttJsonPublisher(String name, ObjectMapper objectMapper,
			Function<T, @Nullable String> topicFn, boolean retained, MqttQos publishQos,
			@Nullable BiFunction<T, Throwable, @Nullable String> errorTopicFn,
			@Nullable BiFunction<T, Throwable, ? extends @Nullable Object> errorItemFn) {
		setDisplayName(requireNonNullArgument(name, "name"));
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.topicFn = requireNonNullArgument(topicFn, "topicFn");
		setRetained(retained);
		setPublishQos(requireNonNullArgument(publishQos, "publishQos"));
		this.errorTopicFn = errorTopicFn;
		this.errorItemFn = errorItemFn;
	}

	@Override
	public Future<?> apply(@Nullable T item) {
		String topic = (item != null ? topicFn.apply(item) : null);
		return publish(item, topic);
	}

	/**
	 * Publish an item to a given topic.
	 * 
	 * @param item
	 *        the item
	 * @param topic
	 *        the topic
	 * @return the publish future
	 */
	protected Future<?> publish(@Nullable T item, @Nullable String topic) {
		return publish(item, topic, isRetained(), getPublishQos());
	}

	/**
	 * Publish an item to a given topic.
	 * 
	 * @param item
	 *        the item
	 * @param topic
	 *        the topic
	 * @param retained
	 *        {@literal true} if the message should have the {@code retained}
	 *        flag set
	 * @param qos
	 *        the publish QoS to use
	 * @return the publish future
	 * @since 1.1
	 */
	protected Future<?> publish(@Nullable T item, @Nullable String topic, boolean retained,
			MqttQos qos) {
		if ( item == null || topic == null ) {
			return CompletableFuture.completedFuture(null);
		}

		final MqttConnection conn = mqttConnection.get();
		if ( conn == null || !conn.isEstablished() ) {
			log.debug("{} MQTT client not avaialable for publishing [{}]", getDisplayName(), item);
			return CompletableFuture
					.failedFuture(new RemoteServiceException("Not connected to " + getDisplayName()));
		}

		try {
			final byte[] payload = objectMapper.writeValueAsBytes(item);
			if ( log.isDebugEnabled() ) {
				JsonNode jsonData = objectMapper.valueToTree(item);
				log.debug("Publishing to MQTT topic {} JSON:\n{}", topic, jsonData);
			}
			if ( log.isTraceEnabled() ) {
				log.trace("Publishing to MQTT topic {}\n{}", topic,
						Base64.getEncoder().encodeToString(payload));
			}
			final Future<?> result = conn.publish(new BasicMqttMessage(topic, retained, qos, payload));
			if ( errorTimeoutMs >= 0 && errorTopicFn != null && errorItemFn != null ) {
				try {
					result.get(errorTimeoutMs, TimeUnit.MILLISECONDS);
				} catch ( ExecutionException e ) {
					if ( e.getCause() instanceof MessageSizeLimitExceeded sle ) {
						final String errTopic = errorTopicFn.apply(item, sle);
						final Object errItem = errorItemFn.apply(item, sle);
						if ( errTopic != null && errItem != null ) {
							try {
								byte[] errPayload = objectMapper.writeValueAsBytes(errItem);
								if ( log.isDebugEnabled() ) {
									JsonNode errJsonData = objectMapper.valueToTree(errItem);
									log.debug("Publishing to MQTT topic {} JSON:\n{}", errTopic,
											errJsonData);
								}
								if ( log.isTraceEnabled() ) {
									log.trace("Publishing to MQTT topic {}\n{}", errTopic,
											Base64.getEncoder().encodeToString(errPayload));
								}
								return conn.publish(
										new BasicMqttMessage(errTopic, retained, qos, errPayload));
							} catch ( Exception e2 ) {
								log.error("Error publishing error item {} to {} topic {}: {}", errItem,
										getDisplayName(), errTopic, e2.getMessage(), e2);
							}
						}
					}
				} catch ( Exception e ) {
					// let everything else bubble back to caller
				}
			}
			return result;
		} catch ( JacksonException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			log.error("Error publishing {} to {} topic {}: {}", item, getDisplayName(), topic, root, e);
			return CompletableFuture.failedFuture(e);
		}
	}

	/**
	 * Get the error timeout.
	 * 
	 * @return the timeout; defaults to {@link #DEFAULT_ERROR_TIMEOUT}
	 */
	public final @Nullable Duration getErrorTimeout() {
		return errorTimeout;
	}

	/**
	 * Set the error timeout.
	 * 
	 * @param errorTimeout
	 *        the timeout to set; if {@code null} or negative then no error
	 *        handling will be used
	 */
	public final void setErrorTimeout(@Nullable Duration errorTimeout) {
		var dur = (errorTimeout != null && !errorTimeout.isNegative() ? errorTimeout : null);
		this.errorTimeout = dur;
		this.errorTimeoutMs = (dur != null ? dur.toMillis() : -1L);
	}

}
