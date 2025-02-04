/* ==================================================================
 * DaoInstructionInputEndpointBiz.java - 29/03/2024 10:48:10 am
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

package net.solarnetwork.central.inin.biz.impl;

import static net.solarnetwork.central.biz.UserEventAppenderBiz.addEvent;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.MimeType;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.biz.InstructionInputEndpointBiz;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.biz.TransformConstants;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.CentralInstructionInputUserEvents;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * DAO implementation of {@link InstructionInputEndpointBiz}.
 *
 * @author matt
 * @version 1.3
 */
public class DaoInstructionInputEndpointBiz
		implements InstructionInputEndpointBiz, CentralInstructionInputUserEvents {

	/** An instruction result parameter for an error message. */
	public static final String ERROR_INSTRUCTION_RESULT_PARAM = "error";

	/** The {@code executionResultDelay} property default value. */
	public static final Duration DEFAULT_EXECUTION_RESULT_DELAY = Duration.ofSeconds(1);

	/** The {@code executionResultMaxWait} property default value. */
	public static final Duration DEFAULT_EXECUTION_RESULT_MAX_WAIT = Duration.ofSeconds(60);

	private static final Map<String, Object> INSTRUCTION_EXEC_TIMEOUT_MESSAGE = Map
			.of(InstructionStatus.MESSAGE_RESULT_PARAM, "Timeout waiting for instruction result.");

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final TaskExecutor taskExecutor;
	private final InstructorBiz instructor;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final EndpointConfigurationDao endpointDao;
	private final TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;
	private final TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;
	private final UserMetadataDao userMetadataDao;
	private final Map<String, RequestTransformService> requestTransformServices;
	private final Map<String, ResponseTransformService> responseTransformServices;
	private UserEventAppenderBiz userEventAppenderBiz;
	private Duration executionResultDelay = DEFAULT_EXECUTION_RESULT_DELAY;
	private Duration executionResultMaxWait = DEFAULT_EXECUTION_RESULT_MAX_WAIT;

	/**
	 * Constructor.
	 *
	 * @param taskExecutor
	 *        the task executor to use
	 * @param instructor
	 *        the instruction service
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param requestTransformDao
	 *        the request transform DAO
	 * @param responseTransformDao
	 *        the response transform DAO
	 * @param userMetadataDao
	 *        the user metadata DAO
	 * @param requestTransformServices
	 *        the request transform services
	 * @param responseTransformServices
	 *        the response transform services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoInstructionInputEndpointBiz(TaskExecutor taskExecutor, InstructorBiz instructor,
			SolarNodeOwnershipDao nodeOwnershipDao, EndpointConfigurationDao endpointDao,
			TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao,
			TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao,
			UserMetadataDao userMetadataDao,
			Collection<RequestTransformService> requestTransformServices,
			Collection<ResponseTransformService> responseTransformServices) {
		super();
		this.taskExecutor = requireNonNullArgument(taskExecutor, "taskExecutor");
		this.instructor = requireNonNullArgument(instructor, "instructor");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.requestTransformDao = requireNonNullArgument(requestTransformDao, "requestTransformDao");
		this.responseTransformDao = requireNonNullArgument(responseTransformDao, "responseTransformDao");
		this.userMetadataDao = requireNonNullArgument(userMetadataDao, "userMetadataDao");
		this.requestTransformServices = requireNonNullArgument(requestTransformServices,
				"requestTransformServices").stream()
						.collect(Collectors.toMap(Identity::getId, Function.identity()));
		this.responseTransformServices = requireNonNullArgument(responseTransformServices,
				"responseTransformServices").stream()
						.collect(Collectors.toMap(Identity::getId, Function.identity()));
	}

	private static LogEventInfo importEvent(String msg, EndpointConfiguration endpoint,
			RequestTransformConfiguration requestXform, ResponseTransformConfiguration responseXform,
			MimeType contentType, MimeType outputType, Map<String, String> parameters,
			NodeInstruction instruction, String... tags) {
		var eventData = new LinkedHashMap<>(8);
		eventData.put(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId());
		eventData.put(REQ_TRANSFORM_ID_DATA_KEY, endpoint.getRequestTransformId());
		if ( requestXform != null ) {
			eventData.put(REQ_TRANSFORM_SERVICE_ID_DATA_KEY, requestXform.getServiceIdentifier());
		}
		if ( contentType != null ) {
			eventData.put(CONTENT_TYPE_DATA_KEY, contentType.toString());
		}
		eventData.put(RES_TRANSFORM_ID_DATA_KEY, endpoint.getResponseTransformId());
		if ( responseXform != null ) {
			eventData.put(RES_TRANSFORM_SERVICE_ID_DATA_KEY, responseXform.getServiceIdentifier());
		}
		if ( outputType != null ) {
			eventData.put(OUTPUT_TYPE_DATA_KEY, outputType.toString());
		}
		if ( parameters != null ) {
			eventData.put(PARAMETERS_DATA_KEY, parameters);
		}
		if ( instruction != null ) {
			eventData.put(INSTRUCTION_DATA_KEY, instruction);
		}
		return event(INSTRUCTION_TAGS, msg, getJSONString(eventData, null), tags);
	}

	private static LogEventInfo importErrorEvent(String msg, EndpointConfiguration endpoint,
			RequestTransformConfiguration requestXform, ResponseTransformConfiguration responseXform,
			MimeType contentType, MimeType outputType, Map<String, String> parameters) {
		return importEvent(msg, endpoint, requestXform, responseXform, contentType, outputType,
				parameters, null, ERROR_TAG);
	}

	@Override
	public List<NodeInstruction> importInstructions(Long userId, UUID endpointId, MimeType contentType,
			InputStream in, Map<String, String> parameters) throws IOException {
		final UserUuidPK endpointPk = new UserUuidPK(requireNonNullArgument(userId, "userId"),
				requireNonNullArgument(endpointId, "endpointId"));
		final EndpointConfiguration endpoint = requireNonNullObject(endpointDao.get(endpointPk),
				endpointPk);

		final UserLongCompositePK xformPk = new UserLongCompositePK(userId,
				requireNonNullArgument(endpoint.getRequestTransformId(), "requestTransformId"));
		final RequestTransformConfiguration xform = requireNonNullObject(
				requestTransformDao.get(xformPk), xformPk);

		final String xformServiceId = requireNonNullArgument(xform.getServiceIdentifier(),
				"transform.serviceIdentifier");
		final RequestTransformService xformService = requireNonNullObject(
				requestTransformServices.get(xformServiceId), xformServiceId);

		if ( endpoint.getRequestContentType() != null ) {
			// force content type to endpoint configuration
			contentType = MimeType.valueOf(endpoint.getRequestContentType());
		}

		if ( !xformService.supportsInput(requireNonNullArgument(in, "in"),
				requireNonNullArgument(contentType, "contentType")) ) {
			String msg = "Transform service %s does not support input type %s with %s."
					.formatted(xformServiceId, contentType, in.getClass().getSimpleName());
			addEvent(userEventAppenderBiz, userId,
					importErrorEvent(msg, endpoint, xform, null, contentType, null, parameters));
			throw new IllegalArgumentException(msg);
		}

		var params = new HashMap<String, Object>(8);
		if ( parameters != null ) {
			params.putAll(parameters);
		}
		params.put(TransformConstants.PARAM_USER_ID, userId);
		params.put(TransformConstants.PARAM_ENDPOINT_ID, endpointId.toString());
		params.put(TransformConstants.PARAM_TRANSFORM_ID, endpoint.getRequestTransformId());
		params.put(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, xform.ident());

		if ( endpoint.getUserMetadataPath() != null && !endpoint.getUserMetadataPath().isBlank() ) {
			String meta = userMetadataDao.jsonMetadataAtPath(userId, endpoint.getUserMetadataPath());
			if ( meta != null ) {
				params.put(TransformConstants.PARAM_USER_METADATA_JSON, meta);
			}
		}

		Iterable<NodeInstruction> instructions;
		try {
			instructions = xformService.transformInput(in, contentType, xform, params);
		} catch ( Exception e ) {
			String msg = "Error executing transform: " + e.getMessage();
			addEvent(userEventAppenderBiz, userId,
					importErrorEvent(msg, endpoint, xform, null, contentType, null, parameters));
			if ( e instanceof IOException ioe ) {
				throw ioe;
			} else if ( e instanceof RuntimeException re ) {
				throw re;
			} else {
				throw new RuntimeException(e);
			}
		}

		// apply endpoint node IDs if configured
		if ( endpoint.getNodeIds() != null && !endpoint.getNodeIds().isEmpty() ) {
			List<NodeInstruction> nodeInstructions = new ArrayList<>(8);
			for ( NodeInstruction instr : instructions ) {
				for ( Long nodeId : endpoint.getNodeIds() ) {
					if ( nodeId.equals(instr.getNodeId()) ) {
						nodeInstructions.add(instr);
					} else {
						nodeInstructions.add(instr.copyWithNodeId(nodeId));
					}
				}
			}
			instructions = nodeInstructions;
		}

		// verify ownership node is owner of endpoint
		int instructionCount = 0;
		for ( NodeInstruction instruction : instructions ) {
			Long nodeId = requireNonNullArgument(instruction.getNodeId(), "nodeId");
			SolarNodeOwnership owner = requireNonNullObject(nodeOwnershipDao.ownershipForNodeId(nodeId),
					nodeId);
			if ( !userId.equals(owner.getUserId()) ) {
				var ex = new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
				addEvent(userEventAppenderBiz, userId, importErrorEvent(ex.getMessage(), endpoint, xform,
						null, contentType, null, parameters));
				throw ex;
			}
			instructionCount++;
		}

		var result = new ArrayList<NodeInstruction>(instructionCount);
		for ( NodeInstruction instruction : instructions ) {
			var queued = instructor.queueInstruction(instruction.getNodeId(), instruction);
			if ( queued != null ) {
				addEvent(userEventAppenderBiz, userId, importEvent(null, endpoint, xform, null,
						contentType, null, parameters, queued, INSTRUCTION_IMPORTED_TAG));
				result.add(queued);
			}
		}

		log.info("Instructions input for user {}, endpoint {}: {}", userId, endpointId, result);

		return result;
	}

	@Override
	public void generateResponse(Long userId, UUID endpointId, List<NodeInstruction> instructions,
			MimeType outputType, OutputStream out, Map<String, String> parameters) throws IOException {
		final UserUuidPK endpointPk = new UserUuidPK(requireNonNullArgument(userId, "userId"),
				requireNonNullArgument(endpointId, "endpointId"));
		final EndpointConfiguration endpoint = requireNonNullObject(endpointDao.get(endpointPk),
				endpointPk);

		final UserLongCompositePK xformPk = new UserLongCompositePK(userId,
				requireNonNullArgument(endpoint.getResponseTransformId(), "responseTransformId"));
		final ResponseTransformConfiguration xform = requireNonNullObject(
				responseTransformDao.get(xformPk), xformPk);

		final String xformServiceId = requireNonNullArgument(xform.getServiceIdentifier(),
				"transform.serviceIdentifier");
		final ResponseTransformService xformService = requireNonNullObject(
				responseTransformServices.get(xformServiceId), xformServiceId);

		final MimeType resType = (endpoint.getResponseContentType() != null
				? MimeType.valueOf(endpoint.getResponseContentType())
				: outputType);

		if ( !xformService.supportsOutputType(requireNonNullArgument(resType, "contentType")) ) {
			String msg = "Transform service %s does not support output type %s."
					.formatted(xformServiceId, resType);
			addEvent(userEventAppenderBiz, userId,
					importErrorEvent(msg, endpoint, null, xform, null, resType, parameters));
			throw new IllegalArgumentException(msg);
		}

		// wait for results
		final int count = instructions.size();
		final long delay = executionResultDelay.toMillis();
		final long backOff = delay / 4;
		final long expire = System.currentTimeMillis() + Math.min(executionResultMaxWait.toMillis(),
				TimeUnit.SECONDS.toMillis(endpoint.getMaxExecutionSeconds()));
		final ConcurrentMap<Long, NodeInstruction> results = new ConcurrentHashMap<>(
				instructions.size());

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
						var instr = instructor.getInstruction(instruction.getId());
						if ( instr == null ) {
							String msg = "Instruction [%d] not found".formatted(instruction.getId());
							addEvent(userEventAppenderBiz, userId, importErrorEvent(msg, endpoint, null,
									xform, null, resType, parameters));
							throw new IllegalStateException(msg);
						} else if ( instr.getState() == InstructionState.Completed
								|| instr.getState() == InstructionState.Declined ) {
							addEvent(userEventAppenderBiz, userId, importEvent(null, endpoint, null,
									xform, null, resType, parameters, instr, INSTRUCTION_EXECUTED_TAG));
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
				String msg = "Timeout waiting for instruction [%d] to complete."
						.formatted(instr.getId());
				addEvent(userEventAppenderBiz, userId,
						importErrorEvent(msg, endpoint, null, xform, null, resType, parameters));
			}
			finalInstructions.add(instr);
		}

		var params = new HashMap<String, Object>(8);
		if ( parameters != null ) {
			params.putAll(parameters);
		}
		params.put(TransformConstants.PARAM_USER_ID, userId);
		params.put(TransformConstants.PARAM_ENDPOINT_ID, endpointId.toString());
		params.put(TransformConstants.PARAM_TRANSFORM_ID, endpoint.getResponseTransformId());
		params.put(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, xform.ident());

		if ( endpoint.getUserMetadataPath() != null && !endpoint.getUserMetadataPath().isBlank() ) {
			String meta = userMetadataDao.jsonMetadataAtPath(userId, endpoint.getUserMetadataPath());
			if ( meta != null ) {
				params.put(TransformConstants.PARAM_USER_METADATA_JSON, meta);
			}
		}

		try {
			xformService.transformOutput(finalInstructions, resType, xform, params, out);
		} catch ( Exception e ) {
			String msg = "Error executing transform: " + e.getMessage();
			addEvent(userEventAppenderBiz, userId,
					importErrorEvent(msg, endpoint, null, xform, null, resType, parameters));
			if ( e instanceof IOException ioe ) {
				throw ioe;
			} else if ( e instanceof RuntimeException re ) {
				throw re;
			} else {
				throw new RuntimeException(e);
			}
		}

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
	 * Set the length of time to delay checking for instruction execution
	 * results.
	 *
	 * @param executionResultDelay
	 *        the executionDelay to set; defaults to
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
