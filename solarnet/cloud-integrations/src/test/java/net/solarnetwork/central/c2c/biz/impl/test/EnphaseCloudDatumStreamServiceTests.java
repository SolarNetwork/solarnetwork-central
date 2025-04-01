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
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService.END_AT_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService.GRANULARITY_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService.START_AT_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService.SYSTEM_DEVICE_ID;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService.MAX_PAGE_SIZE;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseDeviceType.Meter;
import static net.solarnetwork.central.c2c.biz.impl.EnphaseGranularity.FifteenMinute;
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
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.EnphaseDeviceType;
import net.solarnetwork.central.c2c.biz.impl.EnphaseGranularity;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

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
	private SolarNodeOwnershipDao nodeOwnershipDao;

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
	public void dataValues_root() {
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
				.path(EnphaseCloudIntegrationService.LIST_SYSTEMS_PATH)
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

	private String refValue(List<String> idents) {
		return idents.stream().collect(Collectors.joining("/", "/", ""));
	}

	@Test
	public void dataValues_system() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final String systemId = randomLong().toString();

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

		final URI listSystemDevices = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.SYSTEM_DEVICES_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.buildAndExpand(systemId).toUri();

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-devices-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(eq(listSystemDevices), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(res);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(EnphaseCloudDatumStreamService.SYSTEM_ID_FILTER, systemId));

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
			.as("Result generated for system")
			.hasSize(10)
			.satisfies(l -> {
				var siteInvWIdents = List.of(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID, "W");
				and.then(l).element(0)
					.as("Inverter power")
					.returns("Active power", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteInvWIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteInvWIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteInvWhIdents = List.of(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID, "Wh");
				and.then(l).element(1)
					.as("Inverter energy")
					.returns("Active energy", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteInvWhIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteInvWhIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetWIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "W");
				and.then(l).element(2)
					.as("Meter power")
					.returns("Active power", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetWIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetWIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetWhExpIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "WhExp");
				and.then(l).element(3)
					.as("Meter energy")
					.returns("Active energy exported", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetWhExpIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetWhExpIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWAIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWA");
				and.then(l).element(4)
					.as("Meter phase power A")
					.returns("Phase active power - A", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWAIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWAIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWBIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWB");
				and.then(l).element(5)
					.as("Meter phase power B")
					.returns("Phase active power - B", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWBIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWBIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWCIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWC");
				and.then(l).element(6)
					.as("Meter phase power C")
					.returns("Phase active power - C", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWCIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWCIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWhExpAIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpA");
				and.then(l).element(7)
					.as("Meter phase energy A")
					.returns("Phase active energy exported - A", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWhExpAIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWhExpAIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWhExpBIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpB");
				and.then(l).element(8)
					.as("Meter phase energy B")
					.returns("Phase active energy exported - B", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWhExpBIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWhExpBIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
				var siteMetPhaseWhExpCIdents = List.of(systemId, Meter.getKey(), SYSTEM_DEVICE_ID, "PWhExpC");
				and.then(l).element(9)
					.as("Meter phase energy C")
					.returns("Phase active energy exported - C", from(CloudDataValue::getName))
					.as("Identifiers provided")
					.returns(siteMetPhaseWhExpCIdents, from(CloudDataValue::getIdentifiers))
					.as("Reference provided")
					.returns(refValue(siteMetPhaseWhExpCIdents), from(CloudDataValue::getReference))
					.as("No metadata provided")
					.returns(null, from(CloudDataValue::getMetadata))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					;
			})
			;
		// @formatter:on
	}

	private static String systemComponentValueRef(Long systemId, EnphaseDeviceType type,
			String fieldName) {
		return "/%d/%s/%s/%s".formatted(systemId, type.getKey(), SYSTEM_DEVICE_ID, fieldName);
	}

	private static String systemPlaceholderComponentValueRef(EnphaseDeviceType type, String fieldName) {
		return "/{systemId}/%s/%s/%s".formatted(type.getKey(), SYSTEM_DEVICE_ID, fieldName);
	}

	@Test
	public void datum_systemOnly_inverter() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemComponentValueRef(systemId, Inverter, fieldName1));

		final String fieldName2 = "Wh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("wh");
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(systemComponentValueRef(systemId, Inverter, fieldName2));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-inverter-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1738420800L));
		filter.setEndDate(Instant.ofEpochSecond(1738423200L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.INVERTER_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(GRANULARITY_PARAM, EnphaseGranularity.forQueryDateRange(filter.getStartDate(), filter.getEndDate()).getKey())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for inverter telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		String expectedSourceId = datumStream.getSourceId() + "/%d/%s/%s".formatted(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID);
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(expectedSourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("watts", 744);
				expectedSamples1.putInstantaneousSampleValue("wh", 62);

				and.then(list)
					.element(0)
					.as("Datum timestampfrom JSON response")
					.returns(Instant.ofEpochSecond(1738421700), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", 1020);
				expectedSamples2.putInstantaneousSampleValue("wh", 85);

				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.ofEpochSecond(1738422000), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", 1668);
				expectedSamples3.putInstantaneousSampleValue("wh", 139);

				and.then(list)
					.element(2)
					.as("Datum timestamp from JSON response")
					.returns(Instant.ofEpochSecond(1738422300), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;
			})
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is 15min tick of filter start date")
			.returns(FifteenMinute.tickStart(filter.getStartDate(), UTC), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is 15min tick of filter end date")
			.returns(FifteenMinute.tickStart(filter.getEndDate(), UTC), from(DateRangeCriteria::getEndDate))
			;

		and.then(result.getNextQueryFilter())
			.as("Next query filter not provided")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void datum_systemOnly_inverter_mappedSourceId() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		final String inv1SourceId = "inv/1";

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemComponentValueRef(systemId, Inverter, fieldName1));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%d/%s/%s".formatted(systemId, EnphaseDeviceType.Inverter.getKey(), SYSTEM_DEVICE_ID), inv1SourceId
						)
				));
		// @formatter:on

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-inverter-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1738420800L));
		filter.setEndDate(Instant.ofEpochSecond(1738423200L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.INVERTER_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(GRANULARITY_PARAM, EnphaseGranularity.forQueryDateRange(filter.getStartDate(), filter.getEndDate()).getKey())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for inverter telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
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
			;
		// @formatter:on

	}

	@Test
	public void datum_systemOnly_inverter_systemIdPlaceholder() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemPlaceholderComponentValueRef(Inverter, fieldName1));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		datumStream.setServiceProps(
				Map.of(CloudDatumStreamMappingConfiguration.PLACEHOLDERS_SERVICE_PROPERTY,
						Map.of(EnphaseCloudDatumStreamService.SYSTEM_ID_FILTER, systemId)));

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-inverter-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1738420800L));
		filter.setEndDate(Instant.ofEpochSecond(1738423200L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.INVERTER_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(GRANULARITY_PARAM, EnphaseGranularity.forQueryDateRange(filter.getStartDate(), filter.getEndDate()).getKey())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for inverter telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		String expectedSourceId = datumStream.getSourceId() + "/%d/%s/%s".formatted(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID);
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(expectedSourceId, from(Datum::getSourceId))
					;
			})
			;
		// @formatter:on

	}

	@Test
	public void datum_systemOnly_inverter_upperCase() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemPlaceholderComponentValueRef(Inverter, fieldName1));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		datumStream.setServiceProps(Map.of(EnphaseCloudDatumStreamService.UPPER_CASE_SOURCE_ID_SETTING,
				true, CloudDatumStreamMappingConfiguration.PLACEHOLDERS_SERVICE_PROPERTY,
				Map.of(EnphaseCloudDatumStreamService.SYSTEM_ID_FILTER, systemId)));

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-inverter-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1738420800L));
		filter.setEndDate(Instant.ofEpochSecond(1738423200L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.INVERTER_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(GRANULARITY_PARAM, EnphaseGranularity.forQueryDateRange(filter.getStartDate(), filter.getEndDate()).getKey())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for inverter telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		String expectedSourceId = (datumStream.getSourceId() + "/%d/%s/%s".formatted(systemId, Inverter.getKey(), SYSTEM_DEVICE_ID)).toUpperCase();
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(expectedSourceId, from(Datum::getSourceId))
					;
			})
			;
		// @formatter:on

	}

	@Test
	public void datum_systemOnly_inverter_lastReportAtBeforeQueryEndDate() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemComponentValueRef(systemId, Inverter, fieldName1));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-inverter-02.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1738420800L));
		filter.setEndDate(Instant.ofEpochSecond(1738423200L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.INVERTER_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(GRANULARITY_PARAM, EnphaseGranularity.forQueryDateRange(filter.getStartDate(), filter.getEndDate()).getKey())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for inverter telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is 15min tick of filter start date")
			.returns(FifteenMinute.tickStart(filter.getStartDate(), UTC), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is 15min tick of filter end date")
			.returns(FifteenMinute.tickStart(filter.getEndDate(), UTC), from(DateRangeCriteria::getEndDate))
			;

		and.then(result.getNextQueryFilter())
			.as("Next query filter provided")
			.isNotNull()
			.as("Next query start date is last_report_at value from JSON")
			.returns(Instant.ofEpochSecond(1738422360L), from(DateRangeCriteria::getStartDate))
			.as("Next query end date is not provided")
			.returns(null, from(DateRangeCriteria::getEndDate))
			;
		// @formatter:on
	}

	@Test
	public void datum_systemOnly_meter() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String apiKey = randomString();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String accessToken = randomString();
		final String refreshToken = randomString();
		final Long systemId = randomLong();

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

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "W";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemComponentValueRef(systemId, Meter, fieldName1));

		final String fieldName2 = "WhExp";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("wh");
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(systemComponentValueRef(systemId, Meter, fieldName2));

		final String fieldName3 = "PWA";
		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop3.setEnabled(true);
		prop3.setPropertyType(DatumSamplesType.Instantaneous);
		prop3.setPropertyName("watts_a");
		prop3.setScale(0);
		prop3.setValueType(CloudDatumStreamValueType.Reference);
		prop3.setValueReference(systemComponentValueRef(systemId, Meter, fieldName3));

		final String fieldName4 = "PWB";
		final CloudDatumStreamPropertyConfiguration prop4 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop4.setEnabled(true);
		prop4.setPropertyType(DatumSamplesType.Instantaneous);
		prop4.setPropertyName("watts_b");
		prop4.setScale(0);
		prop4.setValueType(CloudDatumStreamValueType.Reference);
		prop4.setValueReference(systemComponentValueRef(systemId, Meter, fieldName4));

		final String fieldName5 = "PWC";
		final CloudDatumStreamPropertyConfiguration prop5 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop5.setEnabled(true);
		prop5.setPropertyType(DatumSamplesType.Instantaneous);
		prop5.setPropertyName("watts_c");
		prop5.setScale(0);
		prop5.setValueType(CloudDatumStreamValueType.Reference);
		prop5.setValueReference(systemComponentValueRef(systemId, Meter, fieldName5));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3, prop4, prop5));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("enphase-system-telemetry-rgm-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.ofEpochSecond(1741087800L));
		filter.setEndDate(Instant.ofEpochSecond(1741118400L));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		// request inverter data
		final URI listSystemInverterTelemetry = UriComponentsBuilder
				.fromUri(EnphaseCloudIntegrationService.BASE_URI)
				.path(EnphaseCloudDatumStreamService.RGM_TELEMETRY_PATH_TEMPLATE)
				.queryParam(EnphaseCloudIntegrationService.API_KEY_PARAM, apiKey)
				.queryParam(START_AT_PARAM, FifteenMinute.tickStart(filter.getStartDate(), UTC).getEpochSecond())
				.queryParam(END_AT_PARAM, FifteenMinute.tickStart(filter.getEndDate(), UTC).getEpochSecond())
				.buildAndExpand(systemId).toUri();

		and.then(uriCaptor.getValue())
			.as("Request URI for RGM telemetry")
			.isEqualTo(listSystemInverterTelemetry)
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		String expectedSourceId = datumStream.getSourceId() + "/%d/%s/%s".formatted(systemId, Meter.getKey(), SYSTEM_DEVICE_ID);
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(34)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(expectedSourceId, from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("watts", (52 + 49 + 45) / 3);
				expectedSamples1.putInstantaneousSampleValue("watts_a", 52);
				expectedSamples1.putInstantaneousSampleValue("watts_b", 49);
				expectedSamples1.putInstantaneousSampleValue("watts_c", 45);
				expectedSamples1.putInstantaneousSampleValue("wh", 21);

				and.then(list)
					.element(2)
					.as("Datum timestampfrom JSON response")
					.returns(Instant.ofEpochSecond(1741090500L), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", (140 + 137) / 2);
				expectedSamples2.putInstantaneousSampleValue("watts_a", 140);
				expectedSamples2.putInstantaneousSampleValue("watts_b", 137);
				expectedSamples2.putInstantaneousSampleValue("wh", 44);

				and.then(list)
					.element(3)
					.as("Datum timestamp from JSON response")
					.returns(Instant.ofEpochSecond(1741091400L), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", (1244 + 1244) / 2);
				expectedSamples3.putInstantaneousSampleValue("watts_a", 1244);
				expectedSamples3.putInstantaneousSampleValue("watts_b", 1244);
				expectedSamples3.putInstantaneousSampleValue("wh", 544);

				and.then(list)
					.element(33)
					.as("Datum timestamp from JSON response")
					.returns(Instant.ofEpochSecond(1741118400L), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;
			})
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is 15min tick of filter start date")
			.returns(FifteenMinute.tickStart(filter.getStartDate(), UTC), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is 15min tick of filter end date")
			.returns(FifteenMinute.tickStart(filter.getEndDate(), UTC), from(DateRangeCriteria::getEndDate))
			;

		and.then(result.getNextQueryFilter())
			.as("Next query filter not provided")
			.isNull()
			;
		// @formatter:on
	}

}
