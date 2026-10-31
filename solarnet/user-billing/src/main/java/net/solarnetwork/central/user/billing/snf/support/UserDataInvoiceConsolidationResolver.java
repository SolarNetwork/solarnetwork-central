/* ==================================================================
 * UserDataInvoiceConsolidationResolver.java - 15/10/2026 10:11:47 am
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

package net.solarnetwork.central.user.billing.snf.support;

import static java.util.Comparator.comparing;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.ACCOUNTING_DATA_PROP;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.DELIVER_MEMBER_INVOICES_PROP;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.INVOICE_CONSOLIDATION_DATA_PROP;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.USER_IDS_PROP;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterMatch;

/**
 * {@link InvoiceConsolidationResolver} that reads the groups from the internal
 * data of the primary account users.
 *
 * <p>
 * A group is configured in the internal data of its primary account user, as an
 * {@code invoiceConsolidation} object whose {@code userIds} array lists the
 * other users in the group, in the order they are to be listed in a
 * consolidated invoice. For example:
 * </p>
 *
 * <pre>{@code { "accounting": "snf", "invoiceConsolidation": { "userIds": [456,
 * 789] } } }</pre>
 *
 * <p>
 * The optional {@code deliverMemberInvoices} property can be set to {@code
 * true} to also deliver each of the other users their own invoice. A
 * configuration that lists no other users is not a group.
 * </p>
 *
 * <p>
 * The primary account user whose group lists a given user is found with a JSON
 * containment query on the internal data of the users, so the user IDs must be
 * listed as numbers: a group that lists a user ID any other way is invalid.
 * </p>
 *
 * <p>
 * The problem of an invalid group is one of the {@code PROBLEM_} message codes
 * of this class, with its message arguments. The messages are in the
 * {@code UserDataInvoiceConsolidationResolver} resource bundle of this class,
 * and the default message of each problem is the base message of the bundle, so
 * a message source without the bundle still resolves it.
 * </p>
 *
 * @author matt @version 1.0
 */
public class UserDataInvoiceConsolidationResolver implements InvoiceConsolidationResolver {

	/** The problem message code for a configuration that is not an object. */
	public static final String PROBLEM_CONFIG_NOT_OBJECT = "error.consolidated.illegalState.confNotObject";

	/** The problem message code for a listed user ID that is not a number. */
	public static final String PROBLEM_USER_ID_NOT_NUMBER = "error.consolidated.illegalState.userIds.valueNotNumber";

	/** The problem message code for a list of user IDs that is not a list. */
	public static final String PROBLEM_USER_IDS_NOT_LIST = "error.consolidated.illegalState.userIds.notList";

	/** The problem message code for a user that does not exist. */
	public static final String PROBLEM_USER_NOT_FOUND = "error.consolidated.userNotFound";

	/** The problem message code for a user listed by more than one group. */
	public static final String PROBLEM_CHILD_IN_MULTIPLE_PARENTS = "error.consolidated.childInMultipleParents";

	/** The problem message code for a user also listed by other groups. */
	public static final String PROBLEM_CHILD_IN_OTHER_PARENTS = "error.consolidated.childInOtherParents";

	/**
	 * The problem message code for a primary account user listed by other
	 * groups.
	 */
	public static final String PROBLEM_NESTED_GROUPS = "error.consolidated.nestedGroups";

	/**
	 * The problem message code for a listed user that has a group of its own.
	 */
	public static final String PROBLEM_CHILD_HAS_CHILDREN = "error.consolidated.childHasChildren";

	/** The problem message code for a user not using the billing system. */
	public static final String PROBLEM_NOT_ENROLLED_FOR_BILLING = "error.consolidated.notEnrolledForBilling";

	/** The problem message code for a user without a billing account. */
	public static final String PROBLEM_NO_BILLING_ACCOUNT = "error.consolidated.noBillingAccount";

	/** The problem message code for a user billed in another currency. */
	public static final String PROBLEM_CURRENCY_MISMATCH = "error.consolidated.currencyMismatch";

	private final UserDao userDao;
	private final AccountDao accountDao;
	private final MessageSource messageSource;

