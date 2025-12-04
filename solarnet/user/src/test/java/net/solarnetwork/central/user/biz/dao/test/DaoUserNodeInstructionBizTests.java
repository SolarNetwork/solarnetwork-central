/* ==================================================================
 * DaoUserNodeInstructionBizTests.java - 16/11/2025 3:33:34 pm
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
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.biz.dao.DaoUserNodeInstructionBiz;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskSimulationOutput;

/**
 * Test cases for the {@link UserNodeInstructionBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserNodeInstructionBizTests {

	private final static String TEST_SCHEDULE = "60";

	@Mock
	private UserNodeInstructionService instructionService;

	@Mock
	private UserNodeInstructionTaskDao instructionTaskDao;

	@Captor
	private ArgumentCaptor<UserNodeInstructionTaskEntity> instructionTaskCaptor;

	@Captor
	private ArgumentCaptor<UserNodeInstructionTaskFilter> instructionTaskFilterCaptor;

	private DaoUserNodeInstructionBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserNodeInstructionBiz(instructionService, instructionTaskDao);

		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		biz.setValidator(factory.getValidator());
	}

	@Test
	public void controlInstructionTaskEntity_save_create() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		// save and retrieve
		given(instructionTaskDao.save(any(UserNodeInstructionTaskEntity.class))).willReturn(pk);
		given(instructionTaskDao.get(pk)).willReturn(entity);

		// WHEN
		final Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		UserNodeInstructionTaskEntityInput input = new UserNodeInstructionTaskEntityInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setTopic(randomString());
		input.setSchedule(TEST_SCHEDULE);
		input.setState(Queued);
		input.setExecuteAt(now());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		UserLongCompositePK unassignedPk = UserLongCompositePK.unassignedEntityIdKey(userId);
		UserNodeInstructionTaskEntity result = biz.saveControlInstructionTask(unassignedPk, input);

		// THEN
		// @formatter:off
		then(instructionTaskDao).should().save(instructionTaskCaptor.capture());

		and.then(instructionTaskCaptor.getValue())
			.as("Entity ID on DAO save is argument to service")
			.returns(unassignedPk, from(UserNodeInstructionTaskEntity::getId))
			.as("Enabled from input passed to DAO")
			.returns(true, from(UserNodeInstructionTaskEntity::isEnabled))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(UserNodeInstructionTaskEntity::getName))
			.as("Node ID from input passed to DAO")
			.returns(input.getNodeId(), from(UserNodeInstructionTaskEntity::getNodeId))
			.as("Topic from input passed to DAO")
			.returns(input.getTopic(), from(UserNodeInstructionTaskEntity::getTopic))
			.as("Schedule from input passed to DAO")
			.returns(input.getSchedule(), from(UserNodeInstructionTaskEntity::getSchedule))
			.as("State from input passed to DAO")
			.returns(input.getState(), from(UserNodeInstructionTaskEntity::getState))
			.as("Exec date input passed to DAO")
			.returns(input.getExecuteAt(), from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Service properties from input passed to DAO")
			.returns(sprops, from(UserNodeInstructionTaskEntity::getServiceProperties))
			.as("Message is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Last execut at is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Result properties is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getResultProperties))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void controlInstructionTaskEntity_save_update() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		// save and retrieve
		given(instructionTaskDao.save(any(UserNodeInstructionTaskEntity.class))).willReturn(pk);
		given(instructionTaskDao.get(pk)).willReturn(entity);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		UserNodeInstructionTaskEntityInput input = new UserNodeInstructionTaskEntityInput();
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setTopic(randomString());
		input.setSchedule(TEST_SCHEDULE);
		input.setState(Queued);
		input.setExecuteAt(now());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		UserNodeInstructionTaskEntity result = biz.saveControlInstructionTask(pk, input);

		// THEN
		// @formatter:off
		then(instructionTaskDao).should().save(instructionTaskCaptor.capture());

		and.then(instructionTaskCaptor.getValue())
			.as("Entity ID on DAO save is argument to service")
			.returns(pk, from(UserNodeInstructionTaskEntity::getId))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(UserNodeInstructionTaskEntity::getName))
			.as("Node ID from input passed to DAO")
			.returns(input.getNodeId(), from(UserNodeInstructionTaskEntity::getNodeId))
			.as("Topic from input passed to DAO")
			.returns(input.getTopic(), from(UserNodeInstructionTaskEntity::getTopic))
			.as("Schedule from input passed to DAO")
			.returns(input.getSchedule(), from(UserNodeInstructionTaskEntity::getSchedule))
			.as("State from input passed to DAO")
			.returns(input.getState(), from(UserNodeInstructionTaskEntity::getState))
			.as("Exec date input passed to DAO")
			.returns(input.getExecuteAt(), from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(UserNodeInstructionTaskEntity::getServiceProperties))
			.as("Message is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Last execut at is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Result properties is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getResultProperties))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void controlInstructionTaskEntity_save_invalidState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		UserNodeInstructionTaskEntityInput input = new UserNodeInstructionTaskEntityInput();
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setTopic(randomString());
		input.setSchedule(TEST_SCHEDULE);
		input.setExecuteAt(now());

		// WHEN
		for ( BasicClaimableJobState state : EnumSet.complementOf(EnumSet.of(Queued, Completed)) ) {
			input.setState(state);
			ValidationException ex = catchThrowableOfType(ValidationException.class,
					() -> biz.saveControlInstructionTask(pk, input));

			// THEN
			// @formatter:off
			and.then(ex)
				.as("Validation exception is thrown because not allowed set %s state", state)
				.isNotNull()
				.extracting(e -> e.getErrors().getFieldError("state"))
				.as("Validation is on the state field")
				.isNotNull()
				;
			// @formatter:on
		}
	}

	@Test
	public void controlInstructionTaskEntity_save_expectedState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		// save and retrieve
		given(instructionTaskDao.updateTask(any(UserNodeInstructionTaskEntity.class), eq(Completed)))
				.willReturn(true);
		given(instructionTaskDao.get(pk)).willReturn(entity);

		// WHEN
		Map<String, Object> sprops = new LinkedHashMap<>(4);
		sprops.put("foo", "bar");

		UserNodeInstructionTaskEntityInput input = new UserNodeInstructionTaskEntityInput();
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setTopic(randomString());
		input.setSchedule(TEST_SCHEDULE);
		input.setState(Queued);
		input.setExecuteAt(now());
		input.setServiceProperties(new LinkedHashMap<>(sprops));
		UserNodeInstructionTaskEntity result = biz.saveControlInstructionTask(pk, input, Completed);

		// THEN
		// @formatter:off
		then(instructionTaskDao).should().updateTask(instructionTaskCaptor.capture(), eq(Completed));

		and.then(instructionTaskCaptor.getValue())
			.as("Entity ID on DAO save is argument to service")
			.returns(pk, from(UserNodeInstructionTaskEntity::getId))
			.as("Name from input passed to DAO")
			.returns(input.getName(), from(UserNodeInstructionTaskEntity::getName))
			.as("Node ID from input passed to DAO")
			.returns(input.getNodeId(), from(UserNodeInstructionTaskEntity::getNodeId))
			.as("Topic from input passed to DAO")
			.returns(input.getTopic(), from(UserNodeInstructionTaskEntity::getTopic))
			.as("Schedule from input passed to DAO")
			.returns(input.getSchedule(), from(UserNodeInstructionTaskEntity::getSchedule))
			.as("State from input passed to DAO")
			.returns(input.getState(), from(UserNodeInstructionTaskEntity::getState))
			.as("Exec date input passed to DAO")
			.returns(input.getExecuteAt(), from(UserNodeInstructionTaskEntity::getExecuteAt))
			.as("Service properties from input passed to DAO")
			.returns(input.getServiceProperties(), from(UserNodeInstructionTaskEntity::getServiceProperties))
			.as("Message is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getMessage))
			.as("Last execut at is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getLastExecuteAt))
			.as("Result properties is null")
			.returns(null, from(UserNodeInstructionTaskEntity::getResultProperties))
			;

		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void controlInstructionTaskEntity_updateState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		// save and retrieve
		given(instructionTaskDao.updateTaskState(pk, Queued)).willReturn(true);
		given(instructionTaskDao.get(pk)).willReturn(entity);

		// WHEN
		UserNodeInstructionTaskEntity result = biz.updateControlInstructionTaskState(pk, Queued);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void controlInstructionTaskEntity_updateState_expectedState() {
		// GIVEN
		Long userId = randomLong();
		Long entityId = randomLong();
		UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		// save and retrieve
		given(instructionTaskDao.updateTaskState(pk, Queued, Completed)).willReturn(true);
		given(instructionTaskDao.get(pk)).willReturn(entity);

		// WHEN
		UserNodeInstructionTaskEntity result = biz.updateControlInstructionTaskState(pk, Queued,
				Completed);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided from DAO")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void controlInstructionTaskEntity_delete() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);
		final UserNodeInstructionTaskEntity entity = new UserNodeInstructionTaskEntity(pk);

		given(instructionTaskDao.entityKey(pk)).willReturn(entity);

		// WHEN
		biz.deleteControlInstructionTask(pk);

		// THEN
		// @formatter:off
		then(instructionTaskDao).should().delete(instructionTaskCaptor.capture());

		and.then(instructionTaskCaptor.getValue())
			.as("DAO passed entity returned from entityKey()")
			.isSameAs(entity)
			;
		// @formatter:on
	}

	@Test
	public void updateEnabled_forTask() {
		// GIVEN
		final Long userId = randomLong();
		final Long entityId = randomLong();
		final UserLongCompositePK pk = new UserLongCompositePK(userId, entityId);

		// WHEN
		final boolean enabled = CommonTestUtils.randomBoolean();
		biz.updateControlInstructionTaskEnabled(pk, enabled);

		// THEN
		// @formatter:off
		then(instructionTaskDao).should().updateEnabledStatus(eq(userId), instructionTaskFilterCaptor.capture(), eq(enabled));
		and.then(instructionTaskFilterCaptor.getValue())
			.as("Task ID provided in DAO filter")
			.returns(entityId, from(UserNodeInstructionTaskFilter::getTaskId))
			;
		// @formatter:on
	}

	@Test
	public void updateEnabled_forUser() {
		// GIVEN
		final Long userId = randomLong();
		final UserLongCompositePK pk = UserLongCompositePK.unassignedEntityIdKey(userId);

		// WHEN
		final boolean enabled = CommonTestUtils.randomBoolean();
		biz.updateControlInstructionTaskEnabled(pk, enabled);

		// THEN
		then(instructionTaskDao).should().updateEnabledStatus(eq(userId), isNull(), eq(enabled));
	}

	@Test
	public void simulate() {
		// GIVEN
		final Long userId = randomLong();

		UserNodeInstructionTaskEntityInput input = new UserNodeInstructionTaskEntityInput();
		input.setName(randomString());
		input.setNodeId(randomLong());
		input.setTopic(randomString());
		input.setSchedule(TEST_SCHEDULE);
		input.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		input.setServiceProperties(Map.of(
				"instruction", Map.of(
						"params", Map.of("foo", "bar")
						)
				));
		// @formatter:on

		final UserNodeInstructionTaskEntity task = input
				.toEntity(UserLongCompositePK.unassignedEntityIdKey(userId));
		final UserNodeInstructionTaskSimulationOutput serviceOutput = new UserNodeInstructionTaskSimulationOutput(
				task, new Instruction(), List.of(), List.of(), TEST_SCHEDULE);
		given(instructionService.simulateControlInstructionTask(any())).willReturn(serviceOutput);

		// WHEN
		UserNodeInstructionTaskSimulationOutput result = biz
				.simulateControlInstructionTaskForUser(userId, input);

		// THEN
		// @formatter:off
		then(instructionService).should().simulateControlInstructionTask(instructionTaskCaptor.capture());
		and.then(instructionTaskCaptor.getValue())
			.as("Task passed to service created from input")
			.usingRecursiveComparison()
			.ignoringFields("executeAt", "state")
			.isEqualTo(task)
			;
		
		and.then(result)
			.as("Result from service returned")
			.isSameAs(serviceOutput)
			;
		// @formatter:on
	}

}
