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
import static java.util.Map.entry;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService.BIN_DATA_URL;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService.SITE_HARDWARE_URL_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction.Avg;
import static net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction.Last;
import static net.solarnetwork.central.c2c.biz.impl.test.CloudIntegrationTestUtils.timeGapValidationMetadata;
import static net.solarnetwork.central.c2c.biz.impl.test.CloudIntegrationTestUtils.timeGapValidationPropertyMetadata;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.Reference;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.SpelExpression;
import static net.solarnetwork.central.datum.domain.DatumValidationType.TIME_GAP_VALIDATION_TYPE;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.jackson.JsonUtils.getObjectFromJSON;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_OPT_MILLIS_UTC;
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
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyFieldFunction;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyGranularity;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.http.OAuth2Utils;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test cases for the {@link AlsoEnergyCloudDatumStreamService} class.
 *
 * @author matt
 * @version 2.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class AlsoEnergyCloudDatumStreamServiceTests implements CloudIntegrationsUserEvents {

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

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Mock
	private DatumEntityDao datumDao;

	@Captor
	private ArgumentCaptor<DatumCriteria> datumCriteriaCaptor;

	@Mock
	private Cache<Long, CloudDataValue[]> siteInventoryCache;

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

		service.setSiteInventoryCache(siteInventoryCache);

	}

	private static String componentValueRef(Long siteId, Long hardwareId, String fieldName, String fn) {
		return "/%d/%d/%s/%s".formatted(siteId, hardwareId, fieldName, fn);
	}

	private static String componentValueRef(String fieldName, String fn) {
		return "/{siteId}/{hardwareId}/%s/%s".formatted(fieldName, fn);
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
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				AlsoEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				AlsoEnergyCloudIntegrationService.USERNAME_SETTING, username,
				AlsoEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of("siteId", siteId));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(UriComponentsBuilder.fromUri(BASE_URI)
					.path(SITE_HARDWARE_URL_TEMPLATE)
					.queryParam(AlsoEnergyCloudDatumStreamService.INCLUDE_ARCHIVED_FIELDS_PARAM, true)
					.queryParam(AlsoEnergyCloudDatumStreamService.INCLUDE_DEVICE_CONFIG_PARAM, true)
					.queryParam(AlsoEnergyCloudDatumStreamService.INCLUDE_DISABLED_HARDWARE_PARAM, true)
					.buildAndExpand(siteId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(results)
			.as("Results provided")
			.hasSize(2)
			.satisfies(devices -> {
				and.then(devices).element(0)
					.as("Identifiers from response")
					.returns(List.of(siteId.toString(), "12345"), from(CloudDataValue::getIdentifiers))
					.as("Device name from response")
					.returns("Elkor Production Meter", from(CloudDataValue::getName))
					.as("No reference for device object")
					.returns(null, from(CloudDataValue::getReference))
					.as("Metadata from response")
					.returns(Map.of("functionCode", "PM"
							, CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, "20647"
							, "deviceType", "ProductionPowerMeter"
							, CloudDataValue.ACTIVE_METADATA, true
							, "flags", Set.of("IsEnabled")
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
				and.then(devices).element(1)
					.as("Identifiers from response")
					.returns(List.of(siteId.toString(), "23456"), from(CloudDataValue::getIdentifiers))
					.as("Device name from response")
					.returns("SolarEdge SE100K Inverter", from(CloudDataValue::getName))
					.as("No reference for device object")
					.returns(null, from(CloudDataValue::getReference))
					.as("Metadata from response")
					.returns(Map.of("functionCode", "PV"
							, "deviceType", "Inverter"
							, CloudDataValue.ACTIVE_METADATA, true
							, "flags", Set.of("IsEnabled")
							, CloudDataValue.RELATED_IDENTIFIER_METADATA, 12345L
							, CloudDataValue.RATED_POWER_METADATA, 100000.0
							, CloudDataValue.AZIMUTH_METADATA, 45
							, CloudDataValue.TILT_METADATA, 10
							), from(CloudDataValue::getMetadata))
					.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
					.as("Has 2 field children")
					.hasSize(2)
					.satisfies(fields -> {
						and.then(fields)
							.element(0)
							.as("Identifiers from response")
							.returns(List.of(siteId.toString(), "23456", "KwAC"), from(CloudDataValue::getIdentifiers))
							.as("Field name from response")
							.returns("KwAC", from(CloudDataValue::getName))
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
										.returns(List.of(siteId.toString(), "23456", "KwAC", fn.name()),
												from(CloudDataValue::getIdentifiers))
										.as("Function name is field + function")
										.returns("KwAC %s".formatted(fn.name()), from(CloudDataValue::getName))
										.as("Feference for function object")
										.returns("/%s/23456/KwAC/%s".formatted(siteId, fn.name()),
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
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(siteId, hardwareId, fieldName2, Last.name()));
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)), from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
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
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)), from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name())
				), from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
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
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(siteId, hardwareId, fieldName2, Last.name()));
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)), from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
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
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(siteId, hardwareId, fieldName2, Last.name()));
		prop2.setEnabled(true);

		final String fieldName3 = "diff";
		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Instantaneous, fieldName3, SpelExpression,
				"""
						hasOffset(1, timestamp) && offset(1, timestamp).props['diff'] != null
						? (offset(1, timestamp).diff * 2)
						: 123""");
		prop3.setEnabled(true);

		final String fieldName4 = "tdiff";
		final CloudDatumStreamPropertyConfiguration prop4 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Instantaneous, fieldName4, SpelExpression,
				"secondsBetween(offset(1, timestamp).timestamp, timestamp)");
		prop4.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3, prop4));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// set clock to near data request, to work with maximum lag setting (default 3h)
		clock.setInstant(Instant.parse("2024-12-30T22:00:00Z"));

		// WHEN
		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(BASE_URI.resolve(BIN_DATA_URL
					+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
							ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
							)), from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				), from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

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

		and.then(result)
			.asInstanceOf(type(CloudDatumStreamQueryResult.class))
			.extracting(CloudDatumStreamQueryResult::getNextQueryFilter)
			.as("Next query filter returned")
			.isNotNull()
			.as("21:54 returned, as +1min after the latest timestamps in the stream")
			.returns(Instant.parse("2024-12-30T21:54:00Z"), from(CloudDatumStreamQueryFilter::getStartDate))
			;

		// @formatter:on
	}

	@Test
	public void requestList_startAtLeastGreatestTimestampPerStream_withinLag() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId1 = randomLong();
		final Long hardwareId1 = randomLong();
		final Long siteId2 = randomLong();
		final Long hardwareId2 = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(fieldName1, Avg.name()));
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(fieldName2, Last.name()));
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(JsonUtils.getStringMap("""
						{
						  "sourceIdMap": {
						    "/%s/%s":  "inv/1",
						    "/%s/%s": "inv/2"
						  }
						}
						""".formatted(siteId1, hardwareId1, siteId2, hardwareId2)));
		// @formatter:on

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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

		// request data streams
		final JsonNode resJson1 = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-03.json", getClass()), ObjectNode.class);
		final var res1 = new ResponseEntity<JsonNode>(resJson1, HttpStatus.OK);
		final JsonNode resJson2 = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-04.json", getClass()), ObjectNode.class);
		final var res2 = new ResponseEntity<JsonNode>(resJson2, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res1).willReturn(res2);

		// get site devices for data validation
		given(siteInventoryCache.get(siteId1)).willReturn(service
				.parseSiteHardware(siteId1,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId1, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));
		given(siteInventoryCache.get(siteId2)).willReturn(service
				.parseSiteHardware(siteId2,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId2, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		// set clock to near data request, to work with maximum lag setting (default 3h)
		clock.setInstant(Instant.parse("2024-12-30T22:00:00Z"));

		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is POST")
					.returns(HttpMethod.POST, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.satisfies(reqs -> {
				and.then(reqs).element(0)
					.as("Request 1 body criteria")
					.returns(List.of(
								Map.of("siteId", siteId1, "hardwareId", hardwareId1, "fieldName", fieldName1, "function", Avg.name()),
								Map.of("siteId", siteId1, "hardwareId", hardwareId1, "fieldName", fieldName2, "function", Last.name())
							), from(RequestEntity::getBody))
					;
				and.then(reqs).element(1)
					.as("Request 2 body criteria")
					.returns(List.of(
								Map.of("siteId", siteId2, "hardwareId", hardwareId2, "fieldName", fieldName1, "function", Avg.name()),
								Map.of("siteId", siteId2, "hardwareId", hardwareId2, "fieldName", fieldName2, "function", Last.name())
							), from(RequestEntity::getBody))
					;
			})
			.extracting(RequestEntity::getUrl)
			.allSatisfy(uri -> {
				and.then(uri)
					.as("Request URL")
					.isEqualTo(BASE_URI.resolve(BIN_DATA_URL
							+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
									ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
									ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
									)))
					;
			})
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(5)
			.satisfies(list -> {
				DatumSamples expectedSamples1 = new DatumSamples();
				expectedSamples1.putInstantaneousSampleValue("watts", 123);
				expectedSamples1.putAccumulatingSampleValue("wattHours", 100);

				and.then(list).element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:51:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples2 = new DatumSamples();
				expectedSamples2.putInstantaneousSampleValue("watts", 124);
				expectedSamples2.putAccumulatingSampleValue("wattHours", 101);

				and.then(list).element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:52:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", 122);
				expectedSamples3.putAccumulatingSampleValue("wattHours", 102);

				and.then(list).element(2)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:53:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples4 = new DatumSamples();
				expectedSamples4.putInstantaneousSampleValue("watts", 1123);
				expectedSamples4.putAccumulatingSampleValue("wattHours", 1100);

				and.then(list).element(3)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:51:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples4, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples5 = new DatumSamples();
				expectedSamples5.putInstantaneousSampleValue("watts", 1124);
				expectedSamples5.putAccumulatingSampleValue("wattHours", 1101);

				and.then(list).element(4)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-12-30T21:52:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples5, from(Datum::asSampleOperations))
					;

			})
			;

		and.then(result)
			.asInstanceOf(type(CloudDatumStreamQueryResult.class))
			.extracting(CloudDatumStreamQueryResult::getNextQueryFilter)
			.as("Next query filter returned")
			.isNotNull()
			.as("21:53 returned, as +1min after the least timestamp out of the greatest timestamps per stream")
			.returns(Instant.parse("2024-12-30T21:53:00Z"), from(CloudDatumStreamQueryFilter::getStartDate))
			;

		// @formatter:on
	}

	@Test
	public void requestList_startAtLeastGreatestTimestampPerStream_outsideLag() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId1 = randomLong();
		final Long hardwareId1 = randomLong();
		final Long siteId2 = randomLong();
		final Long hardwareId2 = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(fieldName1, Avg.name()));
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(fieldName2, Last.name()));
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(JsonUtils.getStringMap("""
						{
						  "sourceIdMap": {
						    "/%s/%s":  "inv/1",
						    "/%s/%s": "inv/2"
						  }
						}
						""".formatted(siteId1, hardwareId1, siteId2, hardwareId2)));
		// @formatter:on

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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

		// request data streams
		final JsonNode resJson1 = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-03.json", getClass()), ObjectNode.class);
		final var res1 = new ResponseEntity<JsonNode>(resJson1, HttpStatus.OK);
		final JsonNode resJson2 = getObjectFromJSON(
				utf8StringResource("alsoenergy-bindata-04.json", getClass()), ObjectNode.class);
		final var res2 = new ResponseEntity<JsonNode>(resJson2, HttpStatus.OK);
		// @formatter:off
		given(restOps.exchange(any(), eq(JsonNode.class)))
			.willReturn(res1)
			.willReturn(res2);
		// @formatter:on

		// get site devices for data validation
		given(siteInventoryCache.get(siteId1)).willReturn(service
				.parseSiteHardware(siteId1,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId1, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));
		given(siteInventoryCache.get(siteId2)).willReturn(service
				.parseSiteHardware(siteId2,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId2, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		// set clock to 1y past data date, to exceed maximum lag setting (default 3h)
		clock.setInstant(Instant.parse("2025-12-30T00:00:00Z"));

		var filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant().minus(10, MINUTES));
		filter.setEndDate(clock.instant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is POST")
					.returns(HttpMethod.POST, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes OAuth Authorization header")
					.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
					;
			})
			.satisfies(reqs -> {
				and.then(reqs).element(0)
					.as("Request 1 body criteria")
					.returns(List.of(
								Map.of("siteId", siteId1, "hardwareId", hardwareId1, "fieldName", fieldName1, "function", Avg.name()),
								Map.of("siteId", siteId1, "hardwareId", hardwareId1, "fieldName", fieldName2, "function", Last.name())
							), from(RequestEntity::getBody))
					;
				and.then(reqs).element(1)
					.as("Request 2 body criteria")
					.returns(List.of(
								Map.of("siteId", siteId2, "hardwareId", hardwareId2, "fieldName", fieldName1, "function", Avg.name()),
								Map.of("siteId", siteId2, "hardwareId", hardwareId2, "fieldName", fieldName2, "function", Last.name())
							), from(RequestEntity::getBody))
					;
			})
			.extracting(RequestEntity::getUrl)
			.allSatisfy(uri -> {
				and.then(uri)
					.as("Request URL")
					.isEqualTo(BASE_URI.resolve(BIN_DATA_URL
							+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
									ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().minus(10, MINUTES).atZone(UTC).toLocalDateTime()),
									ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(clock.instant().atZone(UTC).toLocalDateTime())
									)))
					;
			})
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(5)
			;

		and.then(result)
			.asInstanceOf(type(CloudDatumStreamQueryResult.class))
			.extracting(CloudDatumStreamQueryResult::getNextQueryFilter)
			.as("Next query filter not returned because within lag")
			.isNull()
			;

		// @formatter:on
	}

	@Test
	public void timeGap_withinRequest() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(siteId, hardwareId, fieldName2, Last.name()));
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
				utf8StringResource("alsoenergy-bindata-05.json", getClass()), ObjectNode.class);
		final var res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		final ZonedDateTime startTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00");
		final ZonedDateTime endTs = startTs.plusDays(5);

		// adjust clock to be within granularity period
		clock.setInstant(endTs.plusDays(1).toInstant());

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startTs.toInstant());
		filter.setEndDate(endTs.toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedUri = BASE_URI.resolve(BIN_DATA_URL
				+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getStartDate().atZone(UTC).toLocalDateTime()),
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getEndDate().atZone(UTC).toLocalDateTime())
						));

		final List<Map<String, Object>> expectedBody = List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				);

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(expectedUri, from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(expectedBody, from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
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
					.returns(Instant.parse("2024-11-25T10:00:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples2, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamples3 = new DatumSamples();
				expectedSamples3.putInstantaneousSampleValue("watts", 345600);
				expectedSamples3.putAccumulatingSampleValue("wattHours", 438401);

				and.then(list)
					.element(2)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2024-11-25T10:01:00+00:00"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples3, from(Datum::asSampleOperations))
					;
			})
			;

		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		var events = eventCaptor.getAllValues();
		and.then(events.get(0))
			.as("Event tags for HTTP req")
			.returns(new String[] {CLOUD_INTEGRATIONS_TAG, CLOUD_INTEGRATION_TAG, HTTP_TAG}, from(LogEventInfo::getTags))
			;
		and.then(events.get(1))
			.as("Event tags for data validation")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			;


		// validate that Mark records created for time gap
		and.then(result.getAuxiliary())
			.as("Auxiliary records created for start/end time gap events")
			.hasSize(2)
			.allSatisfy(r -> {
				and.then(r)
					.as("Event type is Mark")
					.returns(DatumAuxiliaryType.Mark, from(DatumAuxiliaryRecord::getType))
					.as("Event kind is Cloud datum Stream kind")
					.returns(datumStream.getKind(), from(DatumAuxiliaryRecord::getKind))
					.as("Event object ID is Cloud Datum Stream ID")
					.returns(datumStream.getObjectId(), from(DatumAuxiliaryRecord::getObjectId))
					.as("Event for expected source")
					.returns(expectedSourceId, from(DatumAuxiliaryRecord::getSourceId))
					;
			})
			.satisfies(records -> {
				final Instant timeGapStartTs =
						ZonedDateTime.parse("2024-11-21T10:00:00+00:00").toInstant();
				final Instant timeGapEndTs =
						ZonedDateTime.parse("2024-11-25T10:00:00+00:00").toInstant();

				and.then(records).element(0, type(DatumAuxiliaryRecord.class))
					.as("Timestamp for time-gap start validation event datum")
					.returns(timeGapStartTs, from(DatumAuxiliaryRecord::getTimestamp))
					.extracting(DatumAuxiliaryRecord::getMetadata)
					.satisfies(meta -> {
						and.then(meta.getInfo())
							.as("Metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationMetadata())
							;
						and.then(meta.getPropertyInfo(TIME_GAP_VALIDATION_TYPE))
							.asInstanceOf(map(String.class, Object.class))
							.as("Property metadata for time-gap start event datum")
							.containsAllEntriesOf(timeGapValidationPropertyMetadata(
									"/%s/%s".formatted(siteId, hardwareId), expectedUri, expectedBody, timeGapStartTs, timeGapEndTs, true, null))
							.as("Correlation ID provided")
							.containsKey(CORRELATION_ID_DATA_KEY)
							;
					})
					.extracting(GeneralDatumMetadata::getInfo, map(String.class, Object.class))
					;
				and.then(records).element(1, type(DatumAuxiliaryRecord.class))
					.as("Timestamp for time-gap end validation event datum")
					.returns(timeGapEndTs, from(DatumAuxiliaryRecord::getTimestamp))
					.extracting(DatumAuxiliaryRecord::getMetadata)
					.satisfies(meta -> {
						and.then(meta.getInfo())
							.as("Metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationMetadata())
							;
						and.then(meta.getPropertyInfo(TIME_GAP_VALIDATION_TYPE))
							.asInstanceOf(map(String.class, Object.class))
							.as("Property metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationPropertyMetadata(
									"/%s/%s".formatted(siteId, hardwareId), expectedUri, expectedBody, timeGapStartTs, timeGapEndTs, false,
									records.toArray(DatumAuxiliaryRecord[]::new)[0].getMetadata().getInfoString(
											TIME_GAP_VALIDATION_TYPE, CORRELATION_ID_DATA_KEY)))
							.as("Correlation ID provided")
							.containsKey(CORRELATION_ID_DATA_KEY)
							;
					})
					;
				})
				;
		// @formatter:on
	}

	@Test
	public void timeGap_lookupPrevDatum() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = randomLong();

		service.setDatumDao(datumDao);

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		final String fieldName2 = "KWh";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef(siteId, hardwareId, fieldName2, Last.name()));
		prop2.setMultiplier(new BigDecimal("1000"));
		prop2.setScale(0);
		prop2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// lookup previous datum for first datum in result set
		final String expectedSourceId = datumStream.getSourceId()
				+ "/%s/%s".formatted(siteId, hardwareId);
		final Instant firstDatumTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00").toInstant();
		final Instant prevDatumTs = ZonedDateTime.parse("2024-11-11T10:00:00+00:00").toInstant();
		final var prevDatum = new DatumEntity(new DatumPK(UUID.randomUUID(), prevDatumTs), null,
				new DatumProperties());
		given(datumDao.findFiltered(any()))
				.willReturn(
						new BasicObjectDatumStreamFilterResults<>(
								Map.of(prevDatum.streamId(), emptyMeta(prevDatum.streamId(), "UTC",
										datumStream.getKind(), nodeId, expectedSourceId)),
								List.of(prevDatum)));

		final ZonedDateTime startTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00");
		final ZonedDateTime endTs = startTs.plusDays(5);

		// adjust clock to be within granularity period
		clock.setInstant(endTs.plusDays(1).toInstant());

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId,
						getObjectFromJSON(
								utf8StringResource("alsoenergy-hardware-02.json.tmpl", getClass())
										.formatted(hardwareId, 23456),
								JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		final var filter = new BasicQueryFilter();
		filter.setStartDate(startTs.toInstant());
		filter.setEndDate(endTs.toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedUri = BASE_URI.resolve(BIN_DATA_URL
				+ "?from=%s&to=%s&binSizes=BinRaw&tz=Z".formatted(
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getStartDate().atZone(UTC).toLocalDateTime()),
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getEndDate().atZone(UTC).toLocalDateTime())
						));

		final List<Map<String, Object>> expectedBody = List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name()),
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName2, "function", Last.name())
				);

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(expectedUri, from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(expectedBody, from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
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
			.as("Prev datum query is for expected source ID")
			.returns(expectedSourceId, from(DatumCriteria::getSourceId))
			;

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
					.returns(firstDatumTs, from(Datum::getTimestamp))
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

		then(userEventAppenderBiz).should(times(2)).addEvent(eq(TEST_USER_ID), eventCaptor.capture());
		var events = eventCaptor.getAllValues();
		and.then(events.get(0))
			.as("Event tags for HTTP req")
			.returns(new String[] {CLOUD_INTEGRATIONS_TAG, CLOUD_INTEGRATION_TAG, HTTP_TAG}, from(LogEventInfo::getTags))
			;
		and.then(events.get(1))
			.as("Event tags for data validation")
			.returns(DATUM_STREAM_DATA_VALIDATION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			;


		// validate that Mark records created for time gap
		and.then(result.getAuxiliary())
			.as("Auxiliary records created for start/end time gap events")
			.hasSize(2)
			.allSatisfy(r -> {
				and.then(r)
					.as("Event type is Mark")
					.returns(DatumAuxiliaryType.Mark, from(DatumAuxiliaryRecord::getType))
					.as("Event kind is Cloud datum Stream kind")
					.returns(datumStream.getKind(), from(DatumAuxiliaryRecord::getKind))
					.as("Event object ID is Cloud Datum Stream ID")
					.returns(datumStream.getObjectId(), from(DatumAuxiliaryRecord::getObjectId))
					.as("Event for expected source")
					.returns(expectedSourceId, from(DatumAuxiliaryRecord::getSourceId))
					;
			})
			.satisfies(records -> {
				final Instant timeGapStartTs = prevDatumTs;
				final Instant timeGapEndTs = firstDatumTs;

				and.then(records).element(0, type(DatumAuxiliaryRecord.class))
					.as("Timestamp for time-gap start validation event datum")
					.returns(timeGapStartTs, from(DatumAuxiliaryRecord::getTimestamp))
					.extracting(DatumAuxiliaryRecord::getMetadata)
					.satisfies(meta -> {
						and.then(meta.getInfo())
							.as("Metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationMetadata())
							;
						and.then(meta.getPropertyInfo(TIME_GAP_VALIDATION_TYPE))
							.asInstanceOf(map(String.class, Object.class))
							.as("Property metadata for time-gap start event datum")
							.containsAllEntriesOf(timeGapValidationPropertyMetadata(
									"/%s/%s".formatted(siteId, hardwareId), expectedUri, expectedBody, timeGapStartTs, timeGapEndTs, true, null))
							.as("Correlation ID provided")
							.containsKey(CORRELATION_ID_DATA_KEY)
							;
					})
					;
				and.then(records).element(1, type(DatumAuxiliaryRecord.class))
					.as("Timestamp for time-gap end validation event datum")
					.returns(timeGapEndTs, from(DatumAuxiliaryRecord::getTimestamp))
					.extracting(DatumAuxiliaryRecord::getMetadata)
					.satisfies(meta -> {
						and.then(meta.getInfo())
							.as("Metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationMetadata())
							;
						and.then(meta.getPropertyInfo(TIME_GAP_VALIDATION_TYPE))
							.asInstanceOf(map(String.class, Object.class))
							.as("Property metadata for time-gap start event datum")
							.containsExactlyInAnyOrderEntriesOf(timeGapValidationPropertyMetadata(
									"/%s/%s".formatted(siteId, hardwareId), expectedUri, expectedBody, timeGapStartTs, timeGapEndTs, false,
									records.toArray(DatumAuxiliaryRecord[]::new)[0].getMetadata().getInfoString(
											TIME_GAP_VALIDATION_TYPE, CORRELATION_ID_DATA_KEY)))
							.as("Correlation ID provided")
							.containsKey(CORRELATION_ID_DATA_KEY)
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void resolveGranularityPeriods() {
		// GIVEN
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				AlsoEnergyCloudDatumStreamService.GRANULARITY_PERIODS_SETTING, Map.of(
						"P1M", AlsoEnergyGranularity.FifteenMinute.name(),
						"P1Y", AlsoEnergyGranularity.Hour.name(),
						"P2Y", AlsoEnergyGranularity.Month.name()
						)
				));
		// @formatter:on

		// WHEN
		SequencedMap<Period, AlsoEnergyGranularity> result = service
				.resolveGranularityPeriods(datumStream);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Granularity periods map returned")
			.isNotNull()
			.containsExactly(
				  entry(Period.ofMonths(1), AlsoEnergyGranularity.FifteenMinute)
				, entry(Period.ofYears(1), AlsoEnergyGranularity.Hour)
				, entry(Period.ofYears(2), AlsoEnergyGranularity.Month)
			)
			;
		// @formatter:on
	}

	@Test
	public void resolveGranularityPeriods_default() {
		// GIVEN
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);

		// WHEN
		SequencedMap<Period, AlsoEnergyGranularity> result = service
				.resolveGranularityPeriods(datumStream);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Default granularity periods map returned")
			.isNotNull()
			.containsExactlyEntriesOf(AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS)
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = { "P3Y", "P1Y", "P1M" })
	public void adjustEndDateForGranularityPeriods_dayWithinPeriod(String offset) {
		// GIVEN
		final Period offsetPeriod = Period.parse(offset);
		final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS;
		final ZonedDateTime start = clock.instant().atZone(UTC).minus(offsetPeriod);
		final ZonedDateTime end = start.plusDays(1);

		// WHEN
		Instant result = service.adjustEndDateForGranularityPeriods(granularityPeriods,
				start.toInstant(), end.toInstant(), UTC);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No change when start/end both within oldest period")
			.isEqualTo(end.toInstant())
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	public void adjustEndDateForGranularityPeriods_crossesPeriod(int granularityIndex) {
		// GIVEN
		final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS;
		final Period periodEnd = List.copyOf(granularityPeriods.keySet()).get(granularityIndex);
		final Period offsetPeriod = periodEnd.plusDays(1);
		final ZonedDateTime start = clock.instant().atZone(UTC).minus(offsetPeriod);
		final ZonedDateTime end = start.plusDays(2);

		// WHEN
		Instant result = service.adjustEndDateForGranularityPeriods(granularityPeriods,
				start.toInstant(), end.toInstant(), UTC);

		// THEN
		// @formatter:off
		and.then(result)
			.as("End capped to period end that range crosses over")
			.isEqualTo(start.plusDays(1).toInstant())
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	public void datum_granularityConstrained(int granularityIndex) {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = 12345L;

		final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS;
		final AlsoEnergyGranularity periodGranularity = List.copyOf(granularityPeriods.values())
				.get(granularityIndex);
		final Period periodEnd = List.copyOf(granularityPeriods.keySet()).get(granularityIndex);

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		final ZonedDateTime startTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00");
		final ZonedDateTime endTs = startTs.plusDays(5);

		// adjust clock to be much beyond oldest granularity period
		clock.setInstant(endTs.plus(periodEnd).plusDays(7).toInstant());

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId, getObjectFromJSON(
						utf8StringResource("alsoenergy-hardware-01.json", getClass()), JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startTs.toInstant());
		filter.setEndDate(endTs.toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedUri = BASE_URI.resolve(BIN_DATA_URL
				+ "?from=%s&to=%s&binSizes=%s&tz=Z".formatted(
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getStartDate().atZone(UTC).toLocalDateTime()),
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(filter.getEndDate().atZone(UTC).toLocalDateTime()),
						periodGranularity.getQueryKey()
						));

		final List<Map<String, Object>> expectedBody = List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name())
				);

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(expectedUri, from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(expectedBody, from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(result).isNotNull();
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	public void datum_granularityConstrained_crossBoundary(int granularityIndex) {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = 12345L;

		final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS;
		final AlsoEnergyGranularity periodGranularity = List.copyOf(granularityPeriods.values())
				.get(granularityIndex);
		final Period periodEnd = List.copyOf(granularityPeriods.keySet()).get(granularityIndex);

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		final ZonedDateTime startTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00");
		final ZonedDateTime endTs = startTs.plusDays(2);

		// adjust clock to be 1d before granularity period
		clock.setInstant(endTs.plus(periodEnd).minusDays(1).toInstant());

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId, getObjectFromJSON(
						utf8StringResource("alsoenergy-hardware-01.json", getClass()), JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startTs.toInstant());
		filter.setEndDate(endTs.toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedUri = BASE_URI.resolve(BIN_DATA_URL
				+ "?from=%s&to=%s&binSizes=%s&tz=Z".formatted(
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(startTs.toLocalDateTime()),
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(startTs.toLocalDateTime().plusDays(1)),
						periodGranularity.getQueryKey()
						));

		final List<Map<String, Object>> expectedBody = List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name())
				);

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(expectedUri, from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(expectedBody, from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(result)
			.isNotNull()
			;
		and.then(result.getUsedQueryFilter())
			.as("Used query start date from filter")
			.returns(startTs.toInstant(), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("Used query end date from period constraint")
			.returns(startTs.plusDays(1).toInstant(), from(CloudDatumStreamQueryFilter::getEndDate))
			.isNotNull()
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	public void datum_granularityConstrained_crossBoundary_crossMaxRange(int granularityIndex) {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final String clientId = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long siteId = randomLong();
		final Long hardwareId = 12345L;

		final SortedMap<Period, AlsoEnergyGranularity> granularityPeriods = AlsoEnergyCloudDatumStreamService.DEFAULT_GRANULARITY_PERIODS;
		final AlsoEnergyGranularity periodGranularity = List.copyOf(granularityPeriods.values())
				.get(granularityIndex);
		final Period periodEnd = List.copyOf(granularityPeriods.keySet()).get(granularityIndex);

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
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
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final String fieldName1 = "KW";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, hardwareId, fieldName1, Avg.name()));
		prop1.setMultiplier(new BigDecimal("1000"));
		prop1.setScale(0);
		prop1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// @formatter:off
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(OAuth2Utils.PASSWORD_GRANT_TYPE)
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
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		final ZonedDateTime startTs = ZonedDateTime.parse("2024-11-21T10:00:00+00:00");
		final ZonedDateTime endTs = startTs.plusDays(10); // larger than max time range

		// adjust clock to be 1d before granularity period
		clock.setInstant(startTs.plus(periodEnd).plusDays(1).toInstant());

		// get site devices for data validation
		given(siteInventoryCache.get(siteId)).willReturn(service
				.parseSiteHardware(siteId, getObjectFromJSON(
						utf8StringResource("alsoenergy-hardware-01.json", getClass()), JsonNode.class))
				.toArray(CloudDataValue[]::new));

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startTs.toInstant());
		filter.setEndDate(endTs.toInstant());
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedUri = BASE_URI.resolve(BIN_DATA_URL
				+ "?from=%s&to=%s&binSizes=%s&tz=Z".formatted(
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(startTs.toLocalDateTime()),
						ISO_DATE_OPT_TIME_OPT_MILLIS_UTC.format(startTs.toLocalDateTime().plusDays(1)),
						periodGranularity.getQueryKey()
						));

		final List<Map<String, Object>> expectedBody = List.of(
				Map.of("siteId", siteId, "hardwareId", hardwareId, "fieldName", fieldName1, "function", Avg.name())
				);

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(expectedUri, from(RequestEntity::getUrl))
			.as("Request body contains criteria")
			.returns(expectedBody, from(RequestEntity::getBody))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION,"Bearer %s".formatted(oauthAccessToken.getTokenValue()))
			;

		and.then(result)
			.isNotNull()
			;
		and.then(result.getUsedQueryFilter())
			.as("Used query start date from filter")
			.returns(startTs.toInstant(), from(CloudDatumStreamQueryFilter::getStartDate))
			.as("Used query end date from period constraint")
			.returns(startTs.plusDays(1).toInstant(), from(CloudDatumStreamQueryFilter::getEndDate))
			.isNotNull()
			;
		and.then(result.getNextQueryFilter())
			.as("No next query becauase constrained by period granularity")
			.isNull()
			;
		// @formatter:on
	}

}
