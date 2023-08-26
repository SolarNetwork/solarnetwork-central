/* ==================================================================
 * MqttNodeInstructionQueueHook.java - 11/11/2021 3:41:40 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.instructor.dao.mqtt;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionQueueHook;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttStats;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * MQTT implementation of {@link NodeInstructionQueueHook}.
 * 
 * <p>
 * This hook will modify queued instructions into the
 * {@link InstructionState#Queuing} state, and then attempt to post the
 * instruction to the connected MQTT broker.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
public class MqttNodeInstructionQueueHook extends BaseMqttConnectionObserver
		implements NodeInstructionQueueHook {

	/**
	 * The default MQTT topic template for node instruction publication.
	 * 
	 * <p>
	 * This template will be passed a single node ID parameter.
	 * </p>
	 */
	public static final String DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE = "node/%s/instr";

	private final ObjectMapper objectMapper;
	private final Executor executor;
	private final NodeInstructionDao nodeInstructionDao;
	private String nodeInstructionTopicTemplate = DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE;

	/**
	 * Constructor.
	 * 
	 * @param objectMapper
	 *        object mapper for messages
	 * @param executor
	 *        an executor
	 * @param nodeInstructionDao
	 *        node instruction DAO
	 * @param mqttStats
	 *        the MQTT stats to use; must support the
	 *        {@link NodeInstructionQueueHookStat} stats
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MqttNodeInstructionQueueHook(ObjectMapper objectMapper, Executor executor,
			NodeInstructionDao nodeInstructionDao, MqttStats mqttStats) {
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.executor = requireNonNullArgument(executor, "executor");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		setDisplayName("NodeInstructionQueueHook MQTT");
		setMqttStats(requireNonNullArgument(mqttStats, "mqttStats"));
	}

	@Override
	public NodeInstruction willQueueNodeInstruction(NodeInstruction instruction) {
		if ( instruction != null && instruction.getNodeId() != null
				&& InstructionState.Queued == instruction.getState() ) {
			// we will change this state to Queuing so batch processing does not pick up
			instruction.setState(InstructionState.Queuing);
		}
		return instruction;
	}

	@Override
	public void didQueueNodeInstruction(NodeInstruction instruction, Long instructionId) {
		if ( instruction != null && instruction.getNodeId() != null && instructionId != null
				&& InstructionState.Queuing == instruction.getState() ) {
			try {
				executor.execute(new PublishNodeInstructionTask(instruction, instructionId));
			} catch ( JsonProcessingException e ) {
				log.error("Error encoding node instruction {} for MQTT payload: {}", instruction.getId(),
						e.getMessage());
			}
		}
	}

	private class PublishNodeInstructionTask implements Runnable {

		private final Long instructionId;
		private final Long nodeId;
		private final String topic;
		private final byte[] payload;

		private PublishNodeInstructionTask(NodeInstruction instruction, Long instructionId)
				throws JsonProcessingException {
			super();
			// create copy with ID set
			this.instructionId = instructionId;
			this.nodeId = instruction.getNodeId();
			this.topic = String.format(nodeInstructionTopicTemplate, instruction.getNodeId());
			Map<String, Object> data = Collections.singletonMap("instructions",
					Collections.singleton(instruction));
			this.payload = objectMapper.writeValueAsBytes(data);
		}

		@Override
		public void run() {
			try {
				MqttConnection conn = mqttConnection.get();
				if ( conn != null ) {
					Future<?> f = conn
							.publish(new BasicMqttMessage(topic, false, getPublishQos(), payload));
					f.get(getPublishTimeoutSeconds(), TimeUnit.SECONDS);
					getMqttStats().incrementAndGet(NodeInstructionQueueHookStat.InstructionsPublished);
				} else {
					throw new RuntimeException("MQTT connection not available");
				}
			} catch ( Exception e ) {
				// error delivering instruction so change state to Queued to fall back to batch processing
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				if ( (e instanceof IOException) || (e instanceof TimeoutException) ) {
					log.info(
							"Failed to publish MQTT instruction {} to node {}, falling back to batch mode: {}",
							instructionId, nodeId, root.toString());
				} else {
					log.error(
							"Failed to publish MQTT instruction {} to node {}, falling back to batch mode: {}",
							instructionId, nodeId, root.toString(), e);
				}
				nodeInstructionDao.compareAndUpdateInstructionState(instructionId, nodeId,
						InstructionState.Queuing, InstructionState.Queued, null);
			}
		}
	}

	/**
	 * Set the node instruction topic template.
	 * 
	 * <p>
	 * This topic template is used when publishing instructions to nodes. The
	 * template will be passed a single node ID parameter.
	 * </p>
	 * 
	 * @param nodeInstructionTopicTemplate
	 *        the template to use; defaults to
	 *        {@link #DEFAULT_NODE_INSTRUCTION_TOPIC_TEMPLATE}
	 */
	public void setNodeInstructionTopicTemplate(String nodeInstructionTopicTemplate) {
		this.nodeInstructionTopicTemplate = nodeInstructionTopicTemplate;
	}

}
