/* ==================================================================
 * AccountEditControllerTests.java - 28 Sept 2026 11:58:04 am
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

package net.solarnetwork.central.reg.web.test;

import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.DATUM_EXPORT;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OCPP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.time.Instant;
import java.util.Set;
import javax.money.Monetary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SimpleSessionStatus;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.reg.web.AccountEditController;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Test cases for the {@link AccountEditController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AccountEditControllerTests {

	/** The edit form view. */
	private static final String EDIT_VIEW = "sec/account/edit";

	@Mock
	private UserAccountBiz userAccountBiz;

	private AccountEditController controller;

	@BeforeEach
	public void setup() {
		controller = new AccountEditController(userAccountBiz);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	private static BindingResult errorsFor(SnAccountCreationInput input) {
		return new BeanPropertyBindingResult(input, AccountEditController.ACCOUNT_INPUT);
	}

	private static Account account(Long userId) {
		final var addr = new Address(new UserLongCompositePK(userId, randomLong()), Instant.now(),
				randomString(), randomEmail(), "NZ", "Pacific/Auckland");
		addr.setStreet(new String[] { "Level 1", "123 Main Street" });
		addr.setLocality("Wellington");
		addr.setRegion("Region");
		addr.setStateOrProvince("State");
		addr.setPostalCode("1001");

		final var acct = new Account(new UserLongCompositePK(userId, randomLong()), Instant.now(), "NZD",
				"en-NZ");
		acct.setAddress(addr);
		return acct;
	}

	@Test
	public void edit() {
		// @formatter:off
		and.then(controller.edit())
			.as("Edit form view returned")
			.isEqualTo(EDIT_VIEW)
			;
		// @formatter:on
	}

	@Test
	public void accountInput_populatedFromAccount() {
		// GIVEN
		final Long userId = randomLong();
		final Account acct = account(userId);
		final Address addr = acct.getAddress();
		final var info = new SnAccountInfo(randomString(), acct, Set.of(OCPP, DATUM_EXPORT));
		given(userAccountBiz.getAccountForUser(userId)).willReturn(info);

		// WHEN
		becomeUser(randomString(), null, userId);

		final SnAccountCreationInput result = controller.accountInput();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form bean created")
			.isNotNull()
			;
		and.then(result.getAddress())
			.as("Address populated from the existing account address")
			.isNotNull()
			.as("Name from address")
			.returns(addr.getName(), from(a -> a.getName()))
			.as("Email from address")
			.returns(addr.getEmail(), from(a -> a.getEmail()))
			.as("Country from address")
			.returns(addr.getCountry(), from(a -> a.getCountry()))
			.as("Time zone from address")
			.returns(addr.getTimeZoneId(), from(a -> a.getTimeZoneId()))
			.as("Locality from address")
			.returns(addr.getLocality(), from(a -> a.getLocality()))
			.as("Region from address")
			.returns(addr.getRegion(), from(a -> a.getRegion()))
			.as("State from address")
			.returns(addr.getStateOrProvince(), from(a -> a.getStateOrProvince()))
			.as("Postal code from address")
			.returns(addr.getPostalCode(), from(a -> a.getPostalCode()))
			.as("First street line from address")
			.returns("Level 1", from(a -> a.getStreet1()))
			.as("Second street line from address")
			.returns("123 Main Street", from(a -> a.getStreet2()))
			;
		and.then(result.getAccount())
			.as("Account populated from the existing account")
			.isNotNull()
			.as("Currency resolved from the account currency code")
			.returns(Monetary.getCurrency("NZD"), from(a -> a.getCurrency()))
			.as("Locale from account")
			.returns("en-NZ", from(a -> a.getLocale()))
			;
		and.then(result.getRequestedEntitlements())
			.as("Requested entitlements pre-selected from the granted entitlements")
			.containsExactlyInAnyOrder(OCPP, DATUM_EXPORT)
			;
		// @formatter:on
	}

	@Test
	public void submit() {
		// GIVEN
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);

		// WHEN
		final String result = controller.submit(input, errors);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Review view returned when the form has no errors")
			.isEqualTo("sec/account/edit-confirm")
			;
		// @formatter:on
	}

	@Test
	public void submit_errors() {
		// GIVEN
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);
		errors.reject("test.error");

		// WHEN
		final String result = controller.submit(input, errors);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form view returned when the form has errors")
			.isEqualTo(EDIT_VIEW)
			;
		// @formatter:on
	}

	@Test
	public void confirm() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);
		final var sessionStatus = new SimpleSessionStatus();

		final var info = new SnAccountInfo(randomString(), account(userId), Set.of());
		given(userAccountBiz.updateAccount(eq(userId), same(input))).willReturn(info);

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Redirect to the account page after the account is updated")
			.isEqualTo("redirect:/u/sec/account")
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean cleared after the account is updated")
			.isTrue()
			;
		// @formatter:on
	}

	@Test
	public void confirm_errors() {
		// GIVEN
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);
		errors.reject("test.error");
		final var sessionStatus = new SimpleSessionStatus();

		// WHEN
		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		then(userAccountBiz).should(never())
			.updateAccount(any(), any())
			;
		and.then(result)
			.as("Form view returned without updating the account")
			.isEqualTo(EDIT_VIEW)
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean preserved so the user can correct the form")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void confirm_validationException() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);
		final var sessionStatus = new SimpleSessionStatus();

		final BindingResult bizErrors = errorsFor(input);
		bizErrors.rejectValue("address.timeZoneId", "invalid.timeZoneId");
		given(userAccountBiz.updateAccount(eq(userId), same(input)))
				.willThrow(new ValidationException(bizErrors));

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form view returned when the service rejects the input")
			.isEqualTo(EDIT_VIEW)
			;
		and.then(errors.getFieldErrors())
			.as("Service validation errors copied onto the form errors")
			.hasSize(1)
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean preserved so the user can correct the form")
			.isFalse()
			;
		// @formatter:on
	}

	@Test
	public void confirm_serviceException() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new SnAccountCreationInput();
		final BindingResult errors = errorsFor(input);
		final var sessionStatus = new SimpleSessionStatus();

		given(userAccountBiz.updateAccount(eq(userId), same(input)))
				.willThrow(new RuntimeException(randomString()));

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form view returned when the service fails")
			.isEqualTo(EDIT_VIEW)
			;
		and.then(errors.getGlobalErrors())
			.as("Service failure reported as a global form error")
			.hasSize(1)
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean preserved so the user can retry")
			.isFalse()
			;
		// @formatter:on
	}

}
