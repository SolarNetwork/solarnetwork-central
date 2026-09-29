/* ==================================================================
 * DefaultUserAccountBizTests.java - 28 Sept 2026 10:12:04 am
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

import static java.util.Collections.emptySet;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.DATUM_EXPORT;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OCPP;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OSCP;
import static net.solarnetwork.central.user.billing.domain.BillingDataConstants.ACCOUNTING_DATA_PROP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.account.biz.impl.DefaultUserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Test cases for the {@link DefaultUserAccountBiz} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DefaultUserAccountBizTests {

	/** The role granted to every account holder. */
	private static final String ROLE_BILLING = "ROLE_BILLING";

	@Mock
	private UserDao userDao;

	@Mock
	private BillingSystemRegistrar registrar1;

	@Mock
	private BillingSystemRegistrar registrar2;

	@Mock
	private SnAccount<?, ?, ?> account;

	@Captor
	private ArgumentCaptor<Set<String>> rolesCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> internalDataCaptor;

	private String systemKey1;
	private String systemKey2;
	private DefaultUserAccountBiz service;

	@BeforeEach
	public void setup() {
		systemKey1 = randomString();
		systemKey2 = randomString();
		service = new DefaultUserAccountBiz(List.of(registrar1, registrar2), userDao);
	}

	private static SnAccountCreationInput creationInput() {
		final var input = new SnAccountCreationInput();

		final var acct = new SnAccountInput();
		acct.setLocale("en-NZ");
		input.setAccount(acct);

		final var addr = new SnAddressInput();
		addr.setName(randomString());
		addr.setEmail(randomEmail());
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		addr.setStreet1("123 Main Street");
		input.setAddress(addr);

		return input;
	}

	private User user(Long userId) {
		final var user = new User(userId, randomEmail());
		user.setCreated(Instant.now());
		given(userDao.get(userId)).willReturn(user);
		return user;
	}

	@Test
	public void createAccount_defaultRegistrar() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		final var input = creationInput();

		// no system key on the input, so the first registrar is the default
		given(registrar1.getAccountingSystemKey()).willReturn(systemKey1);
		given(registrar1.createAccount(eq(userId), same(input))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user)).willReturn(Set.of());

		// WHEN
		SnAccountInfo result = service.createAccount(userId, input);

		// THEN
		// @formatter:off
		then(registrar2).shouldHaveNoInteractions()
			;

		then(userDao).should()
			.storeInternalData(eq(userId), internalDataCaptor.capture())
			;
		and.then(internalDataCaptor.getValue())
			.as("User assigned to the default registrar's accounting system")
			.containsEntry(ACCOUNTING_DATA_PROP, systemKey1)
			;

		then(userDao).should()
			.storeUserRoles(same(user), rolesCaptor.capture())
			;
		and.then(rolesCaptor.getValue())
			.as("Billing role granted, with no entitlement roles requested")
			.containsExactly(ROLE_BILLING)
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("System key of the registrar used returned")
			.returns(systemKey1, from(SnAccountInfo::systemKey))
			.as("Account from registrar returned")
			.returns(account, from(SnAccountInfo::account))
			.as("No entitlements returned, as none requested")
			.returns(emptySet(), from(SnAccountInfo::entitlements))
			;
		// @formatter:on
	}

	@Test
	public void createAccount_explicitRegistrar() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		final var input = creationInput();
		input.setSystemKey(systemKey2);

		given(registrar1.supportsAccountingSystemKey(systemKey2)).willReturn(false);
		given(registrar2.supportsAccountingSystemKey(systemKey2)).willReturn(true);
		given(registrar2.getAccountingSystemKey()).willReturn(systemKey2);
		given(registrar2.createAccount(eq(userId), same(input))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user)).willReturn(Set.of());

		// WHEN
		SnAccountInfo result = service.createAccount(userId, input);

		// THEN
		// @formatter:off
		then(registrar1).should(never())
			.createAccount(any(), any())
			;

		then(userDao).should()
			.storeInternalData(eq(userId), internalDataCaptor.capture())
			;
		and.then(internalDataCaptor.getValue())
			.as("User assigned to the requested accounting system")
			.containsEntry(ACCOUNTING_DATA_PROP, systemKey2)
			;

		and.then(result)
			.as("System key of the requested registrar returned")
			.returns(systemKey2, from(SnAccountInfo::systemKey))
			;
		// @formatter:on
	}

	@Test
	public void createAccount_unsupportedRegistrar() {
		// GIVEN
		final Long userId = randomLong();
		user(userId);
		final var input = creationInput();
		input.setSystemKey(randomString());

		given(registrar1.supportsAccountingSystemKey(input.getSystemKey())).willReturn(false);
		given(registrar2.supportsAccountingSystemKey(input.getSystemKey())).willReturn(false);

		// WHEN
		// @formatter:off
		thenExceptionOfType(IllegalArgumentException.class)
			.as("Exception thrown when no registrar supports the requested system key")
			.isThrownBy(() -> service.createAccount(userId, input))
			.withMessageContaining(input.getSystemKey())
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(userDao).should(never())
			.storeInternalData(any(), any())
			;
		then(userDao).should(never())
			.storeUserRoles(any(), any())
			;
		// @formatter:on
	}

	@Test
	public void createAccount_accountAlreadyExists() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);

		// an accounting system key on the user means they have already registered
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, systemKey1);

		final var input = creationInput();

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when the user already has an account")
			.isThrownBy(() -> service.createAccount(userId, input))
			.as("Registration already confirmed reason given")
			.returns(Reason.REGISTRATION_ALREADY_CONFIRMED, from(AuthorizationException::getReason))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(registrar1).shouldHaveNoInteractions()
			;
		then(registrar2).shouldHaveNoInteractions()
			;
		then(userDao).should(never())
			.storeInternalData(any(), any())
			;
		then(userDao).should(never())
			.storeUserRoles(any(), any())
			;
		// @formatter:on
	}

	@Test
	public void createAccount_unknownUser() {
		// GIVEN
		final Long userId = randomLong();
		given(userDao.get(userId)).willReturn(null);

		final var input = creationInput();

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown for unknown user")
			.isThrownBy(() -> service.createAccount(userId, input))
			.as("Unknown object reason given")
			.returns(Reason.UNKNOWN_OBJECT, from(AuthorizationException::getReason))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(registrar1).shouldHaveNoInteractions()
			;
		then(registrar2).shouldHaveNoInteractions()
			;
		// @formatter:on
	}

	@Test
	public void createAccount_entitlements() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		final var input = creationInput();
		input.setRequestedEntitlements(Set.of(OCPP, DATUM_EXPORT));

		given(registrar1.getAccountingSystemKey()).willReturn(systemKey1);
		given(registrar1.createAccount(eq(userId), same(input))).willAnswer(_ -> account);

		// the user already has an unrelated role, plus an entitlement role not requested now
		given(userDao.getUserRoles(user)).willReturn(Set.of("ROLE_USER", OSCP.getRoleName()));

		// WHEN
		SnAccountInfo result = service.createAccount(userId, input);

		// THEN
		// @formatter:off
		then(userDao).should()
			.storeUserRoles(same(user), rolesCaptor.capture())
			;
		and.then(rolesCaptor.getValue())
			.as("Unrelated roles preserved, billing role added, and requested entitlement roles added")
			.containsExactlyInAnyOrder("ROLE_USER", ROLE_BILLING, OCPP.getRoleName(),
					DATUM_EXPORT.getRoleName())
			.as("Entitlement role not requested is revoked")
			.doesNotContain(OSCP.getRoleName())
			;

		and.then(result)
			.as("Requested entitlements returned")
			.returns(Set.of(OCPP, DATUM_EXPORT), from(SnAccountInfo::entitlements))
			;
		// @formatter:on
	}

	@Test
	public void createAccount_invalidInput() {
		// GIVEN
		final Long userId = randomLong();
		user(userId);

		service.setValidator(Validation.buildDefaultValidatorFactory().getValidator());

		// address is required, so an input without one is not valid
		final var input = new SnAccountCreationInput();

		// WHEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Validation exception thrown for invalid input")
			.isThrownBy(() -> service.createAccount(userId, input))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(registrar1).should(never())
			.createAccount(any(), any())
			;
		then(userDao).should(never())
			.storeInternalData(any(), any())
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, systemKey2);

		given(registrar1.supportsAccountingSystemKey(systemKey2)).willReturn(false);
		given(registrar2.supportsAccountingSystemKey(systemKey2)).willReturn(true);
		given(registrar2.getAccountForUser(same(user))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user))
				.willReturn(Set.of("ROLE_USER", ROLE_BILLING, DATUM_EXPORT.getRoleName()));

		// WHEN
		SnAccountInfo result = service.getAccountForUser(userId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Accounting system key from the user's internal data returned")
			.returns(systemKey2, from(SnAccountInfo::systemKey))
			.as("Account from the matching registrar returned")
			.returns(account, from(SnAccountInfo::account))
			.as("Entitlements derived from the user's entitlement roles, ignoring other roles")
			.returns(Set.of(DATUM_EXPORT), from(SnAccountInfo::entitlements))
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser_notRegistered() {
		// GIVEN
		final Long userId = randomLong();
		user(userId);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when the user has no accounting system assigned")
			.isThrownBy(() -> service.getAccountForUser(userId))
			.as("Registration not confirmed reason given")
			.returns(Reason.REGISTRATION_NOT_CONFIRMED, from(AuthorizationException::getReason))
			;
		// @formatter:on
	}

	@Test
	public void getAccountForUser_unsupportedRegistrar() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, randomString());

		given(registrar1.supportsAccountingSystemKey(any())).willReturn(false);
		given(registrar2.supportsAccountingSystemKey(any())).willReturn(false);

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when no registrar handles the user's system key")
			.isThrownBy(() -> service.getAccountForUser(userId))
			.as("Unknown object reason given")
			.returns(Reason.UNKNOWN_OBJECT, from(AuthorizationException::getReason))
			;
		// @formatter:on
	}

	@Test
	public void updateAccount() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, systemKey2);

		final var input = creationInput();

		given(registrar1.supportsAccountingSystemKey(systemKey2)).willReturn(false);
		given(registrar2.supportsAccountingSystemKey(systemKey2)).willReturn(true);
		given(registrar2.getAccountingSystemKey()).willReturn(systemKey2);
		given(registrar2.updateAccount(eq(userId), same(input))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user)).willReturn(Set.of(ROLE_BILLING));

		// WHEN
		SnAccountInfo result = service.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(userDao).should(never())
			.storeInternalData(any(), any())
			;
		then(userDao).should(never())
			.storeUserRoles(any(), any())
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Accounting system key from the user's internal data returned")
			.returns(systemKey2, from(SnAccountInfo::systemKey))
			.as("Account from the registrar returned")
			.returns(account, from(SnAccountInfo::account))
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_systemKeyOnInputIgnored() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, systemKey1);

		final var input = creationInput();

		// changing registrars is not supported, so the key on the input is ignored
		input.setSystemKey(systemKey2);

		given(registrar1.supportsAccountingSystemKey(systemKey1)).willReturn(true);
		given(registrar1.getAccountingSystemKey()).willReturn(systemKey1);
		given(registrar1.updateAccount(eq(userId), same(input))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user)).willReturn(Set.of(ROLE_BILLING));

		// WHEN
		SnAccountInfo result = service.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(registrar2).shouldHaveNoInteractions()
			;
		and.then(result)
			.as("Registrar already assigned to the user is used")
			.returns(systemKey1, from(SnAccountInfo::systemKey))
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_entitlementsChanged() {
		// GIVEN
		final Long userId = randomLong();
		final User user = user(userId);
		user.putInternalDataValue(ACCOUNTING_DATA_PROP, systemKey1);

		final var input = creationInput();
		input.setRequestedEntitlements(Set.of(OCPP));

		given(registrar1.supportsAccountingSystemKey(systemKey1)).willReturn(true);
		given(registrar1.getAccountingSystemKey()).willReturn(systemKey1);
		given(registrar1.updateAccount(eq(userId), same(input))).willAnswer(_ -> account);

		given(userDao.getUserRoles(user)).willReturn(Set.of(ROLE_BILLING, DATUM_EXPORT.getRoleName()));

		// WHEN
		SnAccountInfo result = service.updateAccount(userId, input);

		// THEN
		// @formatter:off
		then(userDao).should()
			.storeUserRoles(same(user), rolesCaptor.capture())
			;
		and.then(rolesCaptor.getValue())
			.as("Requested entitlement role added and de-selected entitlement role revoked")
			.containsExactlyInAnyOrder(ROLE_BILLING, OCPP.getRoleName())
			;

		and.then(result)
			.as("Entitlements re-read from the user's roles")
			.returns(Set.of(DATUM_EXPORT), from(SnAccountInfo::entitlements))
			;
		// @formatter:on
	}

	@Test
	public void updateAccount_notRegistered() {
		// GIVEN
		final Long userId = randomLong();
		user(userId);

		final var input = creationInput();

		// WHEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Authorization exception thrown when the user has no accounting system assigned")
			.isThrownBy(() -> service.updateAccount(userId, input))
			.as("Unknown object reason given")
			.returns(Reason.UNKNOWN_OBJECT, from(AuthorizationException::getReason))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		then(registrar1).shouldHaveNoInteractions()
			;
		then(registrar2).shouldHaveNoInteractions()
			;
		// @formatter:on
	}

}
