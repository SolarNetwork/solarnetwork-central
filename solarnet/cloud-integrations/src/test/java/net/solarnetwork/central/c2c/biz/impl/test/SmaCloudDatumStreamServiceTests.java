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
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.SOURCE_ID_MAP_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
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
import java.util.SequencedMap;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SmaCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SmaCloudDatumStreamServiceTests {

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

	@Mock
	private Cache<String, ZoneId> systemTimeZoneCache;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

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
				randomLong(), now());
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
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
					.path(SmaCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE).buildAndExpand(systemId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		final SmaMeasurementSetType prop2MeasuermentSet = SmaMeasurementSetType.PowerAc;
		final String prop2MeasurementName = "activePower";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("watts");
		prop2.setScale(1);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(placeholderValueRef(prop2MeasuermentSet, prop2MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.PowerDc;
		final String prop1MeasurementName = "dcPowerInput";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("dcPower");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping properties, for energy and power

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping property

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
			.satisfies(r -> {
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
				randomLong(), now());
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
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream mapping property

		final SmaMeasurementSetType prop1MeasuermentSet = SmaMeasurementSetType.EnergyAndPowerPv;
		final String prop1MeasurementName = "pvGeneration";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderValueRef(prop1MeasuermentSet, prop1MeasurementName));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream

		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
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
					.extracting(RequestEntity::getHeaders, map(String.class, List.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
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
			.satisfies(r -> {
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

}
