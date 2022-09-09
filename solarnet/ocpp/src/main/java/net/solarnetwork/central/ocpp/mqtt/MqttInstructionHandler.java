/* ==================================================================
 * MqttInstructionHandler.java - 2/04/2020 4:30:39 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.mqtt;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.service.Identifiable;
import ocpp.domain.Action;

/**
 * Handle OCPP instruction messages by publishing/subscribing them to/from MQTT.
 * 
 * @author matt
 * @version 2.2
 */
public class MqttInstructionHandler<T extends Enum<T> & Action> extends BaseMqttConnectionObserver
		implements ActionMessageProcessor<JsonNode, Void>, MqttMessageHandler, CentralOcppUserEvents {

	/** The default {@code mqttTopic} property value. */
	public static final String DEFAULT_MQTT_TOPIC = "instr/OCPP_v16";

	/** The default {@code mqttTimeout} property value. */
	public static final int DEFAULT_MQTT_TIMEOUT = 30;

	/** The default {@code publishOnly} property value. */
	public static final boolean DEFAULT_PUBLISH_ONLY = true;

	private String mqttTopic = DEFAULT_MQTT_TOPIC;
	//private int mqttTimeout = DEFAULT_MQTT_TIMEOUT;
	private boolean publishOnly = DEFAULT_PUBLISH_ONLY;

	private final ObjectMapper objectMapper;
	private final NodeInstructionDao instructionDao;
	private final ChargePointRouter chargePointRouter;
	private final CentralChargePointDao chargePointDao;
	private final Class<T> actionClass;
	private UserEventAppenderBiz userEventAppenderBiz;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param actionClass
	 *        the action class
	 * @param instructionDao
	 *        the instruction DAO to use
	 * @param chargePointDao
	 *        the charge point DAO to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @param chargePointRouter
	 *        the router to use
	 */
	public MqttInstructionHandler(Class<T> actionClass, NodeInstructionDao instructionDao,
			CentralChargePointDao chargePointDao, ObjectMapper objectMapper,
			ChargePointRouter chargePointRouter) {
		super();
		this.actionClass = actionClass;
		this.instructionDao = instructionDao;
		this.chargePointDao = chargePointDao;
		this.objectMapper = objectMapper;
		this.chargePointRouter = chargePointRouter;
		setDisplayName("OCPP Instructions");
	}

	@Override
	public Set<Action> getSupportedActions() {
		return null;
	}

	@Override
	public boolean isMessageSupported(ActionMessage<?> message) {
		return true;
	}

	@Override
	public void processActionMessage(ActionMessage<JsonNode> message,
			ActionMessageResultHandler<JsonNode, Void> resultHandler) {
		log.debug("Posting OCPP instruction {} action {} to MQTT topic {} for charge point {}",
				message.getMessageId(), message.getAction(), mqttTopic, message.getClientId());
		MqttConnection conn = mqttConnection.get();
		if ( conn != null && conn.isEstablished() ) {
			try {
				byte[] payload = objectMapper.writeValueAsBytes(message);
				BasicMqttMessage msg = new BasicMqttMessage(mqttTopic, false, MqttQos.AtLeastOnce,
						payload);
				Future<?> f = conn.publish(msg);
				f.get(getPublishTimeoutSeconds(), TimeUnit.SECONDS);
				resultHandler.handleActionMessageResult(message, null, null);
			} catch ( IOException | TimeoutException | ExecutionException | InterruptedException e ) {
				resultHandler.handleActionMessageResult(message, null, e);
			}
		} else {
			log.debug(
					"MQTT connection not available to post OCPP instruction {} action {} for charge point {}",
					message.getMessageId(), message.getAction(), message.getClientId());
			resultHandler.handleActionMessageResult(message, null,
					new IOException("Not connected to MQTT server."));
		}
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		if ( publishOnly ) {
			return;
		}
		try {
			connection.subscribe(mqttTopic, MqttQos.AtLeastOnce, this).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {}", mqttTopic, connection);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {}: {}", mqttTopic, connection,
					e.toString());
		}
	}

	@Override
	public void onMqttMessage(MqttMessage message) {
		if ( !mqttTopic.equals(message.getTopic()) || chargePointRouter == null ) {
			return;
		}
		try {
			JsonNode json = objectMapper.readTree(message.getPayload());
			log.trace("Received OCPP instruction {}", json);
			if ( json.isObject() ) {
				Long instructionId = json.path("messageId").asLong();
				ChargePointIdentity identity = objectMapper.readValue(
						objectMapper.treeAsTokens(json.path("clientId")), ChargePointIdentity.class);
				CentralChargePoint cp = (CentralChargePoint) chargePointDao.getForIdentity(identity);
				if ( cp == null ) {
					log.trace("ChargePoint {} not found for instruction {}; ignoring.", identity,
							instructionId);
					return;
				}

				Action action = null;
				String actionName = json.path("action").textValue();
				JsonNode payload = json.path("message");
				if ( actionName != null ) {
					for ( T a : actionClass.getEnumConstants() ) {
						if ( a.name().equals(actionName) ) {
							action = a;
							break;
						}
					}
				}
				if ( action != null ) {
					ChargePointBroker broker = chargePointRouter.brokerForChargePoint(identity);
					if ( broker != null ) {
						if ( !instructionDao.compareAndUpdateInstructionState(instructionId,
								cp.getNodeId(), InstructionState.Received, InstructionState.Executing,
								null) ) {
							return;
						}
						BasicActionMessage<Object> actionMessage = new BasicActionMessage<Object>(
								identity, action, payload);
						log.info("Sending instruction {} action {} to charge point {}", instructionId,
								action, identity);
						boolean sent = broker.sendMessageToChargePoint(actionMessage,
								(msg, res, err) -> {
									if ( err != null ) {
										Throwable root = err;
										while ( root.getCause() != null ) {
											root = root.getCause();
										}
										log.info(
												"Failed to send instruction {} action {} to charge point {}: {}",
												instructionId, actionMessage.getAction(),
												actionMessage.getClientId(), root.getMessage());
										Map<String, Object> data = singletonMap(ERROR_DATA_KEY,
												format("Error handling OCPP action %s: %s",
														actionMessage.getAction(), root.getMessage()));
										if ( instructionDao.compareAndUpdateInstructionState(
												instructionId, cp.getNodeId(),
												InstructionState.Executing, InstructionState.Declined,
												data) ) {
											generateUserEvent(cp.getUserId(),
													CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
													"Error handling OCPP action", data);

										}
									} else {
										Map<String, Object> resultParameters = null;
										if ( res != null ) {
											resultParameters = JsonUtils
													.getStringMapFromTree(objectMapper.valueToTree(res));
										}
										log.info("Sent instruction {} action {} to charge point {}.",
												instructionId, actionMessage.getAction(),
												actionMessage.getClientId());
										if ( instructionDao.compareAndUpdateInstructionState(
												instructionId, cp.getNodeId(),
												InstructionState.Executing, InstructionState.Completed,
												resultParameters != null && !resultParameters.isEmpty()
														? resultParameters
														: null) ) {
											Map<String, Object> data = new HashMap<>(4);
											data.put(ACTION_DATA_KEY, actionMessage.getAction());
											data.put(CHARGE_POINT_DATA_KEY, identity.getIdentifier());
											data.put(MESSAGE_DATA_KEY, resultParameters);
											generateUserEvent(cp.getUserId(),
													CHARGE_POINT_INSTRUCTION_SENT_TAGS, null, data);
										}
									}
									return true;
								});
						if ( !sent ) {
							Map<String, Object> data = new HashMap<>(4);
							data.put(ACTION_DATA_KEY, actionName);
							data.put(CHARGE_POINT_DATA_KEY, identity.getIdentifier());
							data.put(Identifiable.UID_PROPERTY, getUid());
							generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_TAGS,
									"CP disconnected", data);
						}
					} else {
						log.info(
								"No ChargePointBroker available to send instruction {} action {} to charge point {}",
								instructionId, action, identity);
						Map<String, Object> data = new HashMap<>(4);
						data.put(ACTION_DATA_KEY, actionName);
						data.put(CHARGE_POINT_DATA_KEY, identity.getIdentifier());
						data.put(Identifiable.UID_PROPERTY, getUid());
						generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_TAGS,
								"CP not connected", data);
					}
				} else {
					instructionDao.compareAndUpdateInstructionState(instructionId, cp.getNodeId(),
							InstructionState.Received, InstructionState.Declined,
							singletonMap("error", "Unsupported OCPP action."));
					Map<String, Object> data = new HashMap<>(4);
					data.put(ACTION_DATA_KEY, actionName);
					data.put(CHARGE_POINT_DATA_KEY, identity.getIdentifier());
					generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
							"Unsupported OCPP action", data);
				}
			}
		} catch ( Exception e ) {
			log.error("Error processing MQTT message on topic {}: {}", mqttTopic, e.getMessage());
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
	 * Get the MQTT topic to subscribe to.
	 * 
	 * @return the topic
	 */
	public String getMqttTopic() {
		return mqttTopic;
	}

	/**
	 * Set the MQTT topic to subscribe to.
	 * 
	 * @param mqttTopic
	 *        the topic
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@literal null}
	 */
	public void setMqttTopic(String mqttTopic) {
		if ( mqttTopic == null || mqttTopic.isEmpty() ) {
			throw new IllegalArgumentException("The mqttTopic parameter must not be null.");
		}
		this.mqttTopic = mqttTopic;
	}

	/**
	 * Get the "publish only" setting.
	 * 
	 * @return {@literal true} to not subscribe to the MQTT topic; defaults to
	 *         {@link #DEFAULT_PUBLISH_ONLY}
	 */
	public boolean isPublishOnly() {
		return publishOnly;
	}

	/**
	 * Set the "publish only" setting.
	 * 
	 * @param publishOnly
	 *        {@literal true} to not subscribe to the MQTT topic
	 */
	public void setPublishOnly(boolean publishOnly) {
		this.publishOnly = publishOnly;
	}

	/**
	 * Get the user event appender service.
	 * 
	 * @return the service
	 * @since 2.1
	 */
	public UserEventAppenderBiz getUserEventAppenderBiz() {
		return userEventAppenderBiz;
	}

	/**
	 * Set the user event appender service.
	 * 
	 * @param userEventAppenderBiz
	 *        the service to set
	 * @since 2.1
	 */
	public void setUserEventAppenderBiz(UserEventAppenderBiz userEventAppenderBiz) {
		this.userEventAppenderBiz = userEventAppenderBiz;
	}

}
