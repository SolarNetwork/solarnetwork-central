/* ==================================================================
 * AccountRegistrationControllerTests.java - 28 Sept 2026 11:41:52 am
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
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.SimpleSessionStatus;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.reg.web.AccountRegistrationController;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.domain.User;

/**
 * Test cases for the {@link AccountRegistrationController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AccountRegistrationControllerTests {

	/** The register form view. */
	private static final String REGISTER_VIEW = "sec/account/register";

	@Mock
	private UserBiz userBiz;

	@Mock
	private UserAccountBiz userAccountBiz;

	@Mock
	private SnAccount<?, ?, ?> account;

	private AccountRegistrationController controller;

	@BeforeEach
	public void setup() {
		controller = new AccountRegistrationController(userBiz, userAccountBiz);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	private static BindingResult errorsFor(SnAccountCreationInput input) {
		return new BeanPropertyBindingResult(input, AccountRegistrationController.ACCOUNT_INPUT);
	}

	@Test
	public void register() {
		// @formatter:off
		and.then(controller.register())
			.as("Register form view returned")
			.isEqualTo(REGISTER_VIEW)
			;
		// @formatter:on
	}

	@Test
	public void accountInput_defaultsFromUser() {
		// GIVEN
		final Long userId = randomLong();
		final var user = new User(userId, randomEmail());
		user.setCountry("nz");
		user.setTimeZoneId("Pacific/Auckland");
		user.setLang("en");
		given(userBiz.getUser(userId)).willReturn(user);

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
			.as("Address defaults populated")
			.isNotNull()
			.as("Address email defaults to the user email")
			.returns(user.getEmail(), from(a -> a.getEmail()))
			.as("Address country defaults to the user country")
			.returns(user.getCountry(), from(a -> a.getCountry()))
			.as("Address time zone defaults to the user time zone")
			.returns("Pacific/Auckland", from(a -> a.getTimeZoneId()))
			;
		and.then(result.getAccount())
			.as("Account defaults populated")
			.isNotNull()
			.as("Locale defaults to the user language and country, with the country upper-cased")
			.returns("en-NZ", from(a -> a.getLocale()))
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
			.isEqualTo("sec/account/register-confirm")
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
			.isEqualTo(REGISTER_VIEW)
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

		final var info = new SnAccountInfo(randomString(), account, Set.of());
		given(userAccountBiz.createAccount(eq(userId), same(input))).willReturn(info);

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Completion view returned after the account is created")
			.isEqualTo("sec/account/register-complete")
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean cleared after the account is created")
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
			.createAccount(any(), any())
			;
		and.then(result)
			.as("Form view returned without creating an account")
			.isEqualTo(REGISTER_VIEW)
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
		bizErrors.rejectValue("address.country", "invalid.country");
		given(userAccountBiz.createAccount(eq(userId), same(input)))
				.willThrow(new ValidationException(bizErrors));

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form view returned when the service rejects the input")
			.isEqualTo(REGISTER_VIEW)
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

		final String message = randomString();
		given(userAccountBiz.createAccount(eq(userId), same(input)))
				.willThrow(new RuntimeException(message));

		// WHEN
		becomeUser(randomString(), null, userId);

		final String result = controller.confirm(input, errors, sessionStatus);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Form view returned when the service fails")
			.isEqualTo(REGISTER_VIEW)
			;
		and.then(errors.getGlobalErrors())
			.as("Service failure reported as a global form error")
			.hasSize(1)
			.element(0)
			.as("Exception message used as the error message")
			.returns(message, from(ObjectError::getDefaultMessage))
			;
		and.then(sessionStatus.isComplete())
			.as("Session form bean preserved so the user can retry")
			.isFalse()
			;
		// @formatter:on
	}

}
