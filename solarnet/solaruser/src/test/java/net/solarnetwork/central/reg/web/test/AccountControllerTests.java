/* ==================================================================
 * AccountControllerTests.java - 28 Sept 2026 11:32:09 am
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
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import net.solarnetwork.central.reg.web.AccountController;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccount;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;

/**
 * Test cases for the {@link AccountController} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AccountControllerTests {

	@Mock
	private UserAccountBiz userAccountBiz;

	@Mock
	private SnAccount<?, ?, ?> account;

	private StaticMessageSource messageSource;
	private AccountController controller;

	@BeforeEach
	public void setup() {
		messageSource = new StaticMessageSource();
		controller = new AccountController(userAccountBiz, messageSource);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void home() {
		// GIVEN
		final var model = new ExtendedModelMap();
		model.addAttribute("account", new SnAccountInfo(randomString(), account, Set.of()));

		// WHEN
		final String result = controller.home(model, Locale.ENGLISH);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Account main page view returned")
			.isEqualTo("sec/account/account")
			;
		and.then(model)
			.as("No consolidation problem provided when the account has none")
			.doesNotContainKey("consolidationProblem")
			;
		// @formatter:on
	}

	@Test
	public void home_notRegistered() {
		// GIVEN
		final var model = new ExtendedModelMap();
		model.addAttribute("account", null);

		// WHEN
		final String result = controller.home(model, Locale.ENGLISH);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Account main page view returned")
			.isEqualTo("sec/account/account")
			;
		and.then(model)
			.as("No consolidation problem provided without an account")
			.doesNotContainKey("consolidationProblem")
			;
		// @formatter:on
	}

	@Test
	public void home_consolidationProblem() {
		// GIVEN
		final Long userId = randomLong();
		messageSource.addMessage("test.problem", Locale.GERMAN,
				"Benutzer {0,number,#} ist nicht vorhanden.");
		final var problem = new DefaultMessageSourceResolvable(new String[] { "test.problem" },
				new Object[] { userId }, "User {0,number,#} is not available.");

		final var model = new ExtendedModelMap();
		model.addAttribute("account",
				new SnAccountInfo(randomString(), account, Set.of(), null, null, problem));

		// WHEN
		final String result = controller.home(model, Locale.GERMAN);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Account main page view returned")
			.isEqualTo("sec/account/account")
			;
		and.then(model)
			.as("Consolidation problem resolved in the request locale")
			.containsEntry("consolidationProblem",
					"Benutzer %d ist nicht vorhanden.".formatted(userId))
			;
		// @formatter:on
	}

	@Test
	public void account() {
		// GIVEN
		final Long userId = randomLong();
		final var info = new SnAccountInfo(randomString(), account, Set.of());
		given(userAccountBiz.getAccountForUser(userId)).willReturn(info);

		// WHEN
		becomeUser(randomString(), null, userId);

		final SnAccountInfo result = controller.account();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Account info for the active user returned")
			.isSameAs(info)
			;
		// @formatter:on
	}

	@Test
	public void account_notRegistered() {
		// GIVEN
		final Long userId = randomLong();
		given(userAccountBiz.getAccountForUser(userId))
				.willThrow(new AuthorizationException(Reason.REGISTRATION_NOT_CONFIRMED, userId));

		// WHEN
		becomeUser(randomString(), null, userId);

		final SnAccountInfo result = controller.account();

		// THEN
		// @formatter:off
		and.then(result)
			.as("Null model attribute returned when the user has no account, so the view can offer"
					+ " registration instead of failing")
			.isNull()
			;
		// @formatter:on
	}

}
