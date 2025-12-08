/* ==================================================================
 * SigenergyCloudDatumStreamServiceTests.java - 7/12/2025 6:10:22 am
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

package net.solarnetwork.central.c2c.biz.sigen.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Map.entry;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.SYSTEM_LIST_PATH;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
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
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRegion;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link SigenergyCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SigenergyCloudDatumStreamServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

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
	private Cache<String, CloudDataValue[]> systemDeviceCache;

	@Mock
	private ClientAccessTokenDao clientAccessTokenDao;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Captor
	private ArgumentCaptor<CloudDataValue[]> cloudDataValueArrayCaptor;

	private MutableClock clock;
	private CloudIntegrationsExpressionService expressionService;

	private SigenergyCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.MINUTES), UTC);
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);

		var restOpsHelper = new SigenergyRestOperationsHelper(
				LoggerFactory.getLogger(SigenergyCloudIntegrationService.class), userEventAppenderBiz,
				restOps, CloudIntegrationsUserEvents.INTEGRATION_HTTP_ERROR_TAGS, encryptor,
				serviceIdentifier -> SigenergyCloudIntegrationService.SECURE_SETTINGS, clock,
				JsonUtils.newObjectMapper(), clientAccessTokenDao, new StaticOptionalService<>(null));

		service = new SigenergyCloudDatumStreamService(clock, userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOpsHelper, JsonUtils.newObjectMapper());

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SigenergyCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void dataValues_root() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
			));
		// @formatter:on

		given(integrationDao.get(config.getId())).willReturn(config);

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("sigen-system-list-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(config.getId(), Map.of());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for inverter telemetry")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SYSTEM_LIST_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(results)
			.as("Result generated")
			.hasSize(1)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("System name parsed")
					.returns("Test House", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("ABC123"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"tz", "Pacific/Auckland",
							"l", "Anytown",
							"street", "123 Main Street",
							"gridConnectedTime", Instant.parse("2025-05-05T04:32:01Z"),
							"pvCapacity", 9.2f,
							"batteryCapacity", 9.2f
							))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_system() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
			));
		// @formatter:on

		given(integrationDao.get(config.getId())).willReturn(config);

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("sigen-device-list-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		final Map<String, Object> filter = Map.of(SigenergyCloudDatumStreamService.SYSTEM_ID_FILTER,
				systemId);
		Iterable<CloudDataValue> results = service.dataValues(config.getId(), filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for inverter telemetry")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.path(SigenergyRestOperationsHelper.SYSTEM_DEVICE_LIST_PATH)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey(), systemId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(results)
			.as("Result generated for system + 2 devices")
			.hasSize(3)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Name is 'System'")
					.returns(SigenergyCloudDatumStreamService.SYSTEM_DEVICE_NAME, from(CloudDataValue::getName))
					.as("System ID and sys parsed as identifiers")
					.returns(List.of(systemId, SigenergyCloudDatumStreamService.SYSTEM_DEVICE_ID), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.as("No metadata provided")
					.returns(null,  from(CloudDataValue::getMetadata))
					;

				and.then(l).element(1)
					.as("Serial number parsed as name")
					.returns("110A123", from(CloudDataValue::getName))
					.as("System ID and serial number parsed as identifiers")
					.returns(List.of(systemId, "110A123"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
							entry("serial", "110A123"),
							entry("firmwareVersion", "V100R001C22SPC111B064L"),
							entry("deviceType", "Inverter"),
							entry("status", "Normal"),
							entry("pn", "1104004100"),
							entry("ratedFrequency", 50),
							entry("ratedVoltage", 230),
							entry("maxAbsorbedPower", 11),
							entry("ratedActivePower", 10),
							entry("pvStringNumber", 4),
							entry("maxActivePower", 11)
							))
					;

				and.then(l).element(2)
					.as("Serial number parsed as name")
					.returns("110B123", from(CloudDataValue::getName))
					.as("System ID and serial number parsed as identifiers")
					.returns(List.of(systemId, "110B123"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
							entry("serial", "110B123"),
							entry("firmwareVersion", "V100R001C22SPC111B064L"),
							entry("deviceType", "Battery"),
							entry("status", "Normal"),
							entry("pn", "1113000120"),
							entry("ratedDischargePower", 4.8f),
							entry("ratedChargePower", 4.2f),
							entry("ratedEnergy", 280),
							entry("dischargeEnergy", 7.57f),
							entry("chargeableEnergy", 0.81f)
							))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_system_withCache_miss() {
		// GIVEN
		service.setSystemDeviceCache(systemDeviceCache);

		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
			));
		// @formatter:on

		given(integrationDao.get(config.getId())).willReturn(config);

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("sigen-device-list-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		final Map<String, Object> filter = Map.of(SigenergyCloudDatumStreamService.SYSTEM_ID_FILTER,
				systemId);
		Iterable<CloudDataValue> results = service.dataValues(config.getId(), filter);

		// THEN
		// @formatter:off
		then(systemDeviceCache).should().get(systemId);

		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));
		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for inverter telemetry")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.path(SigenergyRestOperationsHelper.SYSTEM_DEVICE_LIST_PATH)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey(), systemId)
					.toUri(), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		then(systemDeviceCache).should().put(eq(systemId), cloudDataValueArrayCaptor.capture());
		and.then(cloudDataValueArrayCaptor.getValue())
			.as("Same instance as result added to cache")
			.containsExactlyElementsOf(results)
			;

		and.then(results)
			.as("Result generated for 2 devices")
			.hasSize(3)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Name is 'System'")
					.returns(SigenergyCloudDatumStreamService.SYSTEM_DEVICE_NAME, from(CloudDataValue::getName))
					.as("System ID and sys parsed as identifiers")
					.returns(List.of(systemId, SigenergyCloudDatumStreamService.SYSTEM_DEVICE_ID), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.as("No metadata provided")
					.returns(null,  from(CloudDataValue::getMetadata))
					;

				and.then(l).element(1)
					.as("Serial number parsed as name")
					.returns("110A123", from(CloudDataValue::getName))
					.as("System ID and serial number parsed as identifiers")
					.returns(List.of(systemId, "110A123"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
							entry("serial", "110A123"),
							entry("firmwareVersion", "V100R001C22SPC111B064L"),
							entry("deviceType", "Inverter"),
							entry("status", "Normal"),
							entry("pn", "1104004100"),
							entry("ratedFrequency", 50),
							entry("ratedVoltage", 230),
							entry("maxAbsorbedPower", 11),
							entry("ratedActivePower", 10),
							entry("pvStringNumber", 4),
							entry("maxActivePower", 11)
							))
					;

				and.then(l).element(2)
					.as("Serial number parsed as name")
					.returns("110B123", from(CloudDataValue::getName))
					.as("System ID and serial number parsed as identifiers")
					.returns(List.of(systemId, "110B123"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
							entry("serial", "110B123"),
							entry("firmwareVersion", "V100R001C22SPC111B064L"),
							entry("deviceType", "Battery"),
							entry("status", "Normal"),
							entry("pn", "1113000120"),
							entry("ratedDischargePower", 4.8f),
							entry("ratedChargePower", 4.2f),
							entry("ratedEnergy", 280),
							entry("dischargeEnergy", 7.57f),
							entry("chargeableEnergy", 0.81f)
							))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_system_withCache_hit() {
		// GIVEN
		service.setSystemDeviceCache(systemDeviceCache);

		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
			));
		// @formatter:on

		given(integrationDao.get(config.getId())).willReturn(config);

		// get cached value
		final CloudDataValue[] cachedResult = new CloudDataValue[] {
				CloudDataValue.dataValue(List.of(randomString()), randomString()) };
		given(systemDeviceCache.get(systemId)).willReturn(cachedResult);

		// WHEN
		final Map<String, Object> filter = Map.of(SigenergyCloudDatumStreamService.SYSTEM_ID_FILTER,
				systemId);
		Iterable<CloudDataValue> results = service.dataValues(config.getId(), filter);

		// THEN
		// @formatter:off
		then(clientAccessTokenDao).shouldHaveNoInteractions();
		then(restOps).shouldHaveNoInteractions();

		then(systemDeviceCache).shouldHaveNoMoreInteractions();

		and.then(results)
			.as("Result from cache returned")
			.containsExactly(cachedResult)
			;
		// @formatter:on
	}

}
