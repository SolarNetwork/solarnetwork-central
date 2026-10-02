/* ==================================================================
 * UserInstructionInputControllerWebTests.java - 2 Oct 2026 9:21:40 am
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

import static net.solarnetwork.central.reg.config.WebSecurityConfig.INSTRUCTION_INPUT_AUTHORITY;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.reg.web.api.v1.UserInstructionInputController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;
import net.solarnetwork.central.user.inin.domain.EndpointConfigurationInput;

/**
 * Web API level integration tests for saving endpoints with the
 * {@link UserInstructionInputController} class.
 *
 * <p>
 * These tests post the JSON the SolarUser instruction input endpoint form
 * submits, where every value is a string and an empty field is an empty
 * string.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles(SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT)
public class UserInstructionInputControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The endpoint save URL. */
	private static final String ENDPOINTS_URL = "/api/v1/sec/user/inin/endpoints";

	/** The ID the mock service assigns to a saved endpoint. */
	private static final UUID ENDPOINT_ID = UUID.fromString("0198a3a6-34c7-7cbb-9a0f-3c9f3f0a8e11");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UserInstructionInputBiz userInstructionInputBiz;

	/**
	 * Get an authentication for a user with the {@code ROLE_INSTRUCTION_INPUT}
	 * authority required by the instruction input endpoints.
	 *
	 * @return the authentication
	 */
	private static Authentication instructionInputUser() {
		var user = new User(DEFAULT_USERNAME, "password",
				AuthorityUtils.createAuthorityList("ROLE_USER", INSTRUCTION_INPUT_AUTHORITY));
		var principal = new AuthenticatedUser(user, DEFAULT_USER_ID, DEFAULT_NAME, false);
		return new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
	}

	private static MockHttpServletRequestBuilder jsonRequest(MockHttpServletRequestBuilder req,
			String json) {
		// @formatter:off
		return req
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(authentication(instructionInputUser()))
				.with(csrf())
				;
		// @formatter:on
	}

	/**
	 * Get an endpoint request body with a given maximum execution seconds
	 * property.
	 *
	 * @param maxExecutionSecondsProperty
	 *        the complete {@code maxExecutionSeconds} JSON property, including
	 *        a trailing comma, or an empty string to leave the property out
	 * @return the request body
	 */
	private static String endpointJson(String maxExecutionSecondsProperty) {
		// @formatter:off
		return """
				{
					"name":"Test Endpoint",
					"requestTransformId":"1",
					"responseTransformId":"2",
					"nodeIdsValue":"3",
					%s
					"userMetadataPath":"",
					"requestContentType":"",
					"responseContentType":"",
					"enabled":"true"
				}
				""".formatted(maxExecutionSecondsProperty);
		// @formatter:on
	}

	/**
	 * Mock saving an endpoint, capturing the input given to the service.
	 *
	 * @param input
	 *        the reference to capture the input in
	 */
	private void givenSaveConfiguration(AtomicReference<EndpointConfigurationInput> input) {
		given(userInstructionInputBiz.saveConfiguration(any(UserUuidPK.class),
				any(EndpointConfigurationInput.class))).willAnswer(invocation -> {
					EndpointConfigurationInput in = invocation.getArgument(1);
					input.set(in);
					return in.toEntity(new UserUuidPK(DEFAULT_USER_ID, ENDPOINT_ID), Instant.now());
				});
	}

	@Test
	public void createEndpointConfiguration_maxExecutionSeconds_empty() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// the form submits an empty string when the timeout is left empty
		// @formatter:off
		mvc.perform(jsonRequest(post(ENDPOINTS_URL), endpointJson("\"maxExecutionSeconds\":\"\",")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.endpointId").value(ENDPOINT_ID.toString()))
			.andExpect(jsonPath("$.data.maxExecutionSeconds")
					.value(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS))
			;

		// THEN
		and.then(input.get())
			.as("Default maximum execution seconds used when empty")
			.returns(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS,
					from(EndpointConfigurationInput::getMaxExecutionSeconds))
			.as("Name from input")
			.returns("Test Endpoint", from(EndpointConfigurationInput::getName))
			.as("Request transform ID from input")
			.returns(1L, from(EndpointConfigurationInput::getRequestTransformId))
			.as("Response transform ID from input")
			.returns(2L, from(EndpointConfigurationInput::getResponseTransformId))
			.as("Node IDs from input")
			.returns(Set.of(3L), from(EndpointConfigurationInput::getNodeIds))
			.as("Enabled from input")
			.returns(true, from(EndpointConfigurationInput::isEnabled))
			;
		// @formatter:on
	}

	@Test
	public void createEndpointConfiguration_maxExecutionSeconds_null() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// @formatter:off
		mvc.perform(jsonRequest(post(ENDPOINTS_URL), endpointJson("\"maxExecutionSeconds\":null,")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			;

		// THEN
		and.then(input.get())
			.as("Default maximum execution seconds used when null")
			.returns(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS,
					from(EndpointConfigurationInput::getMaxExecutionSeconds))
			;
		// @formatter:on
	}

	@Test
	public void createEndpointConfiguration_maxExecutionSeconds_missing() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// @formatter:off
		mvc.perform(jsonRequest(post(ENDPOINTS_URL), endpointJson("")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			;

		// THEN
		and.then(input.get())
			.as("Default maximum execution seconds used when not provided")
			.returns(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS,
					from(EndpointConfigurationInput::getMaxExecutionSeconds))
			;
		// @formatter:on
	}

	@Test
	public void createEndpointConfiguration_maxExecutionSeconds_value() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// @formatter:off
		mvc.perform(jsonRequest(post(ENDPOINTS_URL), endpointJson("\"maxExecutionSeconds\":\"30\",")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.maxExecutionSeconds").value(30))
			;

		// THEN
		and.then(input.get())
			.as("Maximum execution seconds from input")
			.returns(30, from(EndpointConfigurationInput::getMaxExecutionSeconds))
			;
		// @formatter:on
	}

	@Test
	public void createEndpointConfiguration_maxExecutionSeconds_lessThanOne() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// @formatter:off
		mvc.perform(jsonRequest(post(ENDPOINTS_URL), endpointJson("\"maxExecutionSeconds\":\"0\",")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			;

		// THEN
		and.then(input.get())
			.as("Maximum execution seconds less than 1 saved as 1")
			.returns(1, from(EndpointConfigurationInput::getMaxExecutionSeconds))
			;
		// @formatter:on
	}

	@Test
	public void updateEndpointConfiguration_maxExecutionSeconds_empty() throws Exception {
		// GIVEN
		final var input = new AtomicReference<EndpointConfigurationInput>();
		givenSaveConfiguration(input);

		// WHEN
		// @formatter:off
		mvc.perform(jsonRequest(put(ENDPOINTS_URL + "/" + ENDPOINT_ID),
					endpointJson("\"maxExecutionSeconds\":\"\",")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.maxExecutionSeconds")
					.value(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS))
			;

		// THEN
		and.then(input.get())
			.as("Default maximum execution seconds used when empty")
			.returns(EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS,
					from(EndpointConfigurationInput::getMaxExecutionSeconds))
			;
		// @formatter:on
	}

}