	/**
	 * Constructor.
	 *
	 * @param userDao
	 *        the user DAO
	 * @param accountDao
	 *        the account DAO
	 * @param messageSource
	 *        the message source with the problem messages, to get the default
	 *        message of each problem from
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDataInvoiceConsolidationResolver(UserDao userDao, AccountDao accountDao,
			MessageSource messageSource) {
		super();
		this.userDao = requireNonNullArgument(userDao, "userDao");
		this.accountDao = requireNonNullArgument(accountDao, "accountDao");
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
	}

	private static final class Config {

		private final Long primaryUserId;
		private final List<Long> userIds;
		private final boolean deliverMemberInvoices;
		private @Nullable MessageSourceResolvable problem;

		private Config(Long primaryUserId, List<Long> userIds, boolean deliverMemberInvoices,
				@Nullable MessageSourceResolvable problem) {
			super();
			this.primaryUserId = primaryUserId;
			this.userIds = userIds;
			this.deliverMemberInvoices = deliverMemberInvoices;
			this.problem = problem;
		}

	}

	@Override
	public @Nullable InvoiceConsolidationGroup groupForUser(Long userId) {
		final User user = userDao.get(requireNonNullArgument(userId, "userId"));
		if ( user == null ) {
			return null;
		}
		final List<UserFilterMatch> listedBy = usersListing(userId);
		Config config = parseConfig(userId, user.getInternalDataValue(INVOICE_CONSOLIDATION_DATA_PROP));
		if ( config != null ) {
			// the user is the primary account of a group
			if ( config.problem == null && !listedBy.isEmpty() ) {
				config.problem = problem(PROBLEM_NESTED_GROUPS, userId, userIds(listedBy));
			}
		} else if ( listedBy.isEmpty() ) {
			return null;
		} else {
			// the user is one of the other accounts of a group
			final UserFilterMatch primary = listedBy.getFirst();
			config = parseConfig(nonnull(primary.getId(), "User ID"),
					internalDataValue(primary, INVOICE_CONSOLIDATION_DATA_PROP));
			if ( config == null ) {
				return null;
			}
			if ( config.problem == null && listedBy.size() > 1 ) {
				config.problem = problem(PROBLEM_CHILD_IN_MULTIPLE_PARENTS, userId, userIds(listedBy));
			}
		}
		if ( config.problem == null ) {
			config.problem = groupProblem(config, userId, listedBy);
		}
		return new InvoiceConsolidationGroup(config.primaryUserId, config.userIds,
				config.deliverMemberInvoices, config.problem);
	}

	/**
	 * Create a problem.
	 *
	 * @param code
	 *        the problem message code
	 * @param args
	 *        the problem message arguments
	 * @return the problem, with the base message of the code as its default
	 *         message
	 */
	private MessageSourceResolvable problem(String code, Object... args) {
		return new DefaultMessageSourceResolvable(new String[] { code }, args,
				messageSource.getMessage(code, null, code, Locale.ROOT));
	}

	/**
	 * Find the users whose invoice consolidation configuration lists a user.
	 *
	 * @param userId
	 *        the ID of the user to look for
	 * @return the users, other than {@code userId} itself, ordered by ID
	 */
	private List<UserFilterMatch> usersListing(Long userId) {
		final UserFilterCommand criteria = new UserFilterCommand();
		criteria.setInternalData(
				Map.of(INVOICE_CONSOLIDATION_DATA_PROP, Map.of(USER_IDS_PROP, List.of(userId))));
		final List<UserFilterMatch> result = new ArrayList<>(2);
		for ( UserFilterMatch user : userDao.findFiltered(criteria, null, null, null) ) {
			if ( !userId.equals(user.getId()) ) {
				result.add(user);
			}
		}
		result.sort(comparing(u -> nonnull(u.getId(), "User ID")));
		return result;
	}

	private static List<Long> userIds(List<UserFilterMatch> users) {
		final List<Long> result = new ArrayList<>(users.size());
		for ( UserFilterMatch user : users ) {
			result.add(nonnull(user.getId(), "User ID"));
		}
		return result;
	}

	private static @Nullable Object internalDataValue(UserFilterMatch user, String key) {
		final Map<String, Object> data = user.getInternalData();
		return (data != null ? data.get(key) : null);
	}

