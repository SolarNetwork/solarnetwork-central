/* ==================================================================
 * CentralOcppWebSocketHandler.java - 3/04/2020 4:22:34 pm
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

package net.solarnetwork.central.in.ocpp.json;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_ACTION_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGER_IDENTIFIER_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGE_POINT_ID_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_V16_TOPIC;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.ApplicationMetadata;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.PendingActionMessage;
import net.solarnetwork.ocpp.service.ActionMessageQueue;
import net.solarnetwork.ocpp.web.json.OcppWebSocketHandler;
import net.solarnetwork.service.ServiceLifecycleObserver;
import ocpp.domain.Action;
import ocpp.domain.ErrorCode;
import ocpp.domain.ErrorCodeResolver;
import ocpp.json.ActionPayloadDecoder;

/**
 * Extension of {@link OcppWebSocketHandler} to support queued instructions.
 * 
 * @author matt
 * @version 2.3
 * @since 1.1
 */
public class CentralOcppWebSocketHandler<C extends Enum<C> & Action, S extends Enum<S> & Action>
		extends OcppWebSocketHandler<C, S> implements ServiceLifecycleObserver, CentralOcppUserEvents {

	private CentralChargePointDao chargePointDao;
	private NodeInstructionDao instructionDao;
	private UserEventAppenderBiz userEventAppenderBiz;
	private ChargePointStatusDao chargePointStatusDao;
	private ApplicationMetadata applicationMetadata;

	/**
	 * Constructor.
	 * 
	 * @param chargePointActionClass
	 *        the charge point action class
	 * @param centralSystemActionClass
	 *        the central system action class
	 * @param errorCodeResolver
	 *        the error code resolver
	 * @param executor
	 *        an executor for tasks
	 */
	public CentralOcppWebSocketHandler(Class<C> chargePointActionClass,
			Class<S> centralSystemActionClass, ErrorCodeResolver errorCodeResolver,
			AsyncTaskExecutor executor) {
		super(chargePointActionClass, centralSystemActionClass, errorCodeResolver, executor);
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointActionClass
	 *        the charge point action class
	 * @param centralSystemActionClass
	 *        the central system action class
	 * @param errorCodeResolver
	 *        the error code resolver
	 * @param executor
	 *        an executor for tasks
	 * @param mapper
	 *        the object mapper to use
	 * @param pendingMessageQueue
	 *        a queue to hold pending messages, for individual client IDs
	 * @param centralServiceActionPayloadDecoder
	 *        the action payload decoder to use
	 * @param chargePointActionPayloadDecoder
	 *        for Central Service message the action payload decoder to use for
	 *        Charge Point messages
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public CentralOcppWebSocketHandler(Class<C> chargePointActionClass,
			Class<S> centralSystemActionClass, ErrorCodeResolver errorCodeResolver,
			AsyncTaskExecutor executor, ObjectMapper mapper, ActionMessageQueue pendingMessageQueue,
			ActionPayloadDecoder centralServiceActionPayloadDecoder,
			ActionPayloadDecoder chargePointActionPayloadDecoder) {
		super(chargePointActionClass, centralSystemActionClass, errorCodeResolver, executor, mapper,
				pendingMessageQueue, centralServiceActionPayloadDecoder,
				chargePointActionPayloadDecoder);
	}

	@Override
	public void serviceDidStartup() {
		super.startup();
	}

	@Override
	public void serviceDidShutdown() {
		super.shutdown();
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		ChargePointIdentity clientId = clientId(session);
		if ( clientId != null ) {
			if ( clientId.getUserIdentifier() instanceof Long userId ) {
				final ApplicationMetadata appMeta = getApplicationMetadata();
				if ( appMeta != null && appMeta.getInstanceId() != null ) {
					final ChargePointStatusDao statusDao = getChargePointStatusDao();
					if ( statusDao != null ) {
						try {
							statusDao.updateConnectionStatus(userId, clientId.getIdentifier(),
									appMeta.getInstanceId(), Instant.now());
						} catch ( RuntimeException e ) {
							log.error("Error updating charger {} connection status", clientId, e);
						}
					}
				}
				Map<String, Object> data = singletonMap(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
				generateUserEvent(userId, CHARGE_POINT_CONNECTED_TAGS, null, data);
			}
			// look for instructions
			executor.execute(new ProcessQueuedInstructionsTask(clientId));
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		ChargePointIdentity clientId = clientId(session);
		if ( clientId != null ) {
			if ( clientId.getUserIdentifier() instanceof Long userId ) {
				final ApplicationMetadata appMeta = getApplicationMetadata();
				if ( appMeta != null && appMeta.getInstanceId() != null ) {
					final ChargePointStatusDao statusDao = getChargePointStatusDao();
					if ( statusDao != null ) {
						try {
							statusDao.updateConnectionStatus(userId, clientId.getIdentifier(),
									appMeta.getInstanceId(), null);
						} catch ( RuntimeException e ) {
							log.error("Error updating charger {} disconnection status", clientId, e);
						}
					}
				}
				Map<String, Object> data = singletonMap(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
				generateUserEvent(userId, CHARGE_POINT_DISCONNECTED_TAGS, null, data);
			}
		}
		super.afterConnectionClosed(session, status);
	}

	@Override
	protected void willProcessRequest(PendingActionMessage msg) {
		super.willProcessRequest(msg);
		if ( msg.getMessage().getClientId().getUserIdentifier() instanceof Long ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, msg.getMessage().getClientId().getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, msg.getMessage().getMessageId());
			data.put(ACTION_DATA_KEY, msg.getMessage().getAction());
			data.put(MESSAGE_DATA_KEY, msg.getMessage().getMessage());
			generateUserEvent((Long) msg.getMessage().getClientId().getUserIdentifier(),
					CHARGE_POINT_MESSAGE_RECEIVED_TAGS, null, data);
		}
	}

	@Override
	protected void willProcessCallResponse(PendingActionMessage msg, Object payload,
			Throwable exception) {
		super.willProcessCallResponse(msg, payload, exception);
		if ( msg.getMessage().getClientId().getUserIdentifier() instanceof Long ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, msg.getMessage().getClientId().getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, msg.getMessage().getMessageId());
			data.put(ACTION_DATA_KEY, msg.getMessage().getAction());
			data.put(MESSAGE_DATA_KEY, payload);
			generateUserEvent((Long) msg.getMessage().getClientId().getUserIdentifier(),
					CHARGE_POINT_MESSAGE_RECEIVED_TAGS, null, data);
		}
	}

	@Override
	protected void didSendCall(ChargePointIdentity clientId, String messageId, Action action,
			Object payload, String json, Throwable exception) {
		super.didSendCall(clientId, messageId, action, payload, json, exception);
		if ( clientId.getUserIdentifier() instanceof Long ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, messageId);
			data.put(ACTION_DATA_KEY, action);
			data.put(MESSAGE_DATA_KEY, payload);
			if ( exception != null ) {
				data.put(ERROR_DATA_KEY, exception.getMessage());
			}
			generateUserEvent((Long) clientId.getUserIdentifier(), CHARGE_POINT_MESSAGE_SENT_TAGS, null,
					data);
		}
	}

	@Override
	protected void didSendCallResult(ChargePointIdentity clientId, String messageId, Object payload,
			String json, Throwable exception) {
		super.didSendCallResult(clientId, messageId, payload, json, exception);
		if ( clientId.getUserIdentifier() instanceof Long ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, messageId);
			data.put(MESSAGE_DATA_KEY, payload);
			if ( exception != null ) {
				data.put(ERROR_DATA_KEY, exception.getMessage());
			}
			generateUserEvent((Long) clientId.getUserIdentifier(), CHARGE_POINT_MESSAGE_SENT_TAGS, null,
					data);
		}
	}

	@Override
	protected void didSendCallError(ChargePointIdentity clientId, String messageId, ErrorCode errorCode,
			String errorDescription, Map<String, ?> details, String json, Throwable exception) {
		super.didSendCallError(clientId, messageId, errorCode, errorDescription, details, json,
				exception);
		if ( clientId.getUserIdentifier() instanceof Long ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, messageId);
			data.put(ERROR_DATA_KEY, (exception != null ? exception.getMessage() : errorCode));
			data.put("errorCode", errorCode);
			data.put("errorDescription", errorDescription);
			data.put("errorDetails", details);
			generateUserEvent((Long) clientId.getUserIdentifier(), CHARGE_POINT_MESSAGE_SENT_ERROR_TAGS,
					null, data);
		}
	}

	private class ProcessQueuedInstructionsTask implements Runnable {

		private final ChargePointIdentity identity;

		private ProcessQueuedInstructionsTask(ChargePointIdentity identity) {
			super();
			this.identity = identity;
		}

		@Override
		public void run() {
			if ( chargePointDao == null || instructionDao == null ) {
				return;
			}
			try {
				CentralChargePoint cp = (CentralChargePoint) chargePointDao.getForIdentity(identity);
				if ( cp == null ) {
					return;
				}
				SimpleInstructionFilter filter = new SimpleInstructionFilter();
				filter.setNodeId(cp.getNodeId());
				filter.setStateSet(EnumSet.of(InstructionState.Received));
				FilterResults<EntityMatch> matches = instructionDao.findFiltered(filter, null, null,
						null);
				for ( EntityMatch match : matches ) {
					Instruction instruction;
					if ( match instanceof Instruction ) {
						instruction = (Instruction) match;
					} else {
						instruction = instructionDao.get(match.getId());
					}
					if ( instruction != null && OCPP_V16_TOPIC.equals(instruction.getTopic()) ) {
						processInstruction(instruction, cp);
					}
				}
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.error("{} error processing queued instructions for charger {}: {}",
						root.getClass().getSimpleName(), identity, root.getMessage(), e);
			}
		}

		private Map<String, String> instructionParameterMap(Instruction instruction) {
			Map<String, String> params = instruction.getParams();
			return (params != null ? params : new HashMap<>(0));
		}

		private void processInstruction(Instruction instruction, CentralChargePoint cp) {
			Map<String, String> params = instructionParameterMap(instruction);
			Action action = chargePointAction(params.remove(OCPP_ACTION_PARAM));
			if ( action == null ) {
				Map<String, Object> data = singletonMap(ERROR_DATA_KEY,
						"OCPP action parameter missing or not supported.");
				if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(), cp.getNodeId(),
						InstructionState.Received, InstructionState.Declined, data) ) {
					generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
							"Unsupported action", data);
				}
				return;
			}

			// verify the instruction is for this charge point, first via ID
			try {
				String instructionChargePointId = params.remove(OCPP_CHARGE_POINT_ID_PARAM);
				if ( instructionChargePointId != null
						&& !cp.getId().equals(Long.valueOf(instructionChargePointId)) ) {
					// not for this charge point
					return;
				}
			} catch ( NumberFormatException e ) {
				Map<String, Object> data = singletonMap(ERROR_DATA_KEY,
						"OCPP " + OCPP_CHARGE_POINT_ID_PARAM + " parameter invalid syntax.");
				if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(), cp.getNodeId(),
						InstructionState.Received, InstructionState.Declined, data) ) {
					generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
							"Invalid charge point ID syntax", data);
				}
				return;
			}

			// next via identifier
			String instructionIdentifier = params.remove(OCPP_CHARGER_IDENTIFIER_PARAM);
			if ( instructionIdentifier != null && !instructionIdentifier.equals(cp.getInfo().getId()) ) {
				// not for this charge point
				return;
			}

			// this instruction is for this charge point... send it now
			OcppInstructionUtils.decodeJsonOcppInstructionMessage(getObjectMapper(), action, params,
					getChargePointActionPayloadDecoder(), (e, jsonPayload, payload) -> {
						if ( e != null ) {
							Throwable root = e;
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							Map<String, Object> data = singletonMap(ERROR_DATA_KEY,
									"Error decoding OCPP action message: " + root.getMessage());
							if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(),
									cp.getNodeId(), InstructionState.Received, InstructionState.Declined,
									data) ) {
								generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
										"Invalid OCPP message syntax", data);
							}
							return null;
						}

						if ( !instructionDao.compareAndUpdateInstructionState(instruction.getId(),
								cp.getNodeId(), InstructionState.Received, InstructionState.Executing,
								null) ) {
							return null;
						}

						ActionMessage<Object> message = new BasicActionMessage<Object>(identity,
								UUID.randomUUID().toString(), action, payload);
						sendMessageToChargePoint(message, (msg, res, err) -> {
							if ( err != null ) {
								Throwable root = err;
								while ( root.getCause() != null ) {
									root = root.getCause();
								}
								Map<String, Object> data = singletonMap(ERROR_DATA_KEY, format(
										"Error handling OCPP action %s: %s", action, root.getMessage()));
								if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(),
										cp.getNodeId(), InstructionState.Executing,
										InstructionState.Declined, data) ) {
									generateUserEvent(cp.getUserId(),
											CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
											"Error handling OCPP action", data);
								}
							} else {
								Map<String, Object> resultParameters = null;
								if ( res != null ) {
									resultParameters = JsonUtils
											.getStringMapFromTree(getObjectMapper().valueToTree(res));
								}
								if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(),
										cp.getNodeId(), InstructionState.Executing,
										InstructionState.Completed, resultParameters) ) {
									Map<String, Object> data = new HashMap<>(4);
									data.put(ACTION_DATA_KEY, action);
									data.put(CHARGE_POINT_DATA_KEY, identity.getIdentifier());
									data.put(MESSAGE_DATA_KEY, resultParameters);
									generateUserEvent(cp.getUserId(),
											CHARGE_POINT_INSTRUCTION_ACKNOWLEDGED_TAGS, null, data);
								}
							}
							return true;
						});

						return null;
					});
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
	 * Get the charge point DAO.
	 * 
	 * @return the DAO
	 */
	public CentralChargePointDao getChargePointDao() {
		return chargePointDao;
	}

	/**
	 * Set the charge point DAO.
	 * 
	 * @param chargePointDao
	 *        the DAO
	 */
	public void setChargePointDao(CentralChargePointDao chargePointDao) {
		this.chargePointDao = chargePointDao;
	}

	/**
	 * Get the instruction DAO to use.
	 * 
	 * @return the DAO
	 */
	public NodeInstructionDao getInstructionDao() {
		return instructionDao;
	}

	/**
	 * Set the instruction DAO to use.
	 * 
	 * @param instructionDao
	 *        the DAO to use
	 */
	public void setInstructionDao(NodeInstructionDao instructionDao) {
		this.instructionDao = instructionDao;
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
	 * Get the application metadata.
	 * 
	 * @return the application metadata
	 * @since 2.3
	 */
	public ApplicationMetadata getApplicationMetadata() {
		return applicationMetadata;
	}

	/**
	 * Set the application metadata.
	 * 
	 * @param applicationMetadata
	 *        the application metadata to set
	 * @since 2.3
	 */
	public void setApplicationMetadata(ApplicationMetadata applicationMetadata) {
		this.applicationMetadata = applicationMetadata;
	}

	/**
	 * Get the charge point status DAO.
	 * 
	 * @return the DAO
	 * @since 2.3
	 */
	public ChargePointStatusDao getChargePointStatusDao() {
		return chargePointStatusDao;
	}

	/**
	 * Set the charge point status DAO.
	 * 
	 * @param chargePointStatusDao
	 *        the DAO to set
	 * @since 2.3
	 */
	public void setChargePointStatusDao(ChargePointStatusDao chargePointStatusDao) {
		this.chargePointStatusDao = chargePointStatusDao;
	}

}
