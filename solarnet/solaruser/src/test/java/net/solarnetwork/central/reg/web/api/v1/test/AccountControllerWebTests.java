/* ==================================================================
 * AccountControllerWebTests.java - 28 Sept 2026 6:31:07 pm
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
import static org.assertj.core.api.BDDAssertions.and;
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
import com.jayway.jsonpath.JsonPath;
import net.solarnetwork.central.reg.web.api.v1.AccountController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.security.WithMockSecurityUser;
import net.solarnetwork.central.user.billing.snf.config.SolarNetUserBillingConfiguration;

/**
 * Web API level integration tests for the {@link AccountController} class.
 *
 * <p>
 * The {@code snf-billing} profile is required, as that is what contributes the
 * {@code BillingSystemRegistrar} holding the account.
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
public class AccountControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The account registration URL. */
	private static final String REGISTER_URL = "/api/v1/sec/user/account/register";

	/** The account view URL. */
	private static final String VIEW_URL = "/api/v1/sec/user/account/view";

	/** The account update URL. */
	private static final String UPDATE_URL = "/api/v1/sec/user/account";

	/** The JSON path of the account's address ID. */
	private static final String ADDRESS_ID_PATH = "$.data.account.address.addressId";

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		setupTestLocation();
		setupTestUser(DEFAULT_USER_ID, DEFAULT_USERNAME, TEST_LOC_ID);
	}

	/**
	 * Get an account request body.
	 *
	 * @param streetJson
	 *        the street lines, as a JSON array
	 * @param postalCode
	 *        the postal code
	 * @return the request body
	 */
	private static String accountJson(String streetJson, String postalCode) {
		// @formatter:off
		return """
				{
					"address":{
						"name":"Tester Dude",
						"email":"billing@localhost",
						"street":%s,
						"locality":"Wellington",
						"stateOrProvince":"Wellington",
						"region":"Wellington",
						"postalCode":"%s",
						"country":"NZ",
						"timeZoneId":"Pacific/Auckland"
					},
					"account":{
						"currency":"NZD",
						"locale":"en-NZ"
					},
					"requestedEntitlements":["OCPP"]
				}
				""".formatted(streetJson, postalCode);
		// @formatter:on
	}

	/**
	 * Get the account request body the tests register with.
	 *
	 * @return the request body
	 */
	private static String accountJson() {
		return accountJson("[\"Level 1\",\"123 Main Street\"]", "6011");
	}

	/**
	 * Get a JSON request for a URL and body.
	 *
	 * @param url
	 *        the URL
	 * @param json
	 *        the request body
	 * @return the request
	 */
	private static MockHttpServletRequestBuilder jsonPost(String url, String json) {
		// @formatter:off
		return post(url)
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
				;
		// @formatter:on
	}

	/**
	 * Register the account the tests operate on.
	 *
	 * @return the ID of the account's address
	 */
	private Long givenRegisteredAccount() throws Exception {
		// @formatter:off
		return addressId(mvc.perform(jsonPost(REGISTER_URL, accountJson()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString())
			;
		// @formatter:on
	}

	private static Long addressId(String json) {
		return ((Number) JsonPath.read(json, ADDRESS_ID_PATH)).longValue();
	}

	@Test
	@WithMockSecurityUser
	public void viewAccount() throws Exception {
		// GIVEN
		givenRegisteredAccount();

		// WHEN
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
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
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void viewAccount_notRegistered() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on
	}

	@Test
	public void viewAccount_anonymous() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized())
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void updateAccount_addressChanged() throws Exception {
		// GIVEN
		final Long originalAddressId = givenRegisteredAccount();

		// WHEN
		// @formatter:off
		final String json = mvc
			.perform(jsonPost(UPDATE_URL, accountJson("[\"Level 1\",\"123 Main Street\"]", "6012")))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{"success":true, "data":{
						"systemKey":"snf",
						"account":{"address":{"postalCode":"6012"}}
					}}
					""", JsonCompareMode.LENIENT))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(addressId(json))
			.as("Addresses are append-only, so a changed address is a new address the account now"
					+ " refers to, leaving old invoices rendering with the address they were issued to")
			.isNotEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void updateAccount_noChange() throws Exception {
		// GIVEN
		final Long originalAddressId = givenRegisteredAccount();

		// WHEN
		// @formatter:off
		final String json = mvc.perform(jsonPost(UPDATE_URL, accountJson()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		// @formatter:on

		// THEN
		// @formatter:off
		and.then(addressId(json))
			.as("Resubmitting unchanged details creates no new address")
			.isEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void updateAccount_invalid() throws Exception {
		// GIVEN
		final Long originalAddressId = givenRegisteredAccount();

		// WHEN
		// an empty street array has no non-blank line
		// @formatter:off
		mvc.perform(jsonPost(UPDATE_URL, accountJson("[]", "6011")))
			.andExpect(status().is4xxClientError())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		// @formatter:off
		final String json = mvc.perform(get(VIEW_URL).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{"success":true, "data":{"account":{"address":{
						"street":["Level 1","123 Main Street"]
					}}}}
					""", JsonCompareMode.LENIENT))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		and.then(addressId(json))
			.as("Account unchanged when the request body is invalid")
			.isEqualTo(originalAddressId)
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void updateAccount_notRegistered() throws Exception {
		// WHEN
		// there is no account to update
		// @formatter:off
		mvc.perform(jsonPost(UPDATE_URL, accountJson()))
			.andExpect(status().isForbidden())
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

}
