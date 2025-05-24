/* ==================================================================
 * LocusEnergyCloudIntegrationServiceTests.java - 2/10/2024 1:03:09 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.PASSWORD_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.USERNAME_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;

/**
 * Test cases for the {@link LocusEnergyCloudIntegrationService}
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class LocusEnergyCloudIntegrationServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

	@Mock
	private OAuth2AuthorizedClientManager oauthClientManager;

	@Captor
	private ArgumentCaptor<OAuth2AuthorizeRequest> authRequestCaptor;

	@Mock
	private TextEncryptor encryptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private LocusEnergyCloudIntegrationService service;

	@BeforeEach
	public void setup() {
		service = new LocusEnergyCloudIntegrationService(Collections.singleton(datumStreamService),
				userEventAppenderBiz, encryptor, restOps, oauthClientManager, clock, null);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(LocusEnergyCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void validate_missingAuthSettings() {
		// GIVEN
		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				"foo", "bar"
			));
		// @formatter:on

		// WHEN
		Result<Void> result = service.validate(conf, Locale.getDefault());

		// THEN
		// @formatter:off
		then(restOps).shouldHaveNoInteractions();

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is NOT success")
			.returns(false, from(Result::getSuccess))
			.satisfies(r -> {
				and.then(r.getErrors())
					.as("Error details provided for missing authentication settings")
					.hasSize(5)
					.satisfies(errors -> {
						and.then(errors)
							.as("Error detail")
							.element(0)
							.as("OAuth client ID flagged")
							.returns(OAUTH_CLIENT_ID_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(1)
							.as("OAuth client secret flagged")
							.returns(OAUTH_CLIENT_SECRET_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(2)
							.as("Username flagged")
							.returns(USERNAME_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(3)
							.as("Password flagged")
							.returns(PASSWORD_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(4)
							.as("Partner ID flagged")
							.returns(PARTNER_ID_SETTING, from(ErrorDetail::getLocation))
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void validate_ok() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));

		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration
			.withRegistrationId("test")
			.authorizationGrantType(AuthorizationGrantType.PASSWORD)
			.clientId(randomString())
			.clientSecret(randomString())
			.tokenUri(tokenUri)
			.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		final URI sitesForPartnerId = LocusEnergyCloudIntegrationService.BASE_URI
				.resolve(LocusEnergyCloudIntegrationService.V3_SITES_FOR_PARTNER_ID_URL_TEMPLATE
						.replace("{partnerId}", partnerId.toString()));
		final ResponseEntity<String> res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(eq(sitesForPartnerId), eq(HttpMethod.GET), any(), eq(String.class)))
				.willReturn(res);

		// WHEN

		Result<Void> result = service.validate(conf, Locale.getDefault());

		// THEN
		// @formatter:off
		then(oauthClientManager).should().authorize(authRequestCaptor.capture());

		and.then(authRequestCaptor.getValue())
			.as("OAuth request provided")
			.isNotNull()
			.as("No OAuth2AuthorizedClient provided")
			.returns(null, from(OAuth2AuthorizeRequest::getAuthorizedClient))
			.as("Client registration ID is configuration system identifier")
			.returns(conf.systemIdentifier(), OAuth2AuthorizeRequest::getClientRegistrationId)
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on

	}

}
