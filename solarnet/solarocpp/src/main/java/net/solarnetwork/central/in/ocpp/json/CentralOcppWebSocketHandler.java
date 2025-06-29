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

import static net.solarnetwork.central.ocpp.domain.OcppAppEvents.connectionEvent;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.ApplicationMetadata;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointActionStatusUpdateDao;
import net.solarnetwork.central.ocpp.dao.ChargePointStatusDao;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.event.AppEventPublisher;
import net.solarnetwork.ocpp.domain.Action;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ErrorCode;
import net.solarnetwork.ocpp.domain.ErrorCodeException;
import net.solarnetwork.ocpp.domain.PendingActionMessage;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.service.ActionMessageQueue;
import net.solarnetwork.ocpp.service.ErrorCodeResolver;
import net.solarnetwork.ocpp.web.jakarta.json.OcppWebSocketHandler;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StatTracker;

/**
 * Extension of {@link OcppWebSocketHandler} to support queued instructions.
 * 
 * @author matt
 * @version 2.9
 * @since 1.1
 */
public class CentralOcppWebSocketHandler<C extends Enum<C> & Action, S extends Enum<S> & Action>
		extends OcppWebSocketHandler<C, S>
		implements ServiceLifecycleObserver, CentralOcppUserEvents, SmartLifecycle {

	/** The {@code shutdownTaskMaxWait} property default value (1 minute). */
	public static final Duration DEFAULT_SHUTDOWN_TASK_MAX_WAIT = Duration.ofMinutes(1);

	/** The {@code shutdownTaskPostDelay} property default value (5 seconds). */
	public static final Duration DEFAULT_SHUTDOWN_TASK_POST_DELAY = Duration.ofSeconds(5);

	/** The default value for the statistic logging. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	private final Clock clock;
	private CentralChargePointDao chargePointDao;
	private NodeInstructionDao instructionDao;
	private UserEventAppenderBiz userEventAppenderBiz;
	private ChargePointStatusDao chargePointStatusDao;
	private ChargePointActionStatusUpdateDao chargePointActionStatusUpdateDao;
	private Function<Object, ChargePointConnectorKey> connectorIdExtractor;
	private ApplicationMetadata applicationMetadata;
	private String instructionTopic;

	private AppEventPublisher eventPublisher;
	private CountDownLatch shutdownTaskLatch;
	private Duration shutdownTaskMaxWait = DEFAULT_SHUTDOWN_TASK_MAX_WAIT;
	private Duration shutdownTaskPostDelay = DEFAULT_SHUTDOWN_TASK_POST_DELAY;

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
	 *        the mapper
	 * @param pendingMessageQueue
	 *        a queue to hold pending messages, for individual client IDs
	 * @param centralServiceActionPayloadDecoder
	 *        the action payload decoder to use
	 * @param chargePointActionPayloadDecoder
	 *        for Central Service message the action payload decoder to use for
	 *        Charge Point messages
	 * @param subProtocols
	 *        the WebSocket sub-protocols
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public CentralOcppWebSocketHandler(Class<C> chargePointActionClass,
			Class<S> centralSystemActionClass, ErrorCodeResolver errorCodeResolver,
			AsyncTaskExecutor executor, ObjectMapper mapper, ActionMessageQueue pendingMessageQueue,
			ActionPayloadDecoder centralServiceActionPayloadDecoder,
			ActionPayloadDecoder chargePointActionPayloadDecoder, String... subProtocols) {
		this(Clock.systemUTC(),
				new StatTracker("OCPP WebSocket Handler " + arrayToCommaDelimitedString(subProtocols),
						CentralOcppWebSocketHandler.class.getName() + "-"
								+ arrayToCommaDelimitedString(subProtocols),
						LoggerFactory.getLogger(CentralOcppWebSocketHandler.class),
						DEFAULT_STAT_LOG_UPDATE_COUNT),
				chargePointActionClass, centralSystemActionClass, errorCodeResolver, executor, mapper,
				pendingMessageQueue, centralServiceActionPayloadDecoder, chargePointActionPayloadDecoder,
				subProtocols);
	}

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param stats
	 *        the stats to use
	 * @param chargePointActionClass
	 *        the charge point action class
	 * @param centralSystemActionClass
	 *        the central system action class
	 * @param errorCodeResolver
	 *        the error code resolver
	 * @param executor
	 *        an executor for tasks
	 * @param mapper
	 *        the mapper
	 * @param pendingMessageQueue
	 *        a queue to hold pending messages, for individual client IDs
	 * @param centralServiceActionPayloadDecoder
	 *        the action payload decoder to use
	 * @param chargePointActionPayloadDecoder
	 *        for Central Service message the action payload decoder to use for
	 *        Charge Point messages
	 * @param subProtocols
	 *        the WebSocket sub-protocols
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public CentralOcppWebSocketHandler(Clock clock, StatTracker stats, Class<C> chargePointActionClass,
			Class<S> centralSystemActionClass, ErrorCodeResolver errorCodeResolver,
			AsyncTaskExecutor executor, ObjectMapper mapper, ActionMessageQueue pendingMessageQueue,
			ActionPayloadDecoder centralServiceActionPayloadDecoder,
			ActionPayloadDecoder chargePointActionPayloadDecoder, String... subProtocols) {
		super(stats, chargePointActionClass, centralSystemActionClass, errorCodeResolver, executor,
				mapper, pendingMessageQueue, centralServiceActionPayloadDecoder,
				chargePointActionPayloadDecoder, subProtocols);
		this.clock = requireNonNullArgument(clock, "clock");
	}

	@Override
	public void serviceDidStartup() {
		if ( isStarted() ) {
			return;
		}
		shutdownTaskLatch = null;
		super.startup();
	}

	@Override
	public void serviceDidShutdown() {
		if ( !isStarted() ) {
			return;
		}
		super.shutdown();
	}

	@Override
	public void start() {
		// unused
	}

	@Override
	public void stop() {
		serviceDidShutdown();
	}

	@Override
	public boolean isRunning() {
		return isStarted();
	}

	@Override
	protected void disconnectClients() {
		final int connectedChargerCount = availableChargePointsIds().size();
		final CountDownLatch latch;
		if ( connectedChargerCount > 0 ) {
			latch = (shutdownTaskLatch != null ? shutdownTaskLatch
					: new CountDownLatch(connectedChargerCount));
			if ( shutdownTaskLatch == null ) {
				shutdownTaskLatch = latch;
			}
		} else {
			latch = null;
		}
		super.disconnectClients();
		if ( latch != null ) {
			try {
				if ( shutdownTaskMaxWait.isPositive() ) {
					log.info("Waiting at most {}s for charger disonnections to complete.",
							shutdownTaskMaxWait.toSeconds());
					latch.await(shutdownTaskMaxWait.toMillis(), TimeUnit.MILLISECONDS);
				}
				if ( shutdownTaskPostDelay.isPositive() ) {
					log.info("Waiting for {}s for asynchronous shutdown tasks to complete.",
							shutdownTaskPostDelay.toSeconds());
					Thread.sleep(shutdownTaskPostDelay);
				}
			} catch ( InterruptedException e ) {
				// ignore
			}
		}
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
									appMeta.getInstanceId(), session.getId(), Instant.now(), true);
						} catch ( RuntimeException e ) {
							log.error("Error updating charger {} connection status", clientId, e);
						}
					}
				}
				Map<String, Object> data = Map.of(CHARGE_POINT_DATA_KEY, clientId.getIdentifier(),
						SESSION_ID_DATA_KEY, session.getId());
				generateUserEvent(userId, CHARGE_POINT_CONNECTED_TAGS, null, data);
			}
			final var eventPublisher = getEventPublisher();
			if ( eventPublisher != null ) {
				eventPublisher.postEvent(connectionEvent(clock.instant(), clientId, true));
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		ChargePointIdentity clientId = clientId(session);
		if ( clientId != null ) {
			final var eventPublisher = getEventPublisher();
			if ( eventPublisher != null ) {
				eventPublisher.postEvent(connectionEvent(clock.instant(), clientId, false));
			}
			if ( clientId.getUserIdentifier() instanceof Long userId ) {
				final ApplicationMetadata appMeta = getApplicationMetadata();
				if ( appMeta != null && appMeta.getInstanceId() != null ) {
					final ChargePointStatusDao statusDao = getChargePointStatusDao();
					if ( statusDao != null ) {
						try {
							statusDao.updateConnectionStatus(userId, clientId.getIdentifier(),
									appMeta.getInstanceId(), session.getId(), Instant.now(), false);
						} catch ( RuntimeException e ) {
							log.error("Error updating charger {} disconnection status", clientId, e);
						}
					}
				}
				Map<String, Object> data = Map.of(CHARGE_POINT_DATA_KEY, clientId.getIdentifier(),
						SESSION_ID_DATA_KEY, session.getId());
				generateUserEvent(userId, CHARGE_POINT_DISCONNECTED_TAGS, null, data);
			}
		}
		super.afterConnectionClosed(session, status);
		final CountDownLatch latch = this.shutdownTaskLatch;
		if ( latch != null ) {
			latch.countDown();
		}
	}

	@Override
	protected void willProcessRequest(PendingActionMessage msg) {
		super.willProcessRequest(msg);
		if ( msg.getMessage().getClientId().getUserIdentifier() instanceof Long userId ) {
			final String cpIdentifier = msg.getMessage().getClientId().getIdentifier();
			final Action action = msg.getMessage().getAction();
			final String msgId = msg.getMessage().getMessageId();
			final ChargePointActionStatusUpdateDao statusDao = getChargePointActionStatusUpdateDao();
			if ( statusDao != null ) {
				ChargePointConnectorKey connectorId = (connectorIdExtractor != null
						? connectorIdExtractor.apply(msg.getMessage().getMessage())
						: null);
				try {
					statusDao.updateActionTimestamp(userId, cpIdentifier,
							connectorId != null ? connectorId.getEvseId() : null,
							connectorId != null ? connectorId.getConnectorId() : null, action.getName(),
							msgId, Instant.now());
				} catch ( RuntimeException e ) {
					log.error("Error updating charger {} connector {} {} status",
							msg.getMessage().getClientId(), connectorId, action, e);
				}
			}

			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, cpIdentifier);
			data.put(MESSAGE_ID_DATA_KEY, msgId);
			data.put(ACTION_DATA_KEY, action);
			data.put(MESSAGE_DATA_KEY, msg.getMessage().getMessage());
			generateUserEvent(userId, CHARGE_POINT_MESSAGE_RECEIVED_TAGS, null, data);
		}
	}

	@Override
	protected void willProcessCallResponse(PendingActionMessage msg, Object payload,
			Throwable exception) {
		super.willProcessCallResponse(msg, payload, exception);
		if ( msg.getMessage().getClientId().getUserIdentifier() instanceof Long userId ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, msg.getMessage().getClientId().getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, msg.getMessage().getMessageId());
			data.put(ACTION_DATA_KEY, msg.getMessage().getAction());
			data.put(MESSAGE_DATA_KEY, payload);
			if ( exception != null ) {
				data.put(ERROR_DATA_KEY, exception.getMessage());
				if ( exception instanceof ErrorCodeException err ) {
					data.put("errorCode", err.getErrorCode());
					data.put("errorDescription", err.getErrorDescription());
					data.put("errorDetails", err.getErrorDetails());
				}
			}
			generateUserEvent(userId, CHARGE_POINT_MESSAGE_RECEIVED_TAGS, null, data);
		}
	}

	@Override
	protected void didSendCall(ChargePointIdentity clientId, String messageId, Action action,
			Object payload, String json, Throwable exception) {
		super.didSendCall(clientId, messageId, action, payload, json, exception);
		if ( clientId.getUserIdentifier() instanceof Long userId ) {
			Map<String, Object> data = new LinkedHashMap<>(4);
			data.put(CHARGE_POINT_DATA_KEY, clientId.getIdentifier());
			data.put(MESSAGE_ID_DATA_KEY, messageId);
			data.put(ACTION_DATA_KEY, action);
			data.put(MESSAGE_DATA_KEY, payload);
			if ( exception != null ) {
				data.put(ERROR_DATA_KEY, exception.getMessage());
			}
			generateUserEvent(userId, CHARGE_POINT_MESSAGE_SENT_TAGS, null, data);
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

	private void generateUserEvent(Long userId, List<String> tags, String message, Object data) {
		// bump to executor to not block processor thread
		executor.execute(() -> {
			final UserEventAppenderBiz biz = getUserEventAppenderBiz();
			if ( biz == null ) {
				return;
			}
			String dataStr;
			try {
				dataStr = (data instanceof String s ? s : getObjectMapper().writeValueAsString(data));
			} catch ( JsonProcessingException e ) {
				dataStr = null;
			}
			LogEventInfo event = LogEventInfo.event(tags, message, dataStr);
			biz.addEvent(userId, event);
		});
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

	/**
	 * Get the charge point action status DAO.
	 * 
	 * @return the DAO
	 * @since 2.7
	 */
	public ChargePointActionStatusUpdateDao getChargePointActionStatusUpdateDao() {
		return chargePointActionStatusUpdateDao;
	}

	/**
	 * Set the charge point action status DAO.
	 * 
	 * @param chargePointActionStatusUpdateDao
	 *        the DAO to set
	 * @since 2.7
	 */
	public void setChargePointActionStatusUpdateDao(
			ChargePointActionStatusUpdateDao chargePointActionStatusUpdateDao) {
		this.chargePointActionStatusUpdateDao = chargePointActionStatusUpdateDao;
	}

	/**
	 * Get the connector ID extractor.
	 * 
	 * <p>
	 * This function is responsible for extracting a charger connector ID from
	 * an action message body.
	 * </p>
	 * 
	 * @return the function
	 */
	public Function<Object, ChargePointConnectorKey> getConnectorIdExtractor() {
		return connectorIdExtractor;
	}

	/**
	 * Set the connector ID extractor.
	 * 
	 * @param connectorIdExtractor
	 *        the function to set
	 */
	public void setConnectorIdExtractor(Function<Object, ChargePointConnectorKey> connectorIdExtractor) {
		this.connectorIdExtractor = connectorIdExtractor;
	}

	/**
	 * Get the instruction topic to listen to for OCPP messages.
	 * 
	 * @return the instruction topic to listen to, or {@literal null} to not
	 *         look for OCPP instructions
	 * @since 2.6
	 */
	public String getInstructionTopic() {
		return instructionTopic;
	}

	/**
	 * Set the instruction topic to listen to for OCPP messages.
	 * 
	 * @param instructionTopic
	 *        the instruction topic to set
	 * @since 2.6
	 */
	public void setInstructionTopic(String instructionTopic) {
		this.instructionTopic = instructionTopic;
	}

	/**
	 * Get the maximum amount of time to wait for shutdown tasks to complete.
	 * 
	 * @return the maximum wait time; defaults to
	 *         {@link #DEFAULT_SHUTDOWN_TASK_MAX_WAIT}
	 * @since 2.7
	 */
	public Duration getShutdownTaskMaxWait() {
		return shutdownTaskMaxWait;
	}

	/**
	 * Set the maximum amount of time to wait for shutdown tasks to complete.
	 * 
	 * @param shutdownTaskMaxWait
	 *        the maximum wait time to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @since 2.7
	 */
	public void setShutdownTaskMaxWait(Duration shutdownTaskMaxWait) {
		this.shutdownTaskMaxWait = requireNonNullArgument(shutdownTaskMaxWait, "shutdownTaskMaxWait");
	}

	/**
	 * Get the delay to wait for when {@link #serviceDidShutdown()} is invoked,
	 * after all clients have disconnected, to give time for any asynchronous
	 * event processing to complete.
	 * 
	 * @return the delay; defaults to {@link #DEFAULT_SHUTDOWN_TASK_POST_DELAY}
	 * @since 2.7
	 */
	public Duration getShutdownTaskPostDelay() {
		return shutdownTaskPostDelay;
	}

	/**
	 * Set the delay to wait for when {@link #serviceDidShutdown()} is invoked,
	 * after all clients have disconnected, to give time for any asynchronous
	 * event processing to complete.
	 * 
	 * @param shutdownTaskPostDelay
	 *        the delay to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @since 2.7
	 */
	public void setShutdownTaskPostDelay(Duration shutdownTaskPostDelay) {
		this.shutdownTaskPostDelay = requireNonNullArgument(shutdownTaskPostDelay,
				"shutdownTaskPostDelay");
	}

	/**
	 * Get the event publisher service.
	 * 
	 * @return the service
	 * @since 2.8
	 */
	public AppEventPublisher getEventPublisher() {
		return eventPublisher;
	}

	/**
	 * Configure an event publisher for posting events.
	 * 
	 * @param eventPublisher
	 *        the optional event publisher
	 * @since 2.8
	 */
	public void setEventPublisher(AppEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

}
