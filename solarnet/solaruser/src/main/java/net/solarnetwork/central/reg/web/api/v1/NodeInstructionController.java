/* ==================================================================
 * NodeInstructionController.java - Nov 26, 2012 4:16:58 PM
 *
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.NodeInstructionSerializer;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.reg.config.JsonConfig;
import net.solarnetwork.central.support.FilteredResultsProcessor;
import net.solarnetwork.central.support.OutputSerializationSupportContext;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.codec.PropertySerializerRegistrar;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.Result;

/**
 * Controller for node instruction web service API.
 *
 * @author matt
 * @version 2.4
 */
@GlobalExceptionRestController
@Controller("v1nodeInstructionController")
@RequestMapping(value = "/api/v1/sec/instr")
public class NodeInstructionController {

	/** The {@code executionResultDelay} property default value. */
	public static final Duration DEFAULT_EXECUTION_RESULT_DELAY = Duration.ofMillis(500);

	/** The {@code executionResultMaxWait} property default value. */
	public static final Duration DEFAULT_EXECUTION_RESULT_MAX_WAIT = Duration.ofSeconds(60);

	private static final Map<String, Object> INSTRUCTION_EXEC_TIMEOUT_MESSAGE = Map
			.of(InstructionStatus.MESSAGE_RESULT_PARAM, "Timeout waiting for instruction result.");

	private final TaskExecutor taskExecutor;
	private final ObjectMapper objectMapper;
	private final ObjectMapper cborObjectMapper;
	private final PropertySerializerRegistrar propertySerializerRegistrar;
	private final InstructorBiz instructorBiz;
	private final NodeInstructionDao nodeInstructionDao;

	private Duration executionResultDelay = DEFAULT_EXECUTION_RESULT_DELAY;
	private Duration executionResultMaxWait = DEFAULT_EXECUTION_RESULT_MAX_WAIT;

	/**
	 * Constructor.
	 *
	 * @param taskExecutor
	 *        the task executor
	 * @param instructorBiz
	 *        the instructor service
	 * @param nodeInstructionDao
	 *        the instruction DAO
	 * @param objectMapper
	 *        the object mapper to use for JSON
	 * @param cborObjectMapper
	 *        the mapper to use for CBOR
	 * @param propertySerializerRegistrar
	 *        the registrar to use (may be {@literal null}
	 */
	public NodeInstructionController(TaskExecutor taskExecutor, InstructorBiz instructorBiz,
			NodeInstructionDao nodeInstructionDao, ObjectMapper objectMapper,
			@Qualifier(JsonConfig.CBOR_MAPPER) ObjectMapper cborObjectMapper,
			PropertySerializerRegistrar propertySerializerRegistrar) {
		super();
		this.taskExecutor = requireNonNullArgument(taskExecutor, "taskExecutor");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.nodeInstructionDao = requireNonNullArgument(nodeInstructionDao, "nodeInstructionDao");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.cborObjectMapper = requireNonNullArgument(cborObjectMapper, "cborObjectMapper");
		this.propertySerializerRegistrar = propertySerializerRegistrar;
	}

	/**
	 * View a single instruction, based on its primary key.
	 *
	 * @param instructionId
	 *        the ID of the instruction to view
	 * @return the instruction
	 */
	@RequestMapping(value = "/view", method = RequestMethod.GET, params = "!ids")
	@ResponseBody
	public Result<Instruction> viewInstruction(@RequestParam("id") Long instructionId) {
		Instruction instruction = instructorBiz.getInstruction(instructionId);
		return success(instruction);
	}

	/**
	 * View a set of instructions, based on their primary keys.
	 *
	 * @param instructionIds
	 *        the IDs of the instructions to view
	 * @return the instruction
	 * @since 1.2
	 */
	@RequestMapping(value = "/view", method = RequestMethod.GET, params = "ids")
	@ResponseBody
	public Result<List<NodeInstruction>> viewInstruction(@RequestParam("ids") Set<Long> instructionIds) {
		List<NodeInstruction> results = instructorBiz.getInstructions(instructionIds);
		return success(results);
	}

