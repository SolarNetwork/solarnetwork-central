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
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.ACCOUNTING_DATA_PROP;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnFeatureEntitlement;
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public SnAccountInfo createAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);

		final SnAccountCreationInput req = requireNonNullArgument(input, "input");
		validateInput(req);

		final BillingSystemRegistrar registrar = registrarForKey(input.getSystemKey());

		var account = registrar.createAccount(user.id(), input);

		// assign user to this billing system
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, registrar.getAccountingSystemKey());
		userDao.storeInternalData(user.id(), user.getInternalData());

		// assign requested entitlements
		Set<String> roles = setupRoles(userDao.getUserRoles(user), input.getRequestedEntitlements());
		userDao.storeUserRoles(user, roles);

		return new SnAccountInfo(registrar.getAccountingSystemKey(), account,
				input.getRequestedEntitlements() != null && !input.getRequestedEntitlements().isEmpty()
						? EnumSet.copyOf(input.getRequestedEntitlements())
						: Set.of());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public SnAccountInfo updateAccount(Long userId, SnAccountCreationInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);

		// for now ignore any key on the input (disallow changing registrars)
		final Object accountKey = requireNonNullObject(user.getInternalDataValue(ACCOUNTING_DATA_PROP),
				"Account");

		final SnAccountCreationInput req = requireNonNullArgument(input, "input");
		validateInput(req);

		final BillingSystemRegistrar registrar = registrarForKey(accountKey.toString());

		var account = registrar.updateAccount(user.id(), input);

		// assign requested entitlements
		Set<String> existingRoles = userDao.getUserRoles(user);
		Set<String> roles = setupRoles(existingRoles, input.getRequestedEntitlements());
		if ( !existingRoles.equals(roles) ) {
			userDao.storeUserRoles(user, roles);
		}

		return new SnAccountInfo(registrar.getAccountingSystemKey(), account, userEntitlements(user));
	}

	@Override
	public SnAccountInfo getAccountForUser(Long userId)
			throws IllegalArgumentException, AuthorizationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);
		final Object systemKey = user.getInternalDataValue(ACCOUNTING_DATA_PROP);
		if ( systemKey == null ) {
			throw new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, userId);
		}
		for ( BillingSystemRegistrar r : registrars ) {
			if ( r.supportsAccountingSystemKey(systemKey.toString()) ) {
				var acct = r.getAccountForUser(user);
				return new SnAccountInfo(systemKey.toString(), acct, userEntitlements(user));
			}
		}
		throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
	}

	private Set<String> setupRoles(Set<String> existingRoles,
			@Nullable Set<SnFeatureEntitlement> entitlements) {
		var result = new LinkedHashSet<String>(existingRoles);

		// remove any entitlement roles not specified
		for ( SnFeatureEntitlement entitlement : entitlements == null || entitlements.isEmpty()
				? EnumSet.allOf(SnFeatureEntitlement.class)
				: EnumSet.complementOf(EnumSet.copyOf(entitlements)) ) {
			result.remove(entitlement.getRoleName());
		}

		// always assign billing
		result.add("ROLE_BILLING");

		// add requested entitlements
		if ( entitlements != null && !entitlements.isEmpty() ) {
			for ( SnFeatureEntitlement entitlement : entitlements ) {
				result.add(entitlement.getRoleName());
			}
		}

		return result;
	}

	private Set<SnFeatureEntitlement> userEntitlements(final User user) {
		Set<String> roles = userDao.getUserRoles(user);
		Set<SnFeatureEntitlement> entitlements = new HashSet<>(roles.size());
		for ( String role : roles ) {
			for ( SnFeatureEntitlement entitlement : SnFeatureEntitlement.values() ) {
				if ( role.equals(entitlement.getRoleName()) ) {
					entitlements.add(entitlement);
					break;
				}
			}
		}
		return (!entitlements.isEmpty() ? EnumSet.copyOf(entitlements) : Set.of());
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
