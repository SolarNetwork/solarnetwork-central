/* ==================================================================
 * AccountRegistrationControllerTests.java - 28 Sept 2026 12:26:19 pm
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OSCP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.reg.web.api.v1.AccountRegistrationController;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link AccountRegistrationController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AccountRegistrationControllerTests {

	@Mock
	private UserAccountBiz userAccountBiz;

	@Mock
	private SnAccount<?, ?, ?> account;

	private AccountRegistrationController controller;

	@BeforeEach
	public void setup() {
		controller = new AccountRegistrationController(userAccountBiz);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void register() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new SnAccountCreationInput();
		input.setRequestedEntitlements(Set.of(OSCP));

		final var info = new SnAccountInfo(randomString(), account, Set.of(OSCP));
		given(userAccountBiz.createAccount(eq(userId), same(input))).willReturn(info);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<SnAccountInfo> result = controller.register(input);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("New account info returned")
			.isSameAs(info)
			;
		// @formatter:on
	}

	@Test
	public void register_invalidInput() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new SnAccountCreationInput();
		given(userAccountBiz.createAccount(eq(userId), same(input)))
				.willThrow(new ValidationException(null));

		// WHEN
		becomeUser(randomString(), null, userId);

		// THEN
		// @formatter:off
		thenExceptionOfType(ValidationException.class)
			.as("Service exception propagated, for the global REST exception handler to render")
			.isThrownBy(() -> controller.register(input))
			;
		// @formatter:on
	}

}
