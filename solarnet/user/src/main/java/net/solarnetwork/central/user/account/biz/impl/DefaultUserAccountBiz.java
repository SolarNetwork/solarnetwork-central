/* ==================================================================
 * DefaultUserAccountBiz.java - 26 Sept 2026 7:29:23 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.account.biz.impl;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Default implementation of {@link UserAccountBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DefaultUserAccountBiz implements UserAccountBiz {

	private final List<BillingSystemRegistrar> registrars;
	private final UserDao userDao;

	private @Nullable Validator validator;

	/**
	 * Constructor.
	 * 
	 * @param registrars
	 *        the available registrars; the <b>first</b> registrar will be
	 *        treated as the default system to use if no specific system is
	 *        specified
	 * @param userDao
	 *        the user DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DefaultUserAccountBiz(List<BillingSystemRegistrar> registrars, UserDao userDao) {
		super();
		this.registrars = requireNonNullArgument(registrars, "registrars");
		this.userDao = requireNonNullArgument(userDao, "userDao");
	}

	@Override
	public SnAccount<?, ?, ?> createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);

		final SnAccountCreationInput req = requireNonNullArgument(input, "input");
		validateInput(req);

		final BillingSystemRegistrar registrar = registrarForKey(input.getSystemKey());

		return registrar.createAccount(user.id(), input);
	}

	private BillingSystemRegistrar registrarForKey(@Nullable String key) {
		if ( registrars.isEmpty() ) {
			throw new IllegalArgumentException("The system key [%s] is not supported.".formatted(key));
		}
		if ( key == null || key.isEmpty() ) {
			return registrars.getFirst();
		}
		for ( BillingSystemRegistrar r : registrars ) {
			if ( r.supportsAccountingSystemKey(key) ) {
				return r;
			}
		}
		throw new IllegalArgumentException("The system key [%s] is not supported.".formatted(key));
	}

	private void validateInput(final @Nullable Object input) {
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

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public @Nullable Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

}
