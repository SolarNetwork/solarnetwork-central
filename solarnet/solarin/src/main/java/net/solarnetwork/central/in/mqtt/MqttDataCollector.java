/* ==================================================================
 * MqttDataCollector.java - 10/06/2018 12:57:43 PM
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

package net.solarnetwork.central.in.mqtt;

import static java.util.Collections.singleton;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionException;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumUserEvents;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.util.StatTracker;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * MQTT implementation of upload service.
 *
 * @author matt
 * @version 4.1
 */
public class MqttDataCollector extends BaseMqttConnectionObserver
		implements MqttMessageHandler, CommonUserEvents, DatumUserEvents {

	/** A datum tag that indicates v2 CBOR encoding. */
	public static final String TAG_V2 = "_v2";

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
	public static final Pattern NODE_TOPIC_REGEX = Pattern.compile("node/(\\d+)/(.*)");

	/** The JSON field name for an "object type". */
	public static final String OBJECT_TYPE_FIELD = "__type__";

	/** The InstructionStatus type. */
	public static final String INSTRUCTION_STATUS_TYPE = "InstructionStatus";

	/** The {@link GeneralNodeDatum} or {@link GeneralLocationDatum} type. */
	public static final String GENERAL_NODE_DATUM_TYPE = "datum";

	/**
	 * The JSON field name for a location ID on a {@link GeneralLocationDatum}
	 * value.
	 */
	public static final String LOCATION_ID_FIELD = "locationId";

	/**
	 * The JSON field name for an instruction ID on a {@link Instruction} value.
	 *
	 * @since 1.8
	 */
	public static final String INSTRUCTION_ID_FIELD = "instructionId";

	/**
	 * The user event tags for a node datum message that cannot be parsed.
	 *
	 * @since 4.1
	 */
	public static final List<String> DATUM_PARSE_ERROR_TAGS = List.of(DATUM_TAG, ERROR_TAG, NODE_TAG);

	/**
	 * User event data key for the MQTT topic of a message.
	 *
	 * @since 4.1
	 */
	public static final String TOPIC_DATA_KEY = "topic";

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final NodeInstructionDao nodeInstructionDao;
	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;
	private @Nullable Executor executor;
	private @Nullable SolarNodeOwnershipDao nodeOwnershipDao;
	private @Nullable UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        object mapper for messages
	 * @param dataCollectorBiz
	 *        data collector
	 * @param nodeInstructionDao
	 *        the node instruction DAO
	 * @param mqttStats
	 *        the stats
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public MqttDataCollector(ObjectMapper objectMapper, DataCollectorBiz dataCollectorBiz,
			NodeInstructionDao nodeInstructionDao, StatTracker mqttStats) {
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.dataCollectorBiz = requireNonNullArgument(dataCollectorBiz, "dataCollectorBiz");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		setMqttStats(requireNonNullArgument(mqttStats, "mqttStats"));
		setDisplayName("SolarIn MQTT");
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		final String datumTopics = String.format(nodeDatumTopicTemplate, "+");
		try {
			connection.subscribe(datumTopics, getSubscribeQos(), this).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {}", datumTopics, connection);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {}: {}", datumTopics, connection,
					e.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * When an {@code executor} is configured the message is handled on that
	 * executor, so the connection's I/O thread is not held for the duration of
	 * the datum persistence. The returned stage decides the MQTT
	 * acknowledgement, so a message the executor cannot accept is reported as
	 * not handled and the broker will redeliver it.
	 * </p>
	 *
	 * @since 4.1
	 */
	@Override
	public CompletionStage<?> onMqttMessageAsync(MqttMessage message) {
		final Executor e = getExecutor();
		if ( e == null ) {
			return MqttMessageHandler.super.onMqttMessageAsync(message);
		}
		try {
			return CompletableFuture.runAsync(() -> handleMessage(message), e);
		} catch ( RejectedExecutionException ex ) {
			// at capacity: do not acknowledge, so the broker redelivers the message
			getMqttStats().increment(SolarInCountStat.MessagesRejected);
			log.warn(
					"Rejected message on MQTT topic {}: no capacity to handle it; "
							+ "it will not be acknowledged, so the broker should redeliver it.",
					message.getTopic());
			return CompletableFuture.failedFuture(ex);
		}
	}

	@Override
	public void onMqttMessage(MqttMessage message) {
		handleMessage(message);
	}

	private void handleMessage(MqttMessage message) {
		final String topic = message.getTopic();
		@Nullable
		Long nodeId = null;
		try {
			Matcher m = NODE_TOPIC_REGEX.matcher(topic);
			if ( !m.matches() ) {
				log.info("Unknown topic: {}", topic);
				return;
			}
			nodeId = Long.valueOf(m.group(1));
			final String subTopic = m.group(2);
			assert "datum".equals(subTopic);

			// assume node security role
			SecurityUtils.becomeNode(nodeId);

			parseMqttMessage(objectMapper, message, topic, nodeId, true);
		} catch ( JacksonException | IOException e ) {
			// the payload cannot be parsed, so a redelivery could never succeed: discard
			// it rather than leave it unacknowledged to be redelivered indefinitely
			getMqttStats().increment(SolarInCountStat.MessagesDiscarded);
			final String payload = Base64.getEncoder().encodeToString(message.getPayload());
			log.warn("Discarding unparsable MQTT topic {} Base64 message [{}]: {}", topic, payload,
					e.getMessage());
			if ( nodeId != null ) {
				recordUnparsablePayloadEvent(nodeId, topic, payload, e);
			}
		} catch ( net.solarnetwork.central.security.AuthorizationException e ) {
			log.warn("Authorization exception on MQTT topic [{}]: {}", topic, e.getMessage());
		} catch ( RuntimeException e ) {
			log.error("Error handling MQTT message on topic {}", topic, e);
			throw new RepeatableTaskException(
					"Error handling MQTT message on topic " + topic + ": " + e.getMessage(), e);
		} finally {
			// the security context is thread bound, so it must not outlive this message
			// on a pooled thread
			SecurityContextHolder.clearContext();
		}
	}

	/**
	 * Record a user event for a message payload that cannot be parsed.
	 *
	 * <p>
	 * The message has already been discarded, so any failure here is logged and
	 * otherwise ignored: it must not change how the message is acknowledged.
	 * </p>
	 */
	private void recordUnparsablePayloadEvent(Long nodeId, String topic, String base64Payload,
			Exception cause) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		final SolarNodeOwnershipDao ownershipDao = getNodeOwnershipDao();
		if ( biz == null || ownershipDao == null ) {
			return;
		}
		try {
			final SolarNodeOwnership owner = ownershipDao.ownershipForNodeId(nodeId);
			if ( owner == null ) {
				log.debug("No owner found for node {}; not recording unparsable message event.", nodeId);
				return;
			}
			final var data = new LinkedHashMap<String, Object>(4);
			data.put(NODE_ID_DATA_KEY, nodeId);
			data.put(TOPIC_DATA_KEY, topic);
			data.put(CONTENT_DATA_KEY, base64Payload);
			final String errMsg = cause.getMessage();
			if ( errMsg != null ) {
				data.put(MESSAGE_DATA_KEY, errMsg);
			}
			biz.addEvent(owner.getUserId(), event(DATUM_PARSE_ERROR_TAGS,
					"Discarded datum message that could not be parsed.", getJSONString(data, null)));
		} catch ( RuntimeException e ) {
			log.warn("Failed to record user event for unparsable MQTT topic {} message: {}", topic,
					e.toString());
		}
	}

	private void parseMqttMessage(ObjectMapper objectMapper, MqttMessage message, final String topic,
			final Long nodeId, final boolean checkVersion) throws IOException {
		JsonNode root = objectMapper.readTree(message.getPayload());
		if ( root.isObject() || root.isArray() ) {
			int remainingTries = getTransientErrorTries();
			while ( remainingTries > 0 ) {
				try {
					if ( root.isObject() ) {
						handleNode(nodeId, root, checkVersion);
					} else {
						// V2 stream datum array
						handleStreamDatumNode(root);
					}
					break;
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
	}

	private void handleNode(final Long nodeId, final JsonNode node, final boolean checkVersion) {
		String nodeType = getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
		JsonNode instrId = node.get(INSTRUCTION_ID_FIELD);
		if ( (instrId != null && instrId.isNumber())
				|| INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
			handleInstructionStatus(nodeId, node);
		} else {
			handleGeneralDatum(nodeId, node, checkVersion);
		}
	}

	private void handleInstructionStatus(final Long nodeId, final JsonNode node) {
		getMqttStats().increment(SolarInCountStat.InstructionStatusReceived);
		String instructionId = getStringFieldValue(node, "instructionId", null);
		String instructionState = getStringFieldValue(node, "state", null);
		if ( instructionState == null ) {
			// fall back to legacy form
			instructionState = getStringFieldValue(node, "status", null);
		}
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		if ( instructionId != null && nodeId != null && instructionState != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState state;
			try {
				state = InstructionState.valueOf(instructionState);
			} catch ( Exception e ) {
				log.warn("Ignoring instruction datum {} invalid instruction state value [{}]: {}",
						instructionId, instructionState, e.toString());
				return;
			}
			nodeInstructionDao.updateNodeInstructionState(id, nodeId, state, resultParams);
		}
	}

	private void handleStreamDatumNode(final JsonNode node) {
		try {
			StreamDatum d = objectMapper.treeToValue(node, StreamDatum.class);
			dataCollectorBiz.postStreamDatum(singleton(d));
			getMqttStats().increment(SolarInCountStat.StreamDatumReceived);
		} catch ( JacksonException e ) {
			log.debug("Unable to parse StreamDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralDatum(final Long nodeId, final JsonNode node, final boolean checkVersion) {
		try {
			final Datum d = objectMapper.treeToValue(node, Datum.class);
			final GeneralDatum gd = (d instanceof GeneralDatum g ? g
					: new GeneralDatum(DatumId.datumId(d.getKind(), d.getObjectId(), d.getSourceId(),
							d.getTimestamp()), new DatumSamples(d.asSampleOperations())));

			if ( checkVersion && !gd.asSampleOperations().hasTag(TAG_V2) ) {
				// work-around for all BigDecimal encodings being backwards
				for ( DatumSamplesType type : new DatumSamplesType[] { DatumSamplesType.Instantaneous,
						DatumSamplesType.Accumulating, DatumSamplesType.Status } ) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Map<String, Object> m = (Map) gd.getSampleData(type);
					if ( m == null ) {
						continue;
					}
					for ( Entry<String, Object> e : m.entrySet() ) {
						Object v = e.getValue();
						if ( v instanceof BigDecimal n ) {
							if ( n.scale() != 0 ) {
								BigDecimal swapped = new BigDecimal(n.unscaledValue(), -n.scale());
								e.setValue(swapped);
							}
						}
					}
				}
			}
			gd.asMutableSampleOperations().removeTag(TAG_V2);
			if ( gd.getTags() != null && gd.getTags().isEmpty() ) {
				gd.setTags(null);
			}

			Object ld = convertGeneralDatum(nodeId, gd);
			if ( d.getSourceId() == null ) {
				// ignore, source ID is required
				log.warn("Ignoring datum for node {} with missing source ID: {}", nodeId, node);
			} else {
				if ( ld instanceof GeneralLocationDatum g ) {
					dataCollectorBiz.postGeneralLocationDatum(singleton(g));
				} else if ( ld instanceof GeneralNodeDatum g ) {
					dataCollectorBiz.postGeneralNodeDatum(singleton(g));
				}
			}
			getMqttStats().increment(d.getKind() == ObjectDatumKind.Location
					? checkVersion ? SolarInCountStat.LocationDatumReceived
							: SolarInCountStat.LegacyLocationDatumReceived
					: checkVersion ? SolarInCountStat.NodeDatumReceived
							: SolarInCountStat.LegacyNodeDatumReceived);
		} catch ( JacksonException e ) {
			log.debug("Unable to parse GeneralDatum: {}", e.getMessage());
		}
	}

	private Object convertGeneralDatum(Long nodeId, Datum gd) {
		DatumSamples s = new DatumSamples(gd.asSampleOperations());
		if ( gd.getKind() == ObjectDatumKind.Location ) {
			GeneralLocationDatum gld = new GeneralLocationDatum(gd.getObjectId(), gd.getTimestamp(),
					gd.getSourceId());
			gld.setSamples(s);
			return gld;
		}
		GeneralNodeDatum gnd = new GeneralNodeDatum(nodeId, gd.getTimestamp(), gd.getSourceId());
		gnd.setSamples(s);
		return gnd;
	}

	private String getStringFieldValue(JsonNode node, String fieldName, String placeholder) {
		JsonNode child = node.get(fieldName);
		return (child == null ? placeholder : child.asString());
	}

	/*---------------------
	 * Accessors
	 *------------------ */

	/**
	 * Set the node datum topic template.
	 *
	 * <p>
	 * This topic template will be used to subscribe to node datum topics, using
	 * a {@literal +} wildcard parameter.
	 * </p>
	 *
	 * @param nodeDatumTopicTemplate
	 *        the template to use; defaults to
	 *        {@link #DEFAULT_NODE_DATUM_TOPIC_TEMPLATE}
	 */
	public void setNodeDatumTopicTemplate(String nodeDatumTopicTemplate) {
		this.nodeDatumTopicTemplate = nodeDatumTopicTemplate;
	}

	/**
	 * Get the executor to handle messages on.
	 *
	 * @return the executor, or {@code null} to handle messages on the calling
	 *         thread
	 * @since 4.1
	 */
	public final @Nullable Executor getExecutor() {
		return executor;
	}

	/**
	 * Set the executor to handle messages on.
	 *
	 * <p>
	 * Configuring an executor takes message handling off the MQTT connection's
	 * I/O thread. It should be bounded, and reject work when full, so that
	 * messages beyond its capacity go unacknowledged and are redelivered rather
	 * than accumulating in memory. Messages may then be handled concurrently
	 * and out of order.
	 * </p>
	 *
	 * @param executor
	 *        the executor to set, or {@code null} to handle messages on the
	 *        calling thread
	 * @since 4.1
	 */
	public final void setExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * Get the node ownership DAO.
	 *
	 * @return the DAO, or {@code null}
	 * @since 4.1
	 */
	public final @Nullable SolarNodeOwnershipDao getNodeOwnershipDao() {
		return nodeOwnershipDao;
	}

	/**
	 * Set the node ownership DAO.
	 *
	 * <p>
	 * This is used to resolve the owner of a node, to record user events
	 * against. It is on the message handling path, so it should be cached.
	 * </p>
	 *
	 * @param nodeOwnershipDao
	 *        the DAO to set
	 * @since 4.1
	 */
	public final void setNodeOwnershipDao(@Nullable SolarNodeOwnershipDao nodeOwnershipDao) {
		this.nodeOwnershipDao = nodeOwnershipDao;
	}

	/**
	 * Get the user event appender service.
	 *
	 * @return the service, or {@code null}
	 * @since 4.1
	 */
	public final @Nullable UserEventAppenderBiz getUserEventAppenderBiz() {
		return userEventAppenderBiz;
	}

	/**
	 * Set the user event appender service.
	 *
	 * <p>
	 * When configured along with a {@code nodeOwnershipDao}, a user event
	 * tagged with {@link #DATUM_PARSE_ERROR_TAGS} is recorded for the node
	 * owner whenever a message payload cannot be parsed.
	 * </p>
	 *
	 * @param userEventAppenderBiz
	 *        the service to set
	 * @since 4.1
	 */
	public final void setUserEventAppenderBiz(@Nullable UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

}
