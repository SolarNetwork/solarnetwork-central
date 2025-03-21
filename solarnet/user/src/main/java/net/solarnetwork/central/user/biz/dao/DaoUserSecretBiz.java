/* ==================================================================
 * DaoUserSecretBiz.java - 22/03/2025 9:13:34 am
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
import java.time.Instant;
import java.time.InstantSource;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.UserSecretEntityDao;
import net.solarnetwork.central.user.dao.UserSecretFilter;
import net.solarnetwork.central.user.domain.UserSecret;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.central.user.domain.UserSecretInput;
import net.solarnetwork.dao.FilterResults;

/**
 * DAO implementation of {@link UserSecretBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserSecretBiz implements UserSecretBiz {

	private final InstantSource clock;
	private final UserSecretEntityDao secretDao;
	private final BytesEncryptor bytesEncryptor;

	private Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param clock
	 *        the clock to use
	 * @param bytesEncryptor
	 *        the encryptor to use
	 * @param secretDao
	 *        the secret DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserSecretBiz(InstantSource clock, BytesEncryptor bytesEncryptor,
			UserSecretEntityDao secretDao) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.bytesEncryptor = requireNonNullArgument(bytesEncryptor, "bytesEncryptor");
		this.secretDao = requireNonNullArgument(secretDao, "secretDao");
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public UserSecret saveUserSecret(Long userId, String topicId, String key, UserSecretInput input) {
		requireNonNullArgument(userId, "id");
		requireNonNullArgument(topicId, "topicId");
		requireNonNullArgument(key, "key");

		validateInput(requireNonNullArgument(input, "input"));

		Instant now = clock.instant();

		UserSecretEntity entity = new UserSecretEntity(userId, topicId, key, now, now,
				bytesEncryptor.encrypt(input.getSecret()));
		var id = secretDao.save(entity);
		return (id != null ? secretDao.get(id) : null);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserSecret(Long userId, String topicId, String key) {
		requireNonNullArgument(userId, "id");

		var pk = secretDao.entityKey(new UserStringStringCompositePK(userId,
				topicId != null ? topicId : UserStringStringCompositePK.UNASSIGNED_GROUP_ID,
				key != null ? key : UserStringStringCompositePK.UNASSIGNED_ENTITY_ID));

		secretDao.delete(pk);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	@Override
	public FilterResults<UserSecretEntity, UserStringStringCompositePK> listSecretsForUser(Long userId,
			UserSecretFilter filter) {
		BasicUserSecretFilter f = new BasicUserSecretFilter(filter);
		f.setUserId(userId);
		return secretDao.findFiltered(filter);
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
