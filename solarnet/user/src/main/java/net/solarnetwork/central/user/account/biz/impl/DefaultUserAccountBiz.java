/* ==================================================================
 * DefaultUserAccountBiz.java - 26 Sept 2026 7:29:23 pm
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
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.INVOICE_CONSOLIDATION_DATA_PROP;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.USER_IDS_PROP;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountLinkInput;
import net.solarnetwork.central.user.account.domain.SnFeatureEntitlement;
import net.solarnetwork.central.user.account.domain.SnLinkedAccountInfo;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Default implementation of {@link UserAccountBiz}.
 *
 * <p>
 * Linking a child account to a parent account adds the child to the invoice
 * consolidation group of the parent, which is stored in the internal data of
 * the parent account user. Linking requires both a
 * {@link #setPasswordEncoder(PasswordEncoder)} to verify the credentials of the
 * account to link, and an
 * {@link #setConsolidationResolver(InvoiceConsolidationResolver)} to check the
 * resulting group.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class DefaultUserAccountBiz implements UserAccountBiz {

	/** The error code for account credentials that are not valid. */
	public static final String LINK_CREDENTIALS_ERROR_CODE = "account.consolidated.error.credentials";

	/** The error code for linking an account to itself. */
	public static final String LINK_SELF_ERROR_CODE = "account.consolidated.error.self";

	/** The error code for linking a child account that is already linked. */
	public static final String LINK_LINKED_ERROR_CODE = "account.consolidated.error.linked";

	/** The error code for linking a child account that has children. */
	public static final String LINK_PARENT_ERROR_CODE = "account.consolidated.error.parent";

	/** The error code for linking to a parent account that is a child. */
	public static final String LINK_PARENT_LINKED_ERROR_CODE = "account.consolidated.error.parentLinked";

	/**
	 * The error code for linking accounts that can not be invoiced together.
	 */
	public static final String LINK_INVALID_ERROR_CODE = "account.consolidated.error.invalid";

	/** The error code for account linking not being available. */
	public static final String LINK_UNAVAILABLE_ERROR_CODE = "account.consolidated.error.unavailable";

	/**
	 * The messages to render exception messages with, which are their default
	 * messages.
	 */
	private static final MessageSource DEFAULT_MESSAGES = new StaticMessageSource();

	private final List<BillingSystemRegistrar> registrars;
	private final UserDao userDao;

	private @Nullable Validator validator;
	private @Nullable PasswordEncoder passwordEncoder;
	private @Nullable InvoiceConsolidationResolver consolidationResolver;

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

		// verify an account doesn't already exist
		if ( user.getInternalDataValue(ACCOUNTING_DATA_PROP) != null ) {
			throw new AuthorizationException(Reason.REGISTRATION_ALREADY_CONFIRMED, userId);
		}

		final SnAccountCreationInput req = requireNonNullArgument(input, "input");
		validateInput(req);

		// verify the parent account before creating anything
		final User parentUser = parentAccountUser(input.getParentAccount());

		final BillingSystemRegistrar registrar = registrarForKey(input.getSystemKey());

		var account = registrar.createAccount(user.id(), input);

		// assign user to this billing system
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, registrar.getAccountingSystemKey());
		userDao.storeInternalData(user.id(), user.getInternalData());

		// assign requested entitlements
		Set<String> roles = setupRoles(userDao.getUserRoles(user), input.getRequestedEntitlements());
		userDao.storeUserRoles(user, roles);

		// link to the parent account, now the account exists to check against it
		if ( parentUser != null ) {
			link(parentUser, user);
		}

		return new SnAccountInfo(registrar.getAccountingSystemKey(), account,
				input.getRequestedEntitlements() != null && !input.getRequestedEntitlements().isEmpty()
						? EnumSet.copyOf(input.getRequestedEntitlements())
						: Set.of(),
				(parentUser != null ? SnLinkedAccountInfo.forUser(parentUser) : null), null);
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

		// verify the parent account before changing anything
		final User parentUser = parentAccountUser(input.getParentAccount());

		final BillingSystemRegistrar registrar = registrarForKey(accountKey.toString());

		var account = registrar.updateAccount(user.id(), input);

		// assign requested entitlements
		Set<String> existingRoles = userDao.getUserRoles(user);
		Set<String> roles = setupRoles(existingRoles, input.getRequestedEntitlements());
		if ( !existingRoles.equals(roles) ) {
			userDao.storeUserRoles(user, roles);
		}

		// link to the parent account, now the account is updated to check against it
		if ( parentUser != null ) {
			link(parentUser, user);
		}

		return new SnAccountInfo(registrar.getAccountingSystemKey(), account, userEntitlements(user),
				(parentUser != null ? SnLinkedAccountInfo.forUser(parentUser) : null), null);
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

				SnLinkedAccountInfo parent = null;
				List<SnLinkedAccountInfo> children = null;
				final InvoiceConsolidationResolver resolver = this.consolidationResolver;
				final InvoiceConsolidationGroup group = (resolver != null ? resolver.groupForUser(userId)
						: null);
				if ( group != null && group.isPrimary(userId) ) {
					children = linkedAccounts(group.memberUserIds());
					if ( children.isEmpty() ) {
						children = null;
					}
				} else if ( group != null ) {
					final User parentUser = userDao.get(group.primaryUserId());
					parent = (parentUser != null ? SnLinkedAccountInfo.forUser(parentUser) : null);
				}

				return new SnAccountInfo(systemKey.toString(), acct, userEntitlements(user), parent,
						children, group != null ? group.problem() : null);
			}
		}
		throw new AuthorizationException(Reason.UNKNOWN_OBJECT, userId);
	}

	@Override
	public SnLinkedAccountInfo verifyParentAccount(Long userId, SnAccountLinkInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);
		final User parentUser = authenticatedUser(requireNonNullArgument(input, "input"));
		validateLink(parentUser, user);
		final SnLinkedAccountInfo result = SnLinkedAccountInfo.forUser(parentUser);
		input.verified(result);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public SnLinkedAccountInfo linkAccount(Long userId, SnAccountLinkInput input)
			throws IllegalArgumentException, AuthorizationException, ValidationException {
		final User user = requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")),
				userId);
		final User childUser = authenticatedUser(requireNonNullArgument(input, "input"));
		link(user, childUser);
		return SnLinkedAccountInfo.forUser(childUser);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Collection<SnLinkedAccountInfo> unlinkAccount(Long userId, Long childUserId)
			throws IllegalArgumentException, AuthorizationException {
		requireNonNullObject(userDao.get(requireNonNullArgument(userId, "userId")), userId);
		requireNonNullArgument(childUserId, "childUserId");
		userDao.lockUser(userId);
		final Map<String, Object> config = consolidationConfig(userId);
		final List<Object> memberUserIds = memberUserIds(config);
		if ( memberUserIds.removeIf(id -> isUserId(id, childUserId)) ) {
			// an empty list leaves no group
			storeMemberUserIds(userId, config, memberUserIds);
		}
		return linkedAccounts(memberUserIds);
	}

	/**
	 * Get the user to link an account to as its parent account.
	 *
	 * @param input
	 *        the parent account input
	 * @return the user, or {@code null} if {@code input} does not ask to link
	 *         an account
	 * @throws ValidationException
	 *         if the credentials are not valid
	 */
	private @Nullable User parentAccountUser(@Nullable SnAccountLinkInput input) {
		if ( input == null ) {
			return null;
		}
		final SnLinkedAccountInfo verified = input.getVerifiedAccount();
		if ( verified != null ) {
			final User user = userDao.get(verified.userId());
			if ( user == null ) {
				throw linkException(LINK_CREDENTIALS_ERROR_CODE,
						"The account email or password is not valid.");
			}
			return user;
		}
		final String username = input.getUsername();
		if ( username == null || username.isBlank() ) {
			return null;
		}
		return authenticatedUser(input);
	}

	/**
	 * Get the user for account credentials.
	 *
	 * @param input
	 *        the account credentials
	 * @return the user
	 * @throws ValidationException
	 *         if the credentials are not valid, without saying why, so the
	 *         response does not reveal if an account exists for the email
	 */
	private User authenticatedUser(SnAccountLinkInput input) {
		final PasswordEncoder encoder = this.passwordEncoder;
		if ( encoder == null ) {
			throw linkException(LINK_UNAVAILABLE_ERROR_CODE, "Account linking is not available.");
		}
		final String username = input.getUsername();
		final String password = input.getPassword();
		final User user = (username != null && !username.isBlank() ? userDao.getUserByEmail(username)
				: null);
		final String encodedPassword = (user != null ? user.getPassword() : null);
		if ( user == null || encodedPassword == null || password == null || password.isEmpty()
				|| !Boolean.TRUE.equals(user.getEnabled())
				|| !encoder.matches(password, encodedPassword) ) {
			throw linkException(LINK_CREDENTIALS_ERROR_CODE,
					"The account email or password is not valid.");
		}
		return user;
	}

	/**
	 * Verify a child account can be linked to a parent account.
	 *
	 * @param parent
	 *        the parent account user
	 * @param child
	 *        the child account user
	 * @return {@code true} if the accounts can be linked, {@code false} if they
	 *         are linked already
	 * @throws ValidationException
	 *         if the accounts can not be linked
	 */
	private boolean validateLink(User parent, User child) {
		final InvoiceConsolidationResolver resolver = resolver();
		if ( parent.id().equals(child.id()) ) {
			throw linkException(LINK_SELF_ERROR_CODE, "An account can not be linked to itself.");
		}
		final InvoiceConsolidationGroup childGroup = resolver.groupForUser(child.id());
		if ( childGroup != null ) {
			if ( childGroup.isPrimary(child.id()) ) {
				throw linkException(LINK_PARENT_ERROR_CODE,
						"The account {0} has linked accounts of its own, so it can not be linked to another account.",
						child.getEmail());
			} else if ( childGroup.isPrimary(parent.id()) ) {
				return false;
			}
			throw linkException(LINK_LINKED_ERROR_CODE,
					"The account {0} is already linked to another account.", child.getEmail());
		}
		final InvoiceConsolidationGroup parentGroup = resolver.groupForUser(parent.id());
		if ( parentGroup != null && !parentGroup.isPrimary(parent.id()) ) {
			throw linkException(LINK_PARENT_LINKED_ERROR_CODE,
					"The account {0} is itself linked to another account.", parent.getEmail());
		} else if ( parentGroup != null && !parentGroup.isValid() ) {
			throw linkException(LINK_INVALID_ERROR_CODE, "The accounts can not be linked: {0}",
					parentGroup.problem());
		}
		return true;
	}

	/**
	 * Link a child account to a parent account.
	 *
	 * <p>
	 * Both users are locked, in user ID order, so that concurrent changes to
	 * the groups of either user wait for each other. The resulting group must
	 * be valid, otherwise a {@link ValidationException} is thrown so the
	 * transaction is rolled back.
	 * </p>
	 *
	 * @param parent
	 *        the parent account user
	 * @param child
	 *        the child account user
	 * @throws ValidationException
	 *         if the accounts can not be linked
	 */
	private void link(User parent, User child) {
		final Long parentUserId = parent.id();
		final Long childUserId = child.id();
		if ( parentUserId.equals(childUserId) ) {
			throw linkException(LINK_SELF_ERROR_CODE, "An account can not be linked to itself.");
		}
		final boolean parentFirst = parentUserId.compareTo(childUserId) < 0;
		userDao.lockUser(parentFirst ? parentUserId : childUserId);
		userDao.lockUser(parentFirst ? childUserId : parentUserId);
		if ( !validateLink(parent, child) ) {
			return;
		}

		final Map<String, Object> config = consolidationConfig(parentUserId);
		final List<Object> memberUserIds = memberUserIds(config);
		memberUserIds.add(childUserId);
		storeMemberUserIds(parentUserId, config, memberUserIds);

		final InvoiceConsolidationGroup group = resolver().groupForUser(parentUserId);
		final MessageSourceResolvable problem = (group != null ? group.problem() : null);
		if ( group == null || problem != null ) {
			throw linkException(LINK_INVALID_ERROR_CODE, "The accounts can not be linked: {0}",
					problem != null ? problem : "the group was not saved.");
		}
	}

	private InvoiceConsolidationResolver resolver() {
		final InvoiceConsolidationResolver resolver = this.consolidationResolver;
		if ( resolver == null ) {
			throw linkException(LINK_UNAVAILABLE_ERROR_CODE, "Account linking is not available.");
		}
		return resolver;
	}

	private Map<String, Object> consolidationConfig(Long userId) {
		final Map<String, Object> data = userDao.getInternalData(userId);
		final Map<String, Object> result = new LinkedHashMap<>(4);
		if ( data != null && data.get(INVOICE_CONSOLIDATION_DATA_PROP) instanceof Map<?, ?> config ) {
			for ( Map.Entry<?, ?> e : config.entrySet() ) {
				result.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return result;
	}

	private static List<Object> memberUserIds(Map<String, Object> config) {
		return (config.get(USER_IDS_PROP) instanceof Collection<?> ids ? new ArrayList<Object>(ids)
				: new ArrayList<>(2));
	}

	private void storeMemberUserIds(Long userId, Map<String, Object> config,
			List<Object> memberUserIds) {
		config.put(USER_IDS_PROP, memberUserIds);
		userDao.storeInternalData(userId, Map.of(INVOICE_CONSOLIDATION_DATA_PROP, config));
	}

	private static boolean isUserId(@Nullable Object id, Long userId) {
		return (id instanceof Number n && n.longValue() == userId);
	}

	private List<SnLinkedAccountInfo> linkedAccounts(Collection<?> userIds) {
		final List<SnLinkedAccountInfo> result = new ArrayList<>(userIds.size());
		for ( Object id : userIds ) {
			if ( id instanceof Number n ) {
				final User user = userDao.get(n.longValue());
				if ( user != null ) {
					result.add(SnLinkedAccountInfo.forUser(user));
				}
			}
		}
		return result;
	}

	/**
	 * Create a validation exception for a linking problem.
	 *
	 * <p>
	 * The error is resolved where it is shown, along with any
	 * {@link MessageSourceResolvable} arguments such as the problem of an
	 * invoice consolidation group. The exception message is the default
	 * message.
	 * </p>
	 *
	 * @param code
	 *        the error code
	 * @param defaultMessage
	 *        the default message
	 * @param args
	 *        the message arguments
	 * @return the exception
	 */
	private static ValidationException linkException(String code, String defaultMessage,
			@Nullable Object... args) {
		final Object[] errorArgs = new Object[args.length];
		for ( int i = 0; i < args.length; i++ ) {
			final Object arg = args[i];
			errorArgs[i] = (arg != null ? arg : "");
		}
		final Errors errors = new MapBindingResult(new HashMap<>(0), "parentAccount");
		errors.reject(code, errorArgs, defaultMessage);
		return new ValidationException(DEFAULT_MESSAGES.getMessage(
				new DefaultMessageSourceResolvable(new String[] { code }, errorArgs, defaultMessage),
				Locale.getDefault()), errors, null);
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

	/**
	 * Get the password encoder.
	 *
	 * @return the password encoder
	 * @since 1.1
	 */
	public @Nullable PasswordEncoder getPasswordEncoder() {
		return passwordEncoder;
	}

	/**
	 * Set the password encoder.
	 *
	 * @param passwordEncoder
	 *        the password encoder to verify the credentials of accounts to link
	 *        with
	 * @since 1.1
	 */
	public void setPasswordEncoder(@Nullable PasswordEncoder passwordEncoder) {
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Get the invoice consolidation resolver.
	 *
	 * @return the resolver
	 * @since 1.1
	 */
	public @Nullable InvoiceConsolidationResolver getConsolidationResolver() {
		return consolidationResolver;
	}

	/**
	 * Set the invoice consolidation resolver.
	 *
	 * @param consolidationResolver
	 *        the resolver to find the linked accounts with
	 * @since 1.1
	 */
	public void setConsolidationResolver(@Nullable InvoiceConsolidationResolver consolidationResolver) {
		this.consolidationResolver = consolidationResolver;
	}

}
