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

import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_ACTION_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGER_IDENTIFIER_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGE_POINT_ID_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_V16_TOPIC;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.ActionMessageQueue;
import net.solarnetwork.ocpp.web.json.OcppWebSocketHandler;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeResolver;
import ocpp.json.ActionPayloadDecoder;

/**
 * Extension of {@link OcppWebSocketHandler} to support queued instructions.
 * 
 * @author matt
 * @version 1.1
 * @since 1.1
 */
public class CentralOcppWebSocketHandler<C extends Enum<C> & Action, S extends Enum<S> & Action>
		extends OcppWebSocketHandler<C, S> {

	private CentralChargePointDao chargePointDao;
	private NodeInstructionDao instructionDao;

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
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		super.afterConnectionEstablished(session);
		ChargePointIdentity clientId = clientId(session);
		if ( clientId != null ) {
			// look for instructions
			executor.execute(new ProcessQueuedInstructionsTask(clientId));
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
				instructionDao.compareAndUpdateInstructionState(instruction.getId(), cp.getNodeId(),
						InstructionState.Received, InstructionState.Declined, Collections.singletonMap(
								"error", "OCPP action parameter missing or not supported."));
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
				instructionDao.compareAndUpdateInstructionState(instruction.getId(), cp.getNodeId(),
						InstructionState.Received, InstructionState.Declined,
						Collections.singletonMap("error",
								"OCPP " + OCPP_CHARGE_POINT_ID_PARAM + " parameter invalid syntax."));
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
							instructionDao.compareAndUpdateInstructionState(instruction.getId(),
									cp.getNodeId(), InstructionState.Received, InstructionState.Declined,
									Collections.singletonMap("error",
											"Error decoding OCPP action message: " + root.getMessage()));
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
								instructionDao.compareAndUpdateInstructionState(instruction.getId(),
										cp.getNodeId(), InstructionState.Executing,
										InstructionState.Declined, singletonMap("error",
												"Error handling OCPP action: " + root.getMessage()));
							} else {
								Map<String, Object> resultParameters = null;
								if ( res != null ) {
									resultParameters = JsonUtils
											.getStringMapFromTree(getObjectMapper().valueToTree(res));
								}
								instructionDao.compareAndUpdateInstructionState(instruction.getId(),
										cp.getNodeId(), InstructionState.Executing,
										InstructionState.Completed, resultParameters);
							}
							return true;
						});

						return null;
					});
		}

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

}
