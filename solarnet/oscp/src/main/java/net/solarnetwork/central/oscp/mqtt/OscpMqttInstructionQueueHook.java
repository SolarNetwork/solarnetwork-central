/* ==================================================================
 * OscpMqttInstructionQueueHook.java - 6/10/2022 4:53:22 pm
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

package net.solarnetwork.central.oscp.mqtt;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_ACTION_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_OPTIMIZER_ID_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_MESSAGE_PARAM;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queuing;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.util.OscpInstructionUtils;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.service.RemoteServiceException;
import oscp.v20.AdjustGroupCapacityForecast;

/**
 * Node instruction queue hook to publish OSCP v2.0 instructions to a MQTT
 * topic.
 * 
 * <p>
 * This hook will intercept OSCP 2.0 instructions and post them to a MQTT topic
 * so the OSCP Flexibility Provider system can subscribe and execute the
 * message.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class OscpMqttInstructionQueueHook extends BaseMqttConnectionObserver
		implements NodeInstructionQueueHook, OscpUserEvents, OscpMqttInstructions {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserNodeDao userNodeDao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final ObjectMapper objectMapper;
	private JsonSchemaFactory jsonSchemaFactory;
	private UserEventAppenderBiz userEventAppenderBiz;
	private String mqttTopic = MQTT_TOPIC_V20;

	/**
	 * Constructor.
	 * 
	 * @param stats
	 *        the stats to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param capacityGroupDao
	 *        the capacity group DAO to use
	 * @param capacityOptimizerDao
	 *        the capacity optimizer DAO to use
	 * @param capacityProviderDao
	 *        the capacity provider DAO to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OscpMqttInstructionQueueHook(MqttStats stats, ObjectMapper objectMapper,
			UserNodeDao userNodeDao, CapacityGroupConfigurationDao capacityGroupDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityProviderConfigurationDao capacityProviderDao) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		setDisplayName("OSCP Instruction Publisher");
		setMqttStats(requireNonNullArgument(stats, "stats"));
	}

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		final String topic = instruction.getTopic();
		final Long nodeId = instruction.getNodeId();
		log.trace("Inspecting {} instruction for node {}", topic, nodeId);
		if ( !OscpInstructionUtils.OSCP_V20_TOPIC.equals(topic) ) {
			return instruction;
		}
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			log.trace("UserNode not found for node {}; ignoring OSCPv20 instruction {}", nodeId, topic);
			instruction.setState(Declined);
			return instruction;
		}
		final Map<String, String> params = instruction.getParams();
		final Map<String, Object> eventData = new HashMap<>(4);
		eventData.put(INSTRUCTION_ID_DATA_KEY, instruction.getId());

		final String action = params.get(OSCP_ACTION_PARAM);
		if ( action == null || action.isBlank() ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS, "Missing OSCP action",
					null);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Missing OSCP action"));
			return instruction;
		}
		eventData.put(ACTION_DATA_KEY, action);

		final String coIdString = params.get(OSCP_CAPACITY_OPTIMIZER_ID_PARAM);
		if ( coIdString == null ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Missing capacity optimizer ID", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Missing capacity optimizer ID"));
			incrementInstructionErrorStat(action);
			return instruction;
		}
		eventData.put(CAPACITY_OPTIMIZER_ID_DATA_KEY, coIdString);

		final Long coId;
		try {
			coId = Long.valueOf(coIdString);
		} catch ( NumberFormatException e ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Invalid capacity optimizer ID", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Invalid capacity optimizer ID"));
			incrementInstructionErrorStat(action);
			return instruction;
		}
		eventData.put(CAPACITY_OPTIMIZER_ID_DATA_KEY, coId); // replace string with long

		final String cgIdent = params.get(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM);
		if ( cgIdent == null || cgIdent.isBlank() ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Missing group identifier", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Missing group identifier"));
			incrementInstructionErrorStat(action);
			return instruction;
		}
		eventData.put(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, cgIdent);

		final CapacityOptimizerConfiguration co = capacityOptimizerDao
				.get(new UserLongCompositePK(userNode.getUserId(), coId));
		if ( co == null || !co.isEnabled() ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Unknown capacity optimizer", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Unknown capacity optimizer"));
			incrementInstructionErrorStat(action);
			return instruction;
		}

		final CapacityGroupConfiguration cg = capacityGroupDao
				.findForCapacityOptimizer(userNode.getUserId(), coId, cgIdent);
		if ( cg == null || !cg.isEnabled() ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Unknown group identifier", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Unknown group identifier"));
			incrementInstructionErrorStat(action);
			return instruction;
		}

		final CapacityProviderConfiguration cp = capacityProviderDao
				.get(new UserLongCompositePK(userNode.getUserId(), cg.getCapacityProviderId()));
		if ( cp == null || !cp.isEnabled() ) {
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Unknown capacity provider", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", "Unknown capacity provider"));
			incrementInstructionErrorStat(action);
			return instruction;
		}

		try {
			Object msg = OscpInstructionUtils.decodeJsonOscp20InstructionMessage(params,
					jsonSchemaFactory);

			// for AdjustGroupCapacityForecast make sure group fixed to instruction group
			if ( msg instanceof AdjustGroupCapacityForecast m ) {
				if ( !cg.getIdentifier().equals(m.getGroupId()) ) {
					throw new IllegalArgumentException(
							"The AdjustGroupCapacityForecast message group_identifier must match the instruction %s parameter"
									.formatted(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM));
				}
			}

			eventData.put(CONTENT_DATA_KEY, objectMapper.convertValue(msg, Map.class));
			return new OscpNodeInstruction(instruction, Queuing, userNode.getUserId(), cg, cp, eventData,
					action, msg);
		} catch ( IllegalArgumentException e ) {
			// invalid OSCP message
			incrementInstructionErrorStat(action);
			eventData.put(MESSAGE_DATA_KEY, e.getMessage());
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS, "Invalid OSCP message",
					eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", e.getMessage()));
		} catch ( Exception e ) {
			log.error("Error queuing OSCP {} instruction with data {}: {}", action, eventData,
					e.toString(), e);
			incrementInstructionErrorStat(action);
			eventData.put(MESSAGE_DATA_KEY, e.getMessage());
			generateUserEvent(userNode.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
					"Error handling OSCP message", eventData);
			instruction.setState(Declined);
			instruction.setResultParameters(singletonMap("error", e.getMessage()));
		}
		return instruction;
	}

	private void incrementInstructionQueuedStat(String action) {
		getMqttStats().incrementAndGet(OscpMqttCountStat.InstructionsQueued);
		OscpMqttCountStat actionStat = OscpMqttCountStat.instructionReceivedStat(action);
		if ( actionStat != null ) {
			getMqttStats().incrementAndGet(actionStat);
		}
	}

	private void incrementInstructionErrorStat(String action) {
		getMqttStats().incrementAndGet(OscpMqttCountStat.InstructionErrors);
		OscpMqttCountStat actionStat = OscpMqttCountStat.instructionErrorStat(action);
		if ( actionStat != null ) {
			getMqttStats().incrementAndGet(actionStat);
		}
	}

	public void publishOscpInstructionMessage(Long instructionId, Long nodeId, Long userId,
			CapacityGroupConfiguration group, CapacityProviderConfiguration provider,
			Map<String, Object> eventData, String action, Object msg) {
		log.info("Queueing OSCP instruction {} to MQTT topic {} for Capacity Provider {}", action,
				mqttTopic, provider.getId().ident());
		eventData.put(INSTRUCTION_ID_DATA_KEY, instructionId);
		MqttConnection conn = mqttConnection.get();
		if ( conn != null && conn.isEstablished() ) {
			try {
				Map<String, Object> body = new HashMap<>(8);
				body.put(INSTRUCTION_ID_PARAM, instructionId);
				body.put(NODE_ID_PARAM, nodeId);
				body.put(USER_ID_PARAM, userId);
				body.put(OSCP_ACTION_PARAM, action);
				body.put(OSCP_CAPACITY_OPTIMIZER_ID_PARAM, group.getCapacityOptimizerId());
				body.put(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, group.getIdentifier());
				body.put(OSCP_MESSAGE_PARAM, msg);
				byte[] payload = objectMapper.writeValueAsBytes(body);
				BasicMqttMessage mqttMsg = new BasicMqttMessage(mqttTopic, false, MqttQos.AtLeastOnce,
						payload);
				Future<?> f = conn.publish(mqttMsg);
				f.get(getPublishTimeoutSeconds(), TimeUnit.SECONDS);
				incrementInstructionQueuedStat(action);
				generateUserEvent(userId, OSCP_INSTRUCTION_IN_TAGS, "Queued instruction", eventData);
			} catch ( IOException | TimeoutException | ExecutionException | InterruptedException e ) {
				log.warn(
						"Error queuing OSCP instruction {} action {} to MQTT topic {} for Capacity Provider {}: {}",
						instructionId, action, mqttTopic, provider.getId().ident(), e.toString());
				throw new RemoteServiceException(
						"MQTT error queuing OSCP instruction %d action %s for Capacity Provider %s: %s"
								.formatted(instructionId, action, provider.getId().ident(),
										e.getMessage()),
						e);
			}
		} else {
			log.warn(
					"MQTT connection not available to publish OSCP instruction {} action {} to MQTT topic {} for Capacity Provider {}",
					instructionId, action, mqttTopic, provider.getId().ident());
			throw new RemoteServiceException(
					"MQTT connection not available to publish OSCP instruction %d action %s for Capacity Provider %s"
							.formatted(instructionId, action, provider.getId().ident()));
		}
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( instruction instanceof OscpNodeInstruction instr ) {
			publishOscpInstructionMessage(instructionId, instruction.getNodeId(), instr.userId, instr.cg,
					instr.cp, instr.eventData, instr.action, instr.msg);
		}
	}

	private static class OscpNodeInstruction extends NodeInstruction {

		private static final long serialVersionUID = 2907749503777764999L;

		private final Long userId;
		private final CapacityGroupConfiguration cg;
		private final CapacityProviderConfiguration cp;
		private final Map<String, Object> eventData;
		private final String action;
		private Object msg;

		private OscpNodeInstruction(NodeInstruction instruction, InstructionState state, Long userId,
				CapacityGroupConfiguration cg, CapacityProviderConfiguration cp,
				Map<String, Object> eventData, String action, Object msg) {
			super(instruction);
			setState(state);
			this.userId = userId;
			this.cg = cg;
			this.cp = cp;
			this.eventData = eventData;
			this.action = action;
			this.msg = msg;
		}

	}

	private void generateUserEvent(Long userId, String[] tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String ? (String) data : JsonUtils.getJSONString(data, null));
		LogEventInfo event = new LogEventInfo(tags, message, dataStr);
		biz.addEvent(userId, event);
	}

	/**
	 * Get the JSON schema validator.
	 * 
	 * @return the jsonSchemaFactory the validator
	 */
	public JsonSchemaFactory getJsonSchemaFactory() {
		return jsonSchemaFactory;
	}

	/**
	 * SEt the JSON schema validator to use for OSCP JSON messages.
	 * 
	 * @param jsonSchemaFactory
	 *        the validator to set
	 */
	public void setJsonSchemaFactory(JsonSchemaFactory jsonSchemaFactory) {
		this.jsonSchemaFactory = jsonSchemaFactory;
	}

	/**
	 * Get the user event appender service.
	 * 
	 * @return the service
	 */
	public UserEventAppenderBiz getUserEventAppenderBiz() {
		return userEventAppenderBiz;
	}

	/**
	 * Set the user event appender service.
	 * 
	 * @param userEventAppenderBiz
	 *        the service to set
	 */
	public void setUserEventAppenderBiz(UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

	/**
	 * Get the MQTT topic to publish to.
	 * 
	 * @return the topic
	 */
	public String getMqttTopic() {
		return mqttTopic;
	}

	/**
	 * Set the MQTT topic to publish to.
	 * 
	 * @param mqttTopic
	 *        the topic; if {@literal null} or blank then
	 *        {@link #MQTT_TOPIC_V20} will be set instead
	 */
	public void setMqttTopic(String mqttTopic) {
		this.mqttTopic = (mqttTopic == null || mqttTopic.isBlank() ? MQTT_TOPIC_V20 : mqttTopic);
	}

}
