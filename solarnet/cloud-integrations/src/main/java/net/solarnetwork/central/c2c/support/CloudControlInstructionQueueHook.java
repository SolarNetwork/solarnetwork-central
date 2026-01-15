/* ==================================================================
 * CloudControlInstructionQueueHook.java - 14/11/2025 6:51:30 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.support;

import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queuing;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.biz.CommonInstructionTopic;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.util.StatTracker;

/**
 * Intercept instructions to cloud controls, and direct the instruction to its
 * associated service.
 *
 * <p>
 * When {@link #willQueueNodeInstruction(NodeInstruction)} is called, if the
 * instruction's state is {@code Queued} still and the given {@code topic} is
 * supported by <i>some</li> {@link CloudControlService}, then if a matching
 * {@link CloudControlConfiguration} can be found based on the node ID, topic,
 * and optional control ID, a new instruction whose state has been changed to
 * {@code Queuing} will be returned.
 * </p>
 *
 * <p>
 * Then in the {@link #didQueueNodeInstruction(NodeInstruction, Long)} the
 * updated instruction will be passed to the {@link CloudControlService} and its
 * result will be saved.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class CloudControlInstructionQueueHook extends BasicIdentifiable
		implements NodeInstructionQueueHook, CloudIntegrationsUserEvents {

	/** Error code when an instruction state is not changed from Queuing. */
	public static final String ERROR_CODE_INSTRUCTION_NOT_HANDLED = "CIQH.0001";

	/** Error code when an instruction throws an exception. */
	public static final String ERROR_CODE_INSTRUCTION_THREW_EXCEPTION = "CIQH.0002";

	private static final Logger log = LoggerFactory.getLogger(CloudControlInstructionQueueHook.class);

	private final StatTracker stats;
	private final SolarNodeOwnershipDao nodeOwneshipDao;
	private final CloudControlConfigurationDao controlDao;
	private final NodeInstructionDao nodeInstructionDao;
	private final Map<String, CloudControlService> controlServices;
	private final Set<String> supportedTopics;
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 *
	 * @param stats
	 *        the stats to use
	 * @param nodeOwneshipDao
	 *        the node ownership DAO to use
	 * @param controlDao
	 *        the control DAO to use
	 * @param nodeInstructionDao
	 *        the node instruction DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudControlInstructionQueueHook(StatTracker stats, SolarNodeOwnershipDao nodeOwneshipDao,
			CloudControlConfigurationDao controlDao, NodeInstructionDao nodeInstructionDao,
			Collection<CloudControlService> controlServices) {
		super();
		this.stats = requireNonNullArgument(stats, "stats");
		this.nodeOwneshipDao = requireNonNullArgument(nodeOwneshipDao, "nodeOwneshipDao");
		this.controlDao = requireNonNullArgument(controlDao, "controlDao");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		setDisplayName("CloudControl Instruction Publisher");

		Map<String, CloudControlService> serviceMap = new HashMap<>(controlServices.size());
		Set<String> allTopics = new HashSet<>(controlServices.size() * 4);
		for ( CloudControlService service : controlServices ) {
			serviceMap.put(service.getId(), service);
			allTopics.addAll(service.supportedTopics());
		}
		this.controlServices = Collections.unmodifiableMap(serviceMap);
		this.supportedTopics = Collections.unmodifiableSet(allTopics);
	}

	private static final class CloudControlNodeInstruction extends NodeInstruction {

		@Serial
		private static final long serialVersionUID = 2907749503777764999L;

		private final CloudControlConfiguration control;
		private final Map<String, Object> eventData;

		private CloudControlNodeInstruction(NodeInstruction instruction, InstructionState state,
				CloudControlConfiguration control, Map<String, Object> eventData) {
			super(instruction);
			getInstruction().setState(state);
			this.control = control;
			this.eventData = eventData;
		}

	}

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		if ( instruction == null || instruction.getNodeId() == null
				|| instruction.getInstruction().getState() != InstructionState.Queued ) {
			return instruction;
		}

		// verify topic is supported
		final String topic = instruction.getInstruction().getTopic();
		if ( !supportedTopics.contains(topic) ) {
			return instruction;
		}

		// find user that owns node in instruction
		final Long nodeId = instruction.getNodeId();
		final SolarNodeOwnership nodeOwner = nodeOwneshipDao.ownershipForNodeId(nodeId);
		if ( nodeOwner == null ) {
			log.trace("Node ownership not found for node {}; ignoring instruction {}", nodeId, topic);
			return instruction;
		}

		// find all cloud controls for this user + node + (optional) control ID, use first match
		final var filter = new BasicFilter();
		filter.setUserId(nodeOwner.getUserId());
		filter.setNodeId(nodeId);
		filter.setControlIds(findControlIds(topic, instruction.getInstruction()));
		final var controls = controlDao.findFiltered(filter);
		for ( CloudControlConfiguration control : controls ) {
			final String serviceId = control.getServiceIdentifier();
			final CloudControlService service = controlServices.get(serviceId);
			if ( service != null && service.supportedTopics().contains(topic) ) {
				final Map<String, Object> eventData = new HashMap<>(4);
				eventData.put(CONFIG_ID_DATA_KEY, control.getConfigId());
				eventData.put(INTEGRATION_ID_DATA_KEY, control.getIntegrationId());
				return new CloudControlNodeInstruction(instruction, Queuing, control, eventData);
			}
		}

		return instruction;
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( !(instruction instanceof CloudControlNodeInstruction instr) ) {
			return;
		}

		instr.eventData.put(INSTRUCTION_ID_DATA_KEY, instructionId);

		final String serviceId = instr.control.getServiceIdentifier();
		final CloudControlService service = controlServices.get(serviceId);
		try {
			InstructionStatus status = service.executeInstruction(instr.control.getId(), instruction);
			final InstructionState newState = status != null
					&& status.getInstructionState() != InstructionState.Queuing
							? status.getInstructionState()
							: InstructionState.Declined;
			final var resultParams = new LinkedHashMap<String, Object>(
					status != null && status.getResultParameters() != null ? status.getResultParameters()
							: Map.of());

			instruction.getInstruction().setState(newState);
			instruction.getInstruction().setResultParameters(resultParams);

			if ( nodeInstructionDao.compareAndUpdateInstructionState(instructionId,
					instruction.getNodeId(), Queuing, newState,
					!resultParams.isEmpty() ? resultParams : null) ) {
				incrementInstructionExecutedStat(instr.getInstruction().getTopic(), newState);

				instr.eventData.put(INSTRUCTION_STATE_DATA_KEY, newState.toString());
				if ( status == null || status.getInstructionState() == InstructionState.Queuing ) {
					instr.eventData.put(ERROR_CODE_DATA_KEY, ERROR_CODE_INSTRUCTION_NOT_HANDLED);
				}
				generateUserEvent(instr.control.getUserId(), INTEGRATION_CONTROL_INSTRUCTION_TAGS, null,
						instr.eventData);
			}

		} catch ( Exception e ) {
			log.warn("Error processing cloud control {} instruction {} topic {}: {}",
					instr.control.getIntegrationId(), instructionId, instr.getInstruction().getTopic(),
					e.toString());
			final Map<String, Object> resultParams = Map.of(MESSAGE_DATA_KEY,
					"Error processing instruction: " + e.getMessage());
			instruction.getInstruction().setState(Declined);
			instruction.getInstruction().setResultParameters(resultParams);
			if ( nodeInstructionDao.compareAndUpdateInstructionState(instructionId,
					instruction.getNodeId(), Queuing, Declined, resultParams) ) {
				instr.eventData.put(INSTRUCTION_STATE_DATA_KEY, Declined.toString());
				instr.eventData.put(ERROR_CODE_DATA_KEY, ERROR_CODE_INSTRUCTION_THREW_EXCEPTION);
				incrementInstructionErrorStat(instr.getInstruction().getTopic());
				generateUserEvent(instr.control.getUserId(), INTEGRATION_CONTROL_INSTRUCTION_ERROR_TAGS,
						"Instruction failed", instr.eventData);
			}
		}
	}

	/**
	 * Look for a device control IDs based on known instructions.
	 *
	 * @param instruction
	 *        the instruction to find the control IDs for
	 * @return the control IDs, or {@code null} if none found
	 */
	private String[] findControlIds(final String topic, final Instruction instruction) {
		final CommonInstructionTopic t = CommonInstructionTopic.findForTopic(topic);
		return switch (t) {
			case null -> null;
			case SetControlParameter -> extractParameterKeys(instruction);
			default -> null;
		};
	}

	/**
	 * Treat all instruction parameter names as control IDs and return them.
	 *
	 * @param instruction
	 *        the instruction to extract
	 * @return all parameter keys, or {@code null} if no parameters are
	 *         available
	 */
	private String[] extractParameterKeys(Instruction instruction) {
		final List<InstructionParameter> params = instruction.getParameters();
		if ( params == null || params.isEmpty() ) {
			return null;
		}
		String[] keys = new String[params.size()];
		for ( ListIterator<InstructionParameter> itr = params.listIterator(); itr.hasNext(); ) {
			keys[itr.nextIndex()] = itr.next().getName();
		}
		return keys;
	}

	private void incrementInstructionExecutedStat(String topic, InstructionState resultState) {
		stats.increment(CloudControlInstructionCountStat.InstructionsExecuted);
		stats.increment(topic + "Executed");
		stats.increment(topic + resultState.name());
	}

	private void incrementInstructionErrorStat(String topic) {
		stats.increment(CloudControlInstructionCountStat.InstructionsExecuted);
		stats.increment(CloudControlInstructionCountStat.InstructionErrors);
		stats.increment(topic + "Executed");
		stats.increment(topic + "Errors");
	}

	private void generateUserEvent(Long userId, List<String> tags, String message, Object data) {
		final UserEventAppenderBiz biz = getUserEventAppenderBiz();
		if ( biz == null ) {
			return;
		}
		String dataStr = (data instanceof String s ? s : JsonUtils.getJSONString(data, null));
		LogEventInfo event = LogEventInfo.event(tags, message, dataStr);
		biz.addEvent(userId, event);
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
