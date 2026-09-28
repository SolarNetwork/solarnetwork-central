/* ==================================================================
 * AccountRegistrationControllerWebTests.java - 28 Sept 2026 4:41:18 pm
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

import static net.solarnetwork.central.reg.web.AccountRegistrationController.ACCOUNT_INPUT;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.DATUM_EXPORT;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OCPP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.servlet.http.Cookie;
import net.solarnetwork.central.reg.web.AccountRegistrationController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.account.domain.SnAccountInput;
import net.solarnetwork.central.user.account.domain.SnAddressInput;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;

/**
 * Web integration tests for the {@link AccountRegistrationController} class.
 *
 * <p>
 * The {@code snf-billing} profile is required, as that is what contributes the
 * {@code BillingSystemRegistrar} the account service registers accounts with.
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
public class AccountRegistrationControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The account page URL. */
	private static final String ACCOUNT_URL = "/u/sec/account";

	/** The register form URL. */
	private static final String REGISTER_URL = "/u/sec/account/register";

	/** The register confirm URL. */
	private static final String CONFIRM_URL = "/u/sec/account/register/confirm";

	/** The register form view. */
	private static final String REGISTER_VIEW = "sec/account/register";

	/** The register review view. */
	private static final String CONFIRM_VIEW = "sec/account/register-confirm";

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, TEST_LOC_ID);
	}

	/**
	 * Populate a request with a complete, valid set of form parameters.
	 *
	 * @param request
	 *        the request to populate
	 * @return the request
	 */
	private static MockHttpServletRequestBuilder validForm(MockHttpServletRequestBuilder request) {
		// @formatter:off
		return request
				.param("address.name", "Tester Dude")
				.param("address.email", "billing@localhost")
				.param("address.street1", "Level 1")
				.param("address.street2", "123 Main Street")
				.param("address.locality", "Wellington")
				.param("address.stateOrProvince", "Wellington")
				.param("address.region", "Wellington")
				.param("address.postalCode", "6011")
				.param("address.country", "NZ")
				.param("address.timeZoneId", "Pacific/Auckland")
				.param("account.currency", "NZD")
				.param("account.locale", "en-NZ")
				.with(csrf())
				;
		// @formatter:on
	}

	/**
	 * Perform the review step, returning the session cookies that carry the
	 * form bean on to the confirm step.
	 *
	 * <p>
	 * The form bean is a {@code @SessionAttributes} attribute, and the session
	 * is backed by Spring Session rather than by the mock request session, so
	 * the cookies have to be passed along the way a browser would.
	 * </p>
	 *
	 * @param request
	 *        the review request
	 * @return the session cookies
	 */
	private Cookie[] review(MockHttpServletRequestBuilder request) throws Exception {
		// @formatter:off
		return mvc.perform(request)
			.andExpect(status().isOk())
			.andExpect(view().name(CONFIRM_VIEW))
			.andExpect(model().hasNoErrors())
			.andReturn()
			.getResponse()
			.getCookies()
			;
		// @formatter:on
	}

	/**
	 * Read the active user's account back from the account page.
	 *
	 * @return the account information, or {@code null} if the user has not
	 *         registered for an account
	 */
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

	@Test
	@WithMockSecurityUser
	public void registerForm_defaultsFromUser() throws Exception {
		// WHEN
		// @formatter:off
		var result = mvc.perform(get(REGISTER_URL))
			.andExpect(status().isOk())
			.andExpect(view().name(REGISTER_VIEW))
			.andExpect(model().attributeExists(ACCOUNT_INPUT))
			.andReturn()
			;
		// @formatter:on

		// THEN
		final var input = (SnAccountCreationInput) result.getModelAndView().getModel()
				.get(ACCOUNT_INPUT);

		// @formatter:off
		and.then(input.getAddress())
			.as("Address defaults populated from the active user")
			.isNotNull()
			.as("Email defaults to the user login email")
			.returns(DEFAULT_USERNAME, from(SnAddressInput::getEmail))
			.as("Country defaults to the user location country")
			.returns(TEST_LOC_COUNTRY, from(SnAddressInput::getCountry))
			.as("Time zone defaults to the user location time zone")
			.returns(TEST_TZ, from(SnAddressInput::getTimeZoneId))
			;
		and.then(input.getAccount())
			.as("Locale defaults to the user language and location country")
			.returns("en-NZ", from(SnAccountInput::getLocale))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void submit_review() throws Exception {
		// WHEN
		review(validForm(post(REGISTER_URL)));

		// THEN
		// @formatter:off
		and.then(account())
			.as("No account created by the review step")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void submit_invalid() throws Exception {
		// WHEN
		// a blank country and no street lines fail validation
		// @formatter:off
		mvc.perform(post(REGISTER_URL)
				.param("address.name", "Tester Dude")
				.param("address.email", "billing@localhost")
				.param("address.country", "")
				.param("address.timeZoneId", "Pacific/Auckland")
				.param("account.currency", "NZD")
				.param("account.locale", "en-NZ")
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(view().name(REGISTER_VIEW))
			.andExpect(model().attributeHasFieldErrors(ACCOUNT_INPUT, "address.country",
					"address.street"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(account())
			.as("No account created when the form is invalid")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register() throws Exception {
		// GIVEN
		final Cookie[] session = review(
				validForm(post(REGISTER_URL)).param("requestedEntitlements", OCPP.name())
						.param("requestedEntitlements", DATUM_EXPORT.name()));

		// WHEN
		// @formatter:off
		mvc.perform(post(CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register-complete"))
			.andExpect(model().hasNoErrors())
			;
		// @formatter:on

		// THEN
		final SnAccountInfo info = account();

		// @formatter:off
		and.then(info.systemKey())
			.as("User assigned to the SNF accounting system")
			.isEqualTo("snf")
			;
		and.then(info.account())
			.as("Account created from the submitted form")
			.returns("NZD", from(a -> a.getCurrencyCode()))
			.returns("en-NZ", from(a -> a.getLocale()))
			;
		and.then(info.account().getAddress())
			.as("Address created from the submitted form")
			.returns("Tester Dude", from(a -> a.getName()))
			.returns("billing@localhost", from(a -> a.getEmail()))
			.returns("NZ", from(a -> a.getCountry()))
			.returns("Pacific/Auckland", from(a -> a.getTimeZoneId()))
			.returns("Wellington", from(a -> a.getLocality()))
			.returns("Wellington", from(a -> a.getStateOrProvince()))
			.returns("Wellington", from(a -> a.getRegion()))
			.returns("6011", from(a -> a.getPostalCode()))
			;
		and.then(info.account().getAddress().getStreet())
			.as("Both street lines saved")
			.containsExactly("Level 1", "123 Main Street")
			;
		and.then(info.entitlements())
			.as("Requested entitlements granted")
			.containsExactlyInAnyOrder(OCPP, DATUM_EXPORT)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_noEntitlements() throws Exception {
		// GIVEN
		final Cookie[] session = review(validForm(post(REGISTER_URL)));

		// WHEN
		// @formatter:off
		mvc.perform(post(CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/register-complete"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(account().entitlements())
			.as("No entitlements granted when none are requested")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_alreadyRegistered() throws Exception {
		// GIVEN
		final Cookie[] session = review(validForm(post(REGISTER_URL)));
		mvc.perform(post(CONFIRM_URL).cookie(session).with(csrf()))
				.andExpect(view().name("sec/account/register-complete"));

		final var originalAddressId = account().account().getAddress().getId();

		// WHEN
		// a second registration must not create a second account
		final Cookie[] session2 = review(validForm(post(REGISTER_URL)));

		// @formatter:off
		mvc.perform(post(CONFIRM_URL).cookie(session2).with(csrf()))
			.andExpect(status().isOk())
			.andExpect(view().name(REGISTER_VIEW))
			.andExpect(model().attributeHasErrors(ACCOUNT_INPUT))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(account().account().getAddress().getId())
			.as("Account unchanged by the rejected registration")
			.isEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	public void registerForm_anonymous() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get(REGISTER_URL))
			.andExpect(status().is3xxRedirection())
			;
		// @formatter:on
	}

}
