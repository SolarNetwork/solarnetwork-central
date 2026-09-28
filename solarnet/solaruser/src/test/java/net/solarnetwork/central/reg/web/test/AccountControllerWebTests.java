/* ==================================================================
 * AccountControllerWebTests.java - 28 Sept 2026 6:12:44 pm
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

import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static net.solarnetwork.central.user.account.domain.SnFeatureEntitlement.OCPP;
import static org.assertj.core.api.BDDAssertions.and;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import jakarta.servlet.http.Cookie;
import net.solarnetwork.central.reg.web.AccountController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.account.domain.SnAccountInfo;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;

/**
 * Web integration tests for the {@link AccountController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetUserBillingConfiguration.SNF_BILLING)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The account page URL. */
	private static final String ACCOUNT_URL = "/u/sec/account";

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, TEST_LOC_ID);
	}

	/**
	 * Register an account through the registration pages.
	 */
	private void givenRegisteredAccount() throws Exception {
		final var params = new LinkedMultiValueMap<String, String>(13);
		params.add("address.name", "Tester Dude");
		params.add("address.email", "billing@localhost");
		params.add("address.street1", "123 Main Street");
		params.add("address.locality", "Wellington");
		params.add("address.postalCode", "6011");
		params.add("address.country", "NZ");
		params.add("address.timeZoneId", "Pacific/Auckland");
		params.add("account.currency", "NZD");
		params.add("account.locale", "en-NZ");
		params.add("requestedEntitlements", OCPP.name());

		register(params);
	}

	private void register(MultiValueMap<String, String> params) throws Exception {
		// @formatter:off
		final Cookie[] session = mvc
			.perform(post("/u/sec/account/register").params(params).with(csrf()))
			.andExpect(view().name("sec/account/register-confirm"))
			.andReturn()
			.getResponse()
			.getCookies()
			;
		mvc.perform(post("/u/sec/account/register/confirm").cookie(session).with(csrf()))
			.andExpect(view().name("sec/account/register-complete"))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void accountPage_registered() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		// WHEN
		// @formatter:off
		var result = mvc.perform(get(ACCOUNT_URL))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/account"))
			.andExpect(model().attributeExists("account"))
			.andReturn()
			;
		// @formatter:on

		// THEN
		final var info = (SnAccountInfo) result.getModelAndView().getModel().get("account");

		// @formatter:off
		and.then(info)
			.as("Account info supplied to the view")
			.isNotNull()
			;
		and.then(info.systemKey())
			.as("SNF accounting system key reported")
			.isEqualTo("snf")
			;
		and.then(info.account().getCurrencyCode())
			.as("Currency from the persisted account")
			.isEqualTo("NZD")
			;
		and.then(info.account().getAddress().getName())
			.as("Address from the persisted account")
			.isEqualTo("Tester Dude")
			;
		and.then(info.entitlements())
			.as("Granted entitlements reported")
			.containsExactly(OCPP)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void accountPage_notRegistered() throws Exception {
		// WHEN
		// the page doubles as the sign-up prompt, so it must render without an account
		// @formatter:off
		mvc.perform(get(ACCOUNT_URL))
			.andExpect(status().isOk())
			.andExpect(view().name("sec/account/account"))
			.andExpect(model().attribute("account", (Object) null))
			;
		// @formatter:on
	}

	@Test
	public void accountPage_anonymous() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get(ACCOUNT_URL))
			.andExpect(status().is3xxRedirection())
			;
		// @formatter:on
	}

}
