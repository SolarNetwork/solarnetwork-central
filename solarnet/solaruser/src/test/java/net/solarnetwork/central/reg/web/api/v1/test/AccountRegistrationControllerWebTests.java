/* ==================================================================
 * AccountRegistrationControllerWebTests.java - 28 Sept 2026 6:58:31 pm
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

import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.reg.web.api.v1.AccountRegistrationController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;

/**
 * Web API level integration tests for the {@link AccountRegistrationController}
 * class.
 *
 * <p>
 * The {@code snf-billing} profile is required, as that is what contributes the
 * {@code BillingSystemRegistrar} the account service registers accounts with.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetUserBillingConfiguration.SNF_BILLING)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AccountRegistrationControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The account registration URL. */
	private static final String REGISTER_URL = "/api/v1/sec/user/account/register";

	/** The account view URL, used to read back the registered account. */
	private static final String VIEW_URL = "/api/v1/sec/user/account/view";

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, TEST_LOC_ID);
	}

	/**
	 * Get a registration request for a JSON body.
	 *
	 * @param json
	 *        the request body
	 * @return the request
	 */
	private static MockHttpServletRequestBuilder registerRequest(String json) {
		// @formatter:off
		return post(REGISTER_URL)
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
				;
		// @formatter:on
	}

	/** A complete, valid account creation request body. */
	private static String validJson() {
		// @formatter:off
		return """
				{
					"address":{
						"name":"Tester Dude",
						"email":"billing@localhost",
						"street":["Level 1","123 Main Street"],
						"locality":"Wellington",
						"stateOrProvince":"Wellington",
						"region":"Wellington",
						"postalCode":"6011",
						"country":"NZ",
						"timeZoneId":"Pacific/Auckland"
					},
					"account":{
						"currency":"NZD",
						"locale":"en-NZ"
					},
					"requestedEntitlements":["OCPP"]
				}
				""";
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(registerRequest(validJson()))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{
						"systemKey":"snf",
						"entitlements":["OCPP"],
						"account":{
							"userId":1,
							"currencyCode":"NZD",
							"locale":"en-NZ",
							"address":{
								"userId":1,
								"name":"Tester Dude",
								"email":"billing@localhost",
								"street":["Level 1","123 Main Street"],
								"locality":"Wellington",
								"stateOrProvince":"Wellington",
								"region":"Wellington",
								"postalCode":"6011",
								"country":"NZ",
								"timeZoneId":"Pacific/Auckland"
							}
						}
					}}
					""", JsonCompareMode.LENIENT))
			.andExpect(jsonPath("$.data.account.accountId").isNumber())
			.andExpect(jsonPath("$.data.account.address.addressId").isNumber())
			;
		// @formatter:on

		// THEN
		// the new account is readable straight away
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{"success":true, "data":{
						"systemKey":"snf",
						"entitlements":["OCPP"],
						"account":{"currencyCode":"NZD","address":{"name":"Tester Dude"}}
					}}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_alreadyRegistered() throws Exception {
		// GIVEN
		final String registered = mvc.perform(registerRequest(validJson())).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		// WHEN
		// @formatter:off
		mvc.perform(registerRequest(validJson()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		// the account is exactly what the first registration returned, so the
		// rejected attempt created nothing and changed nothing
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().json(registered, JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_invalid() throws Exception {
		// WHEN
		// an invalid country code, time zone and language tag
		// @formatter:off
		mvc.perform(registerRequest("""
				{
					"address":{
						"name":"Tester Dude",
						"email":"billing@localhost",
						"street":["123 Main Street"],
						"country":"XX",
						"timeZoneId":"Nowhere/Nothing"
					},
					"account":{
						"currency":"NZD",
						"locale":"!not a language tag!"
					}
				}
				"""))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		// no account was created, so the user is still unregistered
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_missingAccount() throws Exception {
		// WHEN
		// the account details are required
		// @formatter:off
		mvc.perform(registerRequest("""
				{
					"address":{
						"name":"Tester Dude",
						"email":"billing@localhost",
						"street":["123 Main Street"],
						"country":"NZ",
						"timeZoneId":"Pacific/Auckland"
					}
				}
				"""))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void register_blankStreet() throws Exception {
		// WHEN
		// a street array with no non-blank line
		// @formatter:off
		mvc.perform(registerRequest(validJson().replace("""
				["Level 1","123 Main Street"]""", """
				["",""]""")))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@Test
	public void register_anonymous() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(registerRequest(validJson()))
			.andExpect(status().isUnauthorized())
			;
		// @formatter:on
	}

}
