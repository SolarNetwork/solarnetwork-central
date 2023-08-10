/* ==================================================================
 * SolarInputDatumObserver.java - 10/08/2023 6:41:01 am
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

package net.solarnetwork.central.datum.mqtt;

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.springframework.transaction.TransactionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.biz.NodeEventObservationRegistrar;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * Observer of SolarInput datum streams.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarInputDatumObserver extends BaseMqttConnectionObserver
		implements NodeEventObservationRegistrar<ObjectDatum> {

	/**
	 * The default MQTT topic template for node data subscription.
	 * 
	 * <p>
	 * This template will be passed a single node ID (or {@literal +} wildcard)
	 * parameter.
	 * </p>
	 */
	public static final String DEFAULT_NODE_DATUM_TOPIC_TEMPLATE = "node/%s/datum";

	/**
	 * A regular expression that matches node topics and returns node ID and
	 * sub-topic groups.
	 */
	public static final Pattern DEFAULT_NODE_TOPIC_REGEX = Pattern.compile("node/(\\d+)/(.*)");

	/** The JSON field name for an "object type". */
	public static final String OBJECT_TYPE_FIELD = "__type__";

	/** The instruction status object type. */
	public static final String INSTRUCTION_STATUS_TYPE = "InstructionStatus";

	/** The datum object type. */
	public static final String GENERAL_NODE_DATUM_TYPE = "datum";

	/** The JSON field name for an instruction ID on an instruction datum. */
	public static final String INSTRUCTION_ID_FIELD = "instructionId";

	private final Executor executor;
	private final ObjectMapper objectMapper;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final DatumStreamMetadataDao datumStreamMetadataDao;

	private Cache<DatumId, ObjectDatumStreamMetadata> metadataCache;
	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;
	private Pattern nodeDatumTopicRegex = DEFAULT_NODE_TOPIC_REGEX;

	private final ConcurrentMap<Long, CopyOnWriteArrayList<Consumer<ObjectDatum>>> observers;
	private final ConcurrentMap<Consumer<ObjectDatum>, MessageHandler> handlers;

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        the executor
	 * @param objectMapper
	 *        object mapper for messages
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param datumStreamMetadataDao
	 *        the stream metadata DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarInputDatumObserver(Executor executor, ObjectMapper objectMapper,
			SolarNodeOwnershipDao nodeOwnershipDao, DatumStreamMetadataDao datumStreamMetadataDao) {
		this(new ConcurrentHashMap<>(64, 0.9f, 4), executor, objectMapper, nodeOwnershipDao,
				datumStreamMetadataDao);
	}

	/**
	 * Constructor.
	 * 
	 * @param observers
	 *        the map to use for holding observer registrations
	 * @param executor
	 *        the executor
	 * @param objectMapper
	 *        object mapper for messages
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param datumStreamMetadataDao
	 *        the stream metadata DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolarInputDatumObserver(
			ConcurrentMap<Long, CopyOnWriteArrayList<Consumer<ObjectDatum>>> observers,
			Executor executor, ObjectMapper objectMapper, SolarNodeOwnershipDao nodeOwnershipDao,
			DatumStreamMetadataDao datumStreamMetadataDao) {
		this.observers = requireNonNullArgument(observers, "observers");
		this.handlers = new ConcurrentHashMap<>(64, 0.9f, 4);
		this.executor = executor;
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.datumStreamMetadataDao = requireNonNullArgument(datumStreamMetadataDao,
				"datumStreamMetadataDao");
		setDisplayName("SolarInput Obs MQTT");
	}

	@Override
	public void registerNodeObserver(final Consumer<ObjectDatum> observer, final Long... nodeIds) {
		requireNonNullArgument(observer, "observer");
		final var handler = handlers.computeIfAbsent(observer, (k) -> new MessageHandler(k));
		for ( Long nodeId : requireNonEmptyArgument(nodeIds, "nodeIds") ) {
			if ( observers.computeIfAbsent(nodeId, (k) -> new CopyOnWriteArrayList<>())
					.addIfAbsent(observer) ) {
				subscribe(mqttConnection.get(), nodeId, handler);
			}
		}
	}

	@Override
	public void unregisterNodeObserver(final Consumer<ObjectDatum> observer, final Long... nodeIds) {
		if ( observer == null ) {
			return;
		}
		if ( nodeIds != null && nodeIds.length > 0 ) {
			for ( Long nodeId : nodeIds ) {
				var list = observers.get(nodeId);
				if ( list != null ) {
					if ( list.remove(observer) ) {
						var handler = handlers.get(observer);
						if ( handler != null ) {
							unsubscribe(mqttConnection.get(), nodeId, handler);
						}
					}
				}
			}
		} else {
			MessageHandler handler = handlers.remove(observer);
			if ( handler == null ) {
				return;
			}
			for ( var entry : observers.entrySet() ) {
				if ( entry.getValue().remove(observer) ) {
					unsubscribe(mqttConnection.get(), entry.getKey(), handler);
				}
			}
		}
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		executor.execute(() -> {
			for ( var entry : observers.entrySet() ) {
				for ( var observer : entry.getValue() ) {
					var handler = handlers.get(observer);
					if ( handler != null ) {
						subscribe(connection, entry.getKey(), handler);
					}
				}
			}
		});
	}

	private void subscribe(MqttConnection connection, Long nodeId, MqttMessageHandler handler) {
		if ( connection == null || !connection.isEstablished() ) {
			return;
		}
		final String topic = String.format(nodeDatumTopicTemplate, nodeId.toString());
		try {
			connection.subscribe(topic, getSubscribeQos(), handler).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {} for {}", topic, connection, handler);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {} for {}: {}", topic, connection, handler,
					e.toString());
		}
	}

	private void unsubscribe(MqttConnection connection, Long nodeId, MqttMessageHandler handler) {
		if ( connection == null || !connection.isEstablished() ) {
			return;
		}
		final String topic = String.format(nodeDatumTopicTemplate, nodeId.toString());
		try {
			connection.unsubscribe(topic, handler).get(getSubscribeTimeoutSeconds(), TimeUnit.SECONDS);
			log.info("Unsubscribed from MQTT topic {} @ {} for {}", topic, connection, handler);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to unsubscribe from MQTT topic {} @ {} for {}: {}", topic, connection,
					handler, e.toString());
		}
	}

	private final class MessageHandler implements MqttMessageHandler, Consumer<ObjectDatum> {

		private final Consumer<ObjectDatum> observer;

		private MessageHandler(Consumer<ObjectDatum> observer) {
			super();
			this.observer = observer;
		}

		@Override
		public void accept(ObjectDatum t) {
			observer.accept(t);
		}

		@Override
		public String toString() {
			return observer.toString();
		}

		@Override
		public void onMqttMessage(MqttMessage message) {
			final String topic = message.getTopic();
			try {
				Matcher m = nodeDatumTopicRegex.matcher(topic);
				if ( !m.matches() ) {
					log.debug("Unsupported node datum topic: {}" + topic);
					return;
				}
				final Long nodeId = Long.valueOf(m.group(1));
				final SolarNodeOwnership owner = nodeOwnershipDao.ownershipForNodeId(nodeId);
				if ( owner == null ) {
					log.debug("Unknown owner for node {}, ignoring", nodeId);
					return;
				}
				if ( owner.isArchived() ) {
					log.debug("Node {} is archived, ignoring", nodeId);
					return;
				}

				ObjectDatum datum = parseMqttMessage(objectMapper, message, topic, nodeId, owner);
				if ( datum != null ) {
					observer.accept(datum);
				}
			} catch ( IOException e ) {
				log.debug("Communication error handling message on MQTT topic {}", topic, e);
			} catch ( Exception e ) {
				log.error("Error handling MQTT message on topic {}", topic, e);
			}
		}

		private ObjectDatum parseMqttMessage(ObjectMapper objectMapper, MqttMessage message,
				String topic, Long nodeId, SolarNodeOwnership owner) throws IOException {
			JsonNode root = objectMapper.readTree(message.getPayload());
			if ( root.isObject() || root.isArray() ) {
				int remainingTries = getTransientErrorTries();
				while ( remainingTries > 0 ) {
					try {
						if ( root.isObject() ) {
							return parseDatum(root, nodeId, owner);
						} else {
							// V2 stream datum array
							return parseStreamDatum(root, nodeId, owner);
						}
					} catch ( RepeatableTaskException | TransactionException e ) {
						remainingTries--;
						if ( remainingTries > 0 ) {
							log.warn(
									"Transient error handling MQTT message on topic {}; will try {} more times",
									topic, remainingTries, e);
						} else {
							throw e;
						}
					}
				}
			}
			return null;
		}

		private ObjectDatum parseDatum(JsonNode json, Long nodeId, SolarNodeOwnership owner) {
			String nodeType = getStringFieldValue(json, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
			JsonNode instrId = json.get(INSTRUCTION_ID_FIELD);
			if ( (instrId != null && instrId.isNumber())
					|| INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
				// do not handle instruction status
				return null;
			}
			try {
				final Datum d = objectMapper.treeToValue(json, Datum.class);
				if ( d.getKind() == ObjectDatumKind.Location ) {
					log.debug("Ignoring node {} Location datum kind: {}", nodeId, d);
					return null;
				}
				if ( d.getSourceId() == null || d.getSourceId().isBlank() ) {
					log.debug("Ignoring node {} datum with missing or empty source ID: {}", nodeId, d);
					return null;
				}

				final DatumId id = DatumId.nodeId(nodeId, d.getSourceId(), d.getTimestamp());
				final ObjectDatumStreamMetadata meta = metadataForDatumId(owner.getUserId(), id);
				if ( meta == null ) {
					log.debug("Ignoring node {} datum with missing stream metadata: {}", nodeId, d);
				}
				return ObjectDatum.forDatum(d, owner.getUserId(), id, meta, true);
			} catch ( Exception e ) {
				log.debug("Unable to parse node {} datum: {}", nodeId, e.getMessage());
			}
			return null;
		}

		private ObjectDatum parseStreamDatum(JsonNode json, Long nodeId, SolarNodeOwnership owner) {
			try {
				final StreamDatum d = objectMapper.treeToValue(json, StreamDatum.class);
				final ObjectDatumStreamMetadata meta = metadataForStreamId(owner.getUserId(),
						d.getStreamId());
				if ( meta == null ) {
					log.debug("Ignoring node {} stream datum with missing stream metadata: {}", nodeId,
							d);
				}
				final DatumId id = DatumId.nodeId(nodeId, meta.getSourceId(), d.getTimestamp());
				return ObjectDatum.forStreamDatum(d, owner.getUserId(), id, meta);
			} catch ( IOException e ) {
				log.debug("Unable to parse node {} stream datum: {}", nodeId, e.getMessage());
			}
			return null;
		}

	}

	private static String getStringFieldValue(JsonNode json, String fieldName, String placeholder) {
		JsonNode child = json.get(fieldName);
		return (child == null ? placeholder : child.asText());
	}

	private ObjectDatumStreamMetadata metadataForDatumId(Long userId, DatumId id) {
		final var cache = getMetadataCache();
		var result = (cache != null ? cache.get(id) : null);
		if ( result != null ) {
			return result;
		}
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setUserId(userId);
		criteria.setObjectKind(ObjectDatumKind.Node);
		criteria.setNodeId(id.getObjectId());
		criteria.setSourceId(id.getSourceId());
		var results = datumStreamMetadataDao.findDatumStreamMetadata(criteria);
		return StreamSupport.stream(results.spliterator(), false).findFirst().orElse(null);
	}

	private ObjectDatumStreamMetadata metadataForStreamId(Long userId, UUID id) {
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setStreamId(id);
		ObjectDatumStreamMetadata meta = datumStreamMetadataDao.findStreamMetadata(criteria);
		return (meta != null && meta.getKind() == ObjectDatumKind.Node ? meta : null);
	}

	/**
	 * Get the metadata cache.
	 * 
	 * @return the metadata cache to use
	 */
	public Cache<DatumId, ObjectDatumStreamMetadata> getMetadataCache() {
		return metadataCache;
	}

	/**
	 * Set the metadata cache
	 * 
	 * @param metadataCache
	 *        the metadata cache to set
	 */
	public void setMetadataCache(Cache<DatumId, ObjectDatumStreamMetadata> metadataCache) {
		this.metadataCache = metadataCache;
	}

	/**
	 * Get the node datum topic template.
	 * 
	 * @return the template
	 */
	public String getNodeDatumTopicTemplate() {
		return nodeDatumTopicTemplate;
	}

	/**
	 * Set the node datum topic template.
	 * 
	 * <p>
	 * This template is used to generate MQTT topics for subscriptions, and must
	 * accept a single string placeholder for a node ID (or wildcard). The
	 * template must work in conjunction with the
	 * {@link #getNodeDatumTopicRegex()} pattern.
	 * </p>
	 * 
	 * @param nodeDatumTopicTemplate
	 *        the template to set; if {@literal null} then
	 *        {@link #DEFAULT_NODE_DATUM_TOPIC_TEMPLATE} will be set instead
	 */
	public void setNodeDatumTopicTemplate(String nodeDatumTopicTemplate) {
		this.nodeDatumTopicTemplate = (nodeDatumTopicTemplate != null ? nodeDatumTopicTemplate
				: DEFAULT_NODE_DATUM_TOPIC_TEMPLATE);
	}

	/**
	 * Get the node datum topic pattern.
	 * 
	 * @return the pattern
	 */
	public Pattern getNodeDatumTopicRegex() {
		return nodeDatumTopicRegex;
	}

	/**
	 * Set the node datum topic pattern.
	 * 
	 * <p>
	 * This pattern is applied to MQTT topics and must return two group matches:
	 * the node ID and the sub-topic. The pattern must work in conjunction with
	 * the {@link #getNodeDatumTopicTemplate()} value.
	 * </p>
	 * 
	 * @param nodeDatumTopicRegex
	 *        the nodeDatumTopicRegex to set; if {@literal null} then
	 *        {@link #DEFAULT_NODE_TOPIC_REGEX} will be set instead
	 */
	public void setNodeDatumTopicRegex(Pattern nodeDatumTopicRegex) {
		this.nodeDatumTopicRegex = (nodeDatumTopicRegex != null ? nodeDatumTopicRegex
				: DEFAULT_NODE_TOPIC_REGEX);
	}

}