	/**
	 * Parse an invoice consolidation configuration.
	 *
	 * @param primaryUserId
	 *        the ID of the user the configuration belongs to
	 * @param config
	 *        the configuration
	 * @return the configuration, or {@code null} if {@code config} is
	 *         {@code null} or lists no other users
	 */
	private @Nullable Config parseConfig(Long primaryUserId, @Nullable Object config) {
		if ( config == null ) {
			return null;
		}
		final List<Long> userIds = new ArrayList<>(4);
		userIds.add(primaryUserId);
		if ( !(config instanceof Map<?, ?> map) ) {
			return new Config(primaryUserId, userIds, false,
					problem(PROBLEM_CONFIG_NOT_OBJECT, String.valueOf(config)));
		}
		MessageSourceResolvable problem = null;
		final Object ids = map.get(USER_IDS_PROP);
		if ( ids instanceof Collection<?> c ) {
			for ( Object id : c ) {
				final Long userId = longValue(id);
				if ( userId == null ) {
					problem = problem(PROBLEM_USER_ID_NOT_NUMBER, USER_IDS_PROP,
							id instanceof String s ? '"' + s + '"' : String.valueOf(id));
				} else if ( !userIds.contains(userId) ) {
					userIds.add(userId);
				}
			}
		} else if ( ids != null ) {
			problem = problem(PROBLEM_USER_IDS_NOT_LIST, USER_IDS_PROP, String.valueOf(ids));
		}
		if ( problem == null && userIds.size() < 2 ) {
			// no other users
			return null;
		}
		final Object deliver = map.get(DELIVER_MEMBER_INVOICES_PROP);
		final boolean deliverMemberInvoices = (deliver instanceof Boolean b ? b
				: deliver != null && Boolean.parseBoolean(deliver.toString()));
		return new Config(primaryUserId, userIds, deliverMemberInvoices, problem);
	}

	private static @Nullable Long longValue(@Nullable Object o) {
		if ( o instanceof Long l ) {
			return l;
		} else if ( o instanceof Integer i ) {
			return i.longValue();
		}
		return null;
	}

	/**
	 * Get the problem with the accounts of a group.
	 *
	 * @param config
	 *        the group configuration
	 * @param userId
	 *        the ID of the user the group was resolved for
	 * @param listedBy
	 *        the users whose configuration lists {@code userId}
	 * @return the problem, or {@code null} if the group is valid
	 */
	private @Nullable MessageSourceResolvable groupProblem(Config config, Long userId,
			List<UserFilterMatch> listedBy) {
		// no user can be in another group
		final Map<Long, User> users = new LinkedHashMap<>(config.userIds.size());
		for ( Long groupUserId : config.userIds ) {
			final User user = userDao.get(groupUserId);
			if ( user == null ) {
				return problem(PROBLEM_USER_NOT_FOUND, groupUserId);
			}
			final boolean primary = config.primaryUserId.equals(groupUserId);
			if ( !primary && parseConfig(groupUserId,
					user.getInternalDataValue(INVOICE_CONSOLIDATION_DATA_PROP)) != null ) {
				return problem(PROBLEM_CHILD_HAS_CHILDREN, groupUserId);
			}
			final List<Long> otherPrimaryUserIds = userIds(
					groupUserId.equals(userId) ? listedBy : usersListing(groupUserId));
			otherPrimaryUserIds.remove(config.primaryUserId);
			if ( !otherPrimaryUserIds.isEmpty() ) {
				return problem(primary ? PROBLEM_NESTED_GROUPS : PROBLEM_CHILD_IN_OTHER_PARENTS,
						groupUserId, otherPrimaryUserIds);
			}
			users.put(groupUserId, user);
		}

		// all users must have an SNF billing account, in the same currency
		String currencyCode = null;
		for ( Map.Entry<Long, User> e : users.entrySet() ) {
			final Long groupUserId = e.getKey();
			final User user = e.getValue();
			final Object accounting = user.getInternalDataValue(ACCOUNTING_DATA_PROP);
			if ( !SnfBillingSystem.ACCOUNTING_SYSTEM_KEY.equals(accounting) ) {
				// TODO SNF should really be the configured accounting system, not hard coded as SNF
				return problem(PROBLEM_NOT_ENROLLED_FOR_BILLING, groupUserId, "SNF");
			}
			final Account account = accountDao.getForUser(groupUserId);
			if ( account == null ) {
				return problem(PROBLEM_NO_BILLING_ACCOUNT, groupUserId);
			}
			if ( currencyCode == null ) {
				currencyCode = account.getCurrencyCode();
			} else if ( !currencyCode.equals(account.getCurrencyCode()) ) {
				return problem(PROBLEM_CURRENCY_MISMATCH, groupUserId, account.getCurrencyCode(),
						currencyCode);
			}
		}
		return null;
	}

}
