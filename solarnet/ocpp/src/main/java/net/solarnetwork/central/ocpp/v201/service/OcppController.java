/* ==================================================================
 * OcppController.java - 18/02/2024 2:14:39 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.v201.service;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.transaction.support.TransactionSynchronization;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.service.BaseOcppController;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.v201.domain.Action;
import net.solarnetwork.ocpp.v201.domain.ActionErrorCode;

/**
 * Manage OCPP 2.0.1 interactions.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppController extends BaseOcppController {

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        a task runner
	 * @param chargePointRouter
	 *        the broker router to push messages to Charge Points with with
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param instructionDao
	 *        the instruction DAO to use
	 * @param chargePointDao
	 *        the {@link ChargePoint} DAO to use
	 * @param chargePointConnectorDao
	 *        the {@link ChargePointConnector} DAO to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppController(Executor executor, ChargePointRouter chargePointRouter,
			UserNodeDao userNodeDao, NodeInstructionDao instructionDao,
			CentralChargePointDao chargePointDao, CentralChargePointConnectorDao chargePointConnectorDao,
			ObjectMapper objectMapper) {
		super(executor, chargePointRouter, userNodeDao, instructionDao, chargePointDao,
				chargePointConnectorDao, objectMapper);
	}

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		final String topic = instruction.getTopic();
		final Long nodeId = instruction.getNodeId();
		log.trace("Inspecting {} instruction for node {}", topic, nodeId);
		if ( !OcppInstructionUtils.OCPP_V16_TOPIC.equals(topic) || nodeId == null ) {
			return instruction;
		}
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
			log.trace("UserNode not found for node {}; ignoring OCPPv16 instruction {}", nodeId, topic);
			return instruction;
		}
		Map<String, String> params = instructionParameterMap(instruction);
		CentralChargePoint cp = chargePointForParameters(userNode, params);
		if ( cp == null ) {
			instruction.setState(InstructionState.Declined);
			instruction.setResultParameters(
					Collections.singletonMap("error", "ChargePoint not specified or not available."));
			return instruction;
		}
		Action action;
		try {
			action = Action.valueOf(params.remove(OcppInstructionUtils.OCPP_ACTION_PARAM));
		} catch ( IllegalArgumentException | NullPointerException e ) {
			instruction.setState(InstructionState.Declined);
			instruction.setResultParameters(
					Collections.singletonMap("error", "OCPP action parameter missing."));
			return instruction;
		}
		return OcppInstructionUtils.decodeJsonOcppInstructionMessage(objectMapper, action, params,
				getChargePointActionPayloadDecoder(), (e, jsonPayload, payload) -> {
					if ( e != null ) {
						Throwable root = e;
						while ( root.getCause() != null ) {
							root = root.getCause();
						}
						instruction.setState(InstructionState.Declined);
						instruction.setResultParameters(singletonMap("error",
								"Error decoding OCPP action message: " + root.getMessage()));
						return instruction;
					}
					return new OcppNodeInstruction(instruction,
							(getInstructionHandler() == null ? InstructionState.Executing
									: InstructionState.Received),
							cp.chargePointIdentity(), action, jsonPayload, payload);
				});
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( !(instruction instanceof OcppNodeInstruction) ) {
			return;
		}
		final OcppNodeInstruction instr = (OcppNodeInstruction) instruction;
		final Long userId = (instr.chargePointIdentity.getUserIdentifier() instanceof Long
				? (Long) instr.chargePointIdentity.getUserIdentifier()
				: null);

		final ActionMessageProcessor<JsonNode, Void> handler = getInstructionHandler();
		if ( handler != null ) {
			log.trace("Passing OCPPv16 instruction {} to processor {}", instructionId, handler);
			BasicActionMessage<JsonNode> cpMsg = new BasicActionMessage<>(instr.chargePointIdentity,
					instr.getId().toString(), instr.action, instr.jsonPayload);
			ActionMessageResultHandler<JsonNode, Void> processor = (msg, res, err) -> {
				if ( err != null ) {
					Throwable root = err;
					while ( root.getCause() != null ) {
						root = root.getCause();
					}
					Map<String, Object> data = singletonMap(ERROR_DATA_KEY, format(
							"Error handling OCPP action %s: %s", instr.action, root.getMessage()));
					instructionDao.compareAndUpdateInstructionState(instructionId, instr.getNodeId(),
							instr.getState(), InstructionState.Declined, data);
					if ( userId != null ) {
						generateUserEvent(userId, CHARGE_POINT_INSTRUCTION_ERROR_TAGS, "Failed to send",
								data);
					}
				} else if ( userId != null ) {
					Map<String, Object> data = new HashMap<>(4);
					data.put(ACTION_DATA_KEY, instr.action);
					data.put(CHARGE_POINT_DATA_KEY, instr.chargePointIdentity.getIdentifier());
					data.put(MESSAGE_DATA_KEY, JsonUtils.getJSONString(cpMsg, "{}"));
					generateUserEvent(userId, CHARGE_POINT_INSTRUCTION_QUEUED_TAGS, null, data);
				}
				return true;
			};
			if ( isActualTransactionActive() ) {
				// we need the instruction state committed before passing to the handler
				registerSynchronization(new TransactionSynchronization() {

					@Override
					public void afterCommit() {
						handler.processActionMessage(cpMsg, processor);
					}

				});
			} else {
				handler.processActionMessage(cpMsg, processor);
			}
			return;
		}

		log.info("Sending OCPPv16 {} to charge point {}", instr.action, instr.chargePointIdentity);
		sendToChargePoint(instr.chargePointIdentity, instr.action, instr.payload, (msg, res, err) -> {
			if ( err != null ) {
				Throwable root = err;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.info("Failed to send OCPPv16 {} to charge point {}: {}", instr.action,
						instr.chargePointIdentity, root.getMessage());
				Map<String, Object> data = singletonMap("error",
						"Error handling OCPP action: " + root.getMessage());
				instructionDao.compareAndUpdateInstructionState(instructionId, instr.getNodeId(),
						instr.getState(), InstructionState.Declined, data);
				if ( userId != null ) {
					generateUserEvent(userId, CHARGE_POINT_INSTRUCTION_ERROR_TAGS, "Failed to send",
							data);
				}
			} else {
				Map<String, Object> resultParameters = null;
				if ( res != null ) {
					resultParameters = JsonUtils.getStringMapFromTree(objectMapper.valueToTree(res));
				}
				log.info("Sent OCPPv16 {} to charge point {}", instr.action, instr.chargePointIdentity);
				instructionDao.compareAndUpdateInstructionState(instructionId, instr.getNodeId(),
						instr.getState(), InstructionState.Completed, resultParameters);
				if ( userId != null ) {
					Map<String, Object> data = new HashMap<>(4);
					data.put(ACTION_DATA_KEY, instr.action);
					data.put(CHARGE_POINT_DATA_KEY, instr.chargePointIdentity.getIdentifier());
					data.put(MESSAGE_DATA_KEY, resultParameters);
					generateUserEvent(userId, CHARGE_POINT_INSTRUCTION_ACKNOWLEDGED_TAGS, null, data);
				}
			}
			return true;
		}, ActionErrorCode.GenericError);
	}

	private static final class OcppNodeInstruction extends NodeInstruction {

		private static final long serialVersionUID = -100774686071322459L;

		private final ChargePointIdentity chargePointIdentity;
		private final Action action;
		private final ObjectNode jsonPayload;
		private final Object payload;

		private OcppNodeInstruction(NodeInstruction instruction, InstructionState state,
				ChargePointIdentity chargePointIdentity, Action action, ObjectNode jsonPayload,
				Object payload) {
			super(instruction);
			setState(state);
			this.chargePointIdentity = chargePointIdentity;
			this.action = action;
			this.jsonPayload = jsonPayload;
			this.payload = payload;
		}
	}

}
