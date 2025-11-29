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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
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
import net.solarnetwork.central.user.dao.BasicUserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskFilter;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO implementation of {@link UserNodeInstructionBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserNodeInstructionBiz implements UserNodeInstructionBiz {

	private final UserNodeInstructionTaskDao controlInstructionTaskDao;

	private Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param controlInstructionTaskDao
	 *        the instruction task DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserNodeInstructionBiz(UserNodeInstructionTaskDao controlInstructionTaskDao) {
		super();
		this.controlInstructionTaskDao = requireNonNullArgument(controlInstructionTaskDao,
				"controlInstructionTaskDao");

	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<UserNodeInstructionTaskEntity, UserLongCompositePK> listControlInstructionTasksForUser(
			Long userId, UserNodeInstructionTaskFilter filter) {
		requireNonNullArgument(userId, "userId");
		BasicUserNodeInstructionTaskFilter f = new BasicUserNodeInstructionTaskFilter(filter);
		f.setUserId(userId);
		return controlInstructionTaskDao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());
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
			controlInstructionTaskDao.updateTaskState(id, desiredState, expectedStates);
		}
		return controlInstructionTaskDao.get(id);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserNodeInstructionTaskEntity saveControlInstructionTask(UserLongCompositePK id,
			UserNodeInstructionTaskEntityInput input, BasicClaimableJobState... expectedStates) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(input, "input");

		validateInput(input);

		UserNodeInstructionTaskEntity entity = input.toEntity(id);
		UserLongCompositePK pk = id;
		if ( expectedStates == null || expectedStates.length < 1 ) {
			pk = controlInstructionTaskDao.save(entity);
		} else {
			if ( !id.allKeyComponentsAreAssigned() ) {
				throw new IllegalArgumentException(
						"The userId and configId components must be provided.");
			}
			controlInstructionTaskDao.updateTask(entity, expectedStates);
		}
		return controlInstructionTaskDao.get(pk);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteControlInstructionTask(UserLongCompositePK id) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		controlInstructionTaskDao.delete(controlInstructionTaskDao.entityKey(id));
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
