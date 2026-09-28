/* ==================================================================
 * AccountEditControllerWebTests.java - 28 Sept 2026 5:38:22 pm
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

import static net.solarnetwork.central.reg.web.AccountEditController.ACCOUNT_INPUT;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.DATUM_EXPORT;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OCPP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import jakarta.servlet.http.Cookie;
import net.solarnetwork.central.reg.web.AccountEditController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.account.domain.SnAccountCreationInput;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;

/**
 * Web integration tests for the {@link AccountEditController} class.
 *
 * <p>
 * The {@code snf-billing} profile is required, as that is what contributes the
 * {@code BillingSystemRegistrar} holding the account being edited.
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
public class AccountEditControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The account page URL, used to read the account state back. */
	private static final String ACCOUNT_URL = "/u/sec/account";

	/** The register form URL, used to establish the account under test. */
	private static final String REGISTER_URL = "/u/sec/account/register";

	/** The register confirm URL. */
	private static final String REGISTER_CONFIRM_URL = "/u/sec/account/register/confirm";

	/** The edit form URL. */
	private static final String EDIT_URL = "/u/sec/account/edit";

	/** The edit confirm URL. */
	private static final String EDIT_CONFIRM_URL = "/u/sec/account/edit/confirm";

	/** The edit form view. */
	private static final String EDIT_VIEW = "sec/account/edit";

	/** The edit review view. */
	private static final String CONFIRM_VIEW = "sec/account/edit-confirm";

	@Autowired
	private MockMvc mvc;

	private Object originalAddressId;

	@BeforeEach
	public void setup() throws Exception {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, TEST_LOC_ID);
	}

	/**
	 * Register the account the edit tests operate on, through the registration
	 * pages, so the starting state is exactly what registration produces.
	 */
	private void givenRegisteredAccount() throws Exception {
		final MultiValueMap<String, String> params = accountFormParams();

		// @formatter:off
		final Cookie[] session = mvc.perform(post(REGISTER_URL).params(params).with(csrf()))
			.andExpect(view().name("sec/account/register-confirm"))
			.andReturn()
			.getResponse()
			.getCookies()
			;
		mvc.perform(post(REGISTER_CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(view().name("sec/account/register-complete"))
			;
		// @formatter:on

		originalAddressId = addressId(account());
	}

	/**
	 * Get a complete, valid set of form parameters, matching the account
	 * established by {@link #givenRegisteredAccount()}.
	 *
	 * <p>
	 * Use {@link MultiValueMap#set(Object, Object)} to change a single field,
	 * so the test posts one value for it rather than two.
	 * </p>
	 *
	 * @return the parameters
	 */
	private static MultiValueMap<String, String> accountFormParams() {
		final var params = new LinkedMultiValueMap<String, String>(13);
		params.add("address.name", "Tester Dude");
		params.add("address.email", "billing@localhost");
		params.add("address.street1", "Level 1");
		params.add("address.street2", "123 Main Street");
		params.add("address.locality", "Wellington");
		params.add("address.stateOrProvince", "Wellington");
		params.add("address.region", "Wellington");
		params.add("address.postalCode", "6011");
		params.add("address.country", "NZ");
		params.add("address.timeZoneId", "Pacific/Auckland");
		params.add("account.currency", "NZD");
		params.add("account.locale", "en-NZ");
		params.add("requestedEntitlements", OCPP.name());
		return params;
	}

	/**
	 * Perform the review step, returning the session cookies that carry the
	 * form bean on to the confirm step.
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
	 * Get the ID of an account's address.
	 *
	 * <p>
	 * Addresses are persisted as an append-only log, so a changed address is a
	 * new address with a new ID, which is how these tests tell an address edit
	 * from an account-only edit.
	 * </p>
	 *
	 * @param info
	 *        the account information
	 * @return the address ID
	 */
	private static Object addressId(SnAccountInfo info) {
		return info.account().getAddress().getId();
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
	public void editForm_populatedFromAccount() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		// WHEN
		// @formatter:off
		var result = mvc.perform(get(EDIT_URL))
			.andExpect(status().isOk())
			.andExpect(view().name(EDIT_VIEW))
			.andExpect(model().attributeExists(ACCOUNT_INPUT))
			.andReturn()
			;
		// @formatter:on

		// THEN
		final var input = (SnAccountCreationInput) result.getModelAndView().getModel()
				.get(ACCOUNT_INPUT);

		// @formatter:off
		and.then(input.getAddress().getName())
			.as("Name populated from the persisted address")
			.isEqualTo("Tester Dude")
			;
		and.then(input.getAddress().getEmail())
			.as("Email populated from the persisted address")
			.isEqualTo("billing@localhost")
			;
		and.then(input.getAddress().getPostalCode())
			.as("Postal code populated from the persisted address")
			.isEqualTo("6011")
			;
		and.then(input.getAddress().getStreet1())
			.as("First street line populated from the persisted address")
			.isEqualTo("Level 1")
			;
		and.then(input.getAddress().getStreet2())
			.as("Second street line populated from the persisted address")
			.isEqualTo("123 Main Street")
			;
		and.then(input.getAccount().getCurrency().getCurrencyCode())
			.as("Currency populated from the persisted account")
			.isEqualTo("NZD")
			;
		and.then(input.getRequestedEntitlements())
			.as("Granted entitlements pre-selected")
			.containsExactly(OCPP)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void submit_invalid() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		// WHEN
		// @formatter:off
		mvc.perform(post(EDIT_URL)
				.param("address.name", "")
				.param("address.email", "not-an-email")
				.param("address.street1", "Level 1")
				.param("address.country", "NZ")
				.param("address.timeZoneId", "Pacific/Auckland")
				.param("account.currency", "NZD")
				.param("account.locale", "en-NZ")
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(view().name(EDIT_VIEW))
			.andExpect(model().attributeHasFieldErrors(ACCOUNT_INPUT, "address.name", "address.email"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(addressId(account()))
			.as("Account address unchanged when the form is invalid")
			.isEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void edit_addressChanged() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		final MultiValueMap<String, String> params = accountFormParams();
		params.set("address.postalCode", "6012");

		final Cookie[] session = review(post(EDIT_URL).params(params).with(csrf()));

		// WHEN
		// @formatter:off
		mvc.perform(post(EDIT_CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/u/sec/account"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		final SnAccountInfo info = account();

		and.then(addressId(info))
			.as("A changed address is saved as a new address the account now refers to, leaving old"
					+ " invoices rendering with the address they were issued to")
			.isNotEqualTo(originalAddressId)
			;
		and.then(info.account().getAddress().getPostalCode())
			.as("Account address is the updated one")
			.isEqualTo("6012")
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void edit_accountChangedOnly() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		final MultiValueMap<String, String> params = accountFormParams();
		params.set("account.currency", "USD");

		final Cookie[] session = review(post(EDIT_URL).params(params).with(csrf()));

		// WHEN
		// @formatter:off
		mvc.perform(post(EDIT_CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/u/sec/account"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		final SnAccountInfo info = account();

		and.then(info.account().getCurrencyCode())
			.as("Currency updated")
			.isEqualTo("USD")
			;
		and.then(addressId(info))
			.as("No new address when only the account details change")
			.isEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void edit_noChange() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		final Cookie[] session = review(post(EDIT_URL).params(accountFormParams()).with(csrf()));

		// WHEN
		// @formatter:off
		mvc.perform(post(EDIT_CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/u/sec/account"))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		final SnAccountInfo info = account();

		and.then(addressId(info))
			.as("Resubmitting unchanged details creates no new address")
			.isEqualTo(originalAddressId)
			;
		and.then(info.account().getCurrencyCode())
			.as("Account unchanged")
			.isEqualTo("NZD")
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void edit_entitlementsChanged() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		// swap the OCPP entitlement for datum export
		final MultiValueMap<String, String> params = accountFormParams();
		params.set("requestedEntitlements", DATUM_EXPORT.name());

		final Cookie[] session = review(post(EDIT_URL).params(params).with(csrf()));

		// WHEN
		// @formatter:off
		mvc.perform(post(EDIT_CONFIRM_URL).cookie(session).with(csrf()))
			.andExpect(status().is3xxRedirection())
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(account().entitlements())
			.as("Newly selected entitlement granted and the de-selected one revoked")
			.containsExactly(DATUM_EXPORT)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void editForm_notRegistered() throws Exception {
		// WHEN
		// no account exists, so the edit form has nothing to populate itself from
		// @formatter:off
		mvc.perform(get(EDIT_URL))
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@Test
	public void editForm_anonymous() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get(EDIT_URL))
			.andExpect(status().is3xxRedirection())
			;
		// @formatter:on
	}

}
