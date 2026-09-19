/* ==================================================================
 * DaoUserNodeInstructionBiz.java - 16/11/2025 3:03:49 pm
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

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.dao.BasicUserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskSimulationOutput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * DAO implementation of {@link UserNodeInstructionBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserNodeInstructionBiz implements UserNodeInstructionBiz {

	private final UserNodeInstructionService instructionService;
	private final UserNodeInstructionTaskDao instructionTaskDao;

	private @Nullable Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param instructionService
	 *        the instruction service
	 * @param instructionTaskDao
	 *        the instruction task DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserNodeInstructionBiz(UserNodeInstructionService instructionService,
			UserNodeInstructionTaskDao instructionTaskDao) {
		super();
		this.instructionService = requireNonNullArgument(instructionService, "instructionService");
		this.instructionTaskDao = requireNonNullArgument(instructionTaskDao, "instructionTaskDao");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> listControlInstructionTasksForUser(
			Long userId, @Nullable UserNodeInstructionTaskFilter filter) {
		requireNonNullArgument(userId, "userId");
		BasicUserNodeInstructionTaskFilter f = new BasicUserNodeInstructionTaskFilter(filter);
		f.setUserId(userId);
		restrictToSecurityPolicy(f);

		return instructionTaskDao.findFiltered(f);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserNodeInstructionTaskEntity updateControlInstructionTaskState(UserLongCompositePK id,
			BasicClaimableJobState desiredState, BasicClaimableJobState @Nullable... expectedStates) {
		final UserLongCompositePK taskId = requireNonNullArgument(id, "id");
		final BasicClaimableJobState destState = requireNonNullArgument(desiredState, "desiredState");
		if ( !taskId.allKeyComponentsAreAssigned() ) {
			throw new IllegalArgumentException("The userId and configId components must be provided.");
		}

		restrictToSecurityPolicy(instructionTaskDao.get(taskId));

		// only update state if a user-settable value (start, stop)
		if ( destState == BasicClaimableJobState.Queued
				|| destState == BasicClaimableJobState.Completed ) {
			instructionTaskDao.updateTaskState(taskId, destState, expectedStates);
		}
		return nonnull(instructionTaskDao.get(taskId), "Instruction task");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserNodeInstructionTaskEntity saveControlInstructionTask(UserLongCompositePK id,
			UserNodeInstructionTaskEntityInput input,
			BasicClaimableJobState @Nullable... expectedStates) {
		final UserLongCompositePK taskId = requireNonNullArgument(id, "id");
		final UserNodeInstructionTaskEntityInput in = requireNonNullArgument(input, "input");

		validateInput(in);

		final UserNodeInstructionTaskEntity entity = in.toEntity(taskId);
		restrictToSecurityPolicy(entity);
		UserLongCompositePK pk = taskId;
		if ( expectedStates == null || expectedStates.length < 1 ) {
			pk = instructionTaskDao.save(entity);
		} else {
			if ( !taskId.allKeyComponentsAreAssigned() ) {
				throw new IllegalArgumentException(
						"The userId and configId components must be provided.");
			}
			instructionTaskDao.updateTask(entity, expectedStates);
		}
		return nonnull(instructionTaskDao.get(pk), "Instruction task");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteControlInstructionTask(UserLongCompositePK id) {
		final UserLongCompositePK taskId = requireNonNullArgument(id, "id");

		final var task = instructionTaskDao.get(taskId);
		if ( task == null ) {
			return;
		}
		restrictToSecurityPolicy(task);

		instructionTaskDao.delete(task);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	@Override
	public UserNodeInstructionTaskSimulationOutput simulateControlInstructionTaskForUser(
			final Long userId, final UserNodeInstructionTaskEntityInput input) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(input, "input");

		// force stating state to match execution job
		input.setState(BasicClaimableJobState.Queued);
		input.setExecuteAt(Instant.now());

		validateInput(input);

		final UserNodeInstructionTaskEntity task = input.toEntity(unassignedEntityIdKey(userId));
		restrictToSecurityPolicy(task);

		return instructionService.simulateControlInstructionTask(task);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void updateControlInstructionTaskEnabled(UserLongCompositePK id, boolean enabled) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");

		BasicUserNodeInstructionTaskFilter filter = new BasicUserNodeInstructionTaskFilter();
		if ( id.entityIdIsAssigned() ) {
			filter.setTaskId(id.getEntityId());
		}
		restrictToSecurityPolicy(filter);

		instructionTaskDao.updateEnabledStatus(id.getUserId(), filter, enabled);
	}

	private void validateInput(final Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final @Nullable Object input, final @Nullable Validator v) {
		if ( input == null || v == null ) {
			return;
		}
		var violations = v.validate(input);
		if ( violations == null || violations.isEmpty() ) {
			return;
		}
		BindingResult errors = ExceptionUtils
				.toBindingResult(new ConstraintViolationException(violations), v);
		if ( errors.hasErrors() ) {
			throw new ValidationException(errors);
		}
	}

	private void restrictToSecurityPolicy(BasicUserNodeInstructionTaskFilter f) {
		final SecurityPolicy policy = SecurityUtils.getActiveSecurityPolicy();
		f.setNodeIds(SecurityUtils.restrictNodeIds(f.getNodeIds(), policy));
	}

	private void restrictToSecurityPolicy(@Nullable UserNodeInstructionTaskEntity task) {
		if ( task == null || task.getNodeId() == null ) {
			return;
		}
		final SecurityPolicy policy = SecurityUtils.getActiveSecurityPolicy();
		if ( policy != null && policy.getNodeIds() != null && policy.getNodeIds() != null
				&& !policy.getNodeIds().contains(task.getNodeId()) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, task.getNodeId());
		}
	}

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public final @Nullable Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public final void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

}