	/**
	 * Get a list of all active instructions for a specific node.
	 *
	 * @param nodeId
	 *        the ID of the node to get instructions for
	 * @return the active instructions for the node
	 */
	@RequestMapping(value = "/viewActive", method = RequestMethod.GET, params = "!nodeIds")
	@ResponseBody
	public Result<List<Instruction>> activeInstructions(@RequestParam("nodeId") Long nodeId) {
		List<Instruction> instructions = instructorBiz.getActiveInstructionsForNode(nodeId);
		return success(instructions);
	}

	/**
	 * Get a list of all active instructions for a set of nodes.
	 *
	 * @param nodeIds
	 *        the IDs of the nodes to get instructions for
	 * @return the active instructions for the nodes
	 * @since 1.2
	 */
	@RequestMapping(value = "/viewActive", method = RequestMethod.GET, params = "nodeIds")
	@ResponseBody
	public Result<List<NodeInstruction>> activeInstructions(@RequestParam("nodeIds") Set<Long> nodeIds) {
		List<NodeInstruction> instructions = instructorBiz.getActiveInstructionsForNodes(nodeIds);
		return success(instructions);
	}

	/**
	 * Get a list of all pending instructions for a specific node.
	 *
	 * @param nodeId
	 *        the ID of the node to get instructions for
	 * @return the pending instructions for the node
	 * @since 1.1
	 */
	@RequestMapping(value = "/viewPending", method = RequestMethod.GET, params = "!nodeIds")
	@ResponseBody
	public Result<List<Instruction>> pendingInstructions(@RequestParam("nodeId") Long nodeId) {
		List<Instruction> instructions = instructorBiz.getPendingInstructionsForNode(nodeId);
		return success(instructions);
	}

	/**
	 * Get a list of all pending instructions for a set of nodes.
	 *
	 * @param nodeIds
	 *        the IDs of the nodes to get instructions for
	 * @return the pending instructions for the nodes
	 * @since 1.2
	 */
	@RequestMapping(value = "/viewPending", method = RequestMethod.GET, params = "nodeIds")
	@ResponseBody
	public Result<List<NodeInstruction>> pendingInstructions(
			@RequestParam("nodeIds") Set<Long> nodeIds) {
		List<NodeInstruction> instructions = instructorBiz.getPendingInstructionsForNodes(nodeIds);
		return success(instructions);
	}

	/**
	 * Enqueue a new instruction.
	 *
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST, params = "!nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Result<NodeInstruction> queueInstruction(NodeInstruction input) {
		validateInstruction(input);
		NodeInstruction instr = instructorBiz.queueInstruction(input.getNodeId(), input);
		return success(instr);
	}

	/**
	 * Enqueue a new instruction.
	 *
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<NodeInstruction> queueInstructionBody(@RequestBody NodeInstruction input) {
		validateInstruction(input);
		NodeInstruction instr = instructorBiz.queueInstruction(input.getNodeId(), input);
		return success(instr);
	}

	/**
	 * Enqueue a new instruction.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param input
	 *        the other instruction data
	 * @return the node instruction
	 * @since 1.3
	 */
	@RequestMapping(value = "/add/{topic}", method = RequestMethod.POST, params = "!nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Result<NodeInstruction> queueInstruction(@PathVariable("topic") String topic,
			NodeInstruction input) {
		input.setTopic(topic);
		return queueInstruction(input);
	}

