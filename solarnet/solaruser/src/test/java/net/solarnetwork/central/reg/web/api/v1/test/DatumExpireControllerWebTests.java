/* ==================================================================
 * DatumExpireControllerWebTests.java - 2 Oct 2026 8:30:12 am
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

import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.central.reg.web.api.v1.DatumExpireController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.datum.expire.domain.ExpireUserDataConfiguration;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Web API level integration tests for saving expire policies with the
 * {@link DatumExpireController} class.
 *
 * <p>
 * These tests post the JSON the SolarUser expire policy form submits, which
 * does not include a user ID: the policy is saved for the acting user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DatumExpireControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The expire policy save URL. */
	private static final String DATA_CONFIGS_URL = "/api/v1/sec/user/expire/configs/data";

	/** The service identifier the SolarUser expire policy form submits. */
	private static final String SERVICE_ID = "net.solarnetwork.central.user.expire.standard.DefaultUserExpireDataFilterService";

	/** The ID the mock service assigns to a saved policy. */
	private static final Long SAVED_ID = 123L;

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UserExpireBiz userExpireBiz;

	private static Authentication user() {
		var user = new User(DEFAULT_USERNAME, "password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		var principal = new AuthenticatedUser(user, DEFAULT_USER_ID, DEFAULT_NAME, false);
		return new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
	}

	private MockHttpServletRequestBuilder saveDataConfigRequest(String json) {
		// @formatter:off
		return post(DATA_CONFIGS_URL)
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(authentication(user()))
				.with(csrf())
				;
		// @formatter:on
	}

	/**
	 * Mock saving a policy, capturing the policy and the ID it had when saved.
	 *
	 * <p>
	 * The controller assigns the saved ID to the policy to return it, so the ID
	 * the service was given has to be captured when the service is invoked.
	 * </p>
	 *
	 * @param config
	 *        the reference to capture the saved policy in
	 * @param configId
	 *        the reference to capture the ID of the saved policy in
	 */
	private void givenSaveConfiguration(AtomicReference<ExpireUserDataConfiguration> config,
			AtomicReference<Long> configId) {
		given(userExpireBiz.saveConfiguration(any())).willAnswer(invocation -> {
			ExpireUserDataConfiguration conf = invocation.getArgument(0);
			config.set(conf);
			configId.set(conf.getId());
			return SAVED_ID;
		});
	}

	@Test
	public void saveDataConfiguration_create() throws Exception {
		// GIVEN
		final var config = new AtomicReference<ExpireUserDataConfiguration>();
		final var configId = new AtomicReference<Long>();
		givenSaveConfiguration(config, configId);

		// WHEN
		// the form submits all values as strings, without any user ID
		// @formatter:off
		mvc.perform(saveDataConfigRequest("""
				{
					"name":"Test Policy",
					"serviceIdentifier":"%s",
					"expireDays":"730",
					"active":"true",
					"datumFilter":{
						"nodeIds":[1,2],
						"sourceIds":["/test/a","/test/b"],
						"aggregationKey":"d"
					}
				}
				""".formatted(SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(SAVED_ID))
			.andExpect(jsonPath("$.data.userId").value(DEFAULT_USER_ID))
			.andExpect(jsonPath("$.data.name").value("Test Policy"))
			.andExpect(jsonPath("$.data.expireDays").value(730))
			.andExpect(jsonPath("$.data.active").value(true))
			;

		// THEN
		and.then(configId.get())
			.as("A policy without an ID is saved with the unassigned ID, to create it")
			.isSameAs(EntityConstants.UNASSIGNED_LONG_ID)
			;

		and.then(config.get())
			.as("Policy saved for the acting user")
			.returns(DEFAULT_USER_ID, from(ExpireUserDataConfiguration::getUserId))
			.as("Name from input")
			.returns("Test Policy", from(ExpireUserDataConfiguration::getName))
			.as("Service identifier from input")
			.returns(SERVICE_ID, from(ExpireUserDataConfiguration::getServiceIdentifier))
			.as("Expire days from input")
			.returns(730, from(ExpireUserDataConfiguration::getExpireDays))
			.as("Active from input")
			.returns(true, from(ExpireUserDataConfiguration::isActive))
			.as("Creation date assigned")
			.doesNotReturn(null, from(ExpireUserDataConfiguration::getCreated))
			.extracting(ExpireUserDataConfiguration::getFilter)
			.as("Node IDs from input")
			.returns(new Long[] { 1L, 2L }, from(DatumFilterCommand::getNodeIds))
			.as("Source IDs from input")
			.returns(new String[] { "/test/a", "/test/b" }, from(DatumFilterCommand::getSourceIds))
			.as("Aggregation from input")
			.returns(Aggregation.Day, from(DatumFilterCommand::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void saveDataConfiguration_create_defaults() throws Exception {
		// GIVEN
		final var config = new AtomicReference<ExpireUserDataConfiguration>();
		final var configId = new AtomicReference<Long>();
		givenSaveConfiguration(config, configId);

		// WHEN
		// @formatter:off
		mvc.perform(saveDataConfigRequest("""
				{
					"name":"Test Policy",
					"serviceIdentifier":"%s"
				}
				""".formatted(SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			;

		// THEN
		and.then(config.get())
			.as("Policy saved for the acting user")
			.returns(DEFAULT_USER_ID, from(ExpireUserDataConfiguration::getUserId))
			.as("Default expire days used when not provided")
			.returns(ExpireUserDataConfiguration.DEFAULT_EXPIRE_DAYS,
					from(ExpireUserDataConfiguration::getExpireDays))
			.as("Policy not active when not provided")
			.returns(false, from(ExpireUserDataConfiguration::isActive))
			.as("No filter when not provided")
			.returns(null, from(ExpireUserDataConfiguration::getFilter))
			;
		// @formatter:on
	}

	@Test
	public void saveDataConfiguration_update() throws Exception {
		// GIVEN
		final var config = new AtomicReference<ExpireUserDataConfiguration>();
		final var configId = new AtomicReference<Long>();
		givenSaveConfiguration(config, configId);

		// WHEN
		// @formatter:off
		mvc.perform(saveDataConfigRequest("""
				{
					"id":"%d",
					"name":"Test Policy",
					"serviceIdentifier":"%s",
					"expireDays":"365",
					"active":"false"
				}
				""".formatted(SAVED_ID, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(SAVED_ID))
			;

		// THEN
		and.then(configId.get())
			.as("A policy with an ID is saved with that ID, to update it")
			.isEqualTo(SAVED_ID)
			;

		and.then(config.get())
			.as("Policy saved for the acting user")
			.returns(DEFAULT_USER_ID, from(ExpireUserDataConfiguration::getUserId))
			.as("Expire days from input")
			.returns(365, from(ExpireUserDataConfiguration::getExpireDays))
			.as("Active from input")
			.returns(false, from(ExpireUserDataConfiguration::isActive))
			;
		// @formatter:on
	}

	@Test
	public void saveDataConfiguration_userIdIgnored() throws Exception {
		// GIVEN
		final var config = new AtomicReference<ExpireUserDataConfiguration>();
		final var configId = new AtomicReference<Long>();
		givenSaveConfiguration(config, configId);

		// WHEN
		// @formatter:off
		mvc.perform(saveDataConfigRequest("""
				{
					"userId":"%d",
					"name":"Test Policy",
					"serviceIdentifier":"%s"
				}
				""".formatted(DEFAULT_USER_ID + 1, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(DEFAULT_USER_ID))
			;

		// THEN
		and.then(config.get())
			.as("Policy saved for the acting user, not the user given in the input")
			.returns(DEFAULT_USER_ID, from(ExpireUserDataConfiguration::getUserId))
			;
		// @formatter:on
	}

	@Test
	public void saveDataConfiguration_nameRequired() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(saveDataConfigRequest("""
				{
					"serviceIdentifier":"%s",
					"expireDays":"730"
				}
				""".formatted(SERVICE_ID)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		then(userExpireBiz).shouldHaveNoInteractions();
	}

}
