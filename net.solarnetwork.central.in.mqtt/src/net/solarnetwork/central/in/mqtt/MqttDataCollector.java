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
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.transaction.TransactionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.common.mqtt.BaseMqttConnectionService;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;
import net.solarnetwork.common.mqtt.MqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 1.6
 */
public class MqttDataCollector extends BaseMqttConnectionService
		implements NodeInstructionQueueHook, MqttConnectionObserver, MqttMessageHandler {

	/** A datum tag that indicates v2 CBOR encoding. */
	public static final String TAG_V2 = "_v2";

	/**
	 * The default MQTT topic template for node instruction publication.
	 * 
	 * <p>
	 * This template will be passed a single node ID parameter.
	 * </p>
	 */
	public static final String DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

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

	/** The default {@code publishOnly} property value. */
	public static final boolean DEFAULT_PUBLISH_ONLY = false;

	/** The default {@code transientErrorTries} property value. */
	public static final int DEFAULT_TRANSIENT_ERROR_TRIES = 3;

	private final ObjectMapper objectMapper;
	private final ObjectMapper objectMapper2;
	private final Executor executor;
	private final DataCollectorBiz dataCollectorBiz;
	private final OptionalService<NodeInstructionDao> nodeInstructionDaoRef;
	private final OptionalServiceCollection<MqttConnectionObserver> connectionObservers;

	private String nodeInstructionTopicTemplate = DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE;
	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;
	private boolean publishOnly = DEFAULT_PUBLISH_ONLY;
	private int transientErrorTries = DEFAULT_TRANSIENT_ERROR_TRIES;

	/**
	 * Constructor.
	 * 
	 * @param connectionFactory
	 *        the factory to use for {@link MqttConnection} instances
	 * @param objectMapper
	 *        object mapper for messages
	 * @param objectMapper2
	 *        object mapper for messages (legacy format)
	 * @param executor
	 *        an executor
	 * @param dataCollectorBiz
	 *        data collector
	 * @param nodeInstructionDao
	 *        node instruction
	 * @param connectionObservers
	 *        optional connection observers
	 */
	public MqttDataCollector(MqttConnectionFactory connectionFactory, ObjectMapper objectMapper,
			ObjectMapper objectMapper2, Executor executor, DataCollectorBiz dataCollectorBiz,
			OptionalService<NodeInstructionDao> nodeInstructionDao,
			OptionalServiceCollection<MqttConnectionObserver> connectionObservers) {
		super(connectionFactory, new MqttStats("MqttDataCollector", 500, SolarInCountStat.values()));
		assert objectMapper != null && executor != null && dataCollectorBiz != null;
		this.objectMapper = objectMapper;
		this.objectMapper2 = objectMapper2 != null ? objectMapper2 : objectMapper;
		this.executor = executor;
		this.dataCollectorBiz = dataCollectorBiz;
		this.nodeInstructionDaoRef = nodeInstructionDao;
		this.connectionObservers = connectionObservers;
	}

	// TODO: need way to dynamically add observers after connection already established;
	//       keep track of connections we've seen and if a new one is added then call
	//       onMqttServerConnectionEstablished() methods on it; similarly do the same
	//       when observer is de-registered

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers.services() ) {
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
		if ( !publishOnly ) {
			final String datumTopics = String.format(nodeDatumTopicTemplate, "+");
			try {
				connection.subscribe(datumTopics, getSubscribeQos(), null)
						.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
				log.info("Subscribed to MQTT topic {} @ {}", datumTopics,
						getMqttConfig().getServerUri());
			} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
				log.error("Failed to subscribe to MQTT topic {} @ {}: {}", datumTopics,
						getMqttConfig().getServerUri(), e.toString());
			}
		}
		if ( connectionObservers != null ) {
			for ( MqttConnectionObserver o : connectionObservers.services() ) {
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

	private static class UseLegacyObjectMapperException extends RuntimeException {

		private static final long serialVersionUID = -6888029007548132227L;

		private UseLegacyObjectMapperException() {
			super();
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

			try {
				parseMqttMessage(objectMapper, message, topic, nodeId, true);
			} catch ( UseLegacyObjectMapperException e ) {
				parseMqttMessage(objectMapper2, message, topic, nodeId, false);
			}
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
		if ( root.isObject() ) {
			int remainingTries = transientErrorTries;
			while ( remainingTries > 0 ) {
				try {
					handleNode(nodeId, root, checkVersion);
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

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		if ( instruction != null && instruction.getNodeId() != null
				&& InstructionState.Queued == instruction.getState() ) {
			// we will change this state to Queuing so batch processing does not pick up
			instruction.setState(InstructionState.Queuing);
		}
		return instruction;
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( instruction != null && instruction.getNodeId() != null && instructionId != null
				&& InstructionState.Queuing == instruction.getState() ) {
			try {
				executor.execute(new PublishNodeInstructionTask(instruction, instructionId));
			} catch ( JsonProcessingException e ) {
				log.error("Error encoding node instruction {} for MQTT payload: {}", instruction.getId(),
						e.getMessage());
			}
		}
	}

	private class PublishNodeInstructionTask implements Runnable {

		private final Long instructionId;
		private final Long nodeId;
		private final String topic;
		private final byte[] payload;

		private PublishNodeInstructionTask(NodeInstruction instruction, Long instructionId)
				throws JsonProcessingException {
			super();
			// create copy with ID set
			this.instructionId = instructionId;
			this.nodeId = instruction.getNodeId();
			this.topic = String.format(nodeInstructionTopicTemplate, instruction.getNodeId());
			Map<String, Object> data = Collections.singletonMap("instructions",
					Collections.singleton(instruction));
			this.payload = objectMapper.writeValueAsBytes(data);
		}

		@Override
		public void run() {
			try {
				MqttConnection conn = connection();
				if ( conn != null ) {
					Future<?> f = conn
							.publish(new BasicMqttMessage(topic, false, getPublishQos(), payload));
					f.get(getMqttConfig().getConnectTimeoutSeconds(), TimeUnit.SECONDS);
					getMqttStats().incrementAndGet(SolarInCountStat.InstructionsPublished);
				} else {
					throw new RuntimeException("MQTT connection not available");
				}
			} catch ( Exception e ) {
				// error delivering instruction so change state to Queued to fall back to batch processing
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}

				NodeInstructionDao dao = (nodeInstructionDaoRef != null ? nodeInstructionDaoRef.service()
						: null);
				if ( dao != null ) {
					log.info(
							"Failed to publish MQTT instruction {} to node {}, falling back to batch mode: {}",
							instructionId, nodeId, root.toString());
					dao.compareAndUpdateInstructionState(instructionId, nodeId, InstructionState.Queuing,
							InstructionState.Queued, null);
				} else {
					log.error("Failed to publish MQTT instruction {} to node {}", instructionId, nodeId,
							root);
				}
			}
		}

	}

	private void handleNode(final Long nodeId, final JsonNode node, final boolean checkVersion) {
		String nodeType = getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
		if ( GENERAL_NODE_DATUM_TYPE.equalsIgnoreCase(nodeType) ) {
			// if we have a location ID, this is actually a GeneralLocationDatum
			final JsonNode locId = node.get(LOCATION_ID_FIELD);
			if ( locId != null && locId.isNumber() ) {
				handleGeneralLocationDatum(node, checkVersion);
			} else {
				handleGeneralNodeDatum(nodeId, node, checkVersion);
			}
		} else if ( INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
			handleInstructionStatus(nodeId, node);
		}
	}

	private void handleInstructionStatus(final Long nodeId, final JsonNode node) {
		getMqttStats().incrementAndGet(SolarInCountStat.InstructionStatusReceived);
		String instructionId = getStringFieldValue(node, "instructionId", null);
		String status = getStringFieldValue(node, "status", null);
		Map<String, Object> resultParams = JsonUtils.getStringMapFromTree(node.get("resultParameters"));
		NodeInstructionDao dao = (nodeInstructionDaoRef != null ? nodeInstructionDaoRef.service()
				: null);
		if ( instructionId != null && nodeId != null && status != null && dao != null ) {
			Long id = Long.valueOf(instructionId);
			InstructionState state = InstructionState.valueOf(status);
			dao.updateNodeInstructionState(id, nodeId, state, resultParams);
		}
	}

	private void handleGeneralNodeDatum(final Long nodeId, final JsonNode node,
			final boolean checkVersion) {
		try {
			GeneralNodeDatum d = objectMapper.treeToValue(node, GeneralNodeDatum.class);
			d.setNodeId(nodeId);
			if ( d.getSourceId() == null ) {
				// ignore, source ID is required
				log.warn("Ignoring datum for node {} with missing source ID: {}", nodeId, node);
			} else {
				if ( d.getSamples() != null ) {
					if ( checkVersion && !d.getSamples().hasTag(TAG_V2) ) {
						throw new UseLegacyObjectMapperException();
					}
					d.getSamples().removeTag(TAG_V2);
					if ( d.getSamples().getTags() != null && d.getSamples().getTags().isEmpty() ) {
						d.getSamples().setTags(null);
					}
				}
				dataCollectorBiz.postGeneralNodeDatum(singleton(d));
			}
			getMqttStats().incrementAndGet(checkVersion ? SolarInCountStat.NodeDatumReceived
					: SolarInCountStat.LegacyNodeDatumReceived);
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralNodeDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralLocationDatum(final JsonNode node, final boolean checkVersion) {
		try {
			GeneralLocationDatum d = objectMapper.treeToValue(node, GeneralLocationDatum.class);
			if ( d.getLocationId() == null ) {
				// ignore, both location ID is required
				log.warn("Ignoring location datum with missing location ID: {}", node);
			} else if ( d.getSourceId() == null ) {
				// ignore, source ID is required
				log.warn("Ignoring location {} datum with missing source ID: {}", d.getLocationId(),
						node);
			} else {
				if ( d.getSamples() != null ) {
					if ( checkVersion && !d.getSamples().hasTag(TAG_V2) ) {
						throw new UseLegacyObjectMapperException();
					}
					d.getSamples().removeTag(TAG_V2);
					if ( d.getSamples().getTags() != null && d.getSamples().getTags().isEmpty() ) {
						d.getSamples().setTags(null);
					}
				}
				dataCollectorBiz.postGeneralLocationDatum(singleton(d));
			}
			getMqttStats().incrementAndGet(checkVersion ? SolarInCountStat.LocationDatumReceived
					: SolarInCountStat.LegacyLocationDatumReceived);
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralLocationDatum: {}", e.getMessage());
		}
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
	 * Set the node instruction topic template.
	 * 
	 * <p>
	 * This topic template is used when publishing instructions to nodes. The
	 * template will be passed a single node ID parameter.
	 * </p>
	 * 
	 * @param nodeInstructionTopicTemplate
	 *        the template to use; defaults to
	 *        {@link #DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE}
	 */
	public void setNodeInstructionTopicTemplate(String nodeInstructionTopicTemplate) {
		this.nodeInstructionTopicTemplate = nodeInstructionTopicTemplate;
	}

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
	 * Get the "publish only" flag.
	 * 
	 * @return {@literal true} if no MQTT subscriptions should be made
	 */
	public boolean isPublishOnly() {
		return publishOnly;
	}

	/**
	 * Set the "publish only" flag.
	 * 
	 * @param publishOnly
	 *        {@literal true} to skip subscribing to any MQTT topics, so that
	 *        only publishing of instructions is handled
	 */
	public void setPublishOnly(boolean publishOnly) {
		this.publishOnly = publishOnly;
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
