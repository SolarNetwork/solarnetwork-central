/* ==================================================================
 * CentralOcppNodeInstructionManager.java - 2/07/2024 4:36:24 pm
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

package net.solarnetwork.central.in.ocpp.json;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_ACTION_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGER_IDENTIFIER_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGE_POINT_ID_PARAM;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.domain.CentralOcppUserEvents;
import net.solarnetwork.central.ocpp.domain.OcppAppEvents;
import net.solarnetwork.central.ocpp.util.OcppInstructionUtils;
import net.solarnetwork.central.support.DelayedOccasionalProcessor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.event.AppEvent;
import net.solarnetwork.event.AppEventHandler;
import net.solarnetwork.ocpp.domain.Action;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.BasicActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.util.StatTracker;

/**
 * Manage queued OCPP node instructions asynchronously, when OCPP clients
 * connect.
 * 
 * @author matt
 * @version 1.0
 */
public class CentralOcppNodeInstructionProvider extends
		DelayedOccasionalProcessor<CentralOcppNodeInstructionProvider.DelayedChargePointIdentifier>
		implements AppEventHandler, CentralOcppUserEvents {

	/** The {@code flushDelay} property default value. */
	public static final Duration DEFAULT_DELAY = Duration.ofSeconds(2);

	/** The default value for the {@code updateDelay} property. */
	public static final long DEFAULT_UPDATE_DELAY = 0;

	/** The default value for the {@code statLogUpdateCount} property. */
	public static final int DEFAULT_STAT_LOG_UPDATE_COUNT = 500;

	/** The default value for the {@code recoveryDelay} property. */
	public static final long DEFAULT_RECOVERY_DELAY = 5000;

	/** The {@code bufferRemovalLagAlertThreshold} default value. */
	public static final int DEFAULT_REMOVAL_LAG_ALERT_THRESHOLD = 500;

	private static final Logger log = LoggerFactory.getLogger(CentralOcppNodeInstructionProvider.class);

	private final Function<String, Action> actionResolver;
	private final ObjectMapper objectMapper;
	private final ActionPayloadDecoder actionPayloadDecoder;
	private final ChargePointBroker chargePointBroker;
	private final CentralChargePointDao chargePointDao;
	private final NodeInstructionDao instructionDao;

	private String instructionTopic;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock
	 * @param stats
	 *        the stats
	 * @param queue
	 *        the queue
	 * @param actionResolver
	 *        the action resolver
	 * @param objectMapper
	 *        the object mapper
	 * @param actionPayloadDecoder
	 *        the action payload decoder
	 * @param chargePointBroker
	 *        the charge point broker
	 * @param chargePointDao
	 *        the charge point DAO
	 * @param instructionDao
	 *        the instruction DAO
	 */
	public CentralOcppNodeInstructionProvider(Clock clock, StatTracker stats, TaskScheduler scheduler,
			BlockingQueue<DelayedChargePointIdentifier> queue, Function<String, Action> actionResolver,
			ObjectMapper objectMapper, ActionPayloadDecoder actionPayloadDecoder,
			ChargePointBroker chargePointBroker, CentralChargePointDao chargePointDao,
			NodeInstructionDao instructionDao) {
		super(clock, stats, scheduler, queue);
		this.actionResolver = requireNonNullArgument(actionResolver, "actionResolver");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.actionPayloadDecoder = requireNonNullArgument(actionPayloadDecoder, "actionPayloadDecoder");
		this.chargePointDao = requireNonNullArgument(chargePointDao, "chargePointDao");
		this.chargePointBroker = requireNonNullArgument(chargePointBroker, "chargePointBroker");
		this.instructionDao = requireNonNullArgument(instructionDao, "instructionDao");
	}

	@Override
	public void handleEvent(AppEvent event) {
		ChargePointIdentity identity = event.getProperty(OcppAppEvents.EVENT_PROP_CHARGE_POINT_IDENTITY,
				ChargePointIdentity.class);
		switch (event.getTopic()) {
			case OcppAppEvents.EVENT_TOPIC_CHARGE_POINT_CONNECTED -> asyncProcessItem(
					new DelayedChargePointIdentifier(identity));
			case OcppAppEvents.EVENT_TOPIC_CHARGE_POINT_DISCONNECTED -> cancelAsyncProcessItem(
					new DelayedChargePointIdentifier(identity));
			default -> {
				break;
			}
		}
	}

	@Override
	protected void processItemInternal(DelayedChargePointIdentifier item) {
		final ChargePointIdentity identity = item.ident;
		final String topic = getInstructionTopic();
		if ( chargePointDao == null || instructionDao == null || topic == null ) {
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
			FilterResults<EntityMatch, Long> matches = instructionDao.findFiltered(filter, null, null,
					null);
			for ( EntityMatch match : matches ) {
				Instruction instruction;
				if ( match instanceof Instruction ) {
					instruction = (Instruction) match;
				} else {
					instruction = instructionDao.get(match.getId());
				}
				if ( instruction != null && topic.equals(instruction.getTopic()) ) {
					processInstruction(instruction, identity, cp);
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

	private Map<String, String> instructionParameterMap(final Instruction instruction) {
		Map<String, String> params = instruction.getParams();
		return (params != null ? params : new HashMap<>(0));
	}

	private void processInstruction(final Instruction instruction, final ChargePointIdentity identity,
			CentralChargePoint cp) {
		Map<String, String> params = instructionParameterMap(instruction);
		Action action = actionResolver.apply(params.remove(OCPP_ACTION_PARAM));
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
		stats.increment(CentralOcppNodeInstructionStatusCount.InstructionsProcessed);
		OcppInstructionUtils.decodeJsonOcppInstructionMessage(objectMapper, action, params,
				actionPayloadDecoder, (e, jsonPayload, payload) -> {
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
					chargePointBroker.sendMessageToChargePoint(message, (msg, res, err) -> {
						if ( err != null ) {
							stats.increment(CentralOcppNodeInstructionStatusCount.InstructionsFailed);
							Throwable root = err;
							while ( root.getCause() != null ) {
								root = root.getCause();
							}
							Map<String, Object> data = singletonMap(ERROR_DATA_KEY, format(
									"Error handling OCPP action %s: %s", action, root.getMessage()));
							if ( instructionDao.compareAndUpdateInstructionState(instruction.getId(),
									cp.getNodeId(), InstructionState.Executing,
									InstructionState.Declined, data) ) {
								generateUserEvent(cp.getUserId(), CHARGE_POINT_INSTRUCTION_ERROR_TAGS,
										"Error handling OCPP action", data);
							}
						} else {
							Map<String, Object> resultParameters = null;
							if ( res != null ) {
								resultParameters = JsonUtils
										.getStringMapFromTree(objectMapper.valueToTree(res));
							}
							stats.increment(CentralOcppNodeInstructionStatusCount.InstructionsSent);
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

	private void generateUserEvent(Long userId, String[] tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr;
		try {
			dataStr = (data instanceof String ? (String) data : objectMapper.writeValueAsString(data));
		} catch ( JsonProcessingException e ) {
			dataStr = null;
		}
		LogEventInfo event = new LogEventInfo(tags, message, dataStr);
		biz.addEvent(userId, event);
	}

	/**
	 * A delayed charge point identifier.
	 */
	public final class DelayedChargePointIdentifier implements Delayed {

		private final Instant ready;
		private final ChargePointIdentity ident;

		private DelayedChargePointIdentifier(ChargePointIdentity ident) {
			super();
			this.ready = clock.instant().plus(CentralOcppNodeInstructionProvider.this.getDelay());
			this.ident = ident;
		}

		@Override
		public int hashCode() {
			return Objects.hash(ident);
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( !(obj instanceof DelayedChargePointIdentifier) ) {
				return false;
			}
			DelayedChargePointIdentifier other = (DelayedChargePointIdentifier) obj;
			return Objects.equals(ident, other.ident);
		}

		@Override
		public int compareTo(Delayed o) {
			// not bothering to check instanceof for performance
			DelayedChargePointIdentifier other = (DelayedChargePointIdentifier) o;
			int result = ready.compareTo(other.ready);
			if ( result == 0 ) {
				result = ident.compareTo(other.ident);
			}
			return result;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return clock.instant().until(ready, unit.toChronoUnit());
		}

	}

	/**
	 * Get the instruction topic to listen to for OCPP messages.
	 * 
	 * @return the instruction topic to listen to, or {@literal null} to not
	 *         look for OCPP instructions
	 */
	public String getInstructionTopic() {
		return instructionTopic;
	}

	/**
	 * Set the instruction topic to listen to for OCPP messages.
	 * 
	 * @param instructionTopic
	 *        the instruction topic to set
	 */
	public void setInstructionTopic(String instructionTopic) {
		this.instructionTopic = instructionTopic;
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
