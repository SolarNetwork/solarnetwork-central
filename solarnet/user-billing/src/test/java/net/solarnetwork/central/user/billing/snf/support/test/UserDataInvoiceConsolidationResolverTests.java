/* ==================================================================
 * UserDataInvoiceConsolidationResolverTests.java - 15/10/2026 10:30:52 am
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

package net.solarnetwork.central.user.billing.snf.support.test;

import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CHILD_HAS_CHILDREN;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CHILD_IN_MULTIPLE_PARENTS;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CHILD_IN_OTHER_PARENTS;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CONFIG_NOT_OBJECT;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CURRENCY_MISMATCH;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_NESTED_GROUPS;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_NOT_ENROLLED_FOR_BILLING;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_NO_BILLING_ACCOUNT;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_USER_IDS_NOT_LIST;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_USER_ID_NOT_NUMBER;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_USER_NOT_FOUND;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import net.solarnetwork.central.domain.UserFilterCommand;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserFilterMatch;
import net.solarnetwork.central.user.domain.UserMatch;
import net.solarnetwork.dao.BasicFilterResults;

/**
 * Test cases for the {@link UserDataInvoiceConsolidationResolver} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UserDataInvoiceConsolidationResolverTests {

	@Mock
	private UserDao userDao;

	@Mock
	private AccountDao accountDao;

	@Captor
	private ArgumentCaptor<UserFilterCommand> filterCaptor;

	private Map<Long, UserMatch> users;
	private ResourceBundleMessageSource messages;
	private UserDataInvoiceConsolidationResolver resolver;

	@BeforeEach
	public void setup() {
		users = new LinkedHashMap<>(8);
		messages = new ResourceBundleMessageSource();
		messages.setBasename(UserDataInvoiceConsolidationResolver.class.getName());
		resolver = new UserDataInvoiceConsolidationResolver(userDao, accountDao, messages);
	}

	/**
	 * Add a user that uses SNF billing.
	 *
	 * @param userId
	 *        the user ID
	 * @param config
	 *        the invoice consolidation configuration, or {@code null} for none
	 */
	private void user(Long userId, @Nullable Object config) {
		final Map<String, Object> data = new LinkedHashMap<>(2);
		data.put("accounting", "snf");
		if ( config != null ) {
			data.put("invoiceConsolidation", config);
		}
		final UserMatch user = new UserMatch(userId, "user%d@localhost".formatted(userId));
		user.setInternalData(data);
		users.put(userId, user);
	}

	/**
	 * Serve the users from the user DAO.
	 *
	 * <p>
	 * The internal data filter matches users whose group lists a user ID, as
	 * the JSON containment query in the database does: only user IDs listed as
	 * numbers match.
	 * </p>
	 */
	private void givenUserDao() {
		given(userDao.get(any())).willAnswer(invocation -> users.get(invocation.getArgument(0)));
		given(userDao.findFiltered(any(), isNull(), isNull(), isNull())).willAnswer(invocation -> {
			final Long userId = listedUserId(invocation.getArgument(0));
			final List<UserFilterMatch> result = users.values().stream()
					.filter(user -> lists(user, userId)).map(UserFilterMatch.class::cast).toList();
			return new BasicFilterResults<>(result);
		});
	}

	private static Long listedUserId(UserFilterCommand filter) {
		final Map<String, Object> data = nonnull(filter.getInternalData(), "Internal data");
		if ( data.get("invoiceConsolidation") instanceof Map<?, ?> config
				&& config.get("userIds") instanceof List<?> ids && ids.size() == 1
				&& ids.getFirst() instanceof Long userId ) {
			return userId;
		}
		throw new IllegalArgumentException("Unexpected user filter: " + data);
	}

	private static boolean lists(User user, Long userId) {
		return (user.getInternalDataValue("invoiceConsolidation") instanceof Map<?, ?> config
				&& config.get("userIds") instanceof Collection<?> ids
				&& ids.stream().anyMatch(id -> id instanceof Number n && n.longValue() == userId));
	}

	/**
	 * Get a problem, with the base message of its code as its default message.
	 */
	private MessageSourceResolvable problem(String code, Object... args) {
		return new DefaultMessageSourceResolvable(new String[] { code }, args,
				messages.getMessage(code, null, Locale.ROOT));
	}

	private void givenAccount(Long userId, String currencyCode) {
		given(accountDao.getForUser(userId))
				.willReturn(new Account(userId * 10, userId, Instant.now(), currencyCode, "en-NZ"));
	}

	@Test
	public void none() {
		// GIVEN
		user(1L, null);
		givenUserDao();

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		then(userDao).should().findFiltered(filterCaptor.capture(), isNull(), isNull(), isNull());
		// @formatter:off
		and.then(filterCaptor.getValue().getInternalData())
			.as("Users whose group lists the user queried")
			.isEqualTo(Map.of("invoiceConsolidation", Map.of("userIds", List.of(1L))))
			;
		and.then(result)
			.as("No group")
			.isNull()
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void none_userNotFound() {
		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No group")
			.isNull()
			;
		// @formatter:on
		then(userDao).should().get(1L);
		then(userDao).shouldHaveNoMoreInteractions();
	}

	@Test
	public void none_noOtherUsers() {
		// GIVEN
		user(1L, Map.of("userIds", List.of()));
		user(2L, Map.of("userIds", List.of(2), "deliverMemberInvoices", true));
		user(3L, Map.of());
		givenUserDao();

		// THEN
		// @formatter:off
		and.then(resolver.groupForUser(1L))
			.as("Empty list of other users is not a group")
			.isNull()
			;
		and.then(resolver.groupForUser(2L))
			.as("Listing only itself is not a group")
			.isNull()
			;
		and.then(resolver.groupForUser(3L))
			.as("No list of other users is not a group")
			.isNull()
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void primary() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(3, 2)));
		user(2L, null);
		user(3L, null);
		givenUserDao();
		givenAccount(1L, "NZD");
		givenAccount(2L, "NZD");
		givenAccount(3L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Valid group with the primary first, then the others in configured order")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 3L, 2L), false))
			;
		// @formatter:on
	}

	@Test
	public void member() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(3, 2)));
		user(2L, null);
		user(3L, null);
		givenUserDao();
		givenAccount(1L, "NZD");
		givenAccount(2L, "NZD");
		givenAccount(3L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(2L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group of the primary that lists the user")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 3L, 2L), false))
			;
		// @formatter:on
	}

	@Test
	public void deliverMemberInvoices() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2), "deliverMemberInvoices", true));
		user(2L, null);
		givenUserDao();
		givenAccount(1L, "NZD");
		givenAccount(2L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(2L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group with option to deliver member invoices")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), true))
			;
		// @formatter:on
	}

	@Test
	public void primaryAndDuplicatesListed() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2, 1, 2)));
		user(2L, null);
		givenUserDao();
		givenAccount(1L, "NZD");
		givenAccount(2L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Primary and duplicate user IDs listed once")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false))
			;
		// @formatter:on
	}

	@Test
	public void invalid_notObject() {
		// GIVEN
		user(1L, "2,3");
		givenUserDao();

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Invalid group of just the primary, as the other users are not known")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L), false,
					problem(PROBLEM_CONFIG_NOT_OBJECT, "2,3")))
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void invalid_userIdNotNumber() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2, "abc")));
		user(2L, null);
		givenUserDao();

		// WHEN
		InvoiceConsolidationGroup primary = resolver.groupForUser(1L);
		InvoiceConsolidationGroup member = resolver.groupForUser(2L);

		// THEN
		final InvoiceConsolidationGroup expected = new InvoiceConsolidationGroup(1L, List.of(1L, 2L),
				false, problem(PROBLEM_USER_ID_NOT_NUMBER, "userIds", "\"abc\""));
		// @formatter:off
		and.then(primary)
			.as("Group invalid for the primary")
			.isEqualTo(expected)
			;
		and.then(member)
			.as("Group invalid for the other user")
			.isEqualTo(expected)
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void invalid_userIdString() {
		// GIVEN
		user(1L, Map.of("userIds", List.of("2")));
		user(2L, null);
		givenUserDao();

		// WHEN
		InvoiceConsolidationGroup primary = resolver.groupForUser(1L);
		InvoiceConsolidationGroup other = resolver.groupForUser(2L);

		// THEN
		// @formatter:off
		and.then(primary)
			.as("A user ID listed as a string makes the group invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L), false,
					problem(PROBLEM_USER_ID_NOT_NUMBER, "userIds", "\"2\"")))
			;
		and.then(other)
			.as("The user listed as a string is not found to be in the group")
			.isNull()
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void invalid_userNotFound() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2)));
		givenUserDao();

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_USER_NOT_FOUND, 2L)))
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void invalid_userNotSnf() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2)));
		users.put(2L, new UserMatch(2L, "user2@localhost"));
		givenUserDao();
		givenAccount(1L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(2L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_NOT_ENROLLED_FOR_BILLING, 2L, "SNF")))
			;
		// @formatter:on
	}

	@Test
	public void invalid_noAccount() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2)));
		user(2L, null);
		givenUserDao();
		givenAccount(1L, "NZD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_NO_BILLING_ACCOUNT, 2L)))
			;
		// @formatter:on
	}

	@Test
	public void invalid_mixedCurrencies() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2)));
		user(2L, null);
		givenUserDao();
		givenAccount(1L, "NZD");
		givenAccount(2L, "USD");

		// WHEN
		InvoiceConsolidationGroup result = resolver.groupForUser(1L);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Group invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_CURRENCY_MISMATCH, 2L, "USD", "NZD")))
			;
		// @formatter:on
	}

	@Test
	public void invalid_userInTwoGroups() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(2)));
		user(2L, null);
		user(3L, Map.of("userIds", List.of(2, 4)));
		user(4L, null);
		user(5L, Map.of("userIds", List.of(6)));
		user(6L, null);
		givenUserDao();
		givenAccount(5L, "NZD");
		givenAccount(6L, "NZD");

		// THEN
		// @formatter:off
		and.then(resolver.groupForUser(2L))
			.as("Group of the first primary that lists the shared user, invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_CHILD_IN_MULTIPLE_PARENTS, 2L, List.of(1L, 3L))))
			;
		and.then(resolver.groupForUser(1L))
			.as("Group of one primary that lists the shared user invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 2L), false,
					problem(PROBLEM_CHILD_IN_OTHER_PARENTS, 2L, List.of(3L))))
			;
		and.then(resolver.groupForUser(3L))
			.as("Group of other primary that lists the shared user invalid")
			.isEqualTo(new InvoiceConsolidationGroup(3L, List.of(3L, 2L, 4L), false,
					problem(PROBLEM_CHILD_IN_OTHER_PARENTS, 2L, List.of(1L))))
			;
		and.then(resolver.groupForUser(4L))
			.as("Group invalid for another user in a group with the shared user")
			.isEqualTo(new InvoiceConsolidationGroup(3L, List.of(3L, 2L, 4L), false,
					problem(PROBLEM_CHILD_IN_OTHER_PARENTS, 2L, List.of(1L))))
			;
		and.then(resolver.groupForUser(5L))
			.as("Group without a shared user valid")
			.isEqualTo(new InvoiceConsolidationGroup(5L, List.of(5L, 6L), false))
			;
		// @formatter:on
		then(accountDao).should().getForUser(5L);
		then(accountDao).should().getForUser(6L);
		then(accountDao).shouldHaveNoMoreInteractions();
	}

	@Test
	public void invalid_nestedGroups() {
		// GIVEN
		user(1L, Map.of("userIds", List.of(3)));
		user(3L, Map.of("userIds", List.of(4)));
		user(4L, null);
		givenUserDao();

		// THEN
		// @formatter:off
		and.then(resolver.groupForUser(1L))
			.as("Group that lists a primary invalid")
			.isEqualTo(new InvoiceConsolidationGroup(1L, List.of(1L, 3L), false,
					problem(PROBLEM_CHILD_HAS_CHILDREN, 3L)))
			;
		and.then(resolver.groupForUser(3L))
			.as("Group of a listed primary invalid")
			.isEqualTo(new InvoiceConsolidationGroup(3L, List.of(3L, 4L), false,
					problem(PROBLEM_NESTED_GROUPS, 3L, List.of(1L))))
			;
		and.then(resolver.groupForUser(4L))
			.as("Group invalid for another user in the group of a listed primary")
			.isEqualTo(new InvoiceConsolidationGroup(3L, List.of(3L, 4L), false,
					problem(PROBLEM_NESTED_GROUPS, 3L, List.of(1L))))
			;
		// @formatter:on
		then(accountDao).shouldHaveNoInteractions();
	}

	@Test
	public void problemMessages() {
		// GIVEN
		final Map<MessageSourceResolvable, String> expected = new LinkedHashMap<>(16);
		expected.put(problem(PROBLEM_CONFIG_NOT_OBJECT, "2,3"),
				"The consolidated billing configuration is not an object: 2,3.");
		expected.put(problem(PROBLEM_USER_ID_NOT_NUMBER, "userIds", "\"abc\""),
				"The userIds value \"abc\" is not a valid user ID.");
		expected.put(problem(PROBLEM_USER_IDS_NOT_LIST, "userIds", "2,3"),
				"The userIds property is not a list of user IDs: 2,3.");
		expected.put(problem(PROBLEM_USER_NOT_FOUND, 12345L), "User 12345 is not available.");
		expected.put(problem(PROBLEM_CHILD_IN_MULTIPLE_PARENTS, 12345L, List.of(1001L, 1002L)),
				"User 12345 is included within the consolidated billing configuration of more than one user: [1001, 1002].");
		expected.put(problem(PROBLEM_CHILD_IN_OTHER_PARENTS, 12345L, List.of(1001L)),
				"User 12345 is also included within the consolidated billing configuration of users [1001].");
		expected.put(problem(PROBLEM_NESTED_GROUPS, 12345L, List.of(1001L)),
				"User 12345 has consolidated billing accounts of its own, and is also included within the consolidated billing configuration of users [1001].");
		expected.put(problem(PROBLEM_CHILD_HAS_CHILDREN, 12345L),
				"User 12345 has consolidated billing accounts of its own.");
		expected.put(problem(PROBLEM_NOT_ENROLLED_FOR_BILLING, 12345L, "SNF"),
				"User 12345 does not have SNF billing enabled.");
		expected.put(problem(PROBLEM_NO_BILLING_ACCOUNT, 12345L),
				"User 12345 is not enrolled for billing.");
		expected.put(problem(PROBLEM_CURRENCY_MISMATCH, 12345L, "USD", "NZD"),
				"User 12345 is billed in USD, not NZD.");

		// THEN
		final StaticMessageSource noMessages = new StaticMessageSource();
		for ( Locale locale : List.of(Locale.ENGLISH, Locale.GERMANY) ) {
			for ( Map.Entry<MessageSourceResolvable, String> e : expected.entrySet() ) {
				// @formatter:off
				and.then(messages.getMessage(e.getKey(), locale))
					.as("Message %s in %s, with user IDs not formatted as numbers", Arrays.toString(e.getKey().getCodes()), locale)
					.isEqualTo(e.getValue())
					;
				and.then(noMessages.getMessage(e.getKey(), locale))
					.as("Default message %s in %s, for a message source without the messages", Arrays.toString(e.getKey().getCodes()), locale)
					.isEqualTo(e.getValue())
					;
				// @formatter:on
			}
		}
	}

}
