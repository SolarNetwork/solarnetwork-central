/* ==================================================================
 * DaoDaoUserNodeInstructionServiceTests.java - 18/11/2025 11:17:58 am
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

package net.solarnetwork.central.user.biz.dao.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Claimed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService.ERROR_MIMSSING_TASK_SCHEDULE;
import static net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService.ERROR_MISSING_INSTRUCTION;
import static net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService.EXCEPTION_TASK_MESSAGE;
import static net.solarnetwork.codec.jackson.JsonUtils.getStringMap;
import static net.solarnetwork.codec.jackson.JsonUtils.getStringMapFromObject;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.AntPathMatcher;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.test.ResultCaptor;
import net.solarnetwork.central.user.biz.InstructionsExpressionService;
import net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UsersUserEvents;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;

/**
 * Test cases for the {@link DaoUserNodeInstructionService} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserNodeInstructionServiceTests implements CommonUserEvents, UsersUserEvents {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private ExecutorService executor;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private InstructorBiz instructorBiz;

	@Mock
	private InstructionsExpressionService expressionService;

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserNodeInstructionTaskDao taskDao;

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Captor
	private ArgumentCaptor<Instruction> instructionCaptor;

	@Captor
	private ArgumentCaptor<UserNodeInstructionTaskEntity> taskCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	private MutableClock clock;
	private AntPathMatcher sourceIdPathMatcher;

	private DaoUserNodeInstructionService service;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(now().truncatedTo(ChronoUnit.HOURS), UTC);
		sourceIdPathMatcher = new AntPathMatcher();
		sourceIdPathMatcher.setCachePatterns(false);
		service = new DaoUserNodeInstructionService(clock, executor, JsonUtils.JSON_OBJECT_MAPPER,
				userEventAppenderBiz, instructorBiz, expressionService, nodeOwnershipDao, taskDao,
				datumDao, datumStreamMetadataDao);
	}

	@Test
	public void claimTask() {
		// GIVEN
		final UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());

		given(taskDao.claimQueuedTask()).willReturn(entity);

		// WHEN
		UserNodeInstructionTaskEntity result = service.claimQueuedTask();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO returned")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void claimTask_null() {
		// GIVEN
		given(taskDao.claimQueuedTask()).willReturn(null);

		// WHEN
		UserNodeInstructionTaskEntity result = service.claimQueuedTask();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null result from DAO returned")
			.isNull()
			;
		// @formatter:on
	}

	private CompletableFuture<UserNodeInstructionTaskEntity> givenTaskExecution() {
		// submit task
		var future = new CompletableFuture<UserNodeInstructionTaskEntity>();
		given(executor.submit(argThat((Callable<UserNodeInstructionTaskEntity> call) -> {
			try {
				future.complete(call.call());
			} catch ( Exception e ) {
				future.completeExceptionally(e);
			}
			return true;
		}))).willReturn(future);
		return future;
	}

	private NodeInstruction answerQueuedInstruction(InvocationOnMock invocation) throws Throwable {
		Long nodeId = invocation.getArgument(0, Long.class);
		Instruction input = invocation.getArgument(1, Instruction.class);
		var queuedInstruction = new NodeInstruction();
		queuedInstruction.setId(randomLong());
		queuedInstruction.setNodeId(nodeId);
		queuedInstruction.setInstruction(new Instruction(input));
		queuedInstruction.getInstruction().setState(InstructionState.Queued);
		queuedInstruction.getInstruction().setStatusDate(clock.instant().plusSeconds(1));
		return queuedInstruction;
	}

	@Test
	public void executeTask() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		final String controlId = randomString();
		final String controlVal = String.valueOf(randomInt());
		final Map<String, Object> params = Map.of(controlId, controlVal);

		// @formatter:off
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);
		UserNodeInstructionTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(instructorBiz).should().queueInstruction(eq(task.getNodeId()), instructionCaptor.capture());
		and.then(instructionCaptor.getValue())
			.as("Instruction to queue topic from task")
			.returns(topic, from(Instruction::getTopic))
			.as("Instruction to queue has no status")
			.returns(null, from(Instruction::getState))
			.as("Instruction to queue params from task")
			.returns(params, from(Instruction::getParams))
			;
		
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Executing));
		and.then(taskCaptor.getValue())
			.as("Task state updated to Queued")
			.returns(Queued, from(UserNodeInstructionTaskEntity::getState))
			.as("Task exec date updated to next offset from previous exec date")
			.returns(startingExecDate.plus(taskSchedule), from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Last exec date updated to clock time")
			.returns(clock.instant(), from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			;
		
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Event for execution generated")
			.hasSize(1)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task success reset event generated")
					.isNotNull()
					.as("Instruction tags provided in event")
					.returns(INSTRUCTION_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task data provided in event data")
					.returns(Map.of(
							CONFIG_ID_DATA_KEY, taskCaptor.getValue().getConfigId(),
							EXECUTE_AT_DATA_KEY, ISO_DATE_TIME_ALT_UTC.format(taskCaptor.getValue().getExecuteAt()),
							NODE_ID_DATA_KEY, taskCaptor.getValue().getNodeId(),
							INSTRUCTION_DATA_KEY, getStringMapFromObject(instructionResultCaptor.getResult())
						), from(e -> getStringMap(e.getData())))
					;
			})
			;

		and.then(resultTask)
			.as("Task from future returned")
			.isNotNull()
			.as("Task from future different instance from one passed in")
			.isNotSameAs(task)
			.as("Result task is same as passed to DAO for update")
			.isSameAs(taskCaptor.getValue())
			;
		// @formatter:on
	}

	@Test
	public void executeTask_clearsResultProperties() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		// @formatter:off
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic
						)
				));
		// @formatter:on

		// include previous execution properties
		task.setLastExecuteAt(startingExecDate.minus(taskSchedule));
		task.setMessage("boom");
		task.setResultProps(Map.of("was", "error"));

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);
		UserNodeInstructionTaskEntity resultTask = result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		and.then(resultTask)
			.as("Task from future returned")
			.isNotNull()
			.as("Task from future different instance from one passed in")
			.isNotSameAs(task)
			
			// we know from executeTask() test that resultTask is same instance as passed to taskDao.update()
			// so we can simply test for cleared properties on the result instance here
			
			.as("Task message cleared")
			.returns(null, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Task result properties cleared")
			.returns(null, from(UserNodeInstructionTaskEntity::getResultProperties))
			;
		// @formatter:on
	}

	@Test
	public void executeTask_invalidSchedule() throws Exception {
		// GIVEN
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String schedule = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(randomString());
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(schedule);
		task.setNodeId(randomLong());

		// execute task
		givenTaskExecution();

		// save updated task 
		given(taskDao.updateTask(any(), eq(Claimed))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		thenThrownBy(() -> {
				result.get(1, TimeUnit.MINUTES);
			}, "Execution throws exception")
			.extracting(Throwable::getCause)
			.as("Exception is IllegalArgument")
			.isInstanceOf(IllegalArgumentException.class)
			;
		
		then(instructorBiz).shouldHaveNoInteractions();

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task state updated to Completed because of error")
			.returns(Completed, from(UserNodeInstructionTaskEntity::getState))
			.as("Task exec date unchanged")
			.returns(startingExecDate, from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Last exec date updated to clock time")
			.returns(clock.instant(), from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Task message set")
			.returns(EXCEPTION_TASK_MESSAGE, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Task result properties present")
			.extracting(UserNodeInstructionTaskEntity::getResultProperties, map(String.class, Object.class))
			.as("Result properties present")
			.isNotNull()
			.as("Error message included")
			.containsEntry(MESSAGE_DATA_KEY, ERROR_MIMSSING_TASK_SCHEDULE)
			.as("Source result property is task schedule")
			.containsEntry(SOURCE_DATA_KEY, schedule)
			;
		
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Event for execution generated")
			.hasSize(1)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task error event generated")
					.isNotNull()
					.as("Instruction error tags provided in event")
					.returns(INSTRUCTION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task data provided in event data")
					.extracting(e -> getStringMap(e.getData()), map(String.class, Object.class))
					.as("Error event config ID is task ID")
					.containsEntry(CONFIG_ID_DATA_KEY, taskCaptor.getValue().getConfigId())
					.as("Error event contains node ID")
					.containsEntry(NODE_ID_DATA_KEY, taskCaptor.getValue().getNodeId())
					.as("Error message included")
					.containsEntry(MESSAGE_DATA_KEY, ERROR_MIMSSING_TASK_SCHEDULE)
					.as("Source result property is task schedule")
					.containsEntry(SOURCE_DATA_KEY, schedule)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void executeTask_noInstruction() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		// execute task
		givenTaskExecution();

		// save updated task 
		given(taskDao.updateTask(any(), eq(Claimed))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		thenThrownBy(() -> {
				result.get(1, TimeUnit.MINUTES);
			}, "Execution throws exception")
			.extracting(Throwable::getCause)
			.as("Exception is IllegalArgument")
			.isInstanceOf(IllegalArgumentException.class)
			;
		
		then(instructorBiz).shouldHaveNoInteractions();
		
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task state updated to Completed because of error")
			.returns(Completed, from(UserNodeInstructionTaskEntity::getState))
			.as("Task exec date unchanged")
			.returns(startingExecDate, from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Last exec date updated to clock time")
			.returns(clock.instant(), from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Task message set")
			.returns(EXCEPTION_TASK_MESSAGE, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Task result properties present")
			.extracting(UserNodeInstructionTaskEntity::getResultProperties, map(String.class, Object.class))
			.as("Result properties present")
			.isNotNull()
			.as("Error message included")
			.containsEntry(MESSAGE_DATA_KEY, ERROR_MISSING_INSTRUCTION)
			.as("Source result property not included")
			.doesNotContainKey(SOURCE_DATA_KEY)
			;
		
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Event for execution generated")
			.hasSize(1)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task error event generated")
					.isNotNull()
					.as("Instruction error tags provided in event")
					.returns(INSTRUCTION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task data provided in event data")
					.extracting(e -> getStringMap(e.getData()), map(String.class, Object.class))
					.as("Error event config ID is task ID")
					.containsEntry(CONFIG_ID_DATA_KEY, taskCaptor.getValue().getConfigId())
					.as("Error event contains node ID")
					.containsEntry(NODE_ID_DATA_KEY, taskCaptor.getValue().getNodeId())
					.as("Error message included")
					.containsEntry(MESSAGE_DATA_KEY, ERROR_MISSING_INSTRUCTION)
					.as("Source result property not included")
					.doesNotContainKey(SOURCE_DATA_KEY)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void executeTask_nodeIdNotAllowed() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		// @formatter:off
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership otherOwner = ownershipFor(task.getNodeId(), randomLong());
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(otherOwner);

		// save updated task 
		given(taskDao.updateTask(any(), eq(Claimed))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		thenThrownBy(() -> {
				result.get(1, TimeUnit.MINUTES);
			}, "Execution throws exception")
			.extracting(Throwable::getCause)
			.as("Exception is IllegalArgument")
			.isInstanceOf(AuthorizationException.class)
			;
		
		then(instructorBiz).shouldHaveNoInteractions();
		
		final AuthorizationException expectedException = new AuthorizationException(Reason.ACCESS_DENIED, task.getNodeId());
		
		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task state updated to Completed because of error")
			.returns(Completed, from(UserNodeInstructionTaskEntity::getState))
			.as("Task exec date unchanged")
			.returns(startingExecDate, from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Last exec date updated to clock time")
			.returns(clock.instant(), from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Task message set")
			.returns(EXCEPTION_TASK_MESSAGE, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Task result properties present")
			.extracting(UserNodeInstructionTaskEntity::getResultProperties, map(String.class, Object.class))
			.as("Result properties present")
			.isNotNull()
			.as("Error message included")
			.containsEntry(MESSAGE_DATA_KEY, expectedException.getMessage())
			.as("Source result property is denied node ID")
			.containsEntry(SOURCE_DATA_KEY, task.getNodeId())
			;
		
		then(userEventAppenderBiz).should().addEvent(eq(TEST_USER_ID), logEventCaptor.capture());
		and.then(logEventCaptor.getAllValues())
			.as("Event for execution generated")
			.hasSize(1)
			.satisfies(events -> {
				and.then(events).element(0)
					.as("Task error event generated")
					.isNotNull()
					.as("Instruction error tags provided in event")
					.returns(INSTRUCTION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
					.as("Task data provided in event data")
					.extracting(e -> getStringMap(e.getData()), map(String.class, Object.class))
					.as("Error event config ID is task ID")
					.containsEntry(CONFIG_ID_DATA_KEY, taskCaptor.getValue().getConfigId())
					.as("Error event contains node ID")
					.containsEntry(NODE_ID_DATA_KEY, taskCaptor.getValue().getNodeId())
					.as("Error message included")
					.containsEntry(MESSAGE_DATA_KEY, expectedException.getMessage())
					.as("Source result property is denied node ID")
					.containsEntry(SOURCE_DATA_KEY, task.getNodeId())
					;
			})
			;
		// @formatter:on
	}

	private NodeInstructionExpressionRoot answerExpressionRoot(InvocationOnMock inv) {
		final SolarNodeOwnership owner = inv.getArgument(0);
		final NodeInstruction instr = inv.getArgument(1);
		final Map<String, ?> params = inv.getArgument(2);
		final DatumStreamsAccessor datumStreamsAccessor = inv.getArgument(3);
		final HttpOperations httpOps = inv.getArgument(4);
		return new NodeInstructionExpressionRoot(owner, instr, params, datumStreamsAccessor, httpOps,
				null, null, null, null);
	}

	@Test
	public void executeTask_expression_setTopic() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		final String controlId = randomString();
		final String controlVal = String.valueOf(randomInt());
		final Map<String, Object> params = Map.of(controlId, controlVal);

		// @formatter:off
		final String expr = randomString();
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						),
				"expressions", List.of(
						Map.of(
								"key", "topic",
								"value", expr
								)
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// evaluate expression, which should update topic to result
		final String exprResult = randomString();
		given(expressionService.sourceIdPathMatcher()).willReturn(sourceIdPathMatcher);
		given(expressionService.createNodeInstructionExpressionRoot(same(owner), any(), any(), any(),
				any())).willAnswer(this::answerExpressionRoot);
		given(expressionService.evaulateExpression(any(), any(), any(), eq(Object.class)))
				.willReturn(exprResult);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);
		result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(instructorBiz).should().queueInstruction(eq(task.getNodeId()), instructionCaptor.capture());
		and.then(instructionCaptor.getValue())
			.as("Instruction to queue topic from expression result")
			.returns(exprResult, from(Instruction::getTopic))
			.as("Instruction to queue has no status")
			.returns(null, from(Instruction::getState))
			.as("Instruction to queue params from task")
			.returns(params, from(Instruction::getParams))
			;
		// @formatter:on
	}

	@Test
	public void executeTask_expression_setExpirationDate() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		final String controlId = randomString();
		final String controlVal = String.valueOf(randomInt());
		final Map<String, Object> params = Map.of(controlId, controlVal);

		// @formatter:off
		final String expr = randomString();
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						),
				"expressions", List.of(
						Map.of(
								"key", "expirationDate",
								"value", expr
								)
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// evaluate expression, which should update topic to result
		final Instant exprResult = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		given(expressionService.sourceIdPathMatcher()).willReturn(sourceIdPathMatcher);
		given(expressionService.createNodeInstructionExpressionRoot(same(owner), any(), any(), any(),
				any())).willAnswer(this::answerExpressionRoot);
		given(expressionService.evaulateExpression(any(), any(), any(), eq(Object.class)))
				.willReturn(exprResult);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);
		result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(instructorBiz).should().queueInstruction(eq(task.getNodeId()), instructionCaptor.capture());
		and.then(instructionCaptor.getValue())
			.as("Instruction to queue topic from task topic")
			.returns(topic, from(Instruction::getTopic))
			.as("Instruction to queue has no status")
			.returns(null, from(Instruction::getState))
			.as("Instruction to queue params from task")
			.returns(params, from(Instruction::getParams))
			.as("Expiration date set from expression result")
			.returns(exprResult, Instruction::getExpirationDate)
			;
		// @formatter:on
	}

	@Test
	public void executeTask_expression_invalidExpirationDate() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		final String controlId = randomString();
		final String controlVal = String.valueOf(randomInt());
		final Map<String, Object> params = Map.of(controlId, controlVal);

		// @formatter:off
		final String expr = randomString();
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						),
				"expressions", List.of(
						Map.of(
								"key", "expirationDate",
								"value", expr
								)
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// evaluate expression, which should update topic to result
		final String exprResult = randomString();
		given(expressionService.sourceIdPathMatcher()).willReturn(sourceIdPathMatcher);
		given(expressionService.createNodeInstructionExpressionRoot(same(owner), any(), any(), any(),
				any())).willAnswer(this::answerExpressionRoot);
		given(expressionService.evaulateExpression(any(), any(), any(), eq(Object.class)))
				.willReturn(exprResult);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);

		// THEN
		// @formatter:off
		thenThrownBy(() -> {
				result.get(1, TimeUnit.MINUTES);
			}, "Execution throws exception")
			.extracting(Throwable::getCause)
			.as("Exception is IllegalArgument")
			.isInstanceOf(IllegalArgumentException.class)
			;

		then(instructorBiz).shouldHaveNoInteractions();

		then(taskDao).should().updateTask(taskCaptor.capture(), eq(Claimed));
		and.then(taskCaptor.getValue())
			.as("Task state updated to Completed because of error")
			.returns(Completed, from(UserNodeInstructionTaskEntity::getState))
			.as("Task exec date unchanged")
			.returns(startingExecDate, from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Last exec date updated to clock time")
			.returns(clock.instant(), from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Task message set")
			.returns(EXCEPTION_TASK_MESSAGE, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Task result properties present")
			.extracting(UserNodeInstructionTaskEntity::getResultProperties, map(String.class, Object.class))
			.as("Result properties present")
			.isNotNull()
			.as("Error message included")
			.containsKey(MESSAGE_DATA_KEY)
			.as("Source result property is the failed expression")
			.containsEntry(SOURCE_DATA_KEY, expr)
			;
		// @formatter:on
	}

	@Test
	public void executeTask_expression_setParameters() throws Exception {
		// GIVEN
		final Duration taskSchedule = Duration.ofHours(1);
		final Instant startingExecDate = clock.instant().minusSeconds(1);
		final String topic = randomString();

		final UserNodeInstructionTaskEntity task = new UserNodeInstructionTaskEntity(TEST_USER_ID,
				randomLong());
		task.setTopic(topic);
		task.setState(Claimed);
		task.setExecuteAt(startingExecDate);
		task.setSchedule(String.valueOf(taskSchedule.toSeconds()));
		task.setNodeId(randomLong());

		final String controlId = randomString();
		final String controlVal = String.valueOf(randomInt());
		final Map<String, Object> params = Map.of(controlId, controlVal);

		// @formatter:off
		final String expr1 = randomString();
		final String expr2 = randomString();
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						),
				"expressions", List.of(
						Map.of(
								"key", "foo",
								"value", expr1
								),
						Map.of(
								"key", "bar",
								"value", expr2
								)
						)
				));
		// @formatter:on

		// execute task
		givenTaskExecution();

		// verify node ownership
		final SolarNodeOwnership owner = ownershipFor(task.getNodeId(), TEST_USER_ID);
		given(nodeOwnershipDao.ownershipForNodeId(task.getNodeId())).willReturn(owner);

		// change task state to Executing
		given(taskDao.updateTaskState(task.getId(), Executing, Claimed)).willReturn(true);

		// evaluate expression, which should update topic to result
		final String exprResult1 = randomString();
		final String exprResult2 = randomString();
		given(expressionService.sourceIdPathMatcher()).willReturn(sourceIdPathMatcher);
		given(expressionService.createNodeInstructionExpressionRoot(same(owner), any(), any(), any(),
				any())).willAnswer(this::answerExpressionRoot);
		given(expressionService.evaulateExpression(eq(expr1), any(), any(), eq(Object.class)))
				.willReturn(exprResult1);
		given(expressionService.evaulateExpression(eq(expr2), any(), any(), eq(Object.class)))
				.willReturn(exprResult2);

		// enqueue instruction
		final ResultCaptor<NodeInstruction> instructionResultCaptor = new ResultCaptor<>(
				this::answerQueuedInstruction);
		given(instructorBiz.queueInstruction(eq(task.getNodeId()), any()))
				.willAnswer(instructionResultCaptor);

		// save updated task with next execution date
		given(taskDao.updateTask(any(), eq(Executing))).willReturn(true);

		// WHEN
		Future<UserNodeInstructionTaskEntity> result = service.executeTask(task);
		result.get(1, TimeUnit.MINUTES);

		// THEN
		// @formatter:off
		then(instructorBiz).should().queueInstruction(eq(task.getNodeId()), instructionCaptor.capture());
		and.then(instructionCaptor.getValue())
			.as("Instruction to queue topic from task topic")
			.returns(topic, from(Instruction::getTopic))
			.as("Instruction to queue has no status")
			.returns(null, from(Instruction::getState))
			.extracting(Instruction::getParams, map(String.class, Object.class))
			.as("Params from task copied to queued instruction")
			.containsAllEntriesOf(params)
			.as("Expression results copied to queued instruction")
			.containsAllEntriesOf(Map.of("foo", exprResult1, "bar", exprResult2))
			;
		// @formatter:on
	}

}
