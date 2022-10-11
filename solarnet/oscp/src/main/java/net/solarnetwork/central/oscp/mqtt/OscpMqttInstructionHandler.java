/* ==================================================================
 * MqttInstructionHandler.java - 6/10/2022 4:35:04 pm
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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.instructor.domain.InstructionState.Completed;
import static net.solarnetwork.central.instructor.domain.InstructionState.Declined;
import static net.solarnetwork.central.instructor.domain.InstructionState.Executing;
import static net.solarnetwork.central.instructor.domain.InstructionState.Queuing;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_ACTION_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_OPTIMIZER_ID_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_MESSAGE_PARAM;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_ASSET_MEASUREMENTS_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_MEASUREMENTS_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.V20;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.util.OscpInstructionUtils;
import net.solarnetwork.central.oscp.util.SystemTaskContext;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.common.mqtt.MqttStats;
import oscp.v20.AdjustGroupCapacityForecast;

/**
 * MQTT subscriber for OSCP v2.0 instructions.
 * 
 * <p>
 * This connection observer will subscribe to a MQTT topic for OSCP 2.0
 * instructions and forward the OSCP message to the associated Capacity Provider
 * and then update the instruction status with the result.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OscpMqttInstructionHandler extends BaseMqttConnectionObserver
		implements MqttMessageHandler, OscpUserEvents, OscpMqttInstructions {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String mqttTopic = MQTT_TOPIC_V20;

	private final Executor taskExecutor;
	private final ObjectMapper objectMapper;
	private final NodeInstructionDao nodeInstructionDao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final ExternalSystemClient client;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 * 
	 * @param stats
	 *        the stats to use
	 * @param taskExecutor
	 *        the task executor
	 * @param objectMapper
	 *        the object mapper to use
	 * @param nodeInstructionDao
	 *        the instruction DAO to use
	 * @param capacityGroupDao
	 *        the capacity group DAO to use
	 * @param capacityOptimizerDao
	 *        the capacity optimizer DAO to use
	 * @param capacityProviderDao
	 *        the capacity provide DAO to use
	 * @param client
	 *        the client to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OscpMqttInstructionHandler(MqttStats stats, Executor taskExecutor, ObjectMapper objectMapper,
			NodeInstructionDao nodeInstructionDao, CapacityGroupConfigurationDao capacityGroupDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityProviderConfigurationDao capacityProviderDao, ExternalSystemClient client) {
		super();
		this.taskExecutor = requireNonNullArgument(taskExecutor, "taskExecutor");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.client = requireNonNullArgument(client, "client");
		setDisplayName("OSCP Instruction Subscriber");
		setMqttStats(requireNonNullArgument(stats, "stats"));
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		try {
			connection.subscribe(mqttTopic, MqttQos.AtLeastOnce, this).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to OSCP MQTT topic {} @ {}", mqttTopic, connection);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to OSCP MQTT topic {} @ {}: {}", mqttTopic, connection,
					e.toString());
		}
	}

	@Override
	public void onMqttMessage(MqttMessage message) {
		if ( !mqttTopic.equals(message.getTopic()) ) {
			return;
		}
		final Map<String, Object> eventData = new HashMap<>(4);
		String action = null;
		Long instructionId = null;
		Long nodeId = null;
		Long userId = null;
		try {
			final JsonNode json = objectMapper.readTree(message.getPayload());
			log.info("Received OSCP instruction {}", json);
			if ( !json.isObject() ) {
				log.error("OSCP instruction malformed; ignoring: {}", json);
				return;
			}
			instructionId = (json.path(INSTRUCTION_ID_PARAM).canConvertToLong()
					? json.path(INSTRUCTION_ID_PARAM).longValue()
					: null);
			nodeId = (json.path(NODE_ID_PARAM).canConvertToLong() ? json.path(NODE_ID_PARAM).longValue()
					: null);
			userId = (json.path(USER_ID_PARAM).canConvertToLong() ? json.path(USER_ID_PARAM).longValue()
					: null);
			if ( instructionId == null || nodeId == null || userId == null ) {
				log.error("OSCP instruction missing ID, user ID, or node ID; ignoring: {}", json);
				return;
			}

			if ( !nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Queuing,
					Executing, null) ) {
				// assumed claimed by another handler
				return;
			}

			eventData.put(INSTRUCTION_ID_DATA_KEY, instructionId);

			action = json.path(OSCP_ACTION_PARAM).textValue();
			if ( action == null || action.isBlank() ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Missing OSCP action", null);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Missing OSCP action"));
				return;
			}
			eventData.put(ACTION_DATA_KEY, action);

			final Long coId = (json.path(OSCP_CAPACITY_OPTIMIZER_ID_PARAM).canConvertToLong()
					? json.path(OSCP_CAPACITY_OPTIMIZER_ID_PARAM).longValue()
					: null);
			if ( coId == null ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Missing capacity optimizer ID",
						eventData);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Missing capacity optimizer ID"));
				return;
			}
			eventData.put(CAPACITY_OPTIMIZER_ID_DATA_KEY, coId);

			final String cgIdent = json.path(OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM).textValue();
			if ( cgIdent == null || cgIdent.isBlank() ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Missing group identifier",
						eventData);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Missing group identifier"));
				return;
			}
			eventData.put(CAPACITY_GROUP_IDENTIFIER_DATA_KEY, cgIdent);

			final CapacityOptimizerConfiguration co = capacityOptimizerDao
					.get(new UserLongCompositePK(userId, coId));
			if ( co == null || !co.isEnabled() ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Unknown capacity optimizer",
						eventData);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Unknown capacity optimizer"));
				return;
			}

			final CapacityGroupConfiguration cg = capacityGroupDao.findForCapacityOptimizer(userId, coId,
					cgIdent);
			if ( cg == null || !cg.isEnabled() ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Unknown group identifier",
						eventData);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Unknown group identifier"));
				return;
			}

			final CapacityProviderConfiguration cp = capacityProviderDao
					.get(new UserLongCompositePK(userId, cg.getCapacityProviderId()));
			if ( cp == null || !cp.isEnabled() ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Unknown capacity provider",
						eventData);
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", "Unknown capacity provider"));
				return;
			}

			Map<String, Object> params = new HashMap<>(2);
			params.put(OSCP_ACTION_PARAM, action);
			params.put(OSCP_MESSAGE_PARAM, json.path(OSCP_MESSAGE_PARAM));

			Object msg = OscpInstructionUtils.decodeJsonOscp20InstructionMessage(params, null); // assume message JSON already validated at this point
			eventData.put(CONTENT_DATA_KEY, objectMapper.convertValue(msg, Map.class));

			// for AdjustGroupCapacityForecast make sure group fixed to instruction group
			if ( msg instanceof AdjustGroupCapacityForecast m ) {
				m.setGroupId(cg.getIdentifier());
			}

			incrementInstructionReceivedStat(action);
			generateUserEvent(userId, OSCP_INSTRUCTION_TAGS, "Processing queued instruction", eventData);
			taskExecutor.execute(
					new SendOscpInstructionTask(instructionId, nodeId, cg, cp, eventData, action, msg));
		} catch ( IllegalArgumentException e ) {
			// invalid OSCP message
			incrementInstructionErrorStat(action);
			eventData.put(MESSAGE_DATA_KEY, e.getMessage());
			if ( userId != null ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Invalid OSCP message",
						eventData);
			}
			if ( instructionId != null && nodeId != null ) {
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined,
						singletonMap("error", "Invalid instruction: %s".formatted(e.getMessage())));
			}
		} catch ( Exception e ) {
			incrementInstructionErrorStat(action);
			log.warn("Error processing OSCP instruction MQTT message [{}]: {}", eventData, e.toString(),
					e);
			eventData.put(MESSAGE_DATA_KEY, e.getMessage());
			if ( userId != null ) {
				generateUserEvent(userId, OSCP_INSTRUCTION_ERROR_TAGS, "Error handling OSCP message",
						eventData);
			}
			if ( instructionId != null && nodeId != null ) {
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", e.getMessage()));
			}
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

	private void incrementInstructionReceivedStat(String action) {
		getMqttStats().incrementAndGet(OscpMqttCountStat.InstructionsReceived);
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

	private class SendOscpInstructionTask implements Runnable, Supplier<String> {

		private final Long instructionId;
		private final Long nodeId;
		private final CapacityGroupConfiguration group;
		private final Map<String, Object> eventData;
		private final String action;
		private final Object msg;
		private final SystemTaskContext<CapacityProviderConfiguration> context;

		private SendOscpInstructionTask(Long instructionId, Long nodeId,
				CapacityGroupConfiguration group, CapacityProviderConfiguration provider,
				Map<String, Object> eventData, String action, Object msg) {
			super();
			this.instructionId = instructionId;
			this.nodeId = nodeId;
			this.group = group;
			this.eventData = eventData;
			this.action = action;
			this.msg = msg;
			this.context = new SystemTaskContext<>(action, OscpRole.CapacityOptimizer, provider,
					OSCP_INSTRUCTION_ERROR_TAGS, OSCP_INSTRUCTION_TAGS, capacityProviderDao,
					Collections.emptyMap());
		}

		@Override
		public String get() {
			context.verifySystemOscpVersion(singleton(V20));
			return switch (action) {
				case "AdjustGroupCapacityForecast" -> ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;
				case "GroupCapacityComplianceError" -> GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH;
				case "UpdateAssetMeasurement" -> UPDATE_ASSET_MEASUREMENTS_URL_PATH;
				case "UpdateGroupMeasurements" -> UPDATE_GROUP_MEASUREMENTS_URL_PATH;
				default -> throw new IllegalArgumentException(
						"Unsupported OSCP 2.0 action [%s]".formatted(action));
			};
		}

		@Override
		public void run() {
			try {
				client.systemExchange(context, HttpMethod.POST, this, msg);
				generateUserEvent(group.getUserId(), OSCP_INSTRUCTION_OUT_TAGS, "Posted instruction",
						eventData);
				if ( !nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId,
						Executing, Completed, null) ) {
					log.warn(
							"Unsuccessful updating {} OSCP instruction {} from Executing to Completed state",
							action, instructionId);
				}
			} catch ( Exception e ) {
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId, Executing,
						Declined, singletonMap("error", e.getMessage()));
				eventData.put(MESSAGE_DATA_KEY, e.getMessage());
				generateUserEvent(group.getUserId(), OSCP_INSTRUCTION_ERROR_TAGS,
						"Error sending OSCP message to capacity provider", eventData);
			}
		}

	}

	/**
	 * Get the MQTT topic to subscribe to.
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

}
