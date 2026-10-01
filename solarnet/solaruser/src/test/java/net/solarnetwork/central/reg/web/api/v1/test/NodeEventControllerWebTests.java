/* ==================================================================
 * NodeEventControllerWebTests.java - 2 Oct 2026 9:14:25 am
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

import static net.solarnetwork.central.reg.config.WebSecurityConfig.EVENT_AUTHORITY;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.Map;
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
import net.solarnetwork.central.reg.web.api.v1.NodeEventController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.datum.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.datum.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Web API level integration tests for saving node event hooks with the
 * {@link NodeEventController} class.
 *
 * <p>
 * These tests post the JSON the SolarUser node event hook form submits, which
 * does not include a user ID: the hook is saved for the acting user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NodeEventControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	/** The node event hook save URL. */
	private static final String HOOKS_URL = "/api/v1/sec/user/event/node/hooks";

	/** An event topic. */
	private static final String TOPIC = "datum/agg/update";

	/** A hook service identifier. */
	private static final String SERVICE_ID = "test.hook.service";

	/** The ID the mock service assigns to a saved hook. */
	private static final Long SAVED_ID = 123L;

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UserEventHookBiz userEventHookBiz;

	/**
	 * Get an authentication for a user with the {@code ROLE_EVENT} authority
	 * required by the node event endpoints.
	 *
	 * @return the authentication
	 */
	private static Authentication eventUser() {
		var user = new User(DEFAULT_USERNAME, "password",
				AuthorityUtils.createAuthorityList("ROLE_USER", EVENT_AUTHORITY));
		var principal = new AuthenticatedUser(user, DEFAULT_USER_ID, DEFAULT_NAME, false);
		return new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
	}

	private MockHttpServletRequestBuilder saveHookRequest(String json) {
		// @formatter:off
		return post(HOOKS_URL)
				.content(json)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.with(authentication(eventUser()))
				.with(csrf())
				;
		// @formatter:on
	}

	/**
	 * Mock saving a hook, capturing the hook given to the service.
	 *
	 * <p>
	 * The controller returns the hook as it is then loaded from the service, so
	 * the saved hook is returned from that as a copy with the saved ID.
	 * </p>
	 *
	 * @param config
	 *        the reference to capture the saved hook in
	 */
	private void givenSaveConfiguration(AtomicReference<UserNodeEventHookConfiguration> config) {
		given(userEventHookBiz.saveConfiguration(any())).willAnswer(invocation -> {
			UserNodeEventHookConfiguration conf = invocation.getArgument(0);
			config.set(conf);
			return new UserLongPK(conf.getUserId(), SAVED_ID);
		});
		given(userEventHookBiz.configurationForUser(any(), eq(UserNodeEventHookConfiguration.class),
				eq(SAVED_ID))).willAnswer(invocation -> {
					UserNodeEventHookConfiguration conf = config.get();
					var saved = new UserNodeEventHookConfiguration(SAVED_ID, invocation.getArgument(0),
							conf.getCreated(), conf.getName(), conf.getServiceIdentifier());
					saved.setTopic(conf.getTopic());
					saved.setNodeIds(conf.getNodeIds());
					saved.setSourceIds(conf.getSourceIds());
					saved.setServiceProps(conf.getServiceProps());
					return saved;
				});
		given(userEventHookBiz.availableNodeEventHookServices()).willReturn(List.of());
	}

	@Test
	public void saveNodeHookConfiguration_create() throws Exception {
		// GIVEN
		final var config = new AtomicReference<UserNodeEventHookConfiguration>();
		givenSaveConfiguration(config);

		// WHEN
		// @formatter:off
		mvc.perform(saveHookRequest("""
				{
					"name":"Test Hook",
					"topic":"%s",
					"serviceIdentifier":"%s",
					"nodeIds":[1,2],
					"sourceIds":["/test/a","/test/**"],
					"serviceProperties":{"url":"https://example.com/hook"}
				}
				""".formatted(TOPIC, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(SAVED_ID))
			.andExpect(jsonPath("$.data.userId").value(DEFAULT_USER_ID))
			.andExpect(jsonPath("$.data.name").value("Test Hook"))
			.andExpect(jsonPath("$.data.topic").value(TOPIC))
			.andExpect(jsonPath("$.data.nodeIds.length()").value(2))
			.andExpect(jsonPath("$.data.sourceIds[1]").value("/test/**"))
			;

		// THEN
		and.then(config.get())
			.as("Hook saved for the acting user")
			.returns(DEFAULT_USER_ID, from(UserNodeEventHookConfiguration::getUserId))
			.as("A hook without an ID is saved without an ID, to create it")
			.returns(null, from(UserNodeEventHookConfiguration::getConfigurationId))
			.as("Name from input")
			.returns("Test Hook", from(UserNodeEventHookConfiguration::getName))
			.as("Topic from input")
			.returns(TOPIC, from(UserNodeEventHookConfiguration::getTopic))
			.as("Service identifier from input")
			.returns(SERVICE_ID, from(UserNodeEventHookConfiguration::getServiceIdentifier))
			.as("Node IDs from input")
			.returns(new Long[] { 1L, 2L }, from(UserNodeEventHookConfiguration::getNodeIds))
			.as("Source IDs from input")
			.returns(new String[] { "/test/a", "/test/**" },
					from(UserNodeEventHookConfiguration::getSourceIds))
			.as("Service properties from input")
			.returns(Map.<String, Object> of("url", "https://example.com/hook"),
					from(UserNodeEventHookConfiguration::getServiceProps))
			.as("Creation date assigned")
			.doesNotReturn(null, from(UserNodeEventHookConfiguration::getCreated))
			;
		// @formatter:on
	}

	@Test
	public void saveNodeHookConfiguration_create_anyNodeAndSource() throws Exception {
		// GIVEN
		final var config = new AtomicReference<UserNodeEventHookConfiguration>();
		givenSaveConfiguration(config);

		// WHEN
		// the form leaves out the node and source IDs when they are left empty
		// @formatter:off
		mvc.perform(saveHookRequest("""
				{
					"name":"Test Hook",
					"topic":"%s",
					"serviceIdentifier":"%s"
				}
				""".formatted(TOPIC, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			;

		// THEN
		and.then(config.get())
			.as("Hook saved for the acting user")
			.returns(DEFAULT_USER_ID, from(UserNodeEventHookConfiguration::getUserId))
			.as("No node IDs when not provided, to match any node")
			.returns(null, from(UserNodeEventHookConfiguration::getNodeIds))
			.as("No source IDs when not provided, to match any source")
			.returns(null, from(UserNodeEventHookConfiguration::getSourceIds))
			.as("No service properties when not provided")
			.returns(null, from(UserNodeEventHookConfiguration::getServiceProps))
			;
		// @formatter:on
	}

	@Test
	public void saveNodeHookConfiguration_update() throws Exception {
		// GIVEN
		final var config = new AtomicReference<UserNodeEventHookConfiguration>();
		givenSaveConfiguration(config);

		// WHEN
		// the form submits the ID as a string
		// @formatter:off
		mvc.perform(saveHookRequest("""
				{
					"id":"%d",
					"name":"Test Hook",
					"topic":"%s",
					"serviceIdentifier":"%s",
					"nodeIds":[3]
				}
				""".formatted(SAVED_ID, TOPIC, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(SAVED_ID))
			;

		// THEN
		and.then(config.get())
			.as("A hook with an ID is saved with that ID, to update it")
			.returns(SAVED_ID, from(UserNodeEventHookConfiguration::getConfigurationId))
			.as("Hook saved for the acting user")
			.returns(DEFAULT_USER_ID, from(UserNodeEventHookConfiguration::getUserId))
			.as("Node IDs from input")
			.returns(new Long[] { 3L }, from(UserNodeEventHookConfiguration::getNodeIds))
			;
		// @formatter:on
	}

	@Test
	public void saveNodeHookConfiguration_userIdIgnored() throws Exception {
		// GIVEN
		final var config = new AtomicReference<UserNodeEventHookConfiguration>();
		givenSaveConfiguration(config);

		// WHEN
		// @formatter:off
		mvc.perform(saveHookRequest("""
				{
					"userId":"%d",
					"name":"Test Hook",
					"topic":"%s",
					"serviceIdentifier":"%s"
				}
				""".formatted(DEFAULT_USER_ID + 1, TOPIC, SERVICE_ID)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.userId").value(DEFAULT_USER_ID))
			;

		// THEN
		and.then(config.get())
			.as("Hook saved for the acting user, not the user given in the input")
			.returns(DEFAULT_USER_ID, from(UserNodeEventHookConfiguration::getUserId))
			;
		// @formatter:on
	}

	@Test
	public void saveNodeHookConfiguration_nameRequired() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(saveHookRequest("""
				{
					"topic":"%s",
					"serviceIdentifier":"%s"
				}
				""".formatted(TOPIC, SERVICE_ID)))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.success").value(false))
			;
		// @formatter:on

		// THEN
		then(userEventHookBiz).shouldHaveNoInteractions();
	}

}
