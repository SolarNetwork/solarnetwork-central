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
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
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
import net.solarnetwork.common.mqtt.support.AsyncMqttServiceSupport;
import net.solarnetwork.common.mqtt.support.MqttStats;
import net.solarnetwork.support.SSLService;
import net.solarnetwork.util.OptionalService;

/**
 * MQTT implementation of upload service.
 * 
 * @author matt
 * @version 1.1
 */
public class MqttDataCollector extends AsyncMqttServiceSupport implements NodeInstructionQueueHook {

	/** The default value for the {@code persistencePath} property. */
	public static final String DEFAULT_PERSISTENCE_PATH = "var/mqtt-solarin";

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

	private final ObjectMapper objectMapper;
	private final DataCollectorBiz dataCollectorBiz;
	private final OptionalService<NodeInstructionDao> nodeInstructionDaoRef;

	private String nodeInstructionTopicTemplate = DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE;
	private String nodeDatumTopicTemplate = DEFAULT_NODE_DATUM_TOPIC_TEMPLATE;

	/**
	 * Constructor.
	 * 
	 * @param executorService
	 *        task service
	 * @param objectMapper
	 *        object mapper for messages
	 * @param dataCollectorBiz
	 *        data collector
	 * @param nodeInstructionDao
	 *        node instruction
	 * @param sslService
	 *        SSL service
	 * @param retryConnect
	 *        {@literal true} to keep retrying to connect to MQTT server
	 */
	public MqttDataCollector(ExecutorService executorService, ObjectMapper objectMapper,
			DataCollectorBiz dataCollectorBiz, OptionalService<NodeInstructionDao> nodeInstructionDao,
			OptionalService<SSLService> sslService, boolean retryConnect) {
		this(executorService, objectMapper, dataCollectorBiz, nodeInstructionDao, sslService, null, null,
				retryConnect);
	}

	/**
	 * Constructor.
	 * 
	 * @param executorService
	 *        task service
	 * @param objectMapper
	 *        object mapper for messages
	 * @param dataCollectorBiz
	 *        data collector
	 * @param nodeInstructionDao
	 *        node instruction
	 * @param sslService
	 *        SSL service
	 * @param serverUri
	 *        MQTT URI to connect to
	 * @param clientId
	 *        the MQTT client ID
	 * @param retryConnect
	 *        {@literal true} to keep retrying to connect to MQTT server
	 */
	public MqttDataCollector(ExecutorService executorService, ObjectMapper objectMapper,
			DataCollectorBiz dataCollectorBiz, OptionalService<NodeInstructionDao> nodeInstructionDao,
			OptionalService<SSLService> sslService, String serverUri, String clientId,
			boolean retryConnect) {
		super(executorService, sslService, retryConnect,
				new MqttStats(serverUri, 500, SolarInCountStat.values()), serverUri, clientId);
		assert executorService != null && objectMapper != null && dataCollectorBiz != null;
		this.objectMapper = objectMapper;
		this.dataCollectorBiz = dataCollectorBiz;
		this.nodeInstructionDaoRef = nodeInstructionDao;
		setPersistencePath(DEFAULT_PERSISTENCE_PATH);
	}

	@Override
	protected void subscribeToTopics(IMqttAsyncClient client) throws MqttException {
		final String datumTopics = String.format(nodeDatumTopicTemplate, "+");
		IMqttToken token = client.subscribe(datumTopics, getSubscribeQos());
		token.waitForCompletion(getMqttTimeout());
		log.info("Subscribed to MQTT topic {} @ {}", datumTopics, client.getServerURI());
	}

	@Override
	protected Throwable exceptionToForceConnectionReestablishment(Throwable cause) {
		Throwable root = cause;
		while ( root.getCause() != null ) {
			if ( root instanceof RepeatableTaskException ) {
				// if we encounter a RTE, forcibly shut down the client and re-establish
				return root;
			}
			root = root.getCause();
		}
		return null;
	}

