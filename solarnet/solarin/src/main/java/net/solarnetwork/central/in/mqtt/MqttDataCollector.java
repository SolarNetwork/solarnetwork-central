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
import static net.solarnetwork.util.ByteUtils.encodeHexString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.transaction.TransactionException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 2.3
 */
public class MqttDataCollector extends BaseMqttConnectionObserver implements MqttMessageHandler {

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

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final NodeInstructionDao nodeInstructionDao;
	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;

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
	 *         if any argument is {@literal null}
	 */
	public MqttDataCollector(ObjectMapper objectMapper, DataCollectorBiz dataCollectorBiz,
			NodeInstructionDao nodeInstructionDao, MqttStats mqttStats) {
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

	@Override
	public void onMqttMessage(MqttMessage message) {
		final String topic = message.getTopic();
		try {
			Matcher m = NODE_TOPIC_REGEX.matcher(topic);
			if ( !m.matches() ) {
				log.info("Unknown topic: {}" + topic);
				return;
			}
			final Long nodeId = Long.valueOf(m.group(1));
			final String subTopic = m.group(2);
			assert "datum".equals(subTopic);

			// assume node security role
			SecurityUtils.becomeNode(nodeId);

			parseMqttMessage(objectMapper, message, topic, nodeId, true);
		} catch ( IOException e ) {
			log.debug("Communication error handling message on MQTT topic {}", topic, e);
			if ( e instanceof JsonParseException jpe ) {
				final byte[] payload = message.getPayload();
				log.warn("Error parsing MQTT topic {} message [{}]: {}", topic,
						encodeHexString(payload, 0, payload.length, false), e.getMessage());
			}
			throw new RepeatableTaskException("Communication error handling message on MQTT topic "
					+ topic + ": " + e.getMessage(), e);
		} catch ( RuntimeException e ) {
			log.error("Error handling MQTT message on topic {}", topic, e);
			throw new RepeatableTaskException(
					"Error handling MQTT message on topic " + topic + ": " + e.getMessage(), e);
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
						handleStreamDatumNode(nodeId, root);
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
		getMqttStats().incrementAndGet(SolarInCountStat.InstructionStatusReceived);
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

	private void handleStreamDatumNode(final Long nodeId, final JsonNode node) {
		try {
			StreamDatum d = objectMapper.treeToValue(node, StreamDatum.class);
			dataCollectorBiz.postStreamDatum(singleton(d));
			getMqttStats().incrementAndGet(SolarInCountStat.StreamDatumReceived);
		} catch ( IOException e ) {
			log.debug("Unable to parse StreamDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralDatum(final Long nodeId, final JsonNode node, final boolean checkVersion) {
		try {
			final Datum d = objectMapper.treeToValue(node, Datum.class);
			final GeneralDatum gd = (d instanceof GeneralDatum ? (GeneralDatum) d
					: new GeneralDatum(
							new DatumId(d.getKind(), d.getObjectId(), d.getSourceId(), d.getTimestamp()),
							new DatumSamples(d.asSampleOperations())));

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
						if ( v instanceof BigDecimal ) {
							BigDecimal n = (BigDecimal) v;
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
				if ( ld instanceof GeneralLocationDatum ) {
					dataCollectorBiz.postGeneralLocationDatum(singleton((GeneralLocationDatum) ld));
				} else if ( ld instanceof GeneralNodeDatum ) {
					dataCollectorBiz.postGeneralNodeDatum(singleton((GeneralNodeDatum) ld));
				}
			}
			getMqttStats().incrementAndGet(d.getKind() == ObjectDatumKind.Location
					? checkVersion ? SolarInCountStat.LocationDatumReceived
							: SolarInCountStat.LegacyLocationDatumReceived
					: checkVersion ? SolarInCountStat.NodeDatumReceived
							: SolarInCountStat.LegacyNodeDatumReceived);
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralDatum: {}", e.getMessage());
		}
	}

	private Object convertGeneralDatum(Long nodeId, Datum gd) {
		DatumSamples s = new DatumSamples(gd.asSampleOperations());
		if ( gd.getKind() == ObjectDatumKind.Location ) {
			GeneralLocationDatum gld = new GeneralLocationDatum();
			gld.setCreated(gd.getTimestamp());
			gld.setSourceId(gd.getSourceId());
			gld.setLocationId(gd.getObjectId());
			gld.setSamples(s);
			return gld;
		}
		GeneralNodeDatum gnd = new GeneralNodeDatum();
		gnd.setNodeId(nodeId);
		gnd.setCreated(gd.getTimestamp());
		gnd.setSourceId(gd.getSourceId());
		gnd.setSamples(s);
		return gnd;
	}

	private String getStringFieldValue(JsonNode node, String fieldName, String placeholder) {
		JsonNode child = node.get(fieldName);
		return (child == null ? placeholder : child.asText());
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

}
