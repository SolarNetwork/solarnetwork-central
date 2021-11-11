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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.transaction.TransactionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 2.0
 */
public class MqttDataCollector extends BaseMqttConnectionService
		implements MqttConnectionObserver, MqttMessageHandler, ServiceLifecycleObserver {

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

	/** The default {@code transientErrorTries} property value. */
	public static final int DEFAULT_TRANSIENT_ERROR_TRIES = 3;

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final NodeInstructionDao nodeInstructionDao;
	private final List<MqttConnectionObserver> connectionObservers;

	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;
	private int transientErrorTries = DEFAULT_TRANSIENT_ERROR_TRIES;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the factory to use for {@link MqttConnection} instances
	 * @param objectMapper
	 *        object mapper for messages
	 * @param dataCollectorBiz
	 *        data collector
	 * @param nodeInstructionDao
	 *        the node instruction DAO
	 * @param connectionObservers
	 *        optional connection observers
	 */
	public MqttDataCollector(MqttConnectionFactory connectionFactory, ObjectMapper objectMapper,
			DataCollectorBiz dataCollectorBiz, NodeInstructionDao nodeInstructionDao,
			List<MqttConnectionObserver> connectionObservers) {
		super(connectionFactory, new MqttStats("MqttDataCollector", 500, SolarInCountStat.values()));
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.dataCollectorBiz = requireNonNullArgument(dataCollectorBiz, "dataCollectorBiz");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		this.connectionObservers = requireNonNullArgument(connectionObservers, "connectionObservers");
	}

	// TODO: need way to dynamically add observers after connection already established;
	//       keep track of connections we've seen and if a new one is added then call
	//       onMqttServerConnectionEstablished() methods on it; similarly do the same
	//       when observer is de-registered

	@Override
	public void serviceDidStartup() {
		super.init();
	}

	@Override
	public void serviceDidShutdown() {
		super.shutdown();
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers ) {
				try {
					o.onMqttServerConnectionLost(connection, willReconnect, cause);
				} catch ( Throwable t ) {
					// naughty!
					Throwable root = t;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.error("Unhandled error in MQTT connection {} lost observer {}: {}",
							getMqttConfig().getServerUri(), o, root.getMessage(), root);
				}
			}
		}
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		final String datumTopics = String.format(nodeDatumTopicTemplate, "+");
		try {
			connection.subscribe(datumTopics, getSubscribeQos(), null)
					.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {}", datumTopics, getMqttConfig().getServerUri());
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {}: {}", datumTopics,
					getMqttConfig().getServerUri(), e.toString());
		}
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers ) {
				try {
					o.onMqttServerConnectionEstablished(connection, reconnected);
				} catch ( Throwable t ) {
					// naughty!
					Throwable root = t;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					log.error("Unhandled error in MQTT connection {} established observer {}: {}",
							getMqttConfig().getServerUri(), o, root.getMessage(), root);
				}
			}
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
			int remainingTries = transientErrorTries;
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
		String status = getStringFieldValue(node, "status", null);
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		if ( instructionId != null && nodeId != null && status != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState state = InstructionState.valueOf(status);
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

	@Override
	public String getPingTestName() {
		return "SolarIn MQTT";
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
	 * Get the number of times to try storing datum in the face of transient
	 * exceptions.
	 * 
	 * @return the number of attempts to try storing datum; defaults to
	 *         {@link #DEFAULT_TRANSIENT_ERROR_TRIES}
	 * @since 1.3
	 */
	public int getTransientErrorTries() {
		return transientErrorTries;
	}

	/**
	 * Set the number of times to try storing datum in the face of transient
	 * exceptions.
	 * 
	 * <p>
	 * If a transient exception is thrown while attempting to store a datum,
	 * 
	 * @param transientErrorTries
	 *        the number of times to attempt storing datum; must be greater than
	 *        {@literal 0}
	 * @since 1.3
	 */
	public void setTransientErrorTries(int transientErrorTries) {
		if ( transientErrorTries < 1 ) {
			transientErrorTries = 1;
		}
		this.transientErrorTries = transientErrorTries;
	}

}
