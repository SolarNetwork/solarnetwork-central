/* ==================================================================
 * UserCloudIntegrationsOAuthControllerTests.java - 11/03/2025 6:46:39 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.reg.config.WebSecurityConfig;
import net.solarnetwork.central.reg.web.UserCloudIntegrationsOAuthController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;

/**
 * Test cases for the {@link UserCloudIntegrationsOAuthController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class UserCloudIntegrationsOAuthControllerTests {

	@Mock
	private UserCloudIntegrationsBiz userCloudIntegrationsBiz;

	@Mock
	private CloudIntegrationService service1;

	@Captor
	private ArgumentCaptor<Map<String, ?>> paramsCaptor;

	private UserCloudIntegrationsOAuthController controller;

	@BeforeEach
	public void setup() {
		controller = new UserCloudIntegrationsOAuthController(userCloudIntegrationsBiz);
	}

	@AfterEach
	public void teardown() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private void becomeUser(Long userId) {
		User userDetails = new User("tester@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar",
				Role.ROLE_USER.toString(), WebSecurityConfig.CLOUD_INTEGRATIONS_AUTHORITY);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void handleOAuthAuthCode() {
		// GIVEN
		final Long userId = randomLong();
		final Long integrationId = randomLong();
		final String serviceId = randomString();
		final String code = randomString();
		final String state = randomString();
		final Locale locale = Locale.getDefault();

		final UserLongCompositePK integrationPk = new UserLongCompositePK(userId, integrationId);
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(
				integrationPk, now());
		integration.setServiceIdentifier(serviceId);

		// look up integration
		given(userCloudIntegrationsBiz.configurationForId(integrationPk,
				CloudIntegrationConfiguration.class)).willReturn(integration);

		// look up referenced service
		given(userCloudIntegrationsBiz.integrationService(serviceId)).willReturn(service1);

		// fetch token
		Map<String, Object> tokenDetails = Map.of("access_token", randomString(), "refresh_token",
				randomString());
		given(service1.fetchAccessToken(same(integration), any(), same(locale)))
				.willReturn((Map) tokenDetails);

		// WHEN
		becomeUser(userId);

		var request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ModelAndView result = controller.handleOAuthAuthCode(integrationId, code, state, null, locale);

		// THEN
		then(service1).should().fetchAccessToken(same(integration), paramsCaptor.capture(),
				same(locale));

		// @formatter:off
		and.then(paramsCaptor.getValue())
			.as("Params include code, state, and redirect URI")
			.hasSize(3)
			.asInstanceOf(map(String.class, Object.class))
			.as("Code provided as service param")
			.containsEntry(CloudIntegrationService.AUTHORIZATION_CODE_PARAM, code)
			.as("State provided as service param")
			.containsEntry(CloudIntegrationService.AUTHORIZATION_STATE_PARAM, state)
			.as("Redirect URI calculated as self and provided as service param")
			.containsEntry(CloudIntegrationService.REDIRECT_URI_PARAM,
				fromMethodCall(on(UserCloudIntegrationsOAuthController.class)
						.handleOAuthAuthCode(0L, "", "", null, Locale.getDefault()))
						.replaceQueryParams(null)
						.buildAndExpand(integrationId).toUri())
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Expected view returned")
			.returns("redirect:sec/c2c/oauth", from(ModelAndView::getViewName))
			;
		// @formatter:on
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void handleOAuthAuthCode_explicitRedirectUri() {
		// GIVEN
		final Long userId = randomLong();
		final Long integrationId = randomLong();
		final String serviceId = randomString();
		final String code = randomString();
		final String state = randomString();
		final String redirectUri = "http://localhost/" + randomString();
		final Locale locale = Locale.getDefault();

		final UserLongCompositePK integrationPk = new UserLongCompositePK(userId, integrationId);
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(
				integrationPk, now());
		integration.setServiceIdentifier(serviceId);

		// look up integration
		given(userCloudIntegrationsBiz.configurationForId(integrationPk,
				CloudIntegrationConfiguration.class)).willReturn(integration);

		// look up referenced service
		given(userCloudIntegrationsBiz.integrationService(serviceId)).willReturn(service1);

		// fetch token
		Map<String, Object> tokenDetails = Map.of("access_token", randomString(), "refresh_token",
				randomString());
		given(service1.fetchAccessToken(same(integration), any(), same(locale)))
				.willReturn((Map) tokenDetails);

		// WHEN
		becomeUser(userId);

		ModelAndView result = controller.handleOAuthAuthCode(integrationId, code, state, redirectUri,
				locale);

		// THEN
		then(service1).should().fetchAccessToken(same(integration), paramsCaptor.capture(),
				same(locale));

		// @formatter:off
		and.then(paramsCaptor.getValue())
			.as("Params include code, state, and redirect URI")
			.hasSize(3)
			.asInstanceOf(map(String.class, Object.class))
			.as("Code provided as service param")
			.containsEntry(CloudIntegrationService.AUTHORIZATION_CODE_PARAM, code)
			.as("State provided as service param")
			.containsEntry(CloudIntegrationService.AUTHORIZATION_STATE_PARAM, state)
			.as("Redirect URI calculated as self and provided as service param")
			.containsEntry(CloudIntegrationService.REDIRECT_URI_PARAM, URI.create(redirectUri))
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Expected view returned")
			.returns("redirect:sec/c2c/oauth", from(ModelAndView::getViewName))
			;
		// @formatter:on
	}

}
