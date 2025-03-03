/* ==================================================================
 * EnphaseCloudDatumStreamServiceTests.java - 4/03/2025 7:46:05 am
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

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.MAX_PAGE_SIZE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpEntity;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;

/**
 * Test cases for the {@link EnphaseCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class EnphaseCloudDatumStreamServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	SolarNodeOwnershipDao nodeOwnershipDao;

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

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Mock
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Mock
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Captor
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private CloudIntegrationsExpressionService expressionService;

	private EnphaseCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new EnphaseCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, restOps,
				oauthClientManager, clock, null);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(EnphaseCloudIntegrationService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void dataValues_systems() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey,
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_ACCESS_TOKEN_SETTING, accessToken,
				OAUTH_REFRESH_TOKEN_SETTING, refreshToken
			));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// NOTE: CLIENT_CREDENTIALS used even though auth-code is technically used, with access/refresh tokens provided
		final ClientRegistration oauthClientReg = ClientRegistration
			.withRegistrationId(integration.systemIdentifier())
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

		final URI listSystems = UriComponentsBuilder.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudIntegrationService.LIST_SYSTEMS_URL)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(EnphaseCloudIntegrationService.PAGE_SIZE_PARAM, MAX_PAGE_SIZE)
				.queryParam(EnphaseCloudIntegrationService.PAGE_PARAM, 1).buildAndExpand().toUri();

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-systems-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(eq(listSystems), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of());

		// THEN
		// @formatter:off
		then(oauthClientManager).should().authorize(authRequestCaptor.capture());

		and.then(authRequestCaptor.getValue())
			.as("OAuth request provided")
			.isNotNull()
			.as("No OAuth2AuthorizedClient provided")
			.returns(null, from(OAuth2AuthorizeRequest::getAuthorizedClient))
			.as("Client registration ID is configuration system identifier")
			.returns(integration.systemIdentifier(), OAuth2AuthorizeRequest::getClientRegistrationId)
			;

		and.then(results)
			.as("Result generated")
			.hasSize(2)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("System name parsed")
					.returns("Site 1", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("2875"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"tz", "US/Eastern",
							"l", "Anytown",
							"st", "MD",
							"postalCode", "20906",
							"c", "US",
							"lastSeenAt", Instant.ofEpochSecond(1740976229)
							))
					;
				and.then(l).element(1)
					.as("System name parsed")
					.returns("Site 2", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("5254"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"tz", "US/Eastern",
							"l", "Greenwich",
							"st", "CT",
							"postalCode", "06831",
							"c", "US",
							"lastSeenAt", Instant.ofEpochSecond(1740978304)
							))
					;
			})
			;
		// @formatter:on
	}

}
