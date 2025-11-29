/* ==================================================================
 * EnphaseCloudIntegrationServiceTests.java - 3/03/2025 3:11:43 pm
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.AUTHORIZATION_CODE_PARAM;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.AUTHORIZATION_STATE_PARAM;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_STATE_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.REDIRECT_URI_PARAM;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.AuthorizationState;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.HttpRequestInfo;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;

/**
 * Test cases for the {@link EnphaseCloudIntegrationService} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class EnphaseCloudIntegrationServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

	@Mock
	private OAuth2AuthorizedClientManager oauthClientManager;

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private RandomGenerator rng;

	@Captor
	private ArgumentCaptor<OAuth2AuthorizeRequest> authRequestCaptor;

	@Captor
	private ArgumentCaptor<RequestEntity<String>> httpRequestCaptor;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> jsonRequestCaptor;

	@Mock
	private TextEncryptor encryptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private EnphaseCloudIntegrationService service;

	@BeforeEach
	public void setup() {
		service = new EnphaseCloudIntegrationService(Collections.singleton(datumStreamService),
				userEventAppenderBiz, encryptor, integrationDao, rng, restOps, oauthClientManager, clock,
				null);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(EnphaseCloudIntegrationService.class.getName(),
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
							.as("API key flagged")
							.returns(API_KEY_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(1)
							.as("OAuth client ID flagged")
							.returns(OAUTH_CLIENT_ID_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(2)
							.as("OAuth client secret flagged")
							.returns(OAUTH_CLIENT_SECRET_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(3)
							.as("OAuth access token flagged")
							.returns(OAUTH_ACCESS_TOKEN_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(4)
							.as("OAuth refresh token flagged")
							.returns(OAUTH_REFRESH_TOKEN_SETTING, from(ErrorDetail::getLocation))
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
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey,
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				OAUTH_ACCESS_TOKEN_SETTING, accessToken,
				OAUTH_REFRESH_TOKEN_SETTING, refreshToken
			));

		// NOTE: CLIENT_CREDENTIALS used even though auth-code is technically used, with access/refresh tokens provided
		final ClientRegistration oauthClientReg = ClientRegistration
			.withRegistrationId("test")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.tokenUri(tokenUri)
			.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		final ResponseEntity<String> res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(any(), eq(String.class))).willReturn(res);

		// WHEN

		Result<Void> result = service.validate(conf, Locale.getDefault());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(String.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for systems")
			.returns(EnphaseCloudIntegrationService.BASE_URI
					.resolve(EnphaseCloudIntegrationService.LIST_SYSTEMS_PATH + "?key=" + apiKey), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

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

	@Test
	public void validate_429_tooManyRequests() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey,
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				OAUTH_ACCESS_TOKEN_SETTING, accessToken,
				OAUTH_REFRESH_TOKEN_SETTING, refreshToken
			));

		// NOTE: CLIENT_CREDENTIALS used even though auth-code is technically used, with access/refresh tokens provided
		final ClientRegistration oauthClientReg = ClientRegistration
			.withRegistrationId("test")
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.tokenUri(tokenUri)
			.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		given(restOps.exchange(any(), eq(String.class))).willThrow(
				HttpClientErrorException.create("Too many requests", HttpStatus.TOO_MANY_REQUESTS,
						"429 TOO_MANY_REQUESTS", new HttpHeaders(), "Too many".getBytes(UTF_8), UTF_8));

		// WHEN

		Result<Void> result = service.validate(conf, Locale.getDefault());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(String.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for systems")
			.returns(EnphaseCloudIntegrationService.BASE_URI
					.resolve(EnphaseCloudIntegrationService.LIST_SYSTEMS_PATH + "?key=" + apiKey), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

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
			.as("Code included")
			.returns("BCI.0002", from(Result::getCode))
			.satisfies(r -> {
				and.then(r.getMessage())
					.as("429 included in message")
					.contains("HTTP 429")
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void authorizationRequestInfo() {
		// GIVEN
		final String clientId = randomString();

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId
			));
		// @formatter:on

		byte[] rand = new byte[32];
		Arrays.fill(rand, (byte) 1);

		// provide specific "random" bytes
		willDoNothing().given(rng).nextBytes(assertArg(a -> {
			and.then(a.length).as("Expected byte array length").isEqualTo(rand.length);
			System.arraycopy(rand, 0, a, 0, rand.length);
		}));

		final URI redirectUri = URI.create("https://%s/auth".formatted(randomString()));

		// WHEN
		HttpRequestInfo result = service.authorizationRequestInfo(conf, redirectUri,
				Locale.getDefault());

		final AuthorizationState expectedState = new AuthorizationState(conf.getConfigId(),
				Base64.getUrlEncoder().encodeToString(DigestUtils.sha3_224(rand)).replace("=", ""));

		// THEN
		// @formatter:off
		then(integrationDao).should().saveOAuthAuthorizationState(conf.getId(), expectedState.token(), null);

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Method is GET")
			.returns("GET", from(HttpRequestInfo::method))
			.as("URI generated with client ID configured on integration")
			.returns(UriComponentsBuilder.fromUri(EnphaseCloudIntegrationService.AUTH_URI)
					.queryParam("response_type", "code")
					.queryParam("client_id", clientId)
					.queryParam("redirect_uri", redirectUri)
					.queryParam("state", expectedState.stateValue())
					.buildAndExpand().toUri()
					, from(HttpRequestInfo::uri))
			.as("No headers provided")
			.returns(null, from(HttpRequestInfo::headers))
			;
		// @formatter:on
	}

	@Test
	public void fetchAccessToken() {
		// GIVEN
		final Long integrationId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String code = randomString();
		final AuthorizationState state = new AuthorizationState(integrationId, randomString());
		final String stateValue = state.stateValue();
		final String redirectUri = "http://localhost/" + randomString();
		final Locale locale = Locale.getDefault();

		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				integrationId, now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				OAUTH_STATE_SETTING, stateValue
			));
		// @formatter:on

		// validate state value
		given(integrationDao.saveOAuthAuthorizationState(conf.getId(), null, state.token()))
				.willReturn(true);

		// request token
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-token-01.json", getClass()), ObjectNode.class);
		given(restOps.exchange(any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<JsonNode>(resJson, HttpStatus.OK));

		// WHEN
		Map<String, Object> params = Map.of(AUTHORIZATION_CODE_PARAM, code, AUTHORIZATION_STATE_PARAM,
				stateValue, REDIRECT_URI_PARAM, redirectUri);
		Map<String, ?> result = service.fetchAccessToken(conf, params, locale);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(jsonRequestCaptor.capture(), eq(JsonNode.class));

		// confirm basic auth provided
		and.then(jsonRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for token")
			.returns(UriComponentsBuilder.fromUri(EnphaseCloudIntegrationService.BASE_URI)
					.path(EnphaseCloudIntegrationService.TOKEN_PATH)
					.queryParam("grant_type", "authorization_code")
					.queryParam(AUTHORIZATION_CODE_PARAM, code).queryParam(REDIRECT_URI_PARAM, redirectUri)
					.buildAndExpand().toUri(), from(RequestEntity::getUrl))
			.satisfies(req -> {
				final HttpHeaders expected = new HttpHeaders();
				expected.setBasicAuth(clientId, clientSecret);

				and.then(req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
					.as("Basic auth provided using cilent ID/secret from integration")
					.isEqualTo(expected.getFirst(HttpHeaders.AUTHORIZATION))
					;
			})
			;

		and.then(result)
			.asInstanceOf(map(String.class, Object.class))
			.hasSize(2)
			.as("Access token from JSON response returned")
			.containsEntry(OAUTH_ACCESS_TOKEN_SETTING, "unique access token")
			.as("Refresh token from JSON response returned")
			.containsEntry(OAUTH_REFRESH_TOKEN_SETTING, "unique refresh token")
			;
		// @formatter:on
	}

}
