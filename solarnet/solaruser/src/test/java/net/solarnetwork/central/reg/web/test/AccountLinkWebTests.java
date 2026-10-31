/* ==================================================================
 * AccountLinkWebTests.java - 15/10/2026 7:05:13 pm
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.central.reg.web.AccountRegistrationController.ACCOUNT_INPUT;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static net.solarnetwork.central.user.billing.snf.support.UserDataInvoiceConsolidationResolver.PROBLEM_CURRENCY_MISMATCH;
import static org.assertj.core.api.BDDAssertions.and;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.money.Monetary;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.servlet.http.Cookie;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.account.biz.UserAccountBiz;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.account.domain.SnLinkedAccountInfo;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;
import net.solarnetwork.central.user.billing.snf.dao.AccountDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.service.PasswordEncoder;

/**
 * Web tests for linking an account to a parent account for consolidated
 * billing, and unlinking it again.
 *
 * <p>
 * The active user's account is the child account: either the active user
 * registers a new account linked to the parent account, or the parent account
 * links the active user's account with its credentials.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetUserBillingConfiguration.SNF_BILLING)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountLinkWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String ACCOUNT_URL = "/u/sec/account";
	private static final String REGISTER_URL = "/u/sec/account/register";
	private static final String CONFIRM_URL = "/u/sec/account/register/confirm";
	private static final String LINK_URL = "/u/sec/account/link";
	private static final String UNLINK_URL = "/u/sec/account/unlink";
	private static final String VIEW_URL = "/api/v1/sec/user/account/view";

	private static final Long PARENT_USER_ID = 2L;
	private static final String PARENT_USERNAME = "parent@localhost";
	private static final String PARENT_NAME = "Tester 2";
	private static final String PARENT_PASSWORD = "parent-secret-4fd8b2";
	private static final String CHILD_PASSWORD = "child-secret-9c1e7a";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserAccountBiz userAccountBiz;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private AccountDao accountDao;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, passwordEncoder.encode(CHILD_PASSWORD),
				TEST_LOC_ID);
		setupTestUser(PARENT_USER_ID, PARENT_USERNAME, passwordEncoder.encode(PARENT_PASSWORD),
				TEST_LOC_ID);
	}

	/**
	 * Run something as another user.
	 *
	 * <p>
	 * The user is set on the test security context, which is what MockMvc
	 * requests run with, the same way {@link WithMockSecurityUser} sets the
	 * active user.
	 * </p>
	 */
	private <T> T asUser(Long userId, String username, Supplier<T> action) {
		final Authentication auth = TestSecurityContextHolder.getContext().getAuthentication();
		final var principal = new AuthenticatedUser(
				new User(username, "password", AuthorityUtils.createAuthorityList("ROLE_USER")), userId,
				null, false);
		try {
			TestSecurityContextHolder.setAuthentication(new UsernamePasswordAuthenticationToken(
					principal, "password", principal.getAuthorities()));
			return action.get();
		} finally {
			if ( auth != null ) {
				TestSecurityContextHolder.setAuthentication(auth);
			} else {
				TestSecurityContextHolder.clearContext();
			}
		}
	}

	/**
	 * Perform a request as the parent account user.
	 */
	private ResultActions asParent(RequestBuilder request) {
		return asUser(PARENT_USER_ID, PARENT_USERNAME, () -> {
			try {
				return mvc.perform(request);
			} catch ( Exception e ) {
				throw new RuntimeException(e);
			}
		});
	}

	private static SnAccountCreationInput accountInput(String email) {
		return accountInput(email, "NZD");
	}

	private static SnAccountCreationInput accountInput(String email, String currency) {
		final var input = new SnAccountCreationInput();
		final var addr = new SnAddressInput();
		addr.setName("Parent Co");
		addr.setEmail(email);
		addr.setStreet1("Level 2");
		addr.setLocality("Wellington");
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		input.setAddress(addr);
		final var acct = new SnAccountInput();
		acct.setCurrency(Monetary.getCurrency(currency));
		acct.setLocale("en-NZ");
		input.setAccount(acct);
		return input;
	}

	private void givenParentAccount() {
		asUser(PARENT_USER_ID, PARENT_USERNAME,
				() -> userAccountBiz.createAccount(PARENT_USER_ID, accountInput(PARENT_USERNAME)));
	}

	private void givenChildAccount(String currency) {
		asUser(DEFAULT_USER_ID, DEFAULT_USERNAME, () -> userAccountBiz.createAccount(DEFAULT_USER_ID,
				accountInput(DEFAULT_USERNAME, currency)));
	}

	private static MockHttpServletRequestBuilder linkForm(String password) {
		// @formatter:off
		return post(LINK_URL)
				.param("username", DEFAULT_USERNAME)
				.param("password", password)
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
				;
		// @formatter:on
	}

	private static MockHttpServletRequestBuilder registerForm(String parentPassword) {
		return registerForm(parentPassword, "NZD");
	}

	private static MockHttpServletRequestBuilder registerForm(String parentPassword, String currency) {
		// @formatter:off
		return post(REGISTER_URL)
				.param("address.name", "Tester Dude")
				.param("address.email", "billing@localhost")
				.param("address.street1", "Level 1")
				.param("address.locality", "Wellington")
				.param("address.country", "NZ")
				.param("address.timeZoneId", "Pacific/Auckland")
				.param("account.currency", currency)
				.param("account.locale", "en-NZ")
				.param("parentAccount.username", PARENT_USERNAME)
				.param("parentAccount.password", parentPassword)
				.with(csrf())
				;
		// @formatter:on
	}

	private @Nullable SnAccountInfo account() throws Exception {
		// @formatter:off
		return (SnAccountInfo) mvc.perform(get(ACCOUNT_URL))
			.andExpect(status().isOk())
			.andReturn()
			.getModelAndView()
			.getModel()
			.get("account")
			;
		// @formatter:on
	}

	private @Nullable String parentUserIds() {
		return jdbcTemplate.queryForObject(
				"SELECT jdata->'invoiceConsolidation'->>'userIds' FROM solaruser.user_user WHERE id = ?",
				String.class, PARENT_USER_ID);
	}

	private boolean sessionsContain(String value) {
		final byte[] bytes = value.getBytes(UTF_8);
		final List<byte[]> attributes = jdbcTemplate.queryForList(
				"SELECT attribute_bytes FROM solaruser.http_session_attributes", byte[].class);
		for ( byte[] attribute : attributes ) {
			for ( int i = 0, max = attribute.length - bytes.length; i <= max; i++ ) {
				int j = 0;
				while ( j < bytes.length && attribute[i + j] == bytes[j] ) {
					j++;
				}
				if ( j == bytes.length ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Register the active user's account linked to the parent account.
	 */
	private void registerLinked() throws Exception {
		final MvcResult review = mvc.perform(registerForm(PARENT_PASSWORD)).andExpect(status().isOk())
				.andExpect(view().name("sec/account/register-confirm")).andExpect(model().hasNoErrors())
				.andReturn();
		final Cookie[] session = review.getResponse().getCookies();
		mvc.perform(post(CONFIRM_URL).cookie(session).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("sec/account/register-complete"))
				.andExpect(model().hasNoErrors());
	}

	@Test
	@WithMockSecurityUser
	public void register_linked() throws Exception {
		// GIVEN
		givenParentAccount();

		// WHEN
		// @formatter:off
		final MvcResult review = mvc.perform(registerForm(PARENT_PASSWORD))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register-confirm"))
			.andExpect(model().hasNoErrors())
			.andReturn()
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(review.getResponse().getContentAsString())
			.as("Review page shows the verified parent account")
			.contains(PARENT_NAME, PARENT_USERNAME)
			;
		and.then(sessionsContain(PARENT_USERNAME))
			.as("Form input with the parent account kept in the session")
			.isTrue()
			;
		and.then(sessionsContain(PARENT_PASSWORD))
			.as("Parent account password never stored in the session")
			.isFalse()
			;
		// @formatter:on

		// WHEN
		// @formatter:off
		mvc.perform(post(CONFIRM_URL).cookie(review.getResponse().getCookies()).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register-complete"))
			.andExpect(model().hasNoErrors())
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(parentUserIds())
			.as("New account added to the invoice consolidation group of the parent")
			.isEqualTo("[%d]".formatted(DEFAULT_USER_ID))
			;
		and.then(account().parentAccount())
			.as("Parent account shown on the account page")
			.isEqualTo(new SnLinkedAccountInfo(PARENT_USER_ID, PARENT_NAME, PARENT_USERNAME))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_wrongParentPassword() throws Exception {
		// GIVEN
		givenParentAccount();

		// WHEN
		// @formatter:off
		final MvcResult result = mvc.perform(registerForm("not-the-password"))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register"))
			.andExpect(model().attributeHasFieldErrorCode(ACCOUNT_INPUT, "parentAccount.username",
					"account.consolidated.error.credentials"))
			.andReturn()
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(result.getResponse().getContentAsString())
			.as("Problem shown on the form")
			.contains("The account email or password is not valid.")
			;
		and.then(parentUserIds())
			.as("Parent account not changed")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void problemMessages() {
		// @formatter:off
		and.then(messageSource.getMessage(PROBLEM_CURRENCY_MISMATCH,
				new Object[] { 12345L, "USD", "NZD" }, Locale.ENGLISH))
			.as("Invoice consolidation problem messages available to resolve where problems are shown")
			.isEqualTo("User 12345 is billed in USD, not NZD.")
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_parentCurrencyMismatch() throws Exception {
		// GIVEN
		givenParentAccount();
		// @formatter:off
		final MvcResult review = mvc.perform(registerForm(PARENT_PASSWORD, "USD"))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register-confirm"))
			.andExpect(model().hasNoErrors())
			.andReturn()
			;
		// @formatter:on

		// WHEN
		// @formatter:off
		final MvcResult result = mvc.perform(post(CONFIRM_URL)
				.cookie(review.getResponse().getCookies()).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register"))
			.andReturn()
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(result.getResponse().getContentAsString())
			.as("Problem of the group the link would make resolved into the error shown on the form")
			.contains("The accounts can not be linked: User %d is billed in USD, not NZD."
					.formatted(DEFAULT_USER_ID))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void accountPage_groupProblem() throws Exception {
		// GIVEN
		givenParentAccount();
		registerLinked();

		// the child account changes currency after it was linked
		final Account childAccount = accountDao.getForUser(DEFAULT_USER_ID);
		childAccount.setCurrencyCode("USD");
		accountDao.save(childAccount);

		final String expectedProblem = "Invoicing is on hold for these accounts until this problem is fixed:";
		final String expectedMessage = "User %d is billed in USD, not NZD.".formatted(DEFAULT_USER_ID);

		// WHEN
		final String parentPage = asParent(get(ACCOUNT_URL)).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();
		final String childPage = mvc.perform(get(ACCOUNT_URL)).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();

		// THEN
		// @formatter:off
		and.then(parentPage)
			.as("Account page of the parent shows the problem with its group, resolved")
			.contains(expectedProblem, expectedMessage)
			;
		and.then(childPage)
			.as("Account page of the child shows the problem with its group, resolved")
			.contains(expectedProblem, expectedMessage)
			;

		// the account JSON leaves the problem out, rather than show it unresolved
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.parentAccount.userId").value(PARENT_USER_ID))
			.andExpect(jsonPath("$.data.consolidationProblem").doesNotExist())
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void parentAccountPage_unlink() throws Exception {
		// GIVEN
		givenParentAccount();
		registerLinked();

		// WHEN
		final String page = asParent(get(ACCOUNT_URL)).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();

		// THEN
		// @formatter:off
		and.then(page)
			.as("Account page of the parent lists the child account, with a button to unlink it")
			.contains(DEFAULT_USERNAME, "data-user-id=\"%d\"".formatted(DEFAULT_USER_ID))
			.as("No problem shown for a valid group")
			.doesNotContain("Invoicing is on hold")
			;
		// @formatter:on

		// WHEN
		final String unlinked = asParent(
				post(UNLINK_URL).param("userId", DEFAULT_USER_ID.toString()).with(csrf()))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// THEN
		// @formatter:off
		and.then(unlinked)
			.as("No child accounts remain")
			.contains("\"data\":[]")
			;
		and.then(parentUserIds())
			.as("Child account removed from the group of the parent")
			.isEqualTo("[]")
			;
		and.then(account().parentAccount())
			.as("Child account no longer has a parent account")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void parentAccountPage_link() throws Exception {
		// GIVEN
		givenParentAccount();
		givenChildAccount("NZD");

		// WHEN
		// @formatter:off
		asParent(linkForm(CHILD_PASSWORD))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(DEFAULT_USER_ID))
			.andExpect(jsonPath("$.data.email").value(DEFAULT_USERNAME))
			;

		// THEN
		and.then(parentUserIds())
			.as("Child account added to the group of the parent")
			.isEqualTo("[%d]".formatted(DEFAULT_USER_ID))
			;
		and.then(account().parentAccount())
			.as("Child account now has the parent account")
			.isEqualTo(new SnLinkedAccountInfo(PARENT_USER_ID, PARENT_NAME, PARENT_USERNAME))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void parentAccountPage_link_wrongPassword() throws Exception {
		// GIVEN
		givenParentAccount();
		givenChildAccount("NZD");

		// WHEN
		// @formatter:off
		asParent(linkForm("not-the-password"))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("The account email or password is not valid."))
			;

		// THEN
		and.then(parentUserIds())
			.as("No group stored for the parent")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void parentAccountPage_link_groupProblem() throws Exception {
		// GIVEN
		givenParentAccount();
		givenChildAccount("USD");

		// WHEN
		// @formatter:off
		asParent(linkForm(CHILD_PASSWORD))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value(
					"The accounts can not be linked: User %d is billed in USD, not NZD."
					.formatted(DEFAULT_USER_ID)))
			;
		// @formatter:on
	}

}
