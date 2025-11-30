/* ==================================================================
 * DaoUserNodeInstructionService.java - 18/11/2025 11:13:25 am
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

package net.solarnetwork.central.user.biz.dao;

import static java.time.temporal.ChronoUnit.SECONDS;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.codec.JsonUtils.getStringMapFromTree;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.web.client.RestClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.InstructionParameter;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import net.solarnetwork.central.user.biz.InstructionsExpressionService;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UsersUserEvents;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.DateUtils;

/**
 * DAO implementation of {@link UserNodeInstructionService}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserNodeInstructionService
		implements UserNodeInstructionService, ServiceLifecycleObserver, UsersUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

	/** The task message used when an exception is thrown during execution. */
	public static final String EXCEPTION_TASK_MESSAGE = "Error executing task.";

	/** The error message when the task schedule is missing. */
	public static final String ERROR_MIMSSING_TASK_SCHEDULE = "Task schedule not provided or usable.";

	/** The error message when the instruction configuration is missing. */
	public static final String ERROR_MISSING_INSTRUCTION = "Missing instruction template.";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Clock clock;
	private final ExecutorService executorService;
	private final ObjectMapper objectMapper;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final InstructorBiz instructorBiz;
	private final InstructionsExpressionService expressionService;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final UserNodeInstructionTaskDao taskDao;
	private final DatumEntityDao datumDao;
	private final DatumStreamMetadataDao datumStreamMetadataDao;

	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;
	private QueryAuditor queryAuditor;
	private HttpOperations httpOperations;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param executor
	 *        the executor; this must be exclusive to this service, as it will
	 *        be shut down when this service is shut down
	 * @param objectMapper
	 *        the JSON mapper to use
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param expressionService
	 *        the expression service
	 * @param instructorBiz
	 *        the instructor service
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param taskDao
	 *        the task DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param datumStreamMetadataDao
	 *        the datum stream metadata DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserNodeInstructionService(Clock clock, ExecutorService executor,
			ObjectMapper objectMapper, UserEventAppenderBiz userEventAppenderBiz,
			InstructorBiz instructorBiz, InstructionsExpressionService expressionService,
			SolarNodeOwnershipDao nodeOwnershipDao, UserNodeInstructionTaskDao taskDao,
			DatumEntityDao datumDao, DatumStreamMetadataDao datumStreamMetadataDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.executorService = requireNonNullArgument(executor, "executor");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.instructorBiz = requireNonNullArgument(instructorBiz, "instructorBiz");
		this.expressionService = requireNonNullArgument(expressionService, "expressionService");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.datumStreamMetadataDao = requireNonNullArgument(datumStreamMetadataDao,
				"datumStreamMetadataDao");
	}

	@Override
	public void serviceDidStartup() {
		// nothing
	}

	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
	@Override
	public void serviceDidShutdown() {
		try {
			executorService.shutdown();
			if ( shutdownMaxWait.isPositive() ) {
				log.info("Waiting at most {}s for user instruction tasks to complete...",
						shutdownMaxWait.getSeconds());
				boolean success = executorService.awaitTermination(shutdownMaxWait.getSeconds(),
						TimeUnit.SECONDS);
				if ( success ) {
					log.info("All user instruction tasks finished.");
				} else {
					log.warn("Timeout waiting {}s for user instruction tasks to complete.",
							shutdownMaxWait.getSeconds());
				}
			}
		} catch ( Exception e ) {
			log.warn("Error shutting down user instruction task service: {}", e.getMessage(), e);
		}
	}

	@Override
	public UserNodeInstructionTaskEntity claimQueuedTask() {
		if ( executorService.isShutdown() ) {
			return null;
		}
		return taskDao.claimQueuedTask();
	}

	@Override
	public Future<UserNodeInstructionTaskEntity> executeTask(UserNodeInstructionTaskEntity task) {
		try {
			return executorService.submit(new UserNodeInstructionTask(task));
		} catch ( RejectedExecutionException e ) {
			log.debug("User instruction task execution rejected, resetting state to Queued: {}",
					e.getMessage());
			// go back to queued
			if ( !taskDao.updateTaskState(task.getId(), Queued, task.getState()) ) {
				log.warn("Failed to update rejected user instruction task {} state from {} to Queued",
						task.getId().ident(), task.getState());
			}
			throw e;
		}
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		return taskDao.resetAbandondedExecutingTasks(olderThan);
	}

	private final class UserNodeInstructionTask implements Callable<UserNodeInstructionTaskEntity> {

		private final UserNodeInstructionTaskEntity task;
		private final BasicClaimableJobState startState;

		private UserNodeInstructionTask(UserNodeInstructionTaskEntity taskInfo) {
			super();
			this.task = requireNonNullArgument(taskInfo, "task").clone();
			this.startState = requireNonNullArgument(taskInfo.getState(), "task.state");
		}

		@Override
		public UserNodeInstructionTaskEntity call() throws Exception {
			try {
				return executeTask();
			} catch ( Exception e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				try {
					if ( log.isDebugEnabled() || !(e instanceof RemoteServiceException) ) {
						// log full stack trace when debug enabled or not a RemoteServiceException
						log.warn("Error executing instruction task {}", task.getId().ident(), e);
					} else {
						// otherwise just print exception message, to cut down on log clutter
						log.warn("Error executing instruction task {}: {}", task.getId().ident(),
								e.toString());
					}
					final var dataErrMsg = (e instanceof RemoteServiceException ? e : t).getMessage();

					final var oldState = task.getState();
					task.setMessage(EXCEPTION_TASK_MESSAGE);

					final var resultProps = new LinkedHashMap<String, Object>(4);
					resultProps.put(MESSAGE_DATA_KEY, dataErrMsg);
					if ( task.getResultProps() != null ) {
						resultProps.putAll(task.getResultProps());
					}
					if ( !resultProps.containsKey(SOURCE_DATA_KEY)
							&& e instanceof AuthorizationException ae && ae.getId() != null ) {
						resultProps.put(SOURCE_DATA_KEY, ae.getId());
					}
					task.setResultProps(resultProps);

					final var eventData = new LinkedHashMap<String, Object>(resultProps);
					eventData.put(CONFIG_ID_DATA_KEY, task.getConfigId());
					eventData.put(NODE_ID_DATA_KEY, task.getNodeId());
					eventData.put(MESSAGE_DATA_KEY, dataErrMsg);

					if ( t instanceof RestClientResponseException || t instanceof IOException ) {
						// reset back to queued to try again if HTTP client or IO error
						log.info(
								"Resetting instruction task {} by changing state from {} to {} after error: {}",
								task.getId().ident(), oldState, Queued, e.toString());
						task.setState(Queued);
						if ( task.getExecuteAt().isBefore(clock.instant()) ) {
							// bump date into future by 1 minute so we do not immediately try to process again
							task.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.SECONDS).plus(1,
									ChronoUnit.MINUTES));
						}
					} else {
						// stop processing job if not what appears to be an API IO exception
						log.info(
								"Stopping instruction task {} by changing state from {} to {} after error: {}",
								task.getId().ident(), oldState, Completed, e.toString());
						task.setState(Completed);
					}
					userEventAppenderBiz.addEvent(task.getUserId(), eventForUserRelatedKey(task.getId(),
							INSTRUCTION_ERROR_TAGS, EXCEPTION_TASK_MESSAGE, eventData));
					if ( !taskDao.updateTask(task, oldState) ) {
						log.warn(
								"Unable to update instruction task {} info with expected state {} with details: {}",
								task.getId().ident(), oldState, task);
					}
				} catch ( Exception e2 ) {
					log.warn("Error updating instruction task {} state after error",
							task.getId().ident(), e2);
					// ignore, return original
				}
				throw e;
			}
		}

		private UserNodeInstructionTaskEntity executeTask() throws Exception {
			final Instant execTime = clock.instant();
			final Instant execDate = task.getExecuteAt();
			final String taskIdent = task.getId().ident();
			final String topic = requireNonNullArgument(task.getTopic(), "topic");

			task.setLastExecuteAt(execTime);

			final Trigger schedule = triggerForSchedule(task.getSchedule());
			if ( schedule == null ) {
				task.putResultProps(Map.of(SOURCE_DATA_KEY, (Object) task.getSchedule()));
				throw new IllegalArgumentException(ERROR_MIMSSING_TASK_SCHEDULE);
			}

			final JsonNode json = (task.getServicePropsJson() == null
					|| task.getServicePropsJson().isEmpty() ? objectMapper.nullNode()
							: objectMapper.readTree(task.getServicePropsJson()));
			final NodeInstruction instrInput = objectMapper.treeToValue(json.path("instruction"),
					NodeInstruction.class);
			if ( instrInput == null ) {
				throw new IllegalArgumentException(ERROR_MISSING_INSTRUCTION);
			}

			// validate node ID allowed
			final SolarNodeOwnership owner = nodeOwnershipDao.ownershipForNodeId(task.getNodeId());
			if ( owner == null || !task.getUserId().equals(owner.getUserId()) ) {
				throw new AuthorizationException(Reason.ACCESS_DENIED, task.getNodeId());
			}

			// copy task configuration to instruction
			instrInput.setNodeId(task.getNodeId());
			instrInput.getInstruction().setTopic(topic);

			final KeyValuePair[] expressions = objectMapper.treeToValue(json.path("expressions"),
					KeyValuePair[].class);
			evaluateExpressions(owner, instrInput, expressions);

			// save task state to Executing
			if ( !taskDao.updateTaskState(task.getId(), Executing, startState) ) {
				log.warn("Failed to update instruction task {} state to Executing @ {}", taskIdent,
						task.getExecuteAt());
				var errMsg = "Failed to update task state from Claimed to Executing.";
				userEventAppenderBiz.addEvent(task.getUserId(),
						eventForUserRelatedKey(task.getId(), INSTRUCTION_ERROR_TAGS, errMsg, Map.of()));
				return task;
			}
			task.setState(Executing);

			final NodeInstruction result = instructorBiz.queueInstruction(task.getNodeId(),
					instrInput.getInstruction());

			// success: update task info to start again tomorrow
			final var now = clock.instant();
			Instant nextExecTime = execDate;
			var ctx = new SimpleTriggerContext(clock);
			while ( nextExecTime.isBefore(now) ) {
				// skip any missed execution times between last actual execution and now...
				ctx.update(nextExecTime,
						(ctx.lastScheduledExecution() == null ? execTime : nextExecTime), now);
				Instant net = schedule.nextExecution(ctx);
				if ( net == null ) {
					break;
				}
				nextExecTime = net.truncatedTo(SECONDS);
			}
			task.setExecuteAt(nextExecTime);

			// reset task back to Queued so it can be executed again
			task.setState(Queued);

			// reset message
			task.setMessage(null);

			// reset props
			task.setResultProps(null);

			// save task state
			if ( !taskDao.updateTask(task, Executing) ) {
				log.warn("Failed to reset instruction task {} @ {}", taskIdent, task.getExecuteAt());
				var errMsg = "Failed to reset task state.";
				var errData = Map.of("executeAt", task.getExecuteAt());
				userEventAppenderBiz.addEvent(task.getUserId(),
						eventForUserRelatedKey(task.getId(), INSTRUCTION_ERROR_TAGS, errMsg, errData));
			} else {
				var msg = "Reset task state";
				userEventAppenderBiz.addEvent(task.getUserId(),
						eventForUserRelatedKey(task.getId(), INSTRUCTION_TAGS, msg, Map.of(
						// @formatter:off
								EXECUTE_AT_DATA_KEY, task.getExecuteAt(),
								NODE_ID_DATA_KEY, (Object) task.getNodeId(),
								INSTRUCTION_DATA_KEY, getStringMapFromTree(objectMapper.valueToTree(result))
								// @formatter:on
						)));
			}

			return task;
		}

		@SuppressWarnings("StatementSwitchToExpressionSwitch")
		private void evaluateExpressions(final SolarNodeOwnership owner,
				final NodeInstruction instrInput, final KeyValuePair[] expressions) {
			if ( expressions == null || expressions.length < 1 ) {
				return;
			}
			final var datumStreamsAccessor = new QueryingDatumStreamsAccessor(
					expressionService.sourceIdPathMatcher(), List.of(), owner.getUserId(), clock,
					datumDao, datumStreamMetadataDao, queryAuditor);

			// each key is a bean property path on an Instruction instance; each value is an expression to run
			final NodeInstructionExpressionRoot exprRoot = expressionService
					.createNodeInstructionExpressionRoot(owner, instrInput, null, datumStreamsAccessor,
							httpOperations);
			final Map<String, Object> combinedParameters = exprRoot.getParameters();

			// combine existing template parameters with expression results, and provide these as
			// expression variables, to allow expressions to refer to earlier results
			if ( instrInput.getInstruction().getParameters() != null ) {
				for ( InstructionParameter param : instrInput.getInstruction().getParameters() ) {
					combinedParameters.put(param.getName(), param.getValue());
				}
			}

			for ( KeyValuePair exprInfo : expressions ) {
				if ( exprInfo.getKey() == null || exprInfo.getKey().isEmpty()
						|| exprInfo.getValue() == null || exprInfo.getValue().isEmpty() ) {
					continue;
				}
				log.trace("Executing task {} expr {} with {}", task.getId(), exprInfo, exprRoot);
				try {
					Object exprResult = expressionService.evaulateExpression(exprInfo.getValue(),
							exprRoot, combinedParameters, Object.class);
					if ( exprResult == null ) {
						continue;
					}
					switch (exprInfo.getKey()) {
						case "expirationDate":
							if ( exprResult instanceof Temporal t ) {
								instrInput.getInstruction()
										.setExpirationDate(DateUtils.timestamp(t, owner.getZone()));
							} else {
								throw new IllegalArgumentException(
										"The expirationDate expression result type [%s] is not valid."
												.formatted(exprResult.getClass()));
							}
							break;

						case "topic":
							instrInput.getInstruction().setTopic(exprResult.toString());
							break;

						default:
							combinedParameters.put(exprInfo.getKey(), exprResult.toString());
					}
				} catch ( RuntimeException e ) {
					task.setResultProps(Map.of(SOURCE_DATA_KEY, exprInfo.getValue()));
					throw e;
				}
			}
			if ( !combinedParameters.isEmpty() ) {
				instrInput.getInstruction().clearParameters();
				for ( Entry<String, Object> e : combinedParameters.entrySet() ) {
					if ( e.getKey() == null || e.getKey().isBlank() || e.getValue() == null ) {
						continue;
					}
					String val = e.getValue().toString();
					if ( val.isBlank() ) {
						continue;
					}
					instrInput.getInstruction().addParameter(e.getKey(), val);
				}
			}
		}
	}

	private Trigger triggerForSchedule(final String schedule) {
		assert schedule != null;
		Trigger t = SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);
		if ( t instanceof PeriodicTrigger pt ) {
			pt.setFixedRate(true);
		}
		return t;
	}

	/**
	 * Get the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @return the maximum wait time, never {@literal null}
	 */
	public final Duration getShutdownMaxWait() {
		return shutdownMaxWait;
	}

	/**
	 * Set the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @param shutdownMaxWait
	 *        the maximum wait time to set; if {@literal null} then
	 *        {@link #DEFAULT_SHUTDOWN_MAX_WAIT} will be used
	 */
	public final void setShutdownMaxWait(Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
	}

	/**
	 * Get the query auditor.
	 *
	 * @return the auditor
	 */
	public final QueryAuditor getQueryAuditor() {
		return queryAuditor;
	}

	/**
	 * Set the query auditor.
	 *
	 * @param queryAuditor
	 *        the auditor to set
	 */
	public final void setQueryAuditor(QueryAuditor queryAuditor) {
		this.queryAuditor = queryAuditor;
	}

	/**
	 * Get the HTTP operations.
	 * 
	 * @return the operations
	 */
	public HttpOperations getHttpOperations() {
		return httpOperations;
	}

	/**
	 * Set the HTTP operations.
	 * 
	 * @param httpOperations
	 *        the operations to set
	 */
	public void setHttpOperations(HttpOperations httpOperations) {
		this.httpOperations = httpOperations;
	}

}
