/* ==================================================================
 * SmaCloudDatumStreamServiceTests.java - 30/03/2025 11:30:33 am
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
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.SOURCE_ID_MAP_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.ACTIVE_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEACTIVATED_AT_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_MODEL_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.MANUFACTURER_METADATA;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.Reference;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.jackson.JsonUtils.getObjectFromJSON;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
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
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.CommonValidationType;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SmaCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.SmaMeasurementSetType;
import net.solarnetwork.central.c2c.biz.impl.SmaPeriod;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.NumberUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test cases for the {@link SmaCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SmaCloudDatumStreamServiceTests implements CloudIntegrationsUserEvents {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

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

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private Cache<String, ZoneId> systemTimeZoneCache;

	@Mock
	private Cache<String, CloudDataValue[]> systemInventoryCache;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Captor
	private ArgumentCaptor<DatumCriteria> datumCriteriaCaptor;

	private MutableClock clock = MutableClock.of(Instant.now(), UTC);

	private CloudIntegrationsExpressionService expressionService;

	private SmaCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SmaCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, restOps,
				oauthClientManager, clock, null);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SmaCloudIntegrationService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		service.setSystemTimeZoneCache(systemTimeZoneCache);
		service.setSystemInventoryCache(systemInventoryCache);

		clock.setInstant(Instant.now().truncatedTo(DAYS));
	}

	@Test
	public void dataValues_root() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final JsonNode resJson = getObjectFromJSON(utf8StringResource("sma-plants-01.json", getClass()),
				ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of());

		// THEN
		// @formatter:off
		then(oauthClientManager).should().authorize(authRequestCaptor.capture());

		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list systems")
			.returns(UriComponentsBuilder.fromUri(SmaCloudIntegrationService.BASE_URI)
					.path(SmaCloudIntegrationService.LIST_SYSTEMS_PATH).buildAndExpand().toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

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
					.returns(List.of("7190000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								"tz", "America/New_York",
								"l", "Anytown",
								"st", "Massachusetts",
								"postalCode", "00001",
								"street", "123 Main Street",
								"c", "US",
								"lat", 42.38f,
								"lon", -71.07f,
								"startDate", Instant.parse("2021-11-03T00:00:00Z")
								));
						expectedMeta.putAll(Map.of(
								"peakPower", 184000,
						        "acNominalPower", 184000,
						        "dcPowerInputMax", 216000,
						        "co2SavingsFactor", 649,
						        "azimuth", 28,
						        "tilt", 24
								));
						and.then(m)
							.as("Metadata extracted")
							.containsExactlyInAnyOrderEntriesOf(expectedMeta)
							;
					})
					;
				and.then(l).element(1)
					.as("System name parsed")
					.returns("Site 2", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("11260000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								"tz", "America/New_York",
								"l", "Springfield",
								"st", "Pennsylvania",
								"postalCode", "00002",
								"street", "234 Main Road",
								"c", "US",
								"lat", 39.82f,
								"lon", -75.38f,
								"startDate", Instant.parse("2023-10-11T00:00:00Z")
								));
						expectedMeta.putAll(Map.of(
								"peakPower", 0,
						        "acNominalPower", 0,
						        "dcPowerInputMax", 0,
						        "co2SavingsFactor", 400
								));
						and.then(m)
							.as("Metadata extracted")
							.containsExactlyInAnyOrderEntriesOf(expectedMeta)
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_system() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomLong().toString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final JsonNode resJson = getObjectFromJSON(utf8StringResource("sma-devices-01.json", getClass()),
				ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SmaCloudDatumStreamService.SYSTEM_ID_FILTER, systemId));

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

		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list system devices")
			.returns(UriComponentsBuilder.fromUri(SmaCloudIntegrationService.BASE_URI)
					.path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
					.queryParam("WithDeactivatedDevices", true)
					.buildAndExpand(systemId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(results)
			.as("Result generated for system")
			.hasSize(10)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Name provided")
					.returns("Satellit Sensor", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, "14"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata provided")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"model", "Satellite Sensor",
							"manufacturer", "SMA Solar Technology AG",
							"active", true,
							"productId", 200053,
							"type", "Sensor technology"
							))
					;
				and.then(l).element(2)
					.as("Name provided")
					.returns("My Inverter 1", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, "16"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata provided")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"model", "STP 6000TL-20",
							"manufacturer", "SMA Solar Technology AG",
							"serial", "3421111",
							"active", true,
							"productId", 9099,
							"type", "Solar Inverters",
							"generatorPower", 6000,
							"generatorPowerDc", 6000
							))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_system_deactivatedInverter() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomLong().toString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final JsonNode resJson = getObjectFromJSON(utf8StringResource("sma-devices-02.json", getClass()),
				ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// look up time zone for deactivatedAt
		final ZoneId systemTimeZone = ZoneId.of("America/New_York");
		given(systemTimeZoneCache.get(systemId)).willReturn(systemTimeZone);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SmaCloudDatumStreamService.SYSTEM_ID_FILTER, systemId));

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

		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list system devices")
			.returns(UriComponentsBuilder.fromUri(SmaCloudIntegrationService.BASE_URI)
					.path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
					.queryParam("WithDeactivatedDevices", true)
					.buildAndExpand(systemId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(results)
			.as("Result generated for system")
			.hasSize(2)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Name provided")
					.returns("My Inverter 1", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, "16"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata provided")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							DEVICE_MODEL_METADATA, "STP 6000TL-20",
							MANUFACTURER_METADATA, "SMA Solar Technology AG",
							DEVICE_SERIAL_NUMBER_METADATA, "3421111",
							ACTIVE_METADATA, true,
							"productId", 9099,
							"type", "Solar Inverters",
							"generatorPower", 6000,
							"generatorPowerDc", 6000
							))
					;
				and.then(l).element(1)
					.as("Name provided")
					.returns("My Inverter 2", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, "17"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata provided")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							DEVICE_MODEL_METADATA, "STP 5000TL-20",
							MANUFACTURER_METADATA, "SMA Solar Technology AG",
							DEVICE_SERIAL_NUMBER_METADATA, "9687867",
							ACTIVE_METADATA, false,
							DEACTIVATED_AT_METADATA, LocalDateTime.parse("2019-04-07T12:30:02")
								.atZone(systemTimeZone).toInstant(),
							"productId", 9098,
							"type", "Solar Inverters",
							"generatorPower", 5000,
							"generatorPowerDc", 5000
							))
					;
			})
			;
		// @formatter:on
	}

	private String refValue(List<String> idents) {
		return idents.stream().collect(Collectors.joining("/", "/", ""));
	}

	@Test
	public void dataValues_device_inverter() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomLong().toString();
		final String deviceId = randomLong().toString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("sma-device-measurement-sets-inverter-01.json", getClass()),
				ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SmaCloudDatumStreamService.SYSTEM_ID_FILTER, systemId,
						SmaCloudDatumStreamService.DEVICE_ID_FILTER, deviceId));

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

		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list measurement sets")
			.returns(UriComponentsBuilder
					.fromUri(SmaCloudIntegrationService.BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_SETS_PATH_TEMPLATE)
					.buildAndExpand(deviceId).toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(results)
			.as("Result generated for device measurement sets")
			.hasSize(3)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Name provided")
					.returns(SmaMeasurementSetType.EnergyAndPowerPv.name(), from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, deviceId, SmaMeasurementSetType.EnergyAndPowerPv.name()), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.satisfies(v -> {
						and.then(v.getMetadata())
							.asInstanceOf(map(String.class, Object.class))
							.as("Metadata provided")
							.containsExactlyInAnyOrderEntriesOf(Map.of(
									"description", SmaMeasurementSetType.EnergyAndPowerPv.getDescription()
									))
							;
					})
					.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
					.as("Has one child for every measurement in set")
					.hasSize(SmaMeasurementSetType.EnergyAndPowerPv.getMeasurements().size())
					.satisfies(measList -> {
						var pvGenIds = List.of(systemId, deviceId, SmaMeasurementSetType.EnergyAndPowerPv.name(), "pvGeneration");
						and.then(measList).element(0)
							.as("Name provided")
							.returns("pvGeneration", from(CloudDataValue::getName))
							.as("Identifiers provided")
							.returns(pvGenIds, from(CloudDataValue::getIdentifiers))
							.as("Reference returned for data value")
							.returns(refValue(pvGenIds), from(CloudDataValue::getReference))
							.as("No children provided")
							.returns(null, from(CloudDataValue::getChildren))
							.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
							.as("Description provided")
							.containsKey("description")
							;
					})
					;
				and.then(l).element(1)
					.as("Name provided")
					.returns("PowerDc", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(List.of(systemId, deviceId, SmaMeasurementSetType.PowerDc.name()), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.satisfies(v -> {
						and.then(v.getMetadata())
							.asInstanceOf(map(String.class, Object.class))
							.as("Metadata provided")
							.containsExactlyInAnyOrderEntriesOf(Map.of(
									"description", SmaMeasurementSetType.PowerDc.getDescription()
									))
							;
					})
					.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
					.as("Has one child for every measurement in set")
					.hasSize(SmaMeasurementSetType.PowerDc.getMeasurements().size())
					.satisfies(measList -> {
						var pvGenIds = List.of(systemId, deviceId, SmaMeasurementSetType.PowerDc.name(), "dcPowerInput");
						and.then(measList).element(0)
							.as("Name provided")
							.returns("dcPowerInput", from(CloudDataValue::getName))
							.as("Identifiers provided")
							.returns(pvGenIds, from(CloudDataValue::getIdentifiers))
							.as("Reference returned for data value")
							.returns(refValue(pvGenIds), from(CloudDataValue::getReference))
							.as("No children provided")
							.returns(null, from(CloudDataValue::getChildren))
							.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
							.as("Description provided")
							.containsKey("description")
							;
					})
					;
			})
			;
		// @formatter:on
	}

	private static String placeholderValueRef(SmaMeasurementSetType measurementSet,
			String measurementName) {
		return "/{systemId}/{deviceId}/%s/%s".formatted(measurementSet.getKey(), measurementName);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_mappedSourceIds() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";
		final String inv2SourceId = "inv/2";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final SmaMeasurementSetType prop2MeasuermentSet = SmaMeasurementSetType.PowerAc;
		final String prop2MeasurementName = "activePower";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Instantaneous, "watts", Reference,
				placeholderValueRef(prop2MeasuermentSet, prop2MeasurementName));
		prop2.setScale(1);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");

		final SequencedMap<String, String> sourceIdMap = new LinkedHashMap<>();
		sourceIdMap.put("/%s/%s".formatted(systemId, device1Id), inv1SourceId);
		sourceIdMap.put("/%s/%s".formatted(systemId, device2Id), inv2SourceId);
		datumStream.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMap));

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		final ResponseEntity<JsonNode> systemDetailsRes = new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK);

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		final LocalDate day = LocalDate.parse("2025-03-28");
		for ( String deviceId : List.of(device1Id, device2Id) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(
					utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-01.json",
							getClass()),
					JsonNode.class), OK));

			expectedUris.add(b
					.replaceQueryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop2MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop2MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(
					utf8StringResource("sma-device-data-Day-PowerAc-01.json", getClass()),
					JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(systemDetailsRes,
				responses.toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(day.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x2 devices parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice * 2)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			.satisfies(list -> {
				Map<String, List<Datum>> datumBySourceId = StreamSupport.stream(list.spliterator(), false)
						.collect(groupingBy(Datum::getSourceId, LinkedHashMap::new, toList()));

				for ( String sourceId : List.of(inv1SourceId, inv2SourceId)) {
					and.then(datumBySourceId.get(sourceId))
						.as("Source [%s] datum resolved", sourceId)
						.hasSize(datumPerDevice)
						.satisfies(invList -> {
							final int index1 = 4;
							final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
									"watts", 46.8f,
									"wh", 4), null, null);

							and.then(invList).element(index1, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index1)
								.returns(expectedTs1, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index1)
								.returns(expectedSamples1, from(GeneralDatum::getSamples))
								;

							final int index2 = 70;
							final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T12:40:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
									"watts", 4969.9f,
									"wh", 413), null, null);

							and.then(invList).element(index2, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index2)
								.returns(expectedTs2, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index2)
								.returns(expectedSamples2, from(GeneralDatum::getSamples))
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_mappedSourceIds_sameSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";
		final String inv2SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final SmaMeasurementSetType prop2MeasuermentSet = SmaMeasurementSetType.PowerAc;
		final String prop2MeasurementName = "activePower";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Instantaneous, "watts", Reference,
				placeholderValueRef(prop2MeasuermentSet, prop2MeasurementName));
		prop2.setScale(1);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");

		final SequencedMap<String, String> sourceIdMap = new LinkedHashMap<>();
		sourceIdMap.put("/%s/%s".formatted(systemId, device1Id), inv1SourceId);
		sourceIdMap.put("/%s/%s".formatted(systemId, device2Id), inv2SourceId);
		datumStream.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMap));

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		final ResponseEntity<JsonNode> systemDetailsRes = new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK);

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		final LocalDate day = LocalDate.parse("2025-03-28");
		final List<String> alts = List.of("a", "b");
		final List<String> devs = List.of(device1Id, device2Id);
		for ( int i = 0; i < alts.size(); i++ ) {
			final String alt = alts.get(i);
			final String deviceId = devs.get(i);
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-04%s.json".formatted(alt),
					getClass()), JsonNode.class), OK));

			expectedUris.add(b
					.replaceQueryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop2MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop2MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					"sma-device-data-Day-PowerAc-04%s.json".formatted(alt), getClass()), JsonNode.class),
					OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(systemDetailsRes,
				responses.toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(day.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x2 devices @ 50% each parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			.satisfies(list -> {
				Map<String, List<Datum>> datumBySourceId = StreamSupport.stream(list.spliterator(), false)
						.collect(groupingBy(Datum::getSourceId, LinkedHashMap::new, toList()));

				for ( String sourceId : List.of(inv1SourceId, inv2SourceId)) {
					and.then(datumBySourceId.get(sourceId))
						.as("Source [%s] datum resolved", sourceId)
						.hasSize(datumPerDevice)
						.satisfies(invList -> {
							final int index1 = 4;
							final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
									"watts", 46.8f,
									"wh", 4), null, null);

							and.then(invList).element(index1, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index1)
								.returns(expectedTs1, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index1)
								.returns(expectedSamples1, from(GeneralDatum::getSamples))
								;

							final int index2 = 70;
							final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T12:40:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
									"watts", 4969.9f,
									"wh", 413), null, null);

							and.then(invList).element(index2, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index2)
								.returns(expectedTs2, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index2)
								.returns(expectedSamples2, from(GeneralDatum::getSamples))
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_mappedSourceIds_sameSourceId_opRangeConstraint() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";
		final String inv2SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final SmaMeasurementSetType prop2MeasuermentSet = SmaMeasurementSetType.PowerAc;
		final String prop2MeasurementName = "activePower";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Instantaneous, "watts", Reference,
				placeholderValueRef(prop2MeasuermentSet, prop2MeasurementName));
		prop2.setScale(1);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");

		final SequencedMap<String, String> sourceIdMap = new LinkedHashMap<>();
		sourceIdMap.put("/%s/%s".formatted(systemId, device1Id), inv1SourceId);
		sourceIdMap.put("/%s/%s".formatted(systemId, device2Id), inv2SourceId);

		final Map<String, Object> datumStreamServiceProps = new LinkedHashMap<>(2);
		datumStreamServiceProps.put(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMap);
		datumStream.setServiceProps(datumStreamServiceProps);

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		final ResponseEntity<JsonNode> systemDetailsRes = new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK);

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		final LocalDate day = LocalDate.parse("2025-03-28");
		final Instant splitTimestamp = day.atTime(11, 0).atZone(systemTimeZone).toInstant();

		// add date range constraint
		// @formatter:off
		datumStreamServiceProps.put(CloudDatumStreamService.OPERATIONAL_DATE_RANGES_SETTING, Map.of(
				// device 1 valid up to split date
				"/%s/%s".formatted(systemId, device1Id), "/%s".formatted(splitTimestamp),

				// device 2 valid from split date
				"/%s/%s".formatted(systemId, device2Id), "%s/".formatted(splitTimestamp)
		));
		// @formatter:on

		final List<String> alts = List.of("a", "b");
		final List<String> devs = List.of(device1Id, device2Id);
		for ( int i = 0; i < alts.size(); i++ ) {
			final String alt = alts.get(i);
			final String deviceId = devs.get(i);
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-04%s.json".formatted(alt),
					getClass()), JsonNode.class), OK));

			expectedUris.add(b
					.replaceQueryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop2MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop2MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					"sma-device-data-Day-PowerAc-04%s.json".formatted(alt), getClass()), JsonNode.class),
					OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(systemDetailsRes,
				responses.toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(day.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x2 devices @ 50% each parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			.satisfies(list -> {
				Map<String, List<Datum>> datumBySourceId = StreamSupport.stream(list.spliterator(), false)
						.collect(groupingBy(Datum::getSourceId, LinkedHashMap::new, toList()));

				for ( String sourceId : List.of(inv1SourceId, inv2SourceId)) {
					and.then(datumBySourceId.get(sourceId))
						.as("Source [%s] datum resolved", sourceId)
						.hasSize(datumPerDevice)
						.satisfies(invList -> {
							final int index1 = 4;
							final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
									"watts", 46.8f,
									"wh", 4), null, null);

							and.then(invList).element(index1, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index1)
								.returns(expectedTs1, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index1)
								.returns(expectedSamples1, from(GeneralDatum::getSamples))
								;

							final int index2 = 70;
							final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T12:40:00").atZone(systemTimeZone).toInstant();
							final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
									"watts", 4969.9f,
									"wh", 413), null, null);

							and.then(invList).element(index2, type(GeneralDatum.class))
								.as("Source [%s] datum %d has expected date", sourceId, index2)
								.returns(expectedTs2, from(Datum::getTimestamp))
								.as("Source [%s] datum %d has expected sample data, combined from measurement set HTTP requests",
										sourceId, index2)
								.returns(expectedSamples2, from(GeneralDatum::getSamples))
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_multiDay_mappedSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();
		for ( LocalDate day = startDay, endDay = startDay.plus(3, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource(
									"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-0%d.json"
											.formatted(DAYS.between(startDay, day) + 1),
									getClass()),
							JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(3, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x1 device x3 days parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice * 3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 4;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 4), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 228;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-30T12:40:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 413), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Dtum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_multiDay_mappedSourceId_opRangeConstraint() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "16"; // 6k inverter from sma-devices-01.json

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		final Map<String, Object> datumStreamServiceProps = new LinkedHashMap<>(2);
		datumStreamServiceProps.put(CloudDatumStreamService.SOURCE_ID_MAP_SETTING,
				Map.of("/%s/%s".formatted(systemId, deviceId), inv1SourceId));
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		datumStream.setServiceProps(datumStreamServiceProps);

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		// get system zone
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();

		// add op range constraint
		datumStreamServiceProps.put(CloudDatumStreamService.OPERATIONAL_DATE_RANGES_SETTING,
				Map.of("/%s/%s".formatted(systemId, deviceId),
						"/%s".formatted(startDay.plusDays(1).atStartOfDay(systemTimeZone).toInstant())));

		// only iterate up to end of date constraint
		for ( LocalDate day = startDay, endDay = startDay.plus(1, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource(
									"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-0%d.json"
											.formatted(DAYS.between(startDay, day) + 1),
									getClass()),
							JsonNode.class), OK));
			if ( day.isEqual(startDay) ) {
				// get system devices for data validation
				expectedUris.add(fromUri(BASE_URI)
						.path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
						.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
				responses.add(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), OK));
			}
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(3, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(systemInventoryCache).should().put(eq(systemId), any());

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x1 device x1 days parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 4;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 4), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_hour_mappedSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();
		for ( LocalDate day = startDay, endDay = startDay.plus(1, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource(
									"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-0%d.json"
											.formatted(DAYS.between(startDay, day) + 1),
									getClass()),
							JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).plusHours(8).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result)
			.as("Datum x1 device x1 day filtered to 1 hour parsed from HTTP responses")
			.hasSize(12)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T08:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 30), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 11;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T08:55:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 160), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Dtum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_indexedValue() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.PowerDc;
		final String prop1MeasurementName = "dcPowerInput";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "dcPower", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-01").atStartOfDay(systemTimeZone)
				.toLocalDate();
		for ( LocalDate day = startDay, endDay = startDay.plus(1, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource("sma-device-data-Day-PowerDc-0%d.json"
									.formatted(DAYS.between(startDay, day) + 1), getClass()),
							JsonNode.class), OK));

			if ( day.isEqual(startDay) ) {
				// get system devices for data validation
				expectedUris.add(fromUri(BASE_URI)
						.path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
						.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
				responses.add(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), OK));
			}
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).plusHours(10).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result)
			.as("Datum x1 device x1 day filtered to 1 hour parsed from HTTP responses")
			.hasSize(12)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-01T10:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"dcPower_a", 874.905f,
						"dcPower_b", 866.89f,
						"dcPower_c", 780.665f), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data (indexed value split into multiple properties)", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_202303() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();
		final Instant givenStartDate = Instant.parse("2023-03-12T04:00:00Z");
		final Instant givenEndDate = Instant.parse("2023-04-01T05:00:00Z");

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.PowerDc;
		final String prop1MeasurementName = "dcPowerInput";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "dcPower", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// get system time zone info
		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json
		given(systemTimeZoneCache.get(systemId)).willReturn(systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2023-03-11");
		final LocalDate endDay = LocalDate.parse("2023-03-19");
		for ( LocalDate day = startDay; day.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			// remap timestamps to given day
			String dayJson = utf8StringResource("sma-device-data-Day-PowerDc-02.json", getClass());
			dayJson = dayJson.replace("2025-03-02", day.plusDays(1).toString()).replace("2025-03-01",
					day.toString());
			responses.add(new ResponseEntity<>(getObjectFromJSON(dayJson, JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(givenStartDate);
		filter.setEndDate(givenEndDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final var httpRequests = httpRequestCaptor.getAllValues();
		and.then(httpRequests)
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result)
			.as("Datum x1 device x7 day x225 + 1 (prev day) datum parsed from HTTP responses")
			.hasSize(1576)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = givenStartDate;
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"dcPower_a", 1.1f,
						"dcPower_b", 2.2f,
						"dcPower_c", 3.3f), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final Instant expectedTs2 = endDay.atStartOfDay(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"dcPower_a", 0.1f,
						"dcPower_b", 0.2f,
						"dcPower_c", 0.3f), null, null);

				and.then(list).last(type(GeneralDatum.class))
					.as("Last datum has expected date")
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Last datum has expected sample data")
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_splitDay_mappedSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();
		for ( LocalDate day = startDay, endDay = startDay.plus(2, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource(
									"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-0%d.json"
											.formatted(DAYS.between(startDay, day) + 1),
									getClass()),
							JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).plusHours(12).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int expectedDatumCount = 17 + 62;
		and.then(result)
			.as("%d Datum (17 >= noon day 1 + 62 < noon day 2 from HTTP responses", expectedDatumCount)
			.hasSize(expectedDatumCount)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T12:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 386), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 78;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-29T11:55:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 512), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Dtum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_multiDay_exceedFilterLimit_mappedSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();
		for ( LocalDate day = startDay, endDay = startDay.plus(3, DAYS); day
				.isBefore(endDay); day = day.plusDays(1) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses
					.add(new ResponseEntity<>(getObjectFromJSON(
							utf8StringResource(
									"sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-0%d.json"
											.formatted(DAYS.between(startDay, day) + 1),
									getClass()),
							JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		service.setMaxFilterTimeRange(Duration.ofDays(3));
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(5, DAYS));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is filter start date")
			.returns(filter.getStartDate(), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is filter end date truncated to max filter range")
			.returns(filter.getStartDate().plus(service.getMaxFilterTimeRange()), from(DateRangeCriteria::getEndDate))
			;

		and.then(result.getNextQueryFilter())
			.as("Next query filter provided")
			.isNotNull()
			.as("Next query start date is filter start date + max filter range")
			.returns(filter.getStartDate().plus(service.getMaxFilterTimeRange()), from(DateRangeCriteria::getStartDate))
			.as("Next query end date is filter start date + max filter range + remaining 2 days")
			.returns(filter.getStartDate().plus(5, DAYS), from(DateRangeCriteria::getEndDate))
			;


		final int datumPerDevice = 79;
		and.then(result)
			.as("%d Datum x1 device x3 days parsed from HTTP responses", datumPerDevice)
			.hasSize(datumPerDevice * 3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 4;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 4), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 228;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-30T12:40:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 413), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Dtum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_multiStreamLag_withinTolerance() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";
		final String inv2SourceId = "inv/2";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping property

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");

		final SequencedMap<String, String> sourceIdMap = new LinkedHashMap<>();
		sourceIdMap.put("/%s/%s".formatted(systemId, device1Id), inv1SourceId);
		sourceIdMap.put("/%s/%s".formatted(systemId, device2Id), inv2SourceId);
		datumStream.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMap));

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range

		final LocalDate day = LocalDate.parse("2025-03-28");
		for ( String deviceId : List.of(device1Id, device2Id) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					deviceId.equals(device1Id)
							? "sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-01.json"
							: "sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-01a.json",
					getClass()), JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(day.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(LocalDateTime.parse("2025-03-28T13:30:00").atZone(systemTimeZone).toInstant());

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int inv1DatumCount = 79;
		final int inv2DatumCount = 50;
		and.then(result)
			.as("%d Datum parsed from HTTP responses", inv1DatumCount + inv2DatumCount)
			.hasSize(inv1DatumCount + inv2DatumCount)
			.satisfies(_ -> {
				and.then(result.getNextQueryFilter())
					.as("Next query filter returned")
					.isNotNull()
					.as("11:00 returned, as the least of all greatest timestamps per stream + 5min")
					.returns(LocalDateTime.parse("2025-03-28T11:00:00").atZone(systemTimeZone).toInstant(),
							from(CloudDatumStreamQueryFilter::getStartDate))
					;
			})
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			.satisfies(list -> {
				Map<String, List<Datum>> datumBySourceId = StreamSupport.stream(list.spliterator(), false)
						.collect(groupingBy(Datum::getSourceId, LinkedHashMap::new, toList()));

				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T13:20:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of("wh", 107), null, null);

				and.then(datumBySourceId.get(inv1SourceId).getLast())
					.asInstanceOf(type(GeneralDatum.class))
					.as("Source [%s] last datum has expected date", inv1SourceId)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Source [%s] last datum has expected sample data", inv1SourceId)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T10:55:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of("wh", 256), null, null);

				and.then(datumBySourceId.get(inv2SourceId).getLast())
					.asInstanceOf(type(GeneralDatum.class))
					.as("Source [%s] last datum has expected date", inv2SourceId)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Source [%s] last datum has expected sample data", inv2SourceId)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_oneZone_multiStreamLag_outsideTolerance() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";
		final String inv2SourceId = "inv/2";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping property

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");

		SequencedMap<String, String> sourceIdMap = new LinkedHashMap<>();
		sourceIdMap.put("/%s/%s".formatted(systemId, device1Id), inv1SourceId);
		sourceIdMap.put("/%s/%s".formatted(systemId, device2Id), inv2SourceId);
		datumStream.setServiceProps(Map.of(SOURCE_ID_MAP_SETTING, sourceIdMap));

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range

		final LocalDate day = LocalDate.parse("2025-03-28");
		for ( String deviceId : List.of(device1Id, device2Id) ) {
			final UriComponentsBuilder b = fromUri(BASE_URI)
					.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
					.queryParam(SmaCloudDatumStreamService.DATE_PARAM, day.toString());

			expectedUris.add(b
					.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
							prop1MeasuermentSet.shouldReturnEnergyValues())
					.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey())
					.toUri());
			responses.add(new ResponseEntity<>(getObjectFromJSON(utf8StringResource(
					deviceId.equals(device1Id)
							? "sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-01.json"
							: "sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-01a.json",
					getClass()), JsonNode.class), OK));
		}

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(day.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, DAYS));

		// setup clock to be far after end of requested data period (outside lag tolerance)
		clock.setInstant(filter.getEndDate().plus(365L, DAYS));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final int inv1DatumCount = 79;
		final int inv2DatumCount = 50;
		and.then(result)
			.as("%d Datum parsed from HTTP responses", inv1DatumCount + inv2DatumCount)
			.hasSize(inv1DatumCount + inv2DatumCount)
			.satisfies(_ -> {
				and.then(result.getNextQueryFilter())
					.as("No next query filter returned because clock is beyond multi stream lag tolerance")
					.isNull()
					;
			})
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					;
			})
			.satisfies(list -> {
				Map<String, List<Datum>> datumBySourceId = StreamSupport.stream(list.spliterator(), false)
						.collect(groupingBy(Datum::getSourceId, LinkedHashMap::new, toList()));

				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T13:20:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of("wh", 107), null, null);

				and.then(datumBySourceId.get(inv1SourceId).getLast())
					.asInstanceOf(type(GeneralDatum.class))
					.as("Source [%s] last datum has expected date", inv1SourceId)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Source [%s] last datum has expected sample data", inv1SourceId)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T10:55:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of("wh", 256), null, null);

				and.then(datumBySourceId.get(inv2SourceId).getLast())
					.asInstanceOf(type(GeneralDatum.class))
					.as("Source [%s] last datum has expected date", inv2SourceId)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Source [%s] last datum has expected sample data", inv2SourceId)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_indexedValue_wholeDay() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.PowerDc;
		final String prop1MeasurementName = "dcPowerInput";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "dcPower", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-01").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-device-data-Day-PowerDc-01.json", getClass()),
						JsonNode.class),
				OK));

		// get system devices for data validation
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
				.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class),
				OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(219)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-01T00:15:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"dcPower_a", 1,
						"dcPower_b", 2,
						"dcPower_c", 3), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data (indexed value split into multiple properties)", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 218;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-02T00:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"dcPower_a", 0.1f,
						"dcPower_b", 0.2f,
						"dcPower_c", 0.3f), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Datum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data (indexed value split into multiple properties)", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_invalidData() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "18"; // 3.6kw inverter from sma-devices-01.json

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(getObjectFromJSON(
				utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-05.json",
						getClass()),
				JsonNode.class), OK));

		// get system devices for data validation
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
				.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class),
				OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T06:50:00").atZone(systemTimeZone).toInstant();
		final Integer expectedGen1 = 10782938;

		final Instant expectedTs3 = LocalDateTime.parse("2025-03-28T07:05:00").atZone(systemTimeZone).toInstant();
		final Integer expectedGen3 = 101010;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(5)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", expectedGen1), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 2;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T07:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 101), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Datum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;

				final int index3 = 3;
				final DatumSamples expectedSamples3 = new DatumSamples(Map.of(
						"wh", expectedGen3), null, null);

				and.then(list).element(index3, type(GeneralDatum.class))
					.as("Datum %d has expected date", index3)
					.returns(expectedTs3, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh failed validation and set to 0", index3)
					.returns(expectedSamples3, from(GeneralDatum::getSamples))
					;

				final int index4 = 4;
				final Instant expectedTs4 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples4 = new DatumSamples(Map.of(
						"wh", 600), null, null);

				and.then(list).element(index4, type(GeneralDatum.class))
					.as("Datum %d has expected date", index4)
					.returns(expectedTs4, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh passed validation because > expected max < 2x max", index4)
					.returns(expectedSamples4, from(GeneralDatum::getSamples))
					;
			})
			;

		then(userEventAppenderBiz).should(times(5)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		var events = eventCaptor.getAllValues();
		and.then(events.get(3))
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/18/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(1).toString(),
					inv1SourceId,
					expectedTs1,
					expectedGen1,
					300000,
					service.getEnergyValidationThreshold(),
					3000.0,
					3600
					))
			;
		and.then(events.get(4))
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/18/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(1).toString(),
					inv1SourceId,
					expectedTs3,
					expectedGen3,
					300000,
					service.getEnergyValidationThreshold(),
					3000.0,
					3600
					))
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_invalidData_customThreshold() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "18"; // 3.6kw inverter from sma-devices-01.json

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final double dataValidationThreshold = 1000.0;
		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						),
				CloudDatumStreamService.ENERGY_VALIDATION_THRESHOLD_SETTING, dataValidationThreshold
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(getObjectFromJSON(
				utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-05.json",
						getClass()),
				JsonNode.class), OK));

		// get system devices for data validation
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
				.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class),
				OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T06:50:00").atZone(systemTimeZone).toInstant();
		final Integer expectedGen1 = 10782938;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(5)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", expectedGen1), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 2;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T07:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 101), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Datum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;

				final int index3 = 3;
				final Instant expectedTs3 = LocalDateTime.parse("2025-03-28T07:05:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples3 = new DatumSamples(Map.of(
						"wh", 101010), null, null);

				and.then(list).element(index3, type(GeneralDatum.class))
					.as("Datum %d has expected date", index3)
					.returns(expectedTs3, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index3)
					.returns(expectedSamples3, from(GeneralDatum::getSamples))
					;

				final int index4 = 4;
				final Instant expectedTs4 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples4 = new DatumSamples(Map.of(
						"wh", 600), null, null);

				and.then(list).element(index4, type(GeneralDatum.class))
					.as("Datum %d has expected date", index4)
					.returns(expectedTs4, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh passed validation because > expected max < 2x max", index4)
					.returns(expectedSamples4, from(GeneralDatum::getSamples))
					;
			})
			;

		then(userEventAppenderBiz).should(times(4)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		and.then(eventCaptor.getAllValues())
			.last()
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/18/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(1).toString(),
					inv1SourceId,
					expectedTs1,
					expectedGen1,
					300000,
					dataValidationThreshold,
					300000.0,
					3600
					))

			;
		// @formatter:on
	}

	/*-
	java.lang.AssertionError: [Event data values]
	Expecting map:
	{"configId"=8898692900732655469L
	, "dataValue"=10782938
	, "dataValueThreshold"=300000.0
	, "ratedPower"=3600
	, "source"="/d1700257562d48/18/EnergyAndPowerPv/pvGeneration"
	, "sourceId"="inv/1"
	, "duration"=300000
	, "timestamp"="2025-03-28 10:50:00Z"
	, "uri"="https://monitoring.smaapis.de/v1/devices/18/measurements/sets/EnergyAndPowerPv/Day?Date=2025-03-28&ReturnEnergyValues=true"
	, "validationThreshold"=1000.0}
	
	
	 */

	@SuppressWarnings("unchecked")
	private Map<String, Object> dataValidationEventData(CloudDatumStreamConfiguration ds, String source,
			String uri, String sourceId, Instant timestamp, Number dataValue, Number timeDiff,
			Number validationThreshold, Number dataValueThreshold, Number ratedPower) {
		// @formatter:off
		List<Entry<String, Object>> entries = new ArrayList<>(List.of(
				  entry(CONFIG_ID_DATA_KEY, ds.getConfigId())
				, entry(SOURCE_DATA_KEY, source)
				, entry(HTTP_URI_DATA_KEY, uri.toString())
				, entry(SOURCE_ID_DATA_KEY, sourceId)
				, entry("timestamp", ISO_DATE_TIME_ALT_UTC.format(timestamp))
				, entry("dataValue", dataValue)
				, entry(DURATION_DATA_KEY, timeDiff)
				, entry("validationThreshold",  NumberUtils.bigDecimalForNumber(validationThreshold))
				, entry("dataValueThreshold", NumberUtils.bigDecimalForNumber(dataValueThreshold))
				, entry("ratedPower", ratedPower)
				));
		// @formatter:on

		return Map.ofEntries(entries.toArray(Entry[]::new));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_timeJump_lookupPrevDatum() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "18"; // 3.6kw inverter from sma-devices-01.json

		service.setDatumDao(datumDao);

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(getObjectFromJSON(
				utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-05.json",
						getClass()),
				JsonNode.class), OK));

		// get system devices for data validation
		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
				.queryParam("WithDeactivatedDevices", true).buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class),
				OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// lookup previous datum for first datum in result set
		final Instant firstDatumTs = LocalDateTime.parse("2025-03-28T06:50:00").atZone(systemTimeZone)
				.toInstant();
		final Instant prevDatumTs = LocalDateTime.parse("2025-03-01T06:50:00").atZone(systemTimeZone)
				.toInstant();
		final var prevDatum = new DatumEntity(new DatumPK(UUID.randomUUID(), prevDatumTs), null,
				new DatumProperties());
		given(datumDao.findFiltered(any()))
				.willReturn(
						new BasicObjectDatumStreamFilterResults<>(
								Map.of(prevDatum.streamId(),
										emptyMeta(prevDatum.streamId(), inv1SourceId,
												datumStream.getKind(), nodeId, inv1SourceId)),
								List.of(prevDatum)));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final Instant failedTs1 = LocalDateTime.parse("2025-03-28T07:05:00").atZone(systemTimeZone).toInstant();
		final Integer failedValue1 = 101010;

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		then(datumDao).should().findFiltered(datumCriteriaCaptor.capture());
		and.then(datumCriteriaCaptor.getValue())
			.as("Prev datum query is for most recent")
			.returns(true, from(DatumCriteria::isMostRecent))
			.as("Prev datum query end date is first datum timestamp")
			.returns(firstDatumTs, from(DatumCriteria::getEndDate))
			.as("Prev datum query is for CloudDatumStream kind")
			.returns(datumStream.getKind(), from(DatumCriteria::getObjectKind))
			.as("Prev datum query is for CloudDatumStream object (node) ID")
			.returns(datumStream.getObjectId(), from(DatumCriteria::getNodeId))
			.as("Prev datum query is for inverter 1 source ID")
			.returns(inv1SourceId, from(DatumCriteria::getSourceId))
			;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(5)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T06:50:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 10782938), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("""
						Datum %d has expected sample data, wh passed validation because previous datum
						timestamp is far in past, boosting the maximum expected energy very high.
						""", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 2;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T07:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 101), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Datum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;

				final int index3 = 3;
				final DatumSamples expectedSamples3 = new DatumSamples(Map.of(
						"wh", failedValue1), null, null);

				and.then(list).element(index3, type(GeneralDatum.class))
					.as("Datum %d has expected date", index3)
					.returns(failedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index3)
					.returns(expectedSamples3, from(GeneralDatum::getSamples))
					;

				final int index4 = 4;
				final Instant expectedTs4 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples4 = new DatumSamples(Map.of(
						"wh", 600), null, null);

				and.then(list).element(index4, type(GeneralDatum.class))
					.as("Datum %d has expected date", index4)
					.returns(expectedTs4, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh passed validation because > expected max < 2x max", index4)
					.returns(expectedSamples4, from(GeneralDatum::getSamples))
					;
			})
			;

		then(userEventAppenderBiz).should(times(4)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		var events = eventCaptor.getAllValues();
		and.then(events.get(3))
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/18/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(1).toString(),
					inv1SourceId,
					failedTs1,
					failedValue1,
					300000,
					service.getEnergyValidationThreshold(),
					3000.0,
					3600
					))
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_invalidData_ignoreValidation() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "18"; // 3.6kw inverter from sma-devices-01.json

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						),
				CloudDatumStreamService.VALIDATION_IGNORE_SETTING, List.of(
						CommonValidationType.EnergySpike.getKey()
						)
				));
		// @formatter:on

		// configure expected HTTP responses

		// HTTP request system time zone info (first check cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York"); // from sma-plant-01.json

		given(systemTimeZoneCache.get(systemId)).willReturn(null, systemTimeZone);

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		expectedUris.add(fromUri(BASE_URI).path(SmaCloudDatumStreamService.SYSTEM_VIEW_PATH_TEMPLATE)
				.buildAndExpand(systemId).toUri());
		responses.add(new ResponseEntity<>(
				getObjectFromJSON(utf8StringResource("sma-plant-01.json", getClass()), JsonNode.class),
				OK));

		// HTTP request measurement set data for each day in filter range, per device per measurement set

		final LocalDate startDay = LocalDate.parse("2025-03-28").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(getObjectFromJSON(
				utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-05.json",
						getClass()),
				JsonNode.class), OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		// cache system time zone
		then(systemTimeZoneCache).should().put(systemId, systemTimeZone);

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(5)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final int index1 = 0;
				final Instant expectedTs1 = LocalDateTime.parse("2025-03-28T06:50:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples1 = new DatumSamples(Map.of(
						"wh", 10782938), null, null);

				and.then(list).element(index1, type(GeneralDatum.class))
					.as("Datum %d has expected date", index1)
					.returns(expectedTs1, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh validation ignored", index1)
					.returns(expectedSamples1, from(GeneralDatum::getSamples))
					;

				final int index2 = 2;
				final Instant expectedTs2 = LocalDateTime.parse("2025-03-28T07:00:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples2 = new DatumSamples(Map.of(
						"wh", 101), null, null);

				and.then(list).element(index2, type(GeneralDatum.class))
					.as("Datum %d has expected date", index2)
					.returns(expectedTs2, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data", index2)
					.returns(expectedSamples2, from(GeneralDatum::getSamples))
					;

				final int index3 = 3;
				final Instant expectedTs3 = LocalDateTime.parse("2025-03-28T07:05:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples3 = new DatumSamples(Map.of(
						"wh", 101010), null, null);

				and.then(list).element(index3, type(GeneralDatum.class))
					.as("Datum %d has expected date", index3)
					.returns(expectedTs3, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh validation ignored", index3)
					.returns(expectedSamples3, from(GeneralDatum::getSamples))
					;

				final int index4 = 4;
				final Instant expectedTs4 = LocalDateTime.parse("2025-03-28T07:10:00").atZone(systemTimeZone).toInstant();
				final DatumSamples expectedSamples4 = new DatumSamples(Map.of(
						"wh", 600), null, null);

				and.then(list).element(index4, type(GeneralDatum.class))
					.as("Datum %d has expected date", index4)
					.returns(expectedTs4, from(Datum::getTimestamp))
					.as("Datum %d has expected sample data, wh validation ignored", index4)
					.returns(expectedSamples4, from(GeneralDatum::getSamples))
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void datum_invalidData_timeJumps() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomString();
		final String deviceId = "16"; // 6kw inverter from sma-devices-01.json

		service.setEnergyValidationThreshold(2.0);

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OAUTH_CLIENT_ID_SETTING, clientId,
				OAUTH_CLIENT_SECRET_SETTING, clientSecret,
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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "wh", Reference,
				placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s".formatted(systemId, deviceId), inv1SourceId
						)
				));
		// @formatter:on

		// get system time zone info (from cache)

		final ZoneId systemTimeZone = ZoneId.of("America/New_York");

		given(systemTimeZoneCache.get(systemId)).willReturn(systemTimeZone);

		// get system devices for data validation
		given(systemInventoryCache.get(systemId)).willReturn(service
				.parseSystemDevices(integration, getObjectFromJSON(
						utf8StringResource("sma-devices-01.json", getClass()), JsonNode.class), systemId)
				.toArray(CloudDataValue[]::new));

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		// HTTP request measurement set data for day

		final LocalDate startDay = LocalDate.parse("2025-07-04").atStartOfDay(systemTimeZone)
				.toLocalDate();

		final UriComponentsBuilder b = fromUri(BASE_URI)
				.path(SmaCloudDatumStreamService.DEVICE_MEASUREMENT_DATA_PATH_TEMPALTE)
				.queryParam(SmaCloudDatumStreamService.DATE_PARAM, startDay.toString());

		expectedUris.add(b
				.queryParam(SmaCloudDatumStreamService.RETURN_ENERGY_VALUES_PARAM,
						prop1MeasuermentSet.shouldReturnEnergyValues())
				.buildAndExpand(deviceId, prop1MeasuermentSet.getKey(), SmaPeriod.Day.getKey()).toUri());
		responses.add(new ResponseEntity<>(getObjectFromJSON(
				utf8StringResource("sma-device-data-Day-EnergyAndPowerPv-ReturnEnergyValues-06.json",
						getClass()),
				JsonNode.class), OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDay.atStartOfDay(systemTimeZone).toInstant());
		filter.setEndDate(startDay.atStartOfDay(systemTimeZone).plusDays(1).toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		then(restOps).should(times(expectedUris.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.extracting(RequestEntity::getUrl)
			.containsExactlyElementsOf(expectedUris)
			;

		final Instant failedTs1 = LocalDateTime.parse("2025-07-04T05:25:00").atZone(systemTimeZone).toInstant();
		final Integer failedValue1 = 165763;
		final Instant failedTs2 = LocalDateTime.parse("2025-07-04T12:50:00").atZone(systemTimeZone).toInstant();
		final Integer failedValue2 = 1132;

		and.then(result)
			.as("Datum for entire day parsed from HTTP responses")
			.hasSize(166)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				and.then(list).element(0, type(GeneralDatum.class))
					.as("Datum 0 has expected date")
					.returns(failedTs1, from(Datum::getTimestamp))
					.as("Datum 0 has expected sample data, wh failed validation and set to 0")
					.returns(new DatumSamples(Map.of(
							"wh", failedValue1), null, null), from(GeneralDatum::getSamples))
					;

				and.then(list).element(57, type(GeneralDatum.class))
					.as("Datum 57 has expected date")
					.returns(LocalDateTime.parse("2025-07-04T10:30:00").atZone(systemTimeZone).toInstant(), from(Datum::getTimestamp))
					.as("Datum 57 has expected sample data, wh passed validation because time diff is 10min from prev datum")
					.returns(new DatumSamples(Map.of(
							"wh", 1353), null, null), from(GeneralDatum::getSamples))
					;

				and.then(list).element(81, type(GeneralDatum.class))
					.as("Datum 81 has expected date")
					.returns(failedTs2, from(Datum::getTimestamp))
					.as("Datum 81 has expected sample data, wh failed validation and set to 0")
					.returns(new DatumSamples(Map.of(
							"wh", failedValue2), null, null), from(GeneralDatum::getSamples))
					;
			})
			;
		then(userEventAppenderBiz).should(times(5)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		var events = eventCaptor.getAllValues();
		and.then(events.get(1))
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/16/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(0).toString(),
					inv1SourceId,
					failedTs1,
					failedValue1,
					300000,
					service.getEnergyValidationThreshold(),
					1000.0,
					6000
					))
			;
		and.then(events.get(2))
			.as("Event tags for control instructions")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(dataValidationEventData(datumStream,
					"/%s/16/EnergyAndPowerPv/pvGeneration".formatted(systemId),
					expectedUris.get(0).toString(),
					inv1SourceId,
					failedTs2,
					failedValue2,
					300000,
					service.getEnergyValidationThreshold(),
					1000.0,
					6000
					))
			;
		// @formatter:on
	}

}
