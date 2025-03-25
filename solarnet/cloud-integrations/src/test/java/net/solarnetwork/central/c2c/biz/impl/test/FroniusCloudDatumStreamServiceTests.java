/* ==================================================================
 * FroniusCloudDatumStreamServiceTests.java - 3/12/2024 12:26:20 pm
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
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.c2c.biz.CloudDatumStreamService.SOURCE_ID_MAP_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.DEFAULT_QUERY_LIMIT;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.DEVICE_HISTORY_URL_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.DEVICE_ID_FILTER;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.END_AT_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.LIMIT_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.OFFSET_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.START_AT_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.SYSTEM_DEVICES_URL_TEMPLATE;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService.SYSTEM_ID_FILTER;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.ACCESS_KEY_ID_HEADER;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.ACCESS_KEY_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.ACCESS_KEY_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.ACCES_KEY_SECRET_HEADER;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService.LIST_SYSTEMS_URL;
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
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.cache.Cache;
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
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.FroniusCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.FroniusCloudIntegrationService;
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
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link FroniusCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class FroniusCloudDatumStreamServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	SolarNodeOwnershipDao nodeOwnershipDao;

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
	private Cache<String, CloudDataValue> systemCache;

	@Captor
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private FroniusCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new FroniusCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, restOps,
				clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(FroniusCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		service.setSystemCache(systemCache);
	}

	@Test
	public void dataValues_root() {
		// GIVEN
		final String apiKey = randomString();
		final String apiSecret = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, apiKey,
				ACCESS_KEY_SECRET_SETTING, apiSecret
			));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("fronius-systems-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(FroniusCloudIntegrationService.BASE_URI.resolve(LIST_SYSTEMS_URL))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("API ID provided in HTTP request header")
			.containsEntry(ACCESS_KEY_ID_HEADER, List.of(apiKey))
			.as("API secret provided in HTTP request header")
			.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(apiSecret))
			;

		and.then(results)
			.as("Result generated")
			.hasSize(2)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("System name parsed")
					.returns("Site 1", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("3e210d7d-2b9e-43d6-830a-000000000000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"tz", "Europe/Berlin",
							"l", "Anytown",
							"st", "State",
							"postalCode", "12345",
							"street", "123 Main Street",
							"c", "DK",
							"installationDate", Instant.parse("2009-06-15T00:00:00Z"),
							"lastImport", Instant.parse("2025-03-24T00:35:07Z"),
							"peakPower", 98340
							))
					;
				and.then(l).element(1)
					.as("System name parsed")
					.returns("Site 2", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("ced6f980-8907-4128-87ea-000000000000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"tz", "Europe/London",
							"installationDate", Instant.parse("2000-01-01T00:00:00Z"),
							"lastImport", Instant.parse("2025-03-23T23:15:08Z"),
							"peakPower", 7600
							))
					;
			})
			;
		// @formatter:on
	}

	private String refValue(String... idents) {
		return Arrays.stream(idents).collect(Collectors.joining("/", "/", ""));
	}

	@Test
	public void dataValues_system() {
		// GIVEN
		final String apiKey = randomString();
		final String apiSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, apiKey,
				ACCESS_KEY_SECRET_SETTING, apiSecret
			));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("fronius-devices-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of(
				SYSTEM_ID_FILTER, systemId));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.satisfies(uri -> {
				URI expectedUri = UriComponentsBuilder.fromUri(BASE_URI)
					.path(SYSTEM_DEVICES_URL_TEMPLATE)
					.buildAndExpand(systemId)
					.toUri()
					;
				and.then(uri)
					.as("Request devices for system")
					.isEqualTo(expectedUri)
					;
			})
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("API ID provided in HTTP request header")
			.containsEntry(ACCESS_KEY_ID_HEADER, List.of(apiKey))
			.as("API secret provided in HTTP request header")
			.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(apiSecret))
			;

		and.then(results)
			.as("Result generated for supported device types, sorted by identifiers")
			.hasSize(21)
			.satisfies(l -> {
				and.then(l).element(14)
					.as("Inverter device name parsed")
					.returns("Primo GEN24 3.6 Plus", from(CloudDataValue::getName))
					.as("Device ID parsed")
					.returns(List.of(
							systemId,
							"7796944e-b09e-424e-a77f-000000000000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"manufacturer", "Fronius",
							"serial", "32555894",
							"active", false,
							"deviceType", "Inverter",
							"numberMPPTrackers", 2,
							"numberPhases", 1,
							"activationDate", Instant.parse("2022-03-23T18:15:23Z"),
							"deactivationDate", Instant.parse("2023-07-28T14:24:06Z")
							))
					;
				and.then(l).element(5)
					.as("Battery device name parsed")
					.returns("BYD Battery-Box Premium HV", from(CloudDataValue::getName))
					.as("Device ID parsed")
					.returns(List.of(
							systemId,
							"3f614216-0d30-4e5f-92e6-000000000000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.hasSize(11)
					.as("Metadata extracted")
					.containsAllEntriesOf(Map.of(
							"manufacturer", "BYD",
							"serial", "P030T020Z2009013172",
							"active", false,
							"deviceType", "Battery",
							"activationDate", Instant.parse("2022-05-17T17:54:48Z"),
							"deactivationDate", Instant.parse("2023-07-28T14:24:06Z"),
							"capacity", 7680,
							"maxChargePower", 7680,
							"maxDischargePower", 7680,
							"maxSOC", 100
							))
					.as("Metadata extracted")
					.containsEntry("minSOC", 5)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValues_device() {
		// GIVEN
		final String apiKey = randomString();
		final String apiSecret = randomString();
		final String systemId = randomString();
		final String deviceId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, apiKey,
				ACCESS_KEY_SECRET_SETTING, apiSecret
			));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("fronius-device-history-inverter-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(res);

		// WHEN

		Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of(
				SYSTEM_ID_FILTER, systemId,
				DEVICE_ID_FILTER, deviceId));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(uriCaptor.getValue())
			.satisfies(uri -> {
				URI expectedUri = UriComponentsBuilder.fromUri(BASE_URI)
					.path(DEVICE_HISTORY_URL_TEMPLATE)
					.queryParam(START_AT_PARAM, clock.instant().truncatedTo(HOURS).minus(1, DAYS))
					.queryParam(END_AT_PARAM, clock.instant().truncatedTo(HOURS))
					.queryParam(LIMIT_PARAM, 1)
					.buildAndExpand(systemId, deviceId)
					.toUri()
					;
				and.then(uri)
					.as("Request device history for past day, limited to first result")
					.isEqualTo(expectedUri)
					;
			})
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("API ID provided in HTTP request header")
			.containsEntry(ACCESS_KEY_ID_HEADER, List.of(apiKey))
			.as("API secret provided in HTTP request header")
			.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(apiSecret))
			;

		and.then(results)
			.as("Result generated for device channels, sorted by identifiers")
			.hasSize(11)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("Channel name parsed")
					.returns("ApparentPower", from(CloudDataValue::getName))
					.as("Device ID parsed")
					.returns(List.of(
							systemId,
							deviceId,
							"ApparentPower"), from(CloudDataValue::getIdentifiers))
					.as("Reference returned for reference value")
					.returns(refValue(systemId, deviceId, "ApparentPower"), from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.as("Metadata extracted")
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"channelType", "Apparent Power",
							"unit", "VA"
							))
					;
				and.then(l).element(10)
					.as("Channel name name parsed")
					.returns("VoltageDC1", from(CloudDataValue::getName))
					.as("Device ID parsed")
					.returns(List.of(
							systemId,
							deviceId,
							"VoltageDC1"), from(CloudDataValue::getIdentifiers))
					.as("Reference returned for reference value")
					.returns(refValue(systemId, deviceId, "VoltageDC1"), from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.containsExactlyInAnyOrderEntriesOf(Map.of(
							"channelType", "Voltage",
							"unit", "V"
							))
					;
			})
			;
		// @formatter:on
	}

	private static String systemDevicePlaceholderComponentValueRef(String channelName) {
		return "/{systemId}/{deviceId}/%s".formatted(channelName);
	}

	@Test
	public void mostRecentDatum_mappedSourceIds_withPlaceholders() {
		// GIVEN
		final String accessKeyId = randomString();
		final String accessKeySecret = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();
		final String device2Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, accessKeyId,
				ACCESS_KEY_SECRET_SETTING, accessKeySecret
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties for energy imp/exp, that support both inverter and meter channels

		final String channel1Name = "EnergyExported";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh_exp");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemDevicePlaceholderComponentValueRef(channel1Name));

		final String channel2Name = "EnergyImported";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("wh_imp");
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(systemDevicePlaceholderComponentValueRef(channel2Name));

		final String channel3Name = "GridEnergyImported";
		final CloudDatumStreamPropertyConfiguration prop3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop3.setEnabled(true);
		prop3.setPropertyType(DatumSamplesType.Instantaneous);
		prop3.setPropertyName("wh_imp");
		prop3.setScale(0);
		prop3.setValueType(CloudDatumStreamValueType.Reference);
		prop3.setValueReference(systemDevicePlaceholderComponentValueRef(channel3Name));

		final String channel4Name = "GridEnergyExported";
		final CloudDatumStreamPropertyConfiguration prop4 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop4.setEnabled(true);
		prop4.setPropertyType(DatumSamplesType.Instantaneous);
		prop4.setPropertyName("wh_exp");
		prop4.setScale(0);
		prop4.setValueType(CloudDatumStreamValueType.Reference);
		prop4.setValueReference(systemDevicePlaceholderComponentValueRef(channel4Name));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2, prop3, prop4));

		// configure datum stream
		final Long nodeId = randomLong();
		final String inv1SourceId = "inv/1";
		final String met1SourceId = "met/1";
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		datumStream.setServiceProps(
				Map.of(SOURCE_ID_MAP_SETTING, Map.of("/%s/%s".formatted(systemId, device1Id),
						inv1SourceId, "/%s/%s".formatted(systemId, device2Id), met1SourceId)));

		// look up lastImport date for system; first try is cache miss, second as cache hit
		final Instant lastImportDate = Instant.parse("2025-03-24T16:45:08Z");
		final CloudDataValue systemInfo = CloudDataValue.intermediateDataValue(List.of(systemId),
				randomString(),
				Map.of(FroniusCloudDatumStreamService.LAST_IMPORT_METADATA, lastImportDate));
		given(systemCache.get(systemId)).willReturn(null).willReturn(systemInfo);

		// HTTP requests: first for system info (cache miss), then one for each device

		// @formatter:off
		final URI expectedSystemInfoUri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(FroniusCloudDatumStreamService.SYSTEM_URL_TEMPLATE)
				.buildAndExpand(systemId)
				.toUri();

		URI expectedDevice1Uri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(DEVICE_HISTORY_URL_TEMPLATE)
				.queryParam(START_AT_PARAM, lastImportDate.truncatedTo(HOURS))
				.queryParam(END_AT_PARAM, lastImportDate.truncatedTo(HOURS).truncatedTo(HOURS).plus(1, HOURS))
				.queryParam(OFFSET_PARAM, 0L)
				.queryParam(LIMIT_PARAM, 1)
				.buildAndExpand(systemId, device1Id)
				.toUri()
				;

		URI expectedDevice2Uri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(DEVICE_HISTORY_URL_TEMPLATE)
				.queryParam(START_AT_PARAM, lastImportDate.truncatedTo(HOURS))
				.queryParam(END_AT_PARAM, lastImportDate.truncatedTo(HOURS).truncatedTo(HOURS).plus(1, HOURS))
				.queryParam(OFFSET_PARAM, 0L)
				.queryParam(LIMIT_PARAM, 1)
				.buildAndExpand(systemId, device2Id)
				.toUri()
				;
		// @formatter:on

		given(restOps.exchange(eq(expectedSystemInfoUri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(
						getObjectFromJSON(utf8StringResource("fronius-system-01.json", getClass()),
								ObjectNode.class),
						OK));

		given(restOps.exchange(eq(expectedDevice1Uri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("fronius-device-history-inverter-01.json", getClass()),
						ObjectNode.class), OK));

		given(restOps.exchange(eq(expectedDevice2Uri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("fronius-device-history-smartmeter-01.json", getClass()),
						ObjectNode.class), OK));

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should(times(3)).exchange(any(), eq(GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(httpEntityCaptor.getAllValues())
			.extracting(HttpEntity::getHeaders)
			.allSatisfy(headers -> {
				and.then(headers)
					.as("API ID provided in HTTP request header")
					.containsEntry(ACCESS_KEY_ID_HEADER, List.of(accessKeyId))
					.as("API secret provided in HTTP request header")
					.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(accessKeySecret))
					;
			})
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
					;
			})
			.satisfies(list -> {
				DatumSamples expectedSamplesInv1 = new DatumSamples();
				expectedSamplesInv1.putInstantaneousSampleValue("wh_exp", 1);
				expectedSamplesInv1.putInstantaneousSampleValue("wh_imp", 2);

				and.then(list)
					.filteredOn(d -> inv1SourceId.equals(d.getSourceId()))
					.as("Most recent inverter datum returned")
					.hasSize(1)
					.element(0)
					.as("Datum timestamp from inverter JSON response")
					.returns(Instant.parse("2025-03-23T01:25:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamplesInv1, from(Datum::asSampleOperations))
					;

				DatumSamples expectedSamplesMet1 = new DatumSamples();
				expectedSamplesMet1.putInstantaneousSampleValue("wh_exp", 12);
				expectedSamplesMet1.putInstantaneousSampleValue("wh_imp", 199);

				and.then(list)
					.filteredOn(d -> met1SourceId.equals(d.getSourceId()))
					.as("Most recent meter datum returned")
					.hasSize(1)
					.element(0)
					.as("Datum timestamp from meter JSON response")
					.returns(Instant.parse("2025-03-23T20:25:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamplesMet1, from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_mappedSourceId_withPlaceholders_multiDay_limitedPageSize_incompleteLastDay() {
		// GIVEN
		final String accessKeyId = randomString();
		final String accessKeySecret = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, accessKeyId,
				ACCESS_KEY_SECRET_SETTING, accessKeySecret
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties for energy imp/exp, that support both inverter and meter channels

		final String channel1Name = "EnergyExported";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh_exp");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemDevicePlaceholderComponentValueRef(channel1Name));

		final String channel2Name = "EnergyImported";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("wh_imp");
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(systemDevicePlaceholderComponentValueRef(channel2Name));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String inv1SourceId = "inv/1";
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		datumStream.setServiceProps(Map.of(SOURCE_ID_MAP_SETTING,
				Map.of("/%s/%s".formatted(systemId, device1Id), inv1SourceId)));

		// HTTP requests: 3x for device: 2x first day paginated results, 1x 2nd day incomplete results

		final Instant startDate = clock.instant().minus(48, HOURS);
		final Instant endDate = clock.instant();

		// @formatter:off
		URI expectedDevice1Day1Page1Uri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(DEVICE_HISTORY_URL_TEMPLATE)
				.queryParam(START_AT_PARAM, startDate)
				.queryParam(END_AT_PARAM, startDate.plus(24, HOURS))
				.queryParam(OFFSET_PARAM, 0L)
				.queryParam(LIMIT_PARAM, 2)
				.buildAndExpand(systemId, device1Id)
				.toUri()
				;

		URI expectedDevice1Day1Page2Uri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(DEVICE_HISTORY_URL_TEMPLATE)
				.queryParam(START_AT_PARAM, startDate)
				.queryParam(END_AT_PARAM, startDate.plus(24, HOURS))
				.queryParam(OFFSET_PARAM, 2L)
				.queryParam(LIMIT_PARAM, 2)
				.buildAndExpand(systemId, device1Id)
				.toUri()
				;

		URI expectedDevice1Day2Page1Uri = UriComponentsBuilder.fromUri(BASE_URI)
				.path(DEVICE_HISTORY_URL_TEMPLATE)
				.queryParam(START_AT_PARAM, startDate.plus(24, HOURS))
				.queryParam(END_AT_PARAM, endDate)
				.queryParam(OFFSET_PARAM, 0L)
				.queryParam(LIMIT_PARAM, 2)
				.buildAndExpand(systemId, device1Id)
				.toUri()
				;

		// @formatter:on

		given(restOps.exchange(eq(expectedDevice1Day1Page1Uri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("fronius-device-history-inverter-02.json", getClass()),
						ObjectNode.class), OK));

		given(restOps.exchange(eq(expectedDevice1Day1Page2Uri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("fronius-device-history-inverter-03.json", getClass()),
						ObjectNode.class), OK));

		given(restOps.exchange(eq(expectedDevice1Day2Page1Uri), eq(GET), any(), eq(JsonNode.class)))
				.willReturn(new ResponseEntity<>(getObjectFromJSON(
						utf8StringResource("fronius-device-history-inverter-04.json", getClass()),
						ObjectNode.class), OK));

		// WHEN
		service.setQueryLimit(2);

		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(3)).exchange(any(), eq(GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(httpEntityCaptor.getAllValues())
			.extracting(HttpEntity::getHeaders)
			.allSatisfy(headers -> {
				and.then(headers)
					.as("API ID provided in HTTP request header")
					.containsEntry(ACCESS_KEY_ID_HEADER, List.of(accessKeyId))
					.as("API secret provided in HTTP request header")
					.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(accessKeySecret))
					;
			})
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is filter start date truncated to hours")
			.returns(filter.getStartDate().truncatedTo(HOURS), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is filter end date truncated to hours")
			.returns(filter.getEndDate().truncatedTo(HOURS), from(DateRangeCriteria::getEndDate))
			;

		and.then(result)
			.as("Datum parsed from HTTP responses")
			.hasSize(5)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream sourceIdMap")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.extracting(Datum::getTimestamp)
			.as("All datum from paginated HTTP requests returned")
			.containsExactly(
					  Instant.parse("2025-03-23T01:25:00Z")
					, Instant.parse("2025-03-23T01:30:00Z")
					, Instant.parse("2025-03-23T01:35:00Z")
					, Instant.parse("2025-03-23T01:40:00Z")
					, Instant.parse("2025-03-24T01:25:00Z")
					)
			;
		// @formatter:on
	}

	@Test
	public void datum_largeDateRange_nextQueryFilter() {
		// GIVEN
		final String accessKeyId = randomString();
		final String accessKeySecret = randomString();
		final String systemId = randomString();
		final String device1Id = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				ACCESS_KEY_ID_SETTING, accessKeyId,
				ACCESS_KEY_SECRET_SETTING, accessKeySecret
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping

		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties for energy imp/exp, that support both inverter and meter channels

		final String channel1Name = "EnergyExported";
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("wh_exp");
		prop1.setScale(0);
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(systemDevicePlaceholderComponentValueRef(channel1Name));

		final String channel2Name = "EnergyImported";
		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Instantaneous);
		prop2.setPropertyName("wh_imp");
		prop2.setScale(0);
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(systemDevicePlaceholderComponentValueRef(channel2Name));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String inv1SourceId = "inv/1";
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		datumStream.setServiceProps(Map.of(SOURCE_ID_MAP_SETTING,
				Map.of("/%s/%s".formatted(systemId, device1Id), inv1SourceId)));

		// HTTP requests: 1x for device day, max 7 days

		final String responseJsonTemplate = """
				{
				  "pvSystemId": "ced6f980-8907-4128-87ea-000000000000",
				  "deviceId": "3b482b62-1754-48f1-a0ca-000000000000",
				  "data": [
				    {
				      "logDateTime": %s,
				      "logDuration": 300,
				      "channels": [
				        {
				          "channelName": "EnergyExported",
				          "channelType": "Energy",
				          "unit": "Wh",
				          "value": 1.0
				        },
				        {
				          "channelName": "EnergyImported",
				          "channelType": "Energy",
				          "unit": "Wh",
				          "value": 2.0
				        }
				      ]
				    }
				  ],
				  "links": {
				    "first": "https://api",
				    "prev": null,
				    "self": "https://api",
				    "next": null,
				    "last": "https://api",
				    "totalItemsCount": 1
				  }
				}
				""";

		final Instant startDate = clock.instant().minus(8, DAYS);
		final Instant endDate = clock.instant();
		final Instant usedEndDate = startDate.plus(7, DAYS);

		List<Instant> datumTimestamps = new ArrayList<>();

		for ( int i = 0; i < 7; i++ ) {
			// @formatter:off
			URI expectedDevice1DayUri = UriComponentsBuilder.fromUri(BASE_URI)
					.path(DEVICE_HISTORY_URL_TEMPLATE)
					.queryParam(START_AT_PARAM, startDate.plus(i, DAYS))
					.queryParam(END_AT_PARAM, startDate.plus(i + 1, DAYS))
					.queryParam(OFFSET_PARAM, 0L)
					.queryParam(LIMIT_PARAM, DEFAULT_QUERY_LIMIT)
					.buildAndExpand(systemId, device1Id)
					.toUri()
					;
			// @formatter:on

			Instant datumTimestamp = startDate.plus(i + 1, DAYS).minusSeconds(300);
			datumTimestamps.add(datumTimestamp);

			given(restOps.exchange(eq(expectedDevice1DayUri), eq(GET), any(), eq(JsonNode.class)))
					.willReturn(new ResponseEntity<>(getObjectFromJSON(
							responseJsonTemplate
									.formatted(JsonUtils.getJSONString(datumTimestamp.toString())),
							ObjectNode.class), OK));

		}

		// WHEN
		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(7)).exchange(any(), eq(GET), httpEntityCaptor.capture(), eq(JsonNode.class));

		and.then(httpEntityCaptor.getAllValues())
			.extracting(HttpEntity::getHeaders)
			.allSatisfy(headers -> {
				and.then(headers)
					.as("API ID provided in HTTP request header")
					.containsEntry(ACCESS_KEY_ID_HEADER, List.of(accessKeyId))
					.as("API secret provided in HTTP request header")
					.containsEntry(ACCES_KEY_SECRET_HEADER, List.of(accessKeySecret))
					;
			})
			;

		and.then(result.getUsedQueryFilter())
			.as("Used query filter provided")
			.isNotNull()
			.as("Used query start date is filter start date truncated to hours")
			.returns(filter.getStartDate().truncatedTo(HOURS), from(DateRangeCriteria::getStartDate))
			.as("Used query end date is filter end date truncated to maximum filter time range")
			.returns(usedEndDate, from(DateRangeCriteria::getEndDate))
			;

		and.then(result)
			.as("Datum parsed from HTTP responses (one per day)")
			.hasSize(7)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream sourceIdMap")
					.returns(inv1SourceId, from(Datum::getSourceId))
					;
			})
			.extracting(Datum::getTimestamp)
			.as("All datum from day HTTP requests returned")
			.containsExactlyElementsOf(datumTimestamps)
			;
		// @formatter:on
	}

}
