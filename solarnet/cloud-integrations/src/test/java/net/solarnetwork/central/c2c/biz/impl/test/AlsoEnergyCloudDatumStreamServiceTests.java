/* ==================================================================
 * AlsoEnergyCloudDatumStreamServiceTests.java - 24/11/2024 6:36:48 am
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
import static java.time.temporal.ChronoUnit.MINUTES;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService.BIN_DATA_URL;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService.SITE_HARDWARE_URL_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction.Avg;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction.Last;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.math.BigDecimal;
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
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link AlsoEnergyCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AlsoEnergyCloudDatumStreamServiceTests {

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

	private AlsoEnergyCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new AlsoEnergyCloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, oauthClientManager, clock, null);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(AlsoEnergyCloudIntegrationService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	private static String componentValueRef(Long siteId, Long hardwareId, String fieldName, String fn) {
		return "/%d/%d/%s/%s".formatted(siteId, hardwareId, fieldName, fn);
	}

	@Test
	public void dataValues_site() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// @formatter:off
		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
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

		// request data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("alsoenergy-hardware-01.json", getClass()), ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of("siteId", siteId));

		// THEN
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(),
				eq(JsonNode.class));

		and.then(uriCaptor.getValue()).as("Request URI").isEqualTo(
				BASE_URI.resolve(SITE_HARDWARE_URL_TEMPLATE.replace("{siteId}", siteId.toString())
						+ "?includeArchivedFields=true&includeDeviceConfig=true"));

		// @formatter:off
		and.then(results)
			.as("Results provided")
			.hasSize(2)
			.satisfies(devices -> {
				and.then(devices)
					.element(0)
					.as("Identifiers from response")
					.returns(List.of(siteId.toString(), "12345"), from(CloudDataValue::getIdentifiers))
					.as("Device name from response")
					.returns("Elkor Production Meter", from(CloudDataValue::getName))
					.as("No reference for device object")
					.returns(null, from(CloudDataValue::getReference))
					.as("Metadata from response")
					.returns(Map.of("functionCode", "PM"
							, "serial", "20647"
							, "deviceType", "ProductionPowerMeter"
							), from(CloudDataValue::getMetadata))
					.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
					.as("Has 2 field children")
					.hasSize(2)
					.satisfies(fields -> {
						and.then(fields)
							.element(0)
							.as("Identifiers from response")
							.returns(List.of(siteId.toString(), "12345", "KW"), from(CloudDataValue::getIdentifiers))
							.as("Field name from response")
							.returns("KW", from(CloudDataValue::getName))
							.as("No reference for field object")
							.returns(null, from(CloudDataValue::getReference))
							.as("No metadata for field object")
							.returns(null, from(CloudDataValue::getMetadata))
							.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
							.as("Has field function children")
							.hasSize(AlsoEnergyFieldFunction.values().length)
							.satisfies(functions -> {
								for ( int i =0, len = AlsoEnergyFieldFunction.values().length; i < len; i++ ) {
									var fn = AlsoEnergyFieldFunction.values()[i];
									and.then(functions).element(i)
										.as("Identifiers from response")
										.returns(List.of(siteId.toString(), "12345", "KW", fn.name()),
												from(CloudDataValue::getIdentifiers))
										.as("Function name is field + function")
										.returns("KW %s".formatted(fn.name()), from(CloudDataValue::getName))
										.as("Feference for function object")
										.returns("/%s/12345/KW/%s".formatted(siteId, fn.name()),
												from(CloudDataValue::getReference))
										.as("No metadata for function")
										.returns(null, CloudDataValue::getMetadata)
										.as("No children for function")
										.returns(null, CloudDataValue::getChildren)
										;
								}
							})
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_singleComponent() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(siteId, hardwareId, fieldName2, Last.name()));

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

		// @formatter:off
		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
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

		// request data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-01.json", getClass()), ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.POST), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(httpEntityCaptor.getValue())
			.as("HTTP request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(HttpEntity::getBody))
			;

		String expectedSourceId = datumStream.getSourceId() + "/%s/%s".formatted(siteId, hardwareId);
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(2)
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
				expectedSamples1.putInstantaneousSampleValue("watts", 123400);
				expectedSamples1.putAccumulatingSampleValue("wattHours", 438201);

				and.then(list)
					.element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-11-21T10:00:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", 234500);
				expectedSamples2.putAccumulatingSampleValue("wattHours", 438301);

				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-11-21T10:01:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_204NoContentResponse() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(siteId, hardwareId, fieldName2, Last.name()));

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

		// @formatter:off
		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
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

		// request data
		final var res = new ResponseEntity<JsonNode>(HttpStatus.NO_CONTENT);
		given(restOps.exchange(any(), eq(HttpMethod.POST), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(httpEntityCaptor.getValue())
			.as("HTTP request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(HttpEntity::getBody))
			;

		and.then(result)
			.as("Empty result from HTTP 204 response")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void requestList_NaN() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(siteId, hardwareId, fieldName2, Last.name()));

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

		// @formatter:off
		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
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

		// request data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-02.json", getClass()), ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.POST), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(httpEntityCaptor.getValue())
			.as("HTTP request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(HttpEntity::getBody))
			;

		String expectedSourceId = datumStream.getSourceId() + "/%s/%s".formatted(siteId, hardwareId);
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
				expectedSamples1.putInstantaneousSampleValue("watts", -964);
				expectedSamples1.putAccumulatingSampleValue("wattHours", 103684305);

				and.then(list)
					.element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:51:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", -698);
				// NOTE: NaN wattHours ignored

				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:52:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", -740);
				expectedSamples3.putAccumulatingSampleValue("wattHours", 103684430);

				and.then(list)
					.element(2)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:53:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestList_offsetExpression() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(siteId, hardwareId, fieldName2, Last.name()));

		final String fieldName3 = "diff";
		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop3.setEnabled(true);
		prop3.setPropertyType(DatumSamplesType.Instantaneous);
		prop3.setPropertyName(fieldName3);
		prop3.setValueType(CloudDatumStreamValueType.SpelExpression);
		prop3.setValueReference("""
				hasOffset(1, timestamp) && offset(1, timestamp).props['diff'] != null
				? (offset(1, timestamp).diff * 2)
				: 123""");

		final String fieldName4 = "tdiff";
		final CloudDatumStreamPropertyConfiguration prop4 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop4.setEnabled(true);
		prop4.setPropertyType(DatumSamplesType.Instantaneous);
		prop4.setPropertyName(fieldName4);
		prop4.setValueType(CloudDatumStreamValueType.SpelExpression);
		prop4.setValueReference("secondsBetween(offset(1, timestamp).timestamp, timestamp)");

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3, prop4));

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
		@SuppressWarnings("removal")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
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

		// request data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-03.json", getClass()), ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.POST), any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("watts", 123);
				expectedSamples1.putInstantaneousSampleValue("diff", 123); // expression default outcome
				// NOTE: no tdiff because no offset(1) available on first datum
				expectedSamples1.putAccumulatingSampleValue("wattHours", 100);

				and.then(list)
					.element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:51:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", 124);
				expectedSamples2.putInstantaneousSampleValue("diff", 246);
				expectedSamples2.putInstantaneousSampleValue("tdiff", 60);
				expectedSamples2.putAccumulatingSampleValue("wattHours", 101);

				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:52:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", 122);
				expectedSamples3.putInstantaneousSampleValue("diff", 492);
				expectedSamples3.putInstantaneousSampleValue("tdiff", 60);
				expectedSamples3.putAccumulatingSampleValue("wattHours", 102);

				and.then(list)
					.element(2)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:53:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

}
