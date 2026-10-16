/* ==================================================================
 * DefaultUserAccountBiz_LinkTests.java - 15/10/2026 6:10:27 pm
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

package net.solarnetwork.central.user.account.biz.impl.test;

import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.ACCOUNTING_DATA_PROP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.user.account.biz.impl.DefaultUserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAccountLinkInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.account.domain.SnLinkedAccountInfo;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.billing.biz.InvoiceConsolidationResolver;
import net.solarnetwork.central.user.billing.domain.InvoiceConsolidationGroup;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Test cases for linking accounts with the {@link DefaultUserAccountBiz}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DefaultUserAccountBiz_LinkTests {

	private static final String SYSTEM_KEY = "snf";
	private static final String PASSWORD = "secret";
	private static final String ENCODED_PASSWORD = "encoded-secret";
	private static final Long PARENT_ID = 1L;
	private static final Long CHILD_ID = 2L;
	private static final Long OTHER_ID = 3L;

	private static final MessageSourceResolvable CURRENCY_PROBLEM = new DefaultMessageSourceResolvable(
			new String[] { "error.consolidated.currencyMismatch" }, new Object[] { 2L, "USD", "NZD" },
			"User {0,number,#} is billed in {1}, not {2}.");

	@Mock
	private UserDao userDao;

	@Mock
	private BillingSystemRegistrar registrar;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private InvoiceConsolidationResolver resolver;

	@Mock
	private SnAccount<?, ?, ?> account;

	@Captor
	private ArgumentCaptor<Map<String, Object>> internalDataCaptor;

	private DefaultUserAccountBiz service;

	@BeforeEach
	public void setup() {
		service = new DefaultUserAccountBiz(List.of(registrar), userDao);
		service.setPasswordEncoder(passwordEncoder);
		service.setConsolidationResolver(resolver);
	}

	private User user(Long userId) {
		final var user = new User(userId, "user%d@localhost".formatted(userId));
		user.setName("User " + userId);
		user.setCreated(Instant.now());
		user.setEnabled(true);
		user.setPassword(ENCODED_PASSWORD);
		return user;
	}

	private User givenUser(Long userId) {
		final User user = user(userId);
		given(userDao.get(userId)).willReturn(user);
		return user;
	}

	private void givenCredentials(User user) {
		given(userDao.getUserByEmail(user.getEmail())).willReturn(user);
		given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);
	}

	private static SnAccountLinkInput credentials(User user) {
		final var input = new SnAccountLinkInput();
		input.setUsername(user.getEmail());
		input.setPassword(PASSWORD);
		return input;
	}

	private static SnAccountCreationInput creationInput() {
		final var input = new SnAccountCreationInput();
		final var acct = new SnAccountInput();
		acct.setLocale("en-NZ");
		input.setAccount(acct);
		final var addr = new SnAddressInput();
		addr.setName("Child");
		addr.setEmail("child@localhost");
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		addr.setStreet1("123 Main Street");
		input.setAddress(addr);
		return input;
	}

	private static Map<String, Object> config(Object... userIds) {
		return Map.of("invoiceConsolidation", Map.of("userIds", List.of(userIds)));
	}

	private static @Nullable Object errorArgument(ValidationException e) {
		final Errors errors = e.getErrors();
		final ObjectError error = (errors != null ? errors.getGlobalError() : null);
		final Object[] args = (error != null ? error.getArguments() : null);
		return (args != null && args.length > 0 ? args[0] : null);
	}

	private static @Nullable String errorCode(ValidationException e) {
		final Errors errors = e.getErrors();
		final ObjectError error = (errors != null ? errors.getGlobalError() : null);
		return (error != null ? error.getCode() : null);
	}

	@Test
	public void getAccountForUser_parent() {
		// GIVEN
		final User parent = givenUser(PARENT_ID);
		parent.putInternalDataValue(ACCOUNTING_DATA_PROP, SYSTEM_KEY);
		final User child = givenUser(CHILD_ID);
		final User other = givenUser(OTHER_ID);
		given(registrar.supportsAccountingSystemKey(SYSTEM_KEY)).willReturn(true);
		given(registrar.getAccountForUser(same(parent))).willAnswer(_ -> account);
		given(userDao.getUserRoles(parent)).willReturn(Set.of());
		given(resolver.groupForUser(PARENT_ID)).willReturn(
				new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, OTHER_ID, CHILD_ID), false));

		// WHEN
		SnAccountInfo result = service.getAccountForUser(PARENT_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No parent account for the primary account of a group")
			.returns(null, from(SnAccountInfo::parentAccount))
			.as("Child accounts are the other accounts of the group, in order")
			.returns(List.of(SnLinkedAccountInfo.forUser(other), SnLinkedAccountInfo.forUser(child)),
					from(SnAccountInfo::childAccounts))
			.as("No problem for a valid group")
			.returns(null, from(SnAccountInfo::consolidationProblem))
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser_child() {
		// GIVEN
		final User parent = givenUser(PARENT_ID);
		final User child = givenUser(CHILD_ID);
		child.putInternalDataValue(ACCOUNTING_DATA_PROP, SYSTEM_KEY);
		given(registrar.supportsAccountingSystemKey(SYSTEM_KEY)).willReturn(true);
		given(registrar.getAccountForUser(same(child))).willAnswer(_ -> account);
		given(userDao.getUserRoles(child)).willReturn(Set.of());
		given(resolver.groupForUser(CHILD_ID)).willReturn(new InvoiceConsolidationGroup(PARENT_ID,
				List.of(PARENT_ID, CHILD_ID), false, CURRENCY_PROBLEM));

		// WHEN
		SnAccountInfo result = service.getAccountForUser(CHILD_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Parent account is the primary account of the group, even if the group is invalid")
			.returns(SnLinkedAccountInfo.forUser(parent), from(SnAccountInfo::parentAccount))
			.as("No child accounts for another account of a group")
			.returns(null, from(SnAccountInfo::childAccounts))
			.as("Problem of the invalid group provided, to show to the user")
			.returns(CURRENCY_PROBLEM, from(SnAccountInfo::consolidationProblem))
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser_noGroup() {
		// GIVEN
		final User user = givenUser(PARENT_ID);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, SYSTEM_KEY);
		given(registrar.supportsAccountingSystemKey(SYSTEM_KEY)).willReturn(true);
		given(registrar.getAccountForUser(same(user))).willAnswer(_ -> account);
		given(userDao.getUserRoles(user)).willReturn(Set.of());

		// WHEN
		SnAccountInfo result = service.getAccountForUser(PARENT_ID);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No parent account")
			.returns(null, from(SnAccountInfo::parentAccount))
			.as("No child accounts")
			.returns(null, from(SnAccountInfo::childAccounts))
			.as("No problem without a group")
			.returns(null, from(SnAccountInfo::consolidationProblem))
			;
		// @formatter:on
	}

	@Test
	public void verifyParentAccount() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);
		final SnAccountLinkInput input = credentials(parent);

		// WHEN
		SnLinkedAccountInfo result = service.verifyParentAccount(CHILD_ID, input);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Parent account returned")
			.isEqualTo(SnLinkedAccountInfo.forUser(parent))
			;
		and.then(input)
			.as("Input marked as verified for the parent account")
			.returns(result, from(SnAccountLinkInput::getVerifiedAccount))
			.as("Password cleared")
			.returns(null, from(SnAccountLinkInput::getPassword))
			;
		// @formatter:on
		then(userDao).should(never()).storeInternalData(any(), any());
	}

	@Test
	public void verifyParentAccount_badPassword() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		given(userDao.getUserByEmail(parent.getEmail())).willReturn(parent);
		final SnAccountLinkInput input = credentials(parent);

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Wrong password rejected")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, input))
			.as("Credentials error")
			.returns(DefaultUserAccountBiz.LINK_CREDENTIALS_ERROR_CODE, from(e -> errorCode(e)))
			;
		and.then(input.getVerifiedAccount())
			.as("Input not verified")
			.isNull()
			;
		// @formatter:on
		then(resolver).shouldHaveNoInteractions();
	}

	@Test
	public void verifyParentAccount_unknownEmail() {
		// GIVEN
		givenUser(CHILD_ID);
		final SnAccountLinkInput input = credentials(user(PARENT_ID));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Unknown email rejected")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, input))
			.as("Same error as a wrong password, so it does not reveal if the email exists")
			.returns(DefaultUserAccountBiz.LINK_CREDENTIALS_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
		then(passwordEncoder).shouldHaveNoInteractions();
	}

	@Test
	public void verifyParentAccount_self() {
		// GIVEN
		final User user = givenUser(CHILD_ID);
		givenCredentials(user);

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Account can not be linked to itself")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, credentials(user)))
			.returns(DefaultUserAccountBiz.LINK_SELF_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
	}

	@Test
	public void verifyParentAccount_childLinked() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);
		given(resolver.groupForUser(CHILD_ID))
				.willReturn(new InvoiceConsolidationGroup(OTHER_ID, List.of(OTHER_ID, CHILD_ID), false));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Account already linked to another account can not be linked")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, credentials(parent)))
			.returns(DefaultUserAccountBiz.LINK_LINKED_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
	}

	@Test
	public void verifyParentAccount_childIsParent() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);
		given(resolver.groupForUser(CHILD_ID))
				.willReturn(new InvoiceConsolidationGroup(CHILD_ID, List.of(CHILD_ID, OTHER_ID), false));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Account with child accounts of its own can not be linked")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, credentials(parent)))
			.returns(DefaultUserAccountBiz.LINK_PARENT_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
	}

	@Test
	public void verifyParentAccount_parentIsChild() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);
		given(resolver.groupForUser(CHILD_ID)).willReturn(null);
		given(resolver.groupForUser(PARENT_ID)).willReturn(
				new InvoiceConsolidationGroup(OTHER_ID, List.of(OTHER_ID, PARENT_ID), false));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Account can not be linked to a child account")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, credentials(parent)))
			.returns(DefaultUserAccountBiz.LINK_PARENT_LINKED_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
	}

	@Test
	public void verifyParentAccount_unavailable() {
		// GIVEN
		service.setConsolidationResolver(null);
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Linking needs the resolver")
			.isThrownBy(() -> service.verifyParentAccount(CHILD_ID, credentials(parent)))
			.returns(DefaultUserAccountBiz.LINK_UNAVAILABLE_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
	}

	@Test
	public void createAccount_verifiedParent() {
		// GIVEN
		final User child = givenUser(CHILD_ID);
		final User parent = givenUser(PARENT_ID);
		final SnAccountCreationInput input = creationInput();
		final SnAccountLinkInput link = new SnAccountLinkInput();
		link.setUsername(parent.getEmail());
		link.verified(SnLinkedAccountInfo.forUser(parent));
		input.setParentAccount(link);

		given(registrar.getAccountingSystemKey()).willReturn(SYSTEM_KEY);
		given(registrar.createAccount(eq(CHILD_ID), same(input))).willAnswer(_ -> account);
		given(userDao.getUserRoles(child)).willReturn(Set.of());
		given(userDao.getInternalData(PARENT_ID)).willReturn(config(OTHER_ID));
		given(resolver.groupForUser(CHILD_ID)).willReturn(null);
		given(resolver.groupForUser(PARENT_ID)).willReturn(
				new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, OTHER_ID), false),
				new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, OTHER_ID, CHILD_ID),
						false));

		// WHEN
		SnAccountInfo result = service.createAccount(CHILD_ID, input);

		// THEN
		InOrder ordered = inOrder(registrar, userDao);
		ordered.verify(registrar).createAccount(CHILD_ID, input);
		ordered.verify(userDao).lockUser(PARENT_ID);
		ordered.verify(userDao).lockUser(CHILD_ID);
		ordered.verify(userDao).storeInternalData(eq(PARENT_ID), internalDataCaptor.capture());
		// @formatter:off
		and.then(internalDataCaptor.getValue())
			.as("Child added to the group of the parent, after the existing accounts")
			.isEqualTo(config(OTHER_ID, CHILD_ID))
			;
		and.then(result)
			.as("Parent account returned")
			.returns(SnLinkedAccountInfo.forUser(parent), from(SnAccountInfo::parentAccount))
			;
		// @formatter:on
		then(passwordEncoder).shouldHaveNoInteractions();
	}

	@Test
	public void createAccount_parentCredentials() {
		// GIVEN
		final User child = givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		givenCredentials(parent);
		final SnAccountCreationInput input = creationInput();
		input.setParentAccount(credentials(parent));

		given(registrar.getAccountingSystemKey()).willReturn(SYSTEM_KEY);
		given(registrar.createAccount(eq(CHILD_ID), same(input))).willAnswer(_ -> account);
		given(userDao.getUserRoles(child)).willReturn(Set.of());
		given(resolver.groupForUser(CHILD_ID)).willReturn(null);
		given(resolver.groupForUser(PARENT_ID)).willReturn(null,
				new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, CHILD_ID), false));

		// WHEN
		SnAccountInfo result = service.createAccount(CHILD_ID, input);

		// THEN
		then(userDao).should().storeInternalData(eq(PARENT_ID), internalDataCaptor.capture());
		// @formatter:off
		and.then(internalDataCaptor.getValue())
			.as("Group created for the parent with the child")
			.isEqualTo(config(CHILD_ID))
			;
		and.then(result)
			.as("Parent account returned")
			.returns(SnLinkedAccountInfo.forUser(parent), from(SnAccountInfo::parentAccount))
			;
		// @formatter:on
	}

	@Test
	public void createAccount_parentCredentials_bad() {
		// GIVEN
		givenUser(CHILD_ID);
		final User parent = user(PARENT_ID);
		given(userDao.getUserByEmail(parent.getEmail())).willReturn(parent);
		final SnAccountCreationInput input = creationInput();
		input.setParentAccount(credentials(parent));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Wrong parent password rejected")
			.isThrownBy(() -> service.createAccount(CHILD_ID, input))
			.returns(DefaultUserAccountBiz.LINK_CREDENTIALS_ERROR_CODE, from(e -> errorCode(e)))
			;
		// @formatter:on
		then(registrar).shouldHaveNoInteractions();
	}

	@Test
	public void createAccount_parentGroupInvalid() {
		// GIVEN
		final User child = givenUser(CHILD_ID);
		final User parent = givenUser(PARENT_ID);
		final SnAccountCreationInput input = creationInput();
		final SnAccountLinkInput link = new SnAccountLinkInput();
		link.setUsername(parent.getEmail());
		link.verified(SnLinkedAccountInfo.forUser(parent));
		input.setParentAccount(link);

		given(registrar.getAccountingSystemKey()).willReturn(SYSTEM_KEY);
		given(registrar.createAccount(eq(CHILD_ID), same(input))).willAnswer(_ -> account);
		given(userDao.getUserRoles(child)).willReturn(Set.of());
		given(resolver.groupForUser(CHILD_ID)).willReturn(null);
		given(resolver.groupForUser(PARENT_ID)).willReturn(null, new InvoiceConsolidationGroup(
				PARENT_ID, List.of(PARENT_ID, CHILD_ID), false, CURRENCY_PROBLEM));

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Linking that makes an invalid group rejected, so the transaction rolls back")
			.isThrownBy(() -> service.createAccount(CHILD_ID, input))
			.returns(DefaultUserAccountBiz.LINK_INVALID_ERROR_CODE, from(e -> errorCode(e)))
			.as("Problem given as the error argument, to resolve where the error is shown")
			.returns(CURRENCY_PROBLEM, from(e -> errorArgument(e)))
			.as("Exception message from the default messages")
			.returns("The accounts can not be linked: User 2 is billed in USD, not NZD.",
					from(Throwable::getMessage))
			;
		// @formatter:on
	}

	@Test
	public void linkAccount() {
		// GIVEN
		givenUser(PARENT_ID);
		final User child = user(CHILD_ID);
		givenCredentials(child);
		given(resolver.groupForUser(CHILD_ID)).willReturn(null);
		given(resolver.groupForUser(PARENT_ID)).willReturn(null,
				new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, CHILD_ID), false));

		// WHEN
		SnLinkedAccountInfo result = service.linkAccount(PARENT_ID, credentials(child));

		// THEN
		then(userDao).should().storeInternalData(eq(PARENT_ID), internalDataCaptor.capture());
		// @formatter:off
		and.then(internalDataCaptor.getValue())
			.as("Child added to the group of the parent")
			.isEqualTo(config(CHILD_ID))
			;
		and.then(result)
			.as("Child account returned")
			.isEqualTo(SnLinkedAccountInfo.forUser(child))
			;
		// @formatter:on
	}

	@Test
	public void linkAccount_alreadyLinked() {
		// GIVEN
		givenUser(PARENT_ID);
		final User child = user(CHILD_ID);
		givenCredentials(child);
		given(resolver.groupForUser(CHILD_ID))
				.willReturn(new InvoiceConsolidationGroup(PARENT_ID, List.of(PARENT_ID, CHILD_ID), false));

		// WHEN
		SnLinkedAccountInfo result = service.linkAccount(PARENT_ID, credentials(child));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Child account returned")
			.isEqualTo(SnLinkedAccountInfo.forUser(child))
			;
		// @formatter:on
		then(userDao).should(never()).storeInternalData(any(), any());
	}

	@Test
	public void unlinkAccount() {
		// GIVEN
		givenUser(PARENT_ID);
		final User other = givenUser(OTHER_ID);
		given(userDao.getInternalData(PARENT_ID)).willReturn(config(CHILD_ID, OTHER_ID));

		// WHEN
		var result = service.unlinkAccount(PARENT_ID, CHILD_ID);

		// THEN
		InOrder ordered = inOrder(userDao);
		ordered.verify(userDao).lockUser(PARENT_ID);
		ordered.verify(userDao).storeInternalData(eq(PARENT_ID), internalDataCaptor.capture());
		// @formatter:off
		and.then(internalDataCaptor.getValue())
			.as("Child removed from the group of the parent")
			.isEqualTo(config(OTHER_ID))
			;
		and.then(result)
			.as("Remaining child accounts returned")
			.containsExactly(SnLinkedAccountInfo.forUser(other))
			;
		// @formatter:on
	}

	@Test
	public void unlinkAccount_last() {
		// GIVEN
		givenUser(PARENT_ID);
		given(userDao.getInternalData(PARENT_ID)).willReturn(config(2));

		// WHEN
		var result = service.unlinkAccount(PARENT_ID, CHILD_ID);

		// THEN
		then(userDao).should().storeInternalData(eq(PARENT_ID), internalDataCaptor.capture());
		// @formatter:off
		and.then(internalDataCaptor.getValue())
			.as("Child removed, leaving an empty list that is no group")
			.isEqualTo(config())
			;
		and.then(result)
			.as("No child accounts remain")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void unlinkAccount_notLinked() {
		// GIVEN
		givenUser(PARENT_ID);
		final User other = givenUser(OTHER_ID);
		given(userDao.getInternalData(PARENT_ID)).willReturn(config(OTHER_ID));

		// WHEN
		var result = service.unlinkAccount(PARENT_ID, CHILD_ID);

		// THEN
		then(userDao).should(never()).storeInternalData(any(), any());
		// @formatter:off
		and.then(result)
			.as("Child accounts unchanged")
			.containsExactly(SnLinkedAccountInfo.forUser(other))
			;
		// @formatter:on
	}

}
