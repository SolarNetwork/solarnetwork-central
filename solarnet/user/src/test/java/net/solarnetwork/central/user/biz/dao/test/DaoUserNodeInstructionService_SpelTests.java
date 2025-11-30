/* ==================================================================
 * DaoUserNodeInstructionService_SpelTests.java - 28/11/2025 9:59:21 am
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
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.test.ResultCaptor;
import net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionService;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UsersUserEvents;
import net.solarnetwork.central.user.support.BasicInstructionsExpressionService;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link DaoUserNodeInstructionService} class using the Spel
 * expression implementation.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserNodeInstructionService_SpelTests implements CommonUserEvents, UsersUserEvents {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private ExecutorService executor;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private InstructorBiz instructorBiz;

	@Mock
	private BasicInstructionsExpressionService expressionService;

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

	private DaoUserNodeInstructionService service;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(now().truncatedTo(ChronoUnit.HOURS), UTC);

		expressionService = new BasicInstructionsExpressionService();

		service = new DaoUserNodeInstructionService(clock, executor, JsonUtils.newObjectMapper(),
				userEventAppenderBiz, instructorBiz, expressionService, nodeOwnershipDao, taskDao,
				datumDao, datumStreamMetadataDao);
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
		task.setServiceProps(Map.of(
				"instruction", Map.of(
						"topic", topic,
						"params", params
						),
				"expressions", List.of(
						Map.of(
								"key", "foo",
								"value", "instruction.topic + '_' + parameters['%s'] + '_changed'".formatted(controlId)
								),
						Map.of(
								"key", "bar",
								"value", "'foo: ' + #foo"
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
			.as("Expression results copied to queued instruction; expressions refer to parameters as variables")
			.containsAllEntriesOf(Map.of(
					"foo", "%s_%s_changed".formatted(topic, controlVal), 
					"bar", "foo: %s_%s_changed".formatted(topic, controlVal)))
			;
		// @formatter:on
	}

}
