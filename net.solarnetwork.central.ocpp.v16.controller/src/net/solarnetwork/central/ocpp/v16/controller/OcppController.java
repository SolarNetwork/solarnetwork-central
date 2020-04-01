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

package net.solarnetwork.central.ocpp.v16.controller;

import static java.util.Collections.singletonMap;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.Authorization;
import net.solarnetwork.ocpp.domain.AuthorizationInfo;
import net.solarnetwork.ocpp.domain.AuthorizationStatus;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.RegistrationStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.support.BasicIdentifiable;
import net.solarnetwork.util.JsonUtils;
import ocpp.domain.Action;
import ocpp.domain.ErrorCodeException;
import ocpp.domain.SchemaValidationException;
import ocpp.json.ActionPayloadDecoder;
import ocpp.v16.ActionErrorCode;
import ocpp.v16.ChargePointAction;
import ocpp.v16.ConfigurationKey;
import ocpp.v16.cp.GetConfigurationRequest;
import ocpp.v16.cp.GetConfigurationResponse;
import ocpp.v16.cp.KeyValue;

/**
 * Manage OCPP 1.6 interactions.
 * 
 * @author matt
 * @version 1.0
 */
public class OcppController extends BasicIdentifiable
		implements ChargePointManager, AuthorizationService, NodeInstructionQueueHook {

	/** The default {@code initialRegistrationStatus} value. */
	public static final RegistrationStatus DEFAULT_INITIAL_REGISTRATION_STATUS = RegistrationStatus.Pending;

	/** A node instruction topic for OCPP v1.6 actions. */
	public static final String OCPP_V16_TOPIC = "OCPP_v16";

	/** A node instruction parameter name for an OCPP v1.6 action name. */
	public static final String OCPP_V16_ACTION_PARAM = "action";

	/**
	 * A node instruction parameter name for an OCPP v1.6 ChargePoint
	 * identifier.
	 */
	public static final String OCPP_V16_CHARGER_IDENTIFIER_PARAM = "chargerIdentifier";

	/**
	 * A node instruction parameter name for an OCPP v1.6 ChargePoint entity ID.
	 */
	public static final String OCPP_V16_CHARGE_POINT_ID_PARAM = "chargePointId";

	private final Executor executor;
	private final UserNodeDao userNodeDao;
	private final NodeInstructionDao instructionDao;
	private final ChargePointRouter chargePointRouter;
	private final CentralAuthorizationDao authorizationDao;
	private final CentralChargePointDao chargePointDao;
	private final CentralChargePointConnectorDao chargePointConnectorDao;
	private RegistrationStatus initialRegistrationStatus;
	private TransactionTemplate transactionTemplate;
	private ActionPayloadDecoder chargePointActionPayloadDecoder;
	private ObjectMapper objectMapper;

	private final Logger log = LoggerFactory.getLogger(getClass());

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
	 * @param authorizationDao
	 *        the {@link Authorization} DAO to use
	 * @param chargePointDao
	 *        the {@link ChargePoint} DAO to use
	 * @param chargePointConnectorDao
	 *        the {@link ChargePointConnector} DAO to use
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public OcppController(Executor executor, ChargePointRouter chargePointRouter,
			UserNodeDao userNodeDao, NodeInstructionDao instructionDao,
			CentralAuthorizationDao authorizationDao, CentralChargePointDao chargePointDao,
			CentralChargePointConnectorDao chargePointConnectorDao) {
		super();
		if ( executor == null ) {
			throw new IllegalArgumentException("The executor parameter must not be null.");
		}
		this.executor = executor;
		if ( chargePointRouter == null ) {
			throw new IllegalArgumentException("The chargePointRouter parameter must not be null.");
		}
		this.chargePointRouter = chargePointRouter;
		if ( userNodeDao == null ) {
			throw new IllegalArgumentException("The userNodeDao parameter must not be null.");
		}
		this.userNodeDao = userNodeDao;
		if ( instructionDao == null ) {
			throw new IllegalArgumentException("The instructionDao parameter must not be null.");
		}
		this.instructionDao = instructionDao;
		if ( authorizationDao == null ) {
			throw new IllegalArgumentException("The authorizationDao parameter must not be null.");
		}
		this.authorizationDao = authorizationDao;
		if ( chargePointDao == null ) {
			throw new IllegalArgumentException("The chargePointDao parameter must not be null.");
		}
		this.chargePointDao = chargePointDao;
		if ( chargePointConnectorDao == null ) {
			throw new IllegalArgumentException(
					"The chargePointConnectorDao parameter must not be null.");
		}
		this.chargePointConnectorDao = chargePointConnectorDao;
		this.initialRegistrationStatus = DEFAULT_INITIAL_REGISTRATION_STATUS;
		this.objectMapper = ocpp.json.support.BaseActionPayloadDecoder.defaultObjectMapper();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ChargePoint registerChargePoint(ChargePointIdentity identity, ChargePointInfo info) {
		log.info("Charge Point registration received: {}", info);

		if ( info == null || info.getId() == null ) {
			throw new IllegalArgumentException("The ChargePoint ID must be provided.");
		}

		ChargePoint cp = chargePointDao.getForIdentity(identity);
		if ( cp == null ) {
			throw new IllegalArgumentException("ChargePoint identifer is not known.");
		}
		if ( cp.isEnabled() ) {
			cp = updateChargePointInfo(cp, info);
		}

		sendToChargePoint(identity, ChargePointAction.GetConfiguration, new GetConfigurationRequest(),
				processConfiguration(cp));

		return cp;
	}

	private ChargePoint updateChargePointInfo(ChargePoint cp, ChargePointInfo info) {
		assert cp != null && cp.getInfo() != null;
		if ( cp.getInfo().isSameAs(info) ) {
			log.info("ChargePoint registration info is unchanged: {}", info);
		} else {
			log.info("Updating ChargePoint registration info {} -> {}", cp.getInfo(), info);
			cp.copyInfoFrom(info);
			chargePointDao.save(cp);
		}
		return cp;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean isChargePointRegistrationAccepted(long chargePointId) {
		ChargePoint cp = chargePointDao.get(chargePointId);
		return cp != null && cp.isEnabled() && cp.getRegistrationStatus() == RegistrationStatus.Accepted;
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
							if ( connId < 1 || connId > cp.getConnectorCount() ) {
								log.info("Deleting excess ChargePointConnector {} from Charge Point {}",
										connId, cp.getId());
								chargePointConnectorDao.delete(e.getValue());
								itr.remove();
							}
						}
					}
				});
			} else if ( err != null ) {
				log.warn("Error requesting configuration from charge point {}: {}",
						chargePoint.getInfo().getId(), err.getMessage());
			}
			return true;
		};
	}

	@Override
	public AuthorizationInfo authorize(final ChargePointIdentity identity, final String idTag) {
		Authorization auth = null;
		if ( identity != null && idTag != null ) {
			CentralChargePoint cp = (CentralChargePoint) chargePointDao.getForIdentity(identity);
			if ( cp != null ) {
				auth = authorizationDao.getForToken(cp.getUserId(), idTag);
			}
		}
		AuthorizationInfo.Builder result = AuthorizationInfo.builder().withId(idTag);
		if ( auth != null ) {
			result.withExpiryDate(auth.getExpiryDate()).withParentId(auth.getParentId());
			if ( !auth.isEnabled() ) {
				result.withStatus(AuthorizationStatus.Blocked);
			} else if ( auth.isExpired() ) {
				result.withStatus(AuthorizationStatus.Expired);
			} else {
				result.withStatus(AuthorizationStatus.Accepted);
			}
		} else {
			result.withStatus(AuthorizationStatus.Invalid);
		}
		return result.build();
	}

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		final String topic = instruction.getTopic();
		final Long nodeId = instruction.getNodeId();
		if ( !OCPP_V16_TOPIC.equals(topic) || nodeId == null ) {
			return instruction;
		}
		UserNode userNode = userNodeDao.get(nodeId);
		if ( userNode == null ) {
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
			action = ChargePointAction.valueOf(params.remove(OCPP_V16_ACTION_PARAM));
		} catch ( IllegalArgumentException | NullPointerException e ) {
			instruction.setState(InstructionState.Declined);
			instruction.setResultParameters(
					Collections.singletonMap("error", "OCPP action parameter missing."));
			return instruction;
		}
		Object payload;
		try {
			if ( chargePointActionPayloadDecoder != null ) {
				ObjectNode json = objectMapper.valueToTree(params);
				payload = chargePointActionPayloadDecoder.decodeActionPayload(action, false, json);
			} else {
				payload = params;
			}
		} catch ( IOException | SchemaValidationException e ) {
			Throwable root = e;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			instruction.setState(InstructionState.Declined);
			instruction.setResultParameters(
					singletonMap("error", "Error decoding OCPP action message: " + root.getMessage()));
			return instruction;
		}
		return new OcppNodeInstruction(instruction, cp.chargePointIdentity(), action, payload);
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( !(instruction instanceof OcppNodeInstruction) ) {
			return;
		}
		OcppNodeInstruction instr = (OcppNodeInstruction) instruction;
		sendToChargePoint(instr.chargePointIdentity, instr.action, instr.payload, (msg, res, err) -> {
			if ( err != null ) {
				Throwable root = err;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				instructionDao.compareAndUpdateInstructionState(instructionId, instr.getNodeId(),
						InstructionState.Executing, InstructionState.Declined,
						singletonMap("error", "Error handling OCPP action: " + root.getMessage()));
			} else {
				Map<String, Object> resultParameters = null;
				if ( res != null ) {
					resultParameters = JsonUtils.getStringMapFromTree(objectMapper.valueToTree(res));
				}
				instructionDao.compareAndUpdateInstructionState(instructionId, instr.getNodeId(),
						InstructionState.Executing, InstructionState.Completed, resultParameters);
			}
			return true;
		});
	}

	private static final class OcppNodeInstruction extends NodeInstruction {

		private static final long serialVersionUID = -100774686071322459L;

		private final ChargePointIdentity chargePointIdentity;
		private final Action action;
		private final Object payload;

		private OcppNodeInstruction(NodeInstruction instruction, ChargePointIdentity chargePointIdentity,
				Action action, Object payload) {
			super(instruction);
			setState(InstructionState.Executing);
			this.chargePointIdentity = chargePointIdentity;
			this.action = action;
			this.payload = payload;
		}
	}

	private Map<String, String> instructionParameterMap(NodeInstruction instruction) {
		List<InstructionParameter> paramList = instruction.getParameters();
		if ( paramList == null || paramList.isEmpty() ) {
			return Collections.emptyMap();
		}
		Map<String, String> params = new LinkedHashMap<>(paramList.size());
		for ( InstructionParameter p : paramList ) {
			params.put(p.getName(), p.getValue());
		}
		return params;
	}

	private CentralChargePoint chargePointForParameters(UserNode userNode,
			Map<String, String> parameters) {
		CentralChargePoint result = null;
		try {
			Long id = Long.valueOf(parameters.remove(OCPP_V16_CHARGE_POINT_ID_PARAM));
			result = chargePointDao.get(userNode.getUserId(), id);
		} catch ( NumberFormatException e ) {
			// try via identifier
			String ident = parameters.remove(OCPP_V16_CHARGER_IDENTIFIER_PARAM);
			if ( ident != null ) {
				result = (CentralChargePoint) chargePointDao.getForIdentifier(userNode.getUserId(),
						ident);
			}
		}
		return result;
	}

	private <T> T tryWithTransaction(TransactionCallback<T> tx) {
		final TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			return tt.execute(tx);
		} else {
			return tx.doInTransaction(null);
		}
	}

	private <T, R> void sendToChargePoint(ChargePointIdentity identity, Action action, T payload,
			ActionMessageResultHandler<T, R> handler) {
		executor.execute(() -> {
			ActionMessage<T> msg = new BasicActionMessage<T>(identity, UUID.randomUUID().toString(),
					action, payload);
			ChargePointBroker broker = chargePointRouter.brokerForChargePoint(identity);
			if ( broker != null ) {
				if ( broker.sendMessageToChargePoint(msg, handler) ) {
					return;
				}
			} else {
				log.warn("No ChargePointBroker available for {}", identity);
			}
			handler.handleActionMessageResult(msg, null,
					new ErrorCodeException(ActionErrorCode.GenericError, "Client not available."));
		});
	}

	/**
	 * Get the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @return the status, never {@literal null}
	 */
	public RegistrationStatus getInitialRegistrationStatus() {
		return initialRegistrationStatus;
	}

	/**
	 * Set the initial {@link RegistrationStatus} to use for newly registered
	 * charge points.
	 * 
	 * @param initialRegistrationStatus
	 *        the status to set
	 * @throws IllegalArgumentException
	 *         if {@code initialRegistrationStatus} is {@literal null}
	 */
	public void setInitialRegistrationStatus(RegistrationStatus initialRegistrationStatus) {
		if ( initialRegistrationStatus == null ) {
			throw new IllegalArgumentException(
					"The initialRegistrationStatus parameter must not be null.");
		}
		this.initialRegistrationStatus = initialRegistrationStatus;
	}

	/**
	 * Get the configured transaction template.
	 * 
	 * @return the transaction template
	 */
	public TransactionTemplate getTransactionTemplate() {
		return transactionTemplate;
	}

	/**
	 * Set the transaction template to use.
	 * 
	 * @param transactionTemplate
	 *        the transaction template to set
	 */
	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Get the ChargePoint action payload decoder.
	 * 
	 * @return the decoder
	 */
	public ActionPayloadDecoder getChargePointActionPayloadDecoder() {
		return chargePointActionPayloadDecoder;
	}

	/**
	 * Set the ChargePoint action payload decoder.
	 * 
	 * @param chargePointActionPayloadDecoder
	 *        the decoder
	 */
	public void setChargePointActionPayloadDecoder(
			ActionPayloadDecoder chargePointActionPayloadDecoder) {
		this.chargePointActionPayloadDecoder = chargePointActionPayloadDecoder;
	}

	/**
	 * Get the {@link ObjectMapper}.
	 * 
	 * @return the mapper
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the {@link ObjectMapper} to use.
	 * 
	 * @param objectMapper
	 *        the mapper
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}
