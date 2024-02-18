/* ==================================================================
 * BaseOcppController.java - 18/02/2024 2:15:33 pm
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

package net.solarnetwork.central.ocpp.service;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.ocpp.domain.Action;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.domain.ErrorCode;
import net.solarnetwork.ocpp.domain.ErrorCodeException;
import net.solarnetwork.ocpp.domain.RegistrationStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;
import net.solarnetwork.service.support.BasicIdentifiable;

/**
 * Base OCPP controller support.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseOcppController extends BasicIdentifiable
		implements ChargePointManager, NodeInstructionQueueHook, CentralOcppUserEvents {

	/** The default {@code initialRegistrationStatus} value. */
	public static final RegistrationStatus DEFAULT_INITIAL_REGISTRATION_STATUS = RegistrationStatus.Pending;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** The executor. */
	protected final Executor executor;

	/** The user node DAO. */
	protected final UserNodeDao userNodeDao;

	/** The node instruction DAO. */
	protected final NodeInstructionDao instructionDao;

	/** The charge point router. */
	protected final ChargePointRouter chargePointRouter;

	/** The charge point DAO. */
	protected final CentralChargePointDao chargePointDao;

	/** The charge point connector DAO. */
	protected final CentralChargePointConnectorDao chargePointConnectorDao;

	/** The object mapper. */
	protected final ObjectMapper objectMapper;

	private RegistrationStatus initialRegistrationStatus;
	private TransactionTemplate transactionTemplate;
	private ActionPayloadDecoder chargePointActionPayloadDecoder;
	private ConnectorStatusDatumPublisher datumPublisher;
	private ActionMessageProcessor<JsonNode, Void> instructionHandler;
	private UserEventAppenderBiz userEventAppenderBiz;

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
	public BaseOcppController(Executor executor, ChargePointRouter chargePointRouter,
			UserNodeDao userNodeDao, NodeInstructionDao instructionDao,
			CentralChargePointDao chargePointDao, CentralChargePointConnectorDao chargePointConnectorDao,
			ObjectMapper objectMapper) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.chargePointRouter = requireNonNullArgument(chargePointRouter, "chargePointRouter");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.instructionDao = requireNonNullArgument(instructionDao, "instructionDao");
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
		this.chargePointConnectorDao = requireNonNullArgument(chargePointConnectorDao,
				"chargePointConnectorDao");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.initialRegistrationStatus = DEFAULT_INITIAL_REGISTRATION_STATUS;
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

		return cp;
	}

	/**
	 * Update a charge point from info.
	 * 
	 * @param cp
	 *        the charge point to update
	 * @param info
	 *        the info
	 * @return the updated charge point
	 */
	protected final ChargePoint updateChargePointInfo(ChargePoint cp, ChargePointInfo info) {
		assert cp != null && cp.getInfo() != null;
		if ( cp.getInfo().isSameAs(info) ) {
			log.info("ChargePoint registration info is unchanged: {}", info);
		} else {
			log.info("Updating ChargePoint registration info {} -> {}", cp.getInfo(), info);
			cp.getInfo().copyFrom(info);
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateChargePointStatus(ChargePointIdentity identity, StatusNotification info) {
		final CentralChargePoint chargePoint = (CentralChargePoint) chargePointDao
				.getForIdentity(identity);
		if ( chargePoint == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, identity);
		}
		log.info("Received Charge Point {} status: {}", identity, info);
		chargePointConnectorDao.saveStatusInfo(chargePoint.getId(), info);
		ConnectorStatusDatumPublisher publisher = getDatumPublisher();
		if ( publisher != null ) {
			publisher.processStatusNotification(chargePoint, info);
		}
	}

	protected static Map<String, String> instructionParameterMap(NodeInstruction instruction) {
		Map<String, String> params = instruction.getParams();
		return (params != null ? params : new HashMap<>(0));
	}

	protected final CentralChargePoint chargePointForParameters(UserNode userNode,
			Map<String, String> parameters) {
		CentralChargePoint result = null;
		try {
			Long id = Long.valueOf(parameters.remove(OcppInstructionUtils.OCPP_CHARGE_POINT_ID_PARAM));
			result = chargePointDao.get(userNode.getUserId(), id);
		} catch ( NumberFormatException e ) {
			// try via identifier
			String ident = parameters.remove(OcppInstructionUtils.OCPP_CHARGER_IDENTIFIER_PARAM);
			if ( ident != null ) {
				result = (CentralChargePoint) chargePointDao.getForIdentifier(userNode.getUserId(),
						ident);
			}
		}
		return result;
	}

	protected final <T> T tryWithTransaction(TransactionCallback<T> tx) {
		final TransactionTemplate tt = getTransactionTemplate();
		if ( tt != null ) {
			return tt.execute(tx);
		} else {
			return tx.doInTransaction(null);
		}
	}

	protected final <T, R> void sendToChargePoint(ChargePointIdentity identity, Action action, T payload,
			ActionMessageResultHandler<T, R> handler, ErrorCode noClientError) {
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
					new ErrorCodeException(noClientError, "Client not available."));
		});
	}

	protected final void generateUserEvent(Long userId, String[] tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String ? (String) data : JsonUtils.getJSONString(data, null));
		LogEventInfo event = new LogEventInfo(tags, message, dataStr);
		biz.addEvent(userId, event);
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
	 * Get the configured datum publisher for status notification updates.
	 * 
	 * @return the datum publisher
	 */
	public ConnectorStatusDatumPublisher getDatumPublisher() {
		return datumPublisher;
	}

	/**
	 * Set a datum publisher for status notification updates.
	 * 
	 * @param datumPublisher
	 *        the datum publisher
	 */
	public void setDatumPublisher(ConnectorStatusDatumPublisher datumPublisher) {
		this.datumPublisher = datumPublisher;
	}

	/**
	 * Get an action processor to handle instructions with.
	 * 
	 * @return the action processor
	 */
	public ActionMessageProcessor<JsonNode, Void> getInstructionHandler() {
		return instructionHandler;
	}

	/**
	 * Set an action processor to handle instructions with.
	 * 
	 * <p>
	 * If this is configured, then instruction handling will be delegated to
	 * this service.
	 * </p>
	 * 
	 * @param instructionHandler
	 *        the handler
	 */
	public void setInstructionHandler(ActionMessageProcessor<JsonNode, Void> instructionHandler) {
		this.instructionHandler = instructionHandler;
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