	/**
	 * Enqueue a new instruction.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param input
	 *        the other instruction data
	 * @return the node instruction
	 * @since 1.4
	 */
	@RequestMapping(value = "/add/{topic}", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Result<NodeInstruction> queueInstructionBody(@PathVariable("topic") String topic,
			@RequestBody NodeInstruction input) {
		input.setTopic(topic);
		validateInstruction(input);
		return queueInstruction(input);
	}

	/**
	 * Enqueue one instruction for multiple nodes.
	 *
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instructions
	 * @since 1.2
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST, params = "nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Result<List<NodeInstruction>> queueInstruction(@RequestParam("nodeIds") Set<Long> nodeIds,
			NodeInstruction input) {
		validateInstruction(input, nodeIds);
		List<NodeInstruction> results = instructorBiz.queueInstructions(nodeIds, input);
		return success(results);
	}

	/**
	 * Enqueue one instruction for multiple nodes.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instructions
	 * @since 1.3
	 */
	@RequestMapping(value = "/add/{topic}", method = RequestMethod.POST, params = "nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public Result<List<NodeInstruction>> queueInstruction(@PathVariable("topic") String topic,
			@RequestParam("nodeIds") Set<Long> nodeIds, NodeInstruction input) {
		input.setTopic(topic);
		return queueInstruction(nodeIds, input);
	}

	private void validateInstruction(NodeInstruction instr) {
		validateInstruction(instr, null);
	}

	private void validateInstruction(NodeInstruction instr, Set<Long> nodeIds) {
		if ( (nodeIds == null && instr.getNodeId() == null) || (nodeIds != null && nodeIds.isEmpty()) ) {
			throw new IllegalArgumentException("The nodeId parameter is required.");
		}
		if ( instr.getTopic() == null || instr.getTopic().isEmpty() ) {
			throw new IllegalArgumentException("The topic parameter is required.");
		}
	}

	/**
	 * Update the state of an existing instruction.
	 *
	 * @param instructionId
	 *        the ID of the instruction to update
	 * @param state
	 *        the desired state
	 * @return the response
	 */
	@RequestMapping(value = "/updateState", method = RequestMethod.POST, params = "!ids")
	@ResponseBody
	public Result<Void> updateInstructionState(@RequestParam("id") Long instructionId,
			@RequestParam("state") InstructionState state) {
		instructorBiz.updateInstructionState(instructionId, state);
		return success();
	}

	/**
	 * Update the state of an existing instruction.
	 *
	 * @param instructionIds
	 *        the IDs of the instructions to update
	 * @param state
	 *        the desired state
	 * @return the response
	 * @since 1.2
	 */
	@RequestMapping(value = "/updateState", method = RequestMethod.POST, params = "ids")
	@ResponseBody
	public Result<Void> updateInstructionState(@RequestParam("ids") Set<Long> instructionIds,
			@RequestParam("state") InstructionState state) {
		instructorBiz.updateInstructionsState(instructionIds, state);
		return success();
	}

	/**
	 * Query for a listing of datum.
	 *
	 * @param cmd
	 *        the query criteria
	 * @param accept
	 *        the HTTP accept header value
	 * @param response
	 *        the HTTP response
	 * @since 2.1
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public void listInstructions(final SimpleInstructionFilter cmd,
			@RequestHeader(HttpHeaders.ACCEPT) final String accept, final HttpServletResponse response)
			throws IOException {
		final List<MediaType> acceptTypes = MediaType.parseMediaTypes(accept);
		try (FilteredResultsProcessor<NodeInstruction> processor = WebUtils
				.filteredResultsProcessorForType(acceptTypes, response,
						new OutputSerializationSupportContext<>(objectMapper, cborObjectMapper,
								NodeInstructionSerializer.INSTANCE, propertySerializerRegistrar))) {
			instructorBiz.findFilteredNodeInstructions(cmd, processor);
		}
	}

	/**
	 * Execute an instruction.
	 *
	 * @param maxWaitMs
	 *        the maximum number of milliseconds to wait for the results
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 * @since 2.1
	 */
	@RequestMapping(value = "/exec", method = RequestMethod.POST, params = "!nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public DeferredResult<Result<NodeInstruction>> execInstruction(
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			NodeInstruction input) {
		validateInstruction(input);
		return handleAsyncResult(instructorBiz.queueInstruction(input.getNodeId(), input), maxWaitMs);
	}

	/**
	 * Execute an instruction.
	 *
	 * @param maxWaitMs
	 *        the maximum number of milliseconds to wait for the results
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 * @since 2.3
	 */
	@RequestMapping(value = "/exec", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public DeferredResult<Result<NodeInstruction>> execInstructionBody(
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			@RequestBody NodeInstruction input) {
		validateInstruction(input);
		return handleAsyncResult(instructorBiz.queueInstruction(input.getNodeId(), input), maxWaitMs);
	}

	/**
	 * Execute an instruction.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param maxWaitMs
	 *        the maximum number of milliseconds to wait for the results
	 * @param input
	 *        the other instruction data
	 * @return the node instruction
	 * @since 2.3
	 */
	@RequestMapping(value = "/exec/{topic}", method = RequestMethod.POST, params = "!nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public DeferredResult<Result<NodeInstruction>> execInstruction(@PathVariable("topic") String topic,
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			NodeInstruction input) {
		input.setTopic(topic);
		return execInstruction(maxWaitMs, input);
	}

	/**
	 * Execute an instruction.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param maxWaitMs
	 *        the maximum number of milliseconds to wait for the results
	 * @param input
	 *        the other instruction data
	 * @return the node instruction
	 * @since 2.3
	 */
	@RequestMapping(value = "/exec/{topic}", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public DeferredResult<Result<NodeInstruction>> execInstructionBody(
			@PathVariable("topic") String topic,
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			@RequestBody NodeInstruction input) {
		input.setTopic(topic);
		validateInstruction(input);
		return execInstruction(maxWaitMs, input);
	}

	/**
	 * Execute one instruction on multiple nodes.
	 *
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param maxWaitMs
	 *        an optional maximum number of milliseconds to wait for results
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instructions
	 * @since 2.3
	 */
	@RequestMapping(value = "/exec", method = RequestMethod.POST, params = "nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public DeferredResult<Result<List<NodeInstruction>>> execInstruction(
			@RequestParam("nodeIds") Set<Long> nodeIds,
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			NodeInstruction input) {
		validateInstruction(input, nodeIds);
		return handleAsyncResult(instructorBiz.queueInstructions(nodeIds, input), maxWaitMs);
	}

	/**
	 * Execute one instruction on multiple nodes.
	 *
	 * <p>
	 * This API call exists to help with API path-based security policy
	 * restrictions, to allow a policy to restrict which topics can be enqueued.
	 * </p>
	 *
	 * @param topic
	 *        the instruction topic
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param maxWaitMs
	 *        an optional maximum number of milliseconds to wait for results
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instructions
	 * @since 2.3
	 */
	@RequestMapping(value = "/exec/{topic}", method = RequestMethod.POST, params = "nodeIds",
			consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@ResponseBody
	public DeferredResult<Result<List<NodeInstruction>>> execInstruction(
			@PathVariable("topic") String topic, @RequestParam("nodeIds") Set<Long> nodeIds,
			@RequestParam(name = "resultMaxWait", required = false) Long maxWaitMs,
			NodeInstruction input) {
		input.setTopic(topic);
		return execInstruction(nodeIds, maxWaitMs, input);
	}

	private DeferredResult<Result<NodeInstruction>> handleAsyncResult(final NodeInstruction instruction,
			final Long maxWaitMs) {
		long allowedMaxWait = executionResultMaxWait.toMillis();
		Long maxWait = (maxWaitMs == null ? allowedMaxWait
				: Math.max(0, Math.min(allowedMaxWait, maxWaitMs)));
		var deferred = new DeferredResult<Result<NodeInstruction>>(maxWait + 200L, instruction);
		// use a virtual thread that can deal with long periods of waiting
		Thread.startVirtualThread(() -> {
			try {
				NodeInstruction result = waitForResult(instruction, maxWait);
				deferred.setResult(success(result));
			} catch ( Throwable t ) {
				// ignore
			}
		});
		return deferred;
	}

	private NodeInstruction waitForResult(final NodeInstruction instruction, final Long maxWaitMs) {
		if ( instruction == null ) {
			return null;
		}
		List<NodeInstruction> results = waitForResults(Collections.singletonList(instruction),
				maxWaitMs);
		if ( results != null && !results.isEmpty() ) {
			return results.getFirst();
		}
		return null;
	}

	private DeferredResult<Result<List<NodeInstruction>>> handleAsyncResult(
			final List<NodeInstruction> instructions, final Long maxWaitMs) {
		long allowedMaxWait = executionResultMaxWait.toMillis();
		long maxWait = (maxWaitMs == null ? allowedMaxWait
				: Math.max(0, Math.min(allowedMaxWait, maxWaitMs)));
		var deferred = new DeferredResult<Result<List<NodeInstruction>>>(maxWait + 200L, instructions);
		Thread.startVirtualThread(() -> {
			try {
				List<NodeInstruction> result = waitForResults(instructions, maxWait);
				deferred.setResult(success(result));
			} catch ( Throwable t ) {
				// ignore
			}
		});
		return deferred;
	}

	private List<NodeInstruction> waitForResults(final List<NodeInstruction> instructions,
			final long maxWaitMs) {
		if ( instructions == null || instructions.isEmpty() || maxWaitMs < 100 ) {
			return instructions;
		}
		final int count = instructions.size();
		final long delay = executionResultDelay.toMillis();
		final long backOff = delay / 4;
		final long expire = System.currentTimeMillis() + maxWaitMs;
		final ConcurrentMap<Long, NodeInstruction> results = new ConcurrentHashMap<>(
				instructions.size());

		// block the calling thread, while spawning tasks to check each instruction
		long currDelay = delay;
		while ( System.currentTimeMillis() < expire && results.size() < count ) {
			try {
				Thread.sleep(currDelay);
			} catch ( InterruptedException e ) {
				// ignore
			}
			final CountDownLatch latch = new CountDownLatch(instructions.size());
			for ( NodeInstruction instruction : instructions ) {
				if ( results.containsKey(instruction.getId()) ) {
					continue;
				}
				taskExecutor.execute(() -> {
					try {
						Thread.sleep(delay);
					} catch ( InterruptedException e ) {
						// ignore
					}
					try {
						var instr = nodeInstructionDao.get(instruction.getId());
						if ( instr == null ) {
							String msg = "Instruction [%d] not found".formatted(instruction.getId());
							throw new IllegalStateException(msg);
						} else if ( instr.getState() == InstructionState.Completed
								|| instr.getState() == InstructionState.Declined ) {
							results.put(instruction.getId(), instr);
						}
					} finally {
						latch.countDown();
					}
				});
			}
			try {
				latch.await(expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
			} catch ( InterruptedException e ) {
				// ignore and continue
			}
			currDelay += backOff;
		}

		var finalInstructions = new ArrayList<NodeInstruction>(instructions.size());
		for ( NodeInstruction instr : instructions ) {
			NodeInstruction updated = results.get(instr.getId());
			if ( updated != null ) {
				instr = updated;
			} else {
				instr = instr.clone();
				instr.setResultParameters(INSTRUCTION_EXEC_TIMEOUT_MESSAGE);
			}
			finalInstructions.add(instr);
		}
		return finalInstructions;
	}

	/**
	 * Set the length of time to delay checking for instruction execution
	 * results.
	 *
	 * @param executionResultDelay
	 *        the execution delay to set; defaults to
	 *        {@link #DEFAULT_EXECUTION_RESULT_DELAY} if {@literal null} or not
	 *        positive
	 */
	public void setExecutionResultDelay(Duration executionResultDelay) {
		this.executionResultDelay = (executionResultDelay != null && executionResultDelay.isPositive()
				? executionResultDelay
				: DEFAULT_EXECUTION_RESULT_DELAY);
	}

	/**
	 * Set the maximum length of time allowed to wait for instruction results.
	 *
	 * @param executionResultMaxWait
	 *        the maximum time to set
	 */
	public void setExecutionResultMaxWait(Duration executionResultMaxWait) {
		this.executionResultMaxWait = executionResultMaxWait;
	}

}
