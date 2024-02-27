/* ==================================================================
 * OcppController.java - 27/02/2020 11:52:28 am
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

package net.solarnetwork.central.ocpp.v16.service;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive;
import static org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
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
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.v16.jakarta.ActionErrorCode;
import net.solarnetwork.ocpp.v16.jakarta.ChargePointAction;
import net.solarnetwork.ocpp.v16.jakarta.ConfigurationKey;
import ocpp.v16.jakarta.cp.GetConfigurationRequest;
import ocpp.v16.jakarta.cp.GetConfigurationResponse;
import ocpp.v16.jakarta.cp.KeyValue;

/**
 * Manage OCPP 1.6 interactions.
 * 
 * @author matt
 * @version 2.9
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointIdentity identity, ChargePointInfo info) {
		ChargePoint cp = super.registerChargePoint(identity, info);

		// request the number of connectors, if no connectors present
		if ( cp.getConnectorCount() < 1 ) {
			var getConfReq = new GetConfigurationRequest();
			getConfReq.getKey().add(ConfigurationKey.NumberOfConnectors.getName());
			sendToChargePoint(identity, ChargePointAction.GetConfiguration, getConfReq,
					processConfiguration(cp), ActionErrorCode.GenericError);
		}

		return cp;
	}

	private ActionMessageResultHandler<GetConfigurationRequest, GetConfigurationResponse> processConfiguration(
			ChargePoint chargePoint) {
		return (msg, confs, err) -> {
			if ( confs != null && confs.getConfigurationKey() != null
					&& !confs.getConfigurationKey().isEmpty() ) {
				tryWithTransaction(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						ChargePoint cp = chargePointDao.get(chargePoint.getId());
						ChargePoint orig = new ChargePoint(cp);
						KeyValue numConnsKey = confs.getConfigurationKey().stream()
								.filter(k -> ConfigurationKey.NumberOfConnectors.getName()
										.equalsIgnoreCase(k.getKey()) && k.getValue() != null)
								.findAny().orElse(null);
						if ( numConnsKey != null ) {
							try {
								cp.setConnectorCount(Integer.parseInt(numConnsKey.getValue()));
							} catch ( NumberFormatException e ) {
								log.error("{} key invalid integer value: [{}]",
										ConfigurationKey.NumberOfConnectors, numConnsKey.getValue());
							}
						}
						if ( !cp.isSameAs(orig) ) {
							chargePointDao.save(cp);
							log.info("Saved configuration changes to Charge Point {}", cp.getId());
						}

						// add missing ChargePointConnector entities; remove excess
						Collection<ChargePointConnector> connectors = chargePointConnectorDao
								.findByChargePointId(cp.getId());
						Map<Integer, ChargePointConnector> existing = connectors.stream().collect(
								Collectors.toMap(cpc -> cpc.getId().getConnectorId(), cpc -> cpc));
						for ( int i = 1; i <= cp.getConnectorCount(); i++ ) {
							if ( !existing.containsKey(i) ) {
								ChargePointConnector conn = new ChargePointConnector(
										new ChargePointConnectorKey(cp.getId(), i), Instant.now());
								conn.setInfo(StatusNotification.builder().withConnectorId(i)
										.withTimestamp(conn.getCreated()).build());
								log.info("Creating ChargePointConnector {} for Charge Point {}", i,
										cp.getId());
								chargePointConnectorDao.save(conn);
							}
						}
						for ( Iterator<Entry<Integer, ChargePointConnector>> itr = existing.entrySet()
								.iterator(); itr.hasNext(); ) {
							Entry<Integer, ChargePointConnector> e = itr.next();
							int connId = e.getKey().intValue();
							if ( connId < 0 || connId > cp.getConnectorCount() ) {
								log.info("Deleting excess ChargePointConnector {} from Charge Point {}",
										connId, cp.getId());
								chargePointConnectorDao.delete(e.getValue());
								itr.remove();
							}
						}
					}
				});
			} else if ( err != null ) {
				log.warn("Unable to request configuration from charge point {}: {}",
						chargePoint.getInfo().getId(), err.getMessage());
			}
			return true;
		};
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
		ChargePointAction action;
		try {
			action = ChargePointAction.valueOf(params.remove(OcppInstructionUtils.OCPP_ACTION_PARAM));
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
		private final ChargePointAction action;
		private final ObjectNode jsonPayload;
		private final Object payload;

		private OcppNodeInstruction(NodeInstruction instruction, InstructionState state,
				ChargePointIdentity chargePointIdentity, ChargePointAction action,
				ObjectNode jsonPayload, Object payload) {
			super(instruction);
			setState(state);
			this.chargePointIdentity = chargePointIdentity;
			this.action = action;
			this.jsonPayload = jsonPayload;
			this.payload = payload;
		}
	}

}
