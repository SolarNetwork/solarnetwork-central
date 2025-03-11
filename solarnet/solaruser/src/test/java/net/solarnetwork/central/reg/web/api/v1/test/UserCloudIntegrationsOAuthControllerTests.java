/* ==================================================================
 * UserCloudIntegrationsOAuthControllerTests.java - 11/03/2025 6:06:02 am
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING_SPECIFIER;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING_SPECIFIER;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.PASSWORD_SETTING_SPECIFIER;
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.USERNAME_SETTING_SPECIFIER;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import java.net.URI;
import java.util.List;
import java.util.Locale;
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
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.HttpRequestInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.reg.config.WebSecurityConfig;
import net.solarnetwork.central.reg.web.api.v1.UserCloudIntegrationsOAuthController;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

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

	@Mock
	private CloudIntegrationService service2;

	@Mock
	private CloudIntegrationService service3;

	@Captor
	private ArgumentCaptor<CloudIntegrationsFilter> filterCaptor;

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

	@Test
	public void listOauthCloudIntegrationConfigurations() {
		// GIVEN
		final Long userId = randomLong();

		BasicFilter filter = new BasicFilter();

		// service that requires access token
		final String service1Id = randomString();
		given(service1.getId()).willReturn(service1Id);
		given(service1.getSettingSpecifiers()).willReturn(
				List.of(OAUTH_ACCESS_TOKEN_SETTING_SPECIFIER, OAUTH_REFRESH_TOKEN_SETTING_SPECIFIER));

		// service that does not require access token
		given(service2.getSettingSpecifiers())
				.willReturn(List.of(USERNAME_SETTING_SPECIFIER, PASSWORD_SETTING_SPECIFIER));

		// service that requires access token
		final String service3Id = randomString();
		given(service3.getId()).willReturn(service3Id);
		given(service3.getSettingSpecifiers()).willReturn(List.of(OAUTH_ACCESS_TOKEN_SETTING_SPECIFIER));

		// find services that require access token
		given(userCloudIntegrationsBiz.availableIntegrationServices())
				.willReturn(List.of(service1, service2, service3));

		// query for integrations that use services that require access token
		final var filterResults = new BasicFilterResults<CloudIntegrationConfiguration, UserLongCompositePK>(
				List.of());
		given(userCloudIntegrationsBiz.listConfigurationsForUser(eq(userId), any(),
				eq(CloudIntegrationConfiguration.class))).willReturn(filterResults);

		// WHEN
		becomeUser(userId);
		Result<FilterResults<CloudIntegrationConfiguration, UserLongCompositePK>> result = controller
				.listOauthCloudIntegrationConfigurations(filter);

		// THEN
		then(userCloudIntegrationsBiz).should().listConfigurationsForUser(eq(userId),
				filterCaptor.capture(), eq(CloudIntegrationConfiguration.class));

		// @formatter:off
		and.then(filterCaptor.getValue())
			.as("Given filter passed to biz")
			.isSameAs(filter)
			.asInstanceOf(type(BasicFilter.class))
			.as("Access token service IDs populated")
			.returns(new String[] {service1Id, service3Id}, from(BasicFilter::getServiceIdentifiers))
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Biz result returned")
			.isSameAs(filterResults)
			;
		// @formatter:on
	}

	@Test
	public void getOAuthCloudIntegrationAuthorizationInfo() {
		// GIVEN
		final Long userId = randomLong();
		final Long integrationId = randomLong();
		final String serviceId = randomString();
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

		// invoke auth method on service
		var request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		final var implicitRedirectUri = fromMethodCall(
				on(net.solarnetwork.central.reg.web.UserCloudIntegrationsOAuthController.class)
						.handleOAuthAuthCode(null, Locale.getDefault())).buildAndExpand().toUri();
		final var reqInfo = new HttpRequestInfo("GET",
				URI.create("http://example.com/" + randomString()), null);
		given(service1.authorizationRequestInfo(same(integration), eq(implicitRedirectUri),
				same(locale))).willReturn(reqInfo);

		// WHEN
		becomeUser(userId);

		Result<HttpRequestInfo> result = controller
				.getOAuthCloudIntegrationAuthorizationInfo(integrationId, null, locale);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Service result returned")
			.isSameAs(reqInfo)
			;
		// @formatter:on
	}

	@Test
	public void getOAuthCloudIntegrationAuthorizationInfo_explicitRedirectUri() {
		// GIVEN
		final Long userId = randomLong();
		final Long integrationId = randomLong();
		final String redirectUri = "http://localhost/" + randomString();
		final String serviceId = randomString();
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

		// invoke auth method on service
		final var reqInfo = new HttpRequestInfo("GET",
				URI.create("http://example.com/" + randomString()), null);
		given(service1.authorizationRequestInfo(same(integration), eq(URI.create(redirectUri)),
				same(locale))).willReturn(reqInfo);

		// WHEN
		becomeUser(userId);
		Result<HttpRequestInfo> result = controller
				.getOAuthCloudIntegrationAuthorizationInfo(integrationId, redirectUri, locale);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Service result returned")
			.isSameAs(reqInfo)
			;
		// @formatter:on
	}

}
