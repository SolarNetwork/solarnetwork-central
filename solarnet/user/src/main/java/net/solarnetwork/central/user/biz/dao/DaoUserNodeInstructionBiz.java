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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
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

/**
 * DAO implementation of {@link UserNodeInstructionBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserNodeInstructionBiz implements UserNodeInstructionBiz {

	private final UserNodeInstructionService instructionService;
	private final UserNodeInstructionTaskDao instructionTaskDao;
	private final TextEncryptor textEncryptor;

	private Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param instructionService
	 *        the instruction service
	 * @param instructionTaskDao
	 *        the instruction task DAO
	 * @param textEncryptor
	 *        the text encryptor to use for task settings
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserNodeInstructionBiz(UserNodeInstructionService instructionService,
			UserNodeInstructionTaskDao instructionTaskDao, TextEncryptor textEncryptor) {
		super();
		this.instructionService = requireNonNullArgument(instructionService, "instructionService");
		this.instructionTaskDao = requireNonNullArgument(instructionTaskDao, "instructionTaskDao");
		this.textEncryptor = requireNonNullArgument(textEncryptor, "textEncryptor");

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> listControlInstructionTasksForUser(
			Long userId, UserNodeInstructionTaskFilter filter) {
		requireNonNullArgument(userId, "userId");
		BasicUserNodeInstructionTaskFilter f = new BasicUserNodeInstructionTaskFilter(filter);
		f.setUserId(userId);
		return digestSensitiveInformation(
				instructionTaskDao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax()));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserNodeInstructionTaskEntity updateControlInstructionTaskState(UserLongCompositePK id,
			BasicClaimableJobState desiredState, BasicClaimableJobState... expectedStates) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(desiredState, "desiredState");
		if ( !id.allKeyComponentsAreAssigned() ) {
			throw new IllegalArgumentException("The userId and configId components must be provided.");
		}
		// only update state if a user-settable value (start, stop)
		if ( desiredState == BasicClaimableJobState.Queued
				|| desiredState == BasicClaimableJobState.Completed ) {
			instructionTaskDao.updateTaskState(id, desiredState, expectedStates);
		}
		return digestSecureSettings(instructionTaskDao.get(id));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserNodeInstructionTaskEntity saveControlInstructionTask(UserLongCompositePK id,
			UserNodeInstructionTaskEntityInput input, BasicClaimableJobState... expectedStates) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(input, "input");

		validateInput(input);

		final UserNodeInstructionTaskEntity entity = input.toEntity(id);
		entity.encryptSettings(textEncryptor::encrypt);
		UserLongCompositePK pk = id;
		if ( expectedStates == null || expectedStates.length < 1 ) {
			pk = instructionTaskDao.save(entity);
		} else {
			if ( !id.allKeyComponentsAreAssigned() ) {
				throw new IllegalArgumentException(
						"The userId and configId components must be provided.");
			}
			instructionTaskDao.updateTask(entity, expectedStates);
		}
		return digestSecureSettings(instructionTaskDao.get(pk));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteControlInstructionTask(UserLongCompositePK id) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		instructionTaskDao.delete(instructionTaskDao.entityKey(id));
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
		task.encryptSettings(textEncryptor::encrypt);
		UserNodeInstructionTaskSimulationOutput result = instructionService
				.simulateControlInstructionTask(task);
		if ( result != null ) {
			digestSecureSettings(result.getTask());
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void updateControlInstructionTaskEnabled(UserLongCompositePK id, boolean enabled) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");

		BasicUserNodeInstructionTaskFilter filter = null;
		if ( id.entityIdIsAssigned() ) {
			filter = new BasicUserNodeInstructionTaskFilter();
			filter.setTaskId(id.getEntityId());
		}

		instructionTaskDao.updateEnabledStatus(id.getUserId(), filter, enabled);
	}

	private void validateInput(final Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final Object input, final Validator v) {
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

	private FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> digestSensitiveInformation(
			FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> results) {
		if ( results == null || results.getReturnedResultCount() < 1 ) {
			return results;
		}
		for ( UserNodeInstructionTaskEntity entity : results ) {
			digestSecureSettings(entity);
		}
		return results;
	}

	private UserNodeInstructionTaskEntity digestSecureSettings(UserNodeInstructionTaskEntity entity) {
		if ( entity != null ) {
			entity.digestSensitiveInformation();
		}
		return entity;
	}

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

}