	@Override
	protected void messageArrivedInternal(String topic, MqttMessage message) throws Exception {
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

			JsonNode root = objectMapper.readTree(message.getPayload());
			if ( root.isObject() ) {
				handleNode(nodeId, root);
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

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		if ( instruction != null && instruction.getNodeId() != null
				&& InstructionState.Queued == instruction.getState() ) {
			// we will change this state to Queing so batch processing does not pick up
			instruction.setState(InstructionState.Queuing);
		}
		return instruction;
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( instruction != null && instruction.getNodeId() != null && instructionId != null
				&& InstructionState.Queuing == instruction.getState() ) {
			try {
				getExecutorService().execute(new PublishNodeInstructionTask(instruction, instructionId));
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
				IMqttAsyncClient client = client();
				if ( client != null ) {
					IMqttDeliveryToken token = client.publish(topic, payload, getPublishQos(), false);
					token.waitForCompletion(getMqttTimeout());
					getStats().incrementAndGet(SolarInCountStat.InstructionsPublished);
				} else {
					throw new RuntimeException("MQTT client not available");
				}
			} catch ( Exception e ) {
				// error delivering instruction so change state to Queued to fall back to batch processing
				NodeInstructionDao dao = (nodeInstructionDaoRef != null ? nodeInstructionDaoRef.service()
						: null);
				if ( dao != null ) {
					log.info(
							"Failed to publish MQTT instruction {} to node {}, falling back to batch mode: {}",
							instructionId, nodeId, e.toString());
					dao.compareAndUpdateInstructionState(instructionId, nodeId, InstructionState.Queuing,
							InstructionState.Queued, null);
				} else {
					log.error("Failed to publish MQTT instruction {} to node {}", instructionId, nodeId,
							e);
				}
			}
		}

	}

	private void handleNode(final Long nodeId, final JsonNode node) {
		String nodeType = getStringFieldValue(node, OBJECT_TYPE_FIELD, GENERAL_NODE_DATUM_TYPE);
		if ( GENERAL_NODE_DATUM_TYPE.equalsIgnoreCase(nodeType) ) {
			// if we have a location ID, this is actually a GeneralLocationDatum
			final JsonNode locId = node.get(LOCATION_ID_FIELD);
			if ( locId != null && locId.isNumber() ) {
				handleGeneralLocationDatum(node);
			} else {
				handleGeneralNodeDatum(nodeId, node);
			}
		} else if ( INSTRUCTION_STATUS_TYPE.equalsIgnoreCase(nodeType) ) {
			handleInstructionStatus(nodeId, node);
		}
	}

	private void handleInstructionStatus(final Long nodeId, final JsonNode node) {
		getStats().incrementAndGet(SolarInCountStat.InstructionStatusReceived);
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

	private void handleGeneralNodeDatum(final Long nodeId, final JsonNode node) {
		getStats().incrementAndGet(SolarInCountStat.NodeDatumReceived);
		try {
			GeneralNodeDatum d = objectMapper.treeToValue(node, GeneralNodeDatum.class);
			d.setNodeId(nodeId);
			dataCollectorBiz.postGeneralNodeDatum(singleton(d));
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralNodeDatum: {}", e.getMessage());
		}
	}

	private void handleGeneralLocationDatum(final JsonNode node) {
		getStats().incrementAndGet(SolarInCountStat.LocationDatumReceived);
		try {
			GeneralLocationDatum d = objectMapper.treeToValue(node, GeneralLocationDatum.class);
			dataCollectorBiz.postGeneralLocationDatum(singleton(d));
		} catch ( IOException e ) {
			log.debug("Unable to parse GeneralLocationDatum: {}", e.getMessage());
		}
	}

	private String getStringFieldValue(JsonNode node, String fieldName, String placeholder) {
		JsonNode child = node.get(fieldName);
		return (child == null ? placeholder : child.asText());
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
	 * either a {@literal +} wildcard parameter.
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
