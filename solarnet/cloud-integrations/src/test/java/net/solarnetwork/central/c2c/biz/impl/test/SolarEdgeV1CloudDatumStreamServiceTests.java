/* ==================================================================
 * SolarEdgeV1CloudDatumStreamServiceTests.java - 24/10/2024 9:33:49 am
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
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Battery;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Meter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeResolution.FifteenMinute;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService.SITE_ID_FILTER;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_ALT;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeResolution;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudIntegrationService;
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
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link SolarEdgeV1CloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SolarEdgeV1CloudDatumStreamServiceTests {

	private static final Logger log = LoggerFactory
			.getLogger(SolarEdgeV1CloudDatumStreamServiceTests.class);

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

	@Mock
	private DatumEntityDao datumDao;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Captor
	private ArgumentCaptor<DatumCriteria> criteriaCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now(), UTC);

	private SolarEdgeV1CloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.JSON_OBJECT_MAPPER;

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SolarEdgeV1CloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolarEdgeV1CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		service.setDatumDao(datumDao);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);

		clock.setInstant(Instant.now().truncatedTo(ChronoUnit.DAYS));
	}

	private static String componentValueRef(Object siteId, SolarEdgeDeviceType deviceType,
			Object componentId, String fieldName) {
		return "/%s/%s/%s/%s".formatted(siteId, deviceType.getKey(), componentId, fieldName);
	}

	private static String placeholderComponentValueRef(SolarEdgeDeviceType deviceType,
			String fieldName) {
		return "/{siteId}/%s/{componentId}/%s".formatted(deviceType.getKey(), fieldName);
	}

	@Test
	public void requestLatest() throws IOException {
		// GIVEN
		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final String meterComponentId = "Production";
		final String batteryComponentId = "11111111111111111111111";
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		c1p1.setEnabled(true);
		c1p1.setPropertyType(DatumSamplesType.Instantaneous);
		c1p1.setPropertyName("watts");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId, "W"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Accumulating);
		c1p2.setPropertyName("wattHours");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId, "TotWhExp"));

		final CloudDatumStreamPropertyConfiguration c2p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c2p1.setEnabled(true);
		c2p1.setPropertyType(DatumSamplesType.Instantaneous);
		c2p1.setPropertyName("watts");
		c2p1.setValueType(CloudDatumStreamValueType.Reference);
		c2p1.setValueReference(componentValueRef(siteId, Meter, meterComponentId, "W"));

		final CloudDatumStreamPropertyConfiguration c2p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c2p2.setEnabled(true);
		c2p2.setPropertyType(DatumSamplesType.Accumulating);
		c2p2.setPropertyName("wattHours");
		c2p2.setValueType(CloudDatumStreamValueType.Reference);
		c2p2.setValueReference(componentValueRef(siteId, Meter, meterComponentId, "TotWh"));

		final CloudDatumStreamPropertyConfiguration c3p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c3p1.setEnabled(true);
		c3p1.setPropertyType(DatumSamplesType.Instantaneous);
		c3p1.setPropertyName("watts");
		c3p1.setValueType(CloudDatumStreamValueType.Reference);
		c3p1.setValueReference(componentValueRef(siteId, Battery, batteryComponentId, "W"));

		final CloudDatumStreamPropertyConfiguration c3p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c3p2.setEnabled(true);
		c3p2.setPropertyType(DatumSamplesType.Accumulating);
		c3p2.setPropertyName("wattHours");
		c3p2.setValueType(CloudDatumStreamValueType.Reference);
		c3p2.setValueReference(componentValueRef(siteId, Battery, batteryComponentId, "TotWhExp"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2, c2p1, c2p2, c3p1, c3p2));

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
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId), "INV/1",
						"/%s/%s/%s".formatted(siteId, Meter.getKey(), meterComponentId), "MET/1",
						"/%s/%s/%s".formatted(siteId, Battery.getKey(), batteryComponentId), "BAT/1"
				)
		));
		// @formatter:on

		// request site time zone info
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-01.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = clock.instant().atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = expectedEndDate.minus(FifteenMinute.getTickDuration());
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// request meter power data
		final JsonNode meterPowerDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-power-data-01.json", getClass()));
		final var meterPowerDataRes = new ResponseEntity<JsonNode>(meterPowerDataJson, HttpStatus.OK);

		// request meter energy data
		final JsonNode meterEnergyDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-energy-data-01.json", getClass()));
		final var meterEnergyDataRes = new ResponseEntity<JsonNode>(meterEnergyDataJson, HttpStatus.OK);

		// request battery data
		final JsonNode batteryDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-storage-data-01.json", getClass()));
		final var storageDataRes = new ResponseEntity<JsonNode>(batteryDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes).willReturn(meterPowerDataRes).willReturn(meterEnergyDataRes)
				.willReturn(storageDataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should(times(5)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri(),

					// inverter data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId)
						.toUri(),

					// meter power data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.POWER_DETAILS_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
						.buildAndExpand(siteId)
						.toUri(),

					// meter energy data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.METERS_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
						.buildAndExpand(siteId).toUri(),

					// battery data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.STORAGE_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId).toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(12)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// battery
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("BAT/1", from(Datum::getSourceId))
					.as("Timestamp from battery data")
					.returns(timestampFmt.parse("2024-10-23 16:19:30", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from battery data")
					.returns(new DatumSamples(Map.of(
								"watts", 0
							), Map.of(
								"wattHours", 5510545
							), null),
						Datum::asSampleOperations)
					;

				// inverter
				and.then(list).element(6)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2024-10-23 16:19:30", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of(
								"watts", 2011
							), Map.of(
								"wattHours", 37779200
							), null),
						Datum::asSampleOperations)
					;
				// meter
				and.then(list).element(10)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("MET/1", from(Datum::getSourceId))
					.as("Timestamp from clock-aligned meter data")
					.returns(timestampFmt.parse("2024-10-23 16:15:00", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from merged meter power and energy data")
					.returns(new DatumSamples(Map.of(
								"watts", 1720.0271f
							), Map.of(
								"wattHours", 37539808
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_multipleInverters_withExpression() throws IOException {
		// GIVEN
		final Long siteId = randomLong();
		final String inverterComponentId1 = randomString();
		final String inverterComponentId2 = randomString();
		final String meterComponentId = "Production";
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1a = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		c1p1a.setEnabled(true);
		c1p1a.setPropertyType(DatumSamplesType.Instantaneous);
		c1p1a.setPropertyName("watts");
		c1p1a.setValueType(CloudDatumStreamValueType.Reference);
		c1p1a.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId1, "W"));

		final CloudDatumStreamPropertyConfiguration c1p2a = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2a.setEnabled(true);
		c1p2a.setPropertyType(DatumSamplesType.Accumulating);
		c1p2a.setPropertyName("wattHours");
		c1p2a.setValueType(CloudDatumStreamValueType.Reference);
		c1p2a.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId1, "TotWhExp"));

		final CloudDatumStreamPropertyConfiguration c1p1b = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c1p1b.setEnabled(true);
		c1p1b.setPropertyType(DatumSamplesType.Instantaneous);
		c1p1b.setPropertyName("watts");
		c1p1b.setValueType(CloudDatumStreamValueType.Reference);
		c1p1b.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId2, "W"));

		final CloudDatumStreamPropertyConfiguration c1p2b = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c1p2b.setEnabled(true);
		c1p2b.setPropertyType(DatumSamplesType.Accumulating);
		c1p2b.setPropertyName("wattHours");
		c1p2b.setValueType(CloudDatumStreamValueType.Reference);
		c1p2b.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId2, "TotWhExp"));

		final CloudDatumStreamPropertyConfiguration c2p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 5, now());
		c2p1.setEnabled(true);
		c2p1.setPropertyType(DatumSamplesType.Instantaneous);
		c2p1.setPropertyName("watts");
		c2p1.setValueType(CloudDatumStreamValueType.Reference);
		c2p1.setValueReference(componentValueRef(siteId, Meter, meterComponentId, "W"));

		final CloudDatumStreamPropertyConfiguration c2p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 6, now());
		c2p2.setEnabled(true);
		c2p2.setPropertyType(DatumSamplesType.Accumulating);
		c2p2.setPropertyName("wattHours");
		c2p2.setValueType(CloudDatumStreamValueType.Reference);
		c2p2.setValueReference(componentValueRef(siteId, Meter, meterComponentId, "TotWh"));

		final CloudDatumStreamPropertyConfiguration c2p3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 7, now());
		c2p3.setEnabled(true);
		c2p3.setPropertyType(DatumSamplesType.Instantaneous);
		c2p3.setPropertyName("invWattsTot");
		c2p3.setValueType(CloudDatumStreamValueType.SpelExpression);
		c2p3.setValueReference("""
				sourceId.contains("MET") ? sum(latestMatching("INV/*", timestamp).![watts]) : null
				""");

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1a, c1p2a, c1p1b, c1p2b, c2p1, c2p2, c2p3));

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
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1), "INV/1",
						"/%s/%s/%s".formatted(siteId, Meter.getKey(), meterComponentId), "MET/1",
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2), "INV/2"
				)
		));
		// @formatter:on

		// request site time zone info
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-01.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = clock.instant().atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = expectedEndDate.minus(FifteenMinute.getTickDuration());
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter 1 data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// request inverter 2 data
		final JsonNode inverterDataJson2 = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-01a.json", getClass()));
		final var inverterDataRes2 = new ResponseEntity<JsonNode>(inverterDataJson2, HttpStatus.OK);

		// request meter power data
		final JsonNode meterPowerDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-power-data-01.json", getClass()));
		final var meterPowerDataRes = new ResponseEntity<JsonNode>(meterPowerDataJson, HttpStatus.OK);

		// request meter energy data
		final JsonNode meterEnergyDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-energy-data-01.json", getClass()));
		final var meterEnergyDataRes = new ResponseEntity<JsonNode>(meterEnergyDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes).willReturn(inverterDataRes2).willReturn(meterPowerDataRes)
				.willReturn(meterEnergyDataRes);

		var streamMeta1 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, "INV/1", new String[] { "watts" },
				new String[] { "wattHours" }, null);
		var streamMeta2 = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
				ObjectDatumKind.Node, nodeId, "INV/2", new String[] { "watts" },
				new String[] { "wattHours" }, null);

		var datumEntity1Watts = new BigDecimal("1001");
		var datumEntity1 = new DatumEntity(streamMeta1.getStreamId(),
				Instant.parse("2024-10-23T16:19:20Z"), null,
				propertiesOf(new BigDecimal[] { datumEntity1Watts },
						new BigDecimal[] { new BigDecimal("1001001") }, null, null));
		var filterResults1 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta1.getStreamId(), streamMeta1, streamMeta2.getStreamId(), streamMeta2),
				List.of(datumEntity1));

		var gapFillResults1 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta1.getStreamId(), streamMeta1, streamMeta2.getStreamId(), streamMeta2),
				List.of());

		var datumEntity2Watts = new BigDecimal("2002");
		var datumEntity2 = new DatumEntity(streamMeta1.getStreamId(),
				Instant.parse("2024-10-23T16:19:20Z"), null,
				propertiesOf(new BigDecimal[] { datumEntity2Watts },
						new BigDecimal[] { new BigDecimal("2002002") }, null, null));
		var filterResults2 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta1.getStreamId(), streamMeta1, streamMeta2.getStreamId(), streamMeta2),
				List.of(datumEntity2));

		var gapFillResults2 = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				Map.of(streamMeta1.getStreamId(), streamMeta1, streamMeta2.getStreamId(), streamMeta2),
				List.of());

		// actual order of DAO queries is not determinate, so have to set up argument filters

		given(datumDao.findFiltered(argThat(c -> {
			return c != null && streamMeta1.getSourceId().equals(c.getSourceId()) && c.getMax() != null
					&& c.getMax() == 1;
		}))).willReturn(filterResults1);

		given(datumDao.findFiltered(argThat(c -> {
			return c != null && streamMeta1.getSourceId().equals(c.getSourceId())
					&& (c.getMax() == null || c.getMax() > 1);
		}))).willReturn(gapFillResults1);

		given(datumDao.findFiltered(argThat(c -> {
			return c != null && streamMeta2.getSourceId().equals(c.getSourceId()) && c.getMax() != null
					&& c.getMax() == 1;
		}))).willReturn(filterResults2);

		given(datumDao.findFiltered(argThat(c -> {
			return c != null && streamMeta2.getSourceId().equals(c.getSourceId())
					&& (c.getMax() == null || c.getMax() > 1);
		}))).willReturn(gapFillResults2);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should(times(5)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri(),

					// inverter 1 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId1)
						.toUri(),

					// inverter 2 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId2)
						.toUri(),

					// meter power data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.POWER_DETAILS_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
						.buildAndExpand(siteId)
						.toUri(),

					// meter energy data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.METERS_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
						.buildAndExpand(siteId).toUri()
			)
			;

		then(datumDao).should(times(4)).findFiltered(criteriaCaptor.capture());
		and.then(criteriaCaptor.getAllValues())
			.hasSize(4)
			.satisfies(l -> {
				final Instant genDatumDate = Instant.parse("2024-10-23T20:15:00Z");
				and.then(l)
					.filteredOn(c -> streamMeta1.getSourceId().equals(c.getSourceId()) && c.getMax() != null && c.getMax() == 1)
					.as("Query for INV1")
					.hasSize(1)
					.element(0)
					.as("Query for user")
					.returns(TEST_USER_ID, from(DatumCriteria::getUserId))
					.as("Query for stream node")
					.returns(nodeId, from(DatumCriteria::getNodeId))
					.as("Query for INV1 stream source")
					.returns(streamMeta1.getSourceId(), from(DatumCriteria::getSourceId))
					.as("Query for at most 1 datum as GEN timestamp before oldest available")
					.returns(1, from(DatumCriteria::getMax))
					.as("Query end date is from GEN timestamp +1ms for <=")
					.returns(genDatumDate.plusMillis(1), from(DatumCriteria::getEndDate))
					;
				final Instant inv1OldestDate = Instant.parse("2024-10-23T20:19:30Z");
				and.then(l)
					.filteredOn(c -> streamMeta1.getSourceId().equals(c.getSourceId()) && (c.getMax() == null || c.getMax() > 1))
					.as("Query for INV1 gapfill")
					.hasSize(1)
					.element(0)
					.as("Gap fill query for user")
					.returns(TEST_USER_ID, from(DatumCriteria::getUserId))
					.as("Gap fill query for stream node")
					.returns(nodeId, from(DatumCriteria::getNodeId))
					.as("Gap fill query for INV1 stream source")
					.returns(streamMeta1.getSourceId(), from(DatumCriteria::getSourceId))
					.as("Gap fill query end date is INV1 oldest available")
					.returns(inv1OldestDate, from(DatumCriteria::getEndDate))
					.as("Gap fill query start date is first discovered INV1 datum +1ms for >")
					.returns(datumEntity1.getTimestamp().plusMillis(1), from(DatumCriteria::getStartDate))
					;
				and.then(l)
					.filteredOn(c -> streamMeta2.getSourceId().equals(c.getSourceId()) && c.getMax() != null && c.getMax() == 1)
					.as("Query for INV2")
					.hasSize(1)
					.element(0)
					.as("Query for user")
					.returns(TEST_USER_ID, from(DatumCriteria::getUserId))
					.as("Query for stream node")
					.returns(nodeId, from(DatumCriteria::getNodeId))
					.as("Query for INV2 stream source")
					.returns(streamMeta2.getSourceId(), from(DatumCriteria::getSourceId))
					.as("Query for at most 1 datum as GEN timestamp before oldest available")
					.returns(1, from(DatumCriteria::getMax))
					.as("Query end date is from GEN timestamp +1ms for <=")
					.returns(genDatumDate.plusMillis(1), from(DatumCriteria::getEndDate))
					;
				final Instant inv2OldestDate = Instant.parse("2024-10-23T20:17:30Z");
				and.then(l)
					.filteredOn(c -> streamMeta2.getSourceId().equals(c.getSourceId()) && (c.getMax() == null || c.getMax() > 1))
					.as("Query for INV2 gapfill")
					.hasSize(1)
					.element(0)
					.as("Gap fill query for user")
					.returns(TEST_USER_ID, from(DatumCriteria::getUserId))
					.as("Gap fill query for stream node")
					.returns(nodeId, from(DatumCriteria::getNodeId))
					.as("Gap fill query for INV2 stream source")
					.returns(streamMeta2.getSourceId(), from(DatumCriteria::getSourceId))
					.as("Gap fill query end date is INV2 oldest available")
					.returns(inv2OldestDate, from(DatumCriteria::getEndDate))
					.as("Gap fill query start date is first discovered INV2 datum +1ms for >")
					.returns(datumEntity2.getTimestamp().plusMillis(1), from(DatumCriteria::getStartDate))
					;
			})
			;


		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(10)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// inverter 1
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2024-10-23 16:19:30", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of(
								"watts", 2011
							), Map.of(
								"wattHours", 37779200
							), null),
						Datum::asSampleOperations)
					;
				// inverter 2
				and.then(list).element(4)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/2", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2024-10-23 16:17:30", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of(
								"watts", 2011
							), Map.of(
								"wattHours", 37779200
							), null),
						Datum::asSampleOperations)
					;
				// meter
				and.then(list).element(8)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("MET/1", from(Datum::getSourceId))
					.as("Timestamp from clock-aligned meter data")
					.returns(timestampFmt.parse("2024-10-23 16:15:00", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from merged meter power and energy data")
					.returns(new DatumSamples(Map.of(
								"watts", 1720.0271f,
								"invWattsTot", datumEntity1Watts.add(datumEntity2Watts).intValue()
							), Map.of(
								"wattHours", 37539808
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@SuppressWarnings("unchecked")
	@Test
	public void simulation_inverterSumExpression() throws IOException {
		// GIVEN
		final Long siteId = 2883L;

		// the order of these is set to match the order returned by the solaredge-v1-site-inventory-03.json data,
		// because wildcard values will be resolved in that order and we need the requests to align with our
		// test expectations
		final List<String> inverterComponentIds = List.of("7E140000-03", "7E140000-01", "7E140000-06",
				"7E140000-02", "7E140000-07", "7E140000-05", "7E140000-04");

		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration invWattsProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		invWattsProp.setEnabled(true);
		invWattsProp.setPropertyType(DatumSamplesType.Instantaneous);
		invWattsProp.setPropertyName("watts");
		invWattsProp.setValueType(CloudDatumStreamValueType.Reference);
		invWattsProp.setValueReference("/{siteId}/inv/*/W");

		final CloudDatumStreamPropertyConfiguration invWattHoursProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		invWattHoursProp.setEnabled(true);
		invWattHoursProp.setPropertyType(DatumSamplesType.Accumulating);
		invWattHoursProp.setPropertyName("wattHours");
		invWattHoursProp.setValueType(CloudDatumStreamValueType.Reference);
		invWattHoursProp.setValueReference("/{siteId}/inv/*/TotWhExp");

		final CloudDatumStreamPropertyConfiguration meterWattsProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		meterWattsProp.setEnabled(true);
		meterWattsProp.setPropertyType(DatumSamplesType.Instantaneous);
		meterWattsProp.setPropertyName("watts");
		meterWattsProp.setValueType(CloudDatumStreamValueType.Reference);
		meterWattsProp.setValueReference("/{siteId}/met/*/W");

		final CloudDatumStreamPropertyConfiguration meterWattsExprProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		meterWattsExprProp.setEnabled(true);
		meterWattsExprProp.setPropertyType(DatumSamplesType.Instantaneous);
		meterWattsExprProp.setPropertyName("wattsInvSum");
		meterWattsExprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		meterWattsExprProp.setValueReference("""
				sourceId.contains("GEN") ? sum(latestMatching("INV/*", timestamp).![watts]) : null
				""");

		final CloudDatumStreamPropertyConfiguration meterWattHoursExprProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 5, now());
		meterWattHoursExprProp.setEnabled(true);
		meterWattHoursExprProp.setPropertyType(DatumSamplesType.Accumulating);
		meterWattHoursExprProp.setPropertyName("wattHoursInvSum");
		meterWattHoursExprProp.setValueType(CloudDatumStreamValueType.SpelExpression);
		meterWattHoursExprProp.setValueReference("""
				sourceId.contains("GEN") ? sum(latestMatching("INV/*", timestamp).![wattHours]) : null
				""");

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(invWattsProp, invWattHoursProp, meterWattsProp, meterWattsExprProp,
						meterWattHoursExprProp));

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
		datumStream.setServiceProps(JsonUtils.getStringMap("""
						{
						  "sourceIdMap": {
						    "/2883/met/Production":  "GEN/1",
						    "/2883/inv/7E140000-04": "INV/4",
						    "/2883/inv/7E140000-07": "INV/7",
						    "/2883/inv/7E140000-03": "INV/3",
						    "/2883/inv/7E140000-02": "INV/2",
						    "/2883/inv/7E140000-01": "INV/1",
						    "/2883/inv/7E140000-06": "INV/6",
						    "/2883/inv/7E140000-05": "INV/5"
						  },
						  "placeholders": {
						    "siteId": 2883
						  }
						}
						"""));
		// @formatter:on

		final List<URI> expectedUris = new ArrayList<>();
		final List<ResponseEntity<JsonNode>> responses = new ArrayList<>();

		// request site time zone info
		expectedUris
				.add(fromUri(BASE_URI).path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId).toUri());
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-03.json", getClass()));
		responses.add(new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK));

		// request site inventory to resolve inverter ref wildcards
		expectedUris.add(
				fromUri(BASE_URI).path(SolarEdgeV1CloudDatumStreamService.SITE_INVENTORY_URL_TEMPLATE)
						.buildAndExpand(siteId).toUri());
		final JsonNode siteInventoryJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-inventory-03.json", getClass()));
		responses.add(new ResponseEntity<JsonNode>(siteInventoryJson, HttpStatus.OK));

		// check change log for each device in inventory
		for ( String invComponentId : inverterComponentIds ) {
			expectedUris.add(fromUri(BASE_URI)
					.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_CHANGELOG_URL_TEMPLATE)
					.buildAndExpand(siteId, invComponentId).toUri());
			final JsonNode emptyChangeLog = objectMapper.readTree("""
					{
					  "ChangeLog": {
					    "count": 0,
					    "list": []
					  }
					}
					""");
			responses.add(new ResponseEntity<JsonNode>(emptyChangeLog, HttpStatus.OK));
		}

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = LocalDateTime.parse("2025-02-27T12:00:00")
				.atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = expectedEndDate.minus(FifteenMinute.getTickDuration());
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter data
		for ( String invComponentId : inverterComponentIds ) {
			expectedUris.add(fromUri(BASE_URI)
					.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
					.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
					.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
					.buildAndExpand(siteId, invComponentId).toUri());
			final String suffix = invComponentId.split("-")[1];
			final JsonNode inverterDataJson = objectMapper.readTree(utf8StringResource(
					"solaredge-v1-inverter-data-03-%s.json".formatted(suffix), getClass()));
			responses.add(new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK));
		}

		// request meter power data
		expectedUris.add(fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.POWER_DETAILS_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
				.buildAndExpand(siteId).toUri());
		final JsonNode meterPowerDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-power-data-03.json", getClass()));
		responses.add(new ResponseEntity<JsonNode>(meterPowerDataJson, HttpStatus.OK));

		// request meter energy data
		expectedUris.add(fromUri(BASE_URI).path(SolarEdgeV1CloudDatumStreamService.METERS_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
				.buildAndExpand(siteId).toUri());
		final JsonNode meterEnergyDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-energy-data-03.json", getClass()));
		responses.add(new ResponseEntity<JsonNode>(meterEnergyDataJson, HttpStatus.OK));

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(responses.get(0),
				responses.subList(1, responses.size()).toArray(ResponseEntity[]::new));

		// perform datum lookup to satisfy latestMatching('INV/*') expressions
		var inverterDatumStreamMetadatasByComponentId = new HashMap<UUID, ObjectDatumStreamMetadata>();
		var datumDaoMock = given(datumDao.findFiltered(any()));
		var datumDaoTimestamp = expectedStartDate.minusMinutes(5).toInstant();
		var allDaoDatum = new ArrayList<DatumEntity>();
		int i = 0;
		for ( String invComponentId : inverterComponentIds ) {
			int invOffset = Integer.parseInt(invComponentId.split("-")[1]);
			var streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland",
					ObjectDatumKind.Node, nodeId, "INV/%d".formatted(invOffset),
					new String[] { "watts" }, new String[] { "wattHours" }, null);
			inverterDatumStreamMetadatasByComponentId.put(streamMeta.getStreamId(), streamMeta);
			i++;
			var datumEntityWatts = new BigDecimal(1000 + i);
			var datumEntityWattHours = new BigDecimal((1000 + i) * 100 + i);
			var datumEntity = new DatumEntity(streamMeta.getStreamId(), datumDaoTimestamp, null,
					propertiesOf(new BigDecimal[] { datumEntityWatts },
							new BigDecimal[] { datumEntityWattHours }, null, null));
			var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
					inverterDatumStreamMetadatasByComponentId, List.of(datumEntity));
			var gapFillResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
					inverterDatumStreamMetadatasByComponentId, List.of());

			datumDaoMock = datumDaoMock.willReturn(filterResults).willReturn(gapFillResults);

			allDaoDatum.add(datumEntity);
		}

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.parse("2025-02-27T16:45:00Z"));
		filter.setEndDate(Instant.parse("2025-02-27T17:00:00Z"));
		Iterable<Datum> result = service.datum(datumStream, filter);

		log.info("Results: {}", JsonUtils.getJSONString(result));

		// THEN
		// @formatter:off
		then(restOps).should(times(responses.size())).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactlyElementsOf(expectedUris)
			;

		// will invoke DAO 2x per inverter stream
		then(datumDao).should(times(14)).findFiltered(criteriaCaptor.capture());

		var datumCriterias = criteriaCaptor.getAllValues();
		log.info("Datum searches: {}", JsonUtils.getJSONString(datumCriterias));

		var resultListSorted = StreamSupport.stream(result.spliterator(), false).sorted(
				Comparator.comparing(Datum::getSourceId).thenComparing(Datum::getTimestamp)).toList();

		and.then(resultListSorted)
			.as("Datum parsed from HTTP response, 3x inverters + 1 meter")
			.hasSize(7 * 3 + 1)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// meter
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("GEN/1", from(Datum::getSourceId))
					.as("Timestamp from clock-aligned meter data")
					.returns(timestampFmt.parse("2025-02-27 11:45:00", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from merged meter power and expressions")
					.returns(new DatumSamples(Map.of(
								"watts", 11059.199f,
								"wattsInvSum", (int)allDaoDatum.stream().mapToLong(
											d -> d.getProperties().instantaneousValue(0).longValue()).sum()
							), Map.of(
								"wattHoursInvSum", (int)allDaoDatum.stream().mapToLong(
										d -> d.getProperties().accumulatingValue(0).longValue()).sum()
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void testDateRangeHandling() throws IOException {
		// GIVEN
		final Instant endAt = Instant.parse("2025-02-28T02:00:38.696382784Z");
		final Instant startAt = Instant.parse("2025-02-28T01:30:00Z");
		clock.setInstant(endAt);

		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		c1p1.setEnabled(true);
		c1p1.setPropertyType(DatumSamplesType.Instantaneous);
		c1p1.setPropertyName("watts");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(componentValueRef(siteId, Inverter, inverterComponentId, "W"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1));

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
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId), "INV/1"
				)
		));
		// @formatter:on

		// request site time zone info
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-01.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = endAt.atZone(siteTimeZone).truncatedTo(ChronoUnit.HOURS);
		final ZonedDateTime expectedStartDate = startAt.atZone(siteTimeZone);
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startAt);
		filter.setEndDate(endAt);
		service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri(),

					// inverter data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId)
						.toUri()
			)
			;
		// @formatter:on
	}

	@Test
	public void datum_multiStreamLag_withinTolerance() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "7E140000-01";
		final String inverterComponentId2 = "7E140000-02";
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderComponentValueRef(Inverter, "W"));

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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				"INV/1");
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				"INV/2");

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(LocalDateTime.parse("2025-02-27T11:45:00").atZone(siteTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));

		// request site time zone info
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-03.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = filter.getEndDate().atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = filter.getStartDate().atZone(siteTimeZone);
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter 1 data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-03-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// request inverter 2 data
		final JsonNode inverterDataJson2 = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-03-01a.json", getClass()));
		final var inverterDataRes2 = new ResponseEntity<JsonNode>(inverterDataJson2, HttpStatus.OK);

		// note response order based on site details plan
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes).willReturn(inverterDataRes2);

		// WHEN

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(3)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri(),

					// inverter 1 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId1)
						.toUri(),

					// inverter 2 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId2)
						.toUri()
			)
			;

		then(datumDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(5)
			.satisfies(_ -> {
				and.then(result.getNextQueryFilter())
					.as("Next query filter returned")
					.isNotNull()
					.as("11:45 returned, as the least of all greatest timestamps per stream, truncated at 15min")
					.returns(LocalDateTime.parse("2025-02-27T11:45:00").atZone(siteTimeZone).toInstant(),
							from(CloudDatumStreamQueryFilter::getStartDate))
					;
			})
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// first - inverter 1
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2025-02-27 11:46:04", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of("watts", 1557), null, null), from(Datum::asSampleOperations))
					;
				// last - inverter 2
				and.then(list).element(4)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/2", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2025-02-27 11:51:04", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of("watts", 1549), null, null), from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_multiStreamLag_outsideTolerance() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "7E140000-01";
		final String inverterComponentId2 = "7E140000-02";
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(placeholderComponentValueRef(Inverter, "W"));

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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				"INV/1");
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				"INV/2");

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(LocalDateTime.parse("2025-02-27T11:45:00").atZone(siteTimeZone).toInstant());
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));

		// request site time zone info
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-03.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = filter.getEndDate().atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = filter.getStartDate().atZone(siteTimeZone);
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter 1 data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-03-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// request inverter 2 data
		final JsonNode inverterDataJson2 = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-03-01a.json", getClass()));
		final var inverterDataRes2 = new ResponseEntity<JsonNode>(inverterDataJson2, HttpStatus.OK);

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes).willReturn(inverterDataRes2);

		// WHEN

		// setup clock to be far after end of requested data period (outside lag tolerance)
		clock.setInstant(filter.getEndDate().plus(365L, ChronoUnit.DAYS));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(3)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri(),

					// inverter 1 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId1)
						.toUri(),

					// inverter 2 data
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
						.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
						.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
						.buildAndExpand(siteId, inverterComponentId2)
						.toUri()
			)
			;

		then(datumDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(5)
			.satisfies(_ -> {
				and.then(result.getNextQueryFilter())
					.as("No next query filter returned because clock is beyond multi stream lag tolerance")
					.isNull()
					;
			})
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// first - inverter 1
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2025-02-27 11:46:04", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of("watts", 1557), null, null), from(Datum::asSampleOperations))
					;
				// last - inverter 2
				and.then(list).element(4)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/2", from(Datum::getSourceId))
					.as("Timestamp from inverter data")
					.returns(timestampFmt.parse("2025-02-27 11:51:04", Instant::from), from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of("watts", 1549), null, null), from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void dataValue_replacedInverter() throws IOException {
		// GIVEN
		final Long siteId = randomLong();
		final String inverterComponentId = "7E140000-01"; // from site-inventory-01.json
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// request site inventory to resolve data values
		final JsonNode siteInventoryJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-inventory-01.json", getClass()));
		final var siteInventoryRes = new ResponseEntity<JsonNode>(siteInventoryJson, HttpStatus.OK);

		// check change log for each device in inventory
		final JsonNode emptyChangeLog = objectMapper
				.readTree(utf8StringResource("solaredge-v1-changelog-01.json", getClass()));
		final var emptyChangeLogRes = new ResponseEntity<JsonNode>(emptyChangeLog, HttpStatus.OK);

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteInventoryRes)
				.willReturn(emptyChangeLogRes);

		// WHEN
		Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SITE_ID_FILTER, siteId));

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API token header")
					.containsEntry(SolarEdgeV1CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site inventory
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.SITE_INVENTORY_URL_TEMPLATE)
						.buildAndExpand(siteId).toUri(),

					// change log
					fromUri(BASE_URI)
						.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_CHANGELOG_URL_TEMPLATE)
						.buildAndExpand(siteId, inverterComponentId)
						.toUri()
			)
			;

		and.then(results)
			.as("Result generated for inverters")
			.hasSize(1)
			.element(0)
			.returns("Inverters", from(CloudDataValue::getName))
			.satisfies(d -> {
				and.then(d.getChildren())
					.as("Two inverters added (live and replaced)")
					.hasSize(3)
					.satisfies(l -> {
						// live inverter
						and.then(l).element(0)
							.as("Name parsed")
							.returns("Inverter 1", from(CloudDataValue::getName))
							.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
							.satisfies(m -> {
								var expectedMeta = new HashMap<String, Object>(16);
								expectedMeta.putAll(Map.of(
										CloudDataValue.MANUFACTURER_METADATA, "SolarEdge",
										CloudDataValue.DEVICE_MODEL_METADATA, "SE14.4K",
										CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA, "DSP1: 1.13.1938, DSP2: 1.13.1938, CPU: 3.2537.0",
										CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, inverterComponentId
										));
								and.then(m)
									.as("Metadata extracted")
									.containsExactlyInAnyOrderEntriesOf(expectedMeta)
									;
							})
							;

						// 1st replaced
						and.then(l).element(1)
							.as("Replaced name is serial number")
							.returns("6E140000-02", from(CloudDataValue::getName))
							.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
							.satisfies(m -> {
								var expectedMeta = new HashMap<String, Object>(16);
								expectedMeta.putAll(Map.of(
										CloudDataValue.REPLACED_BY_METADATA, "/%s/inv/%s".formatted(siteId, inverterComponentId),
										CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, "6E140000-02"
										));
								and.then(m)
									.as("Metadata extracted")
									.containsExactlyInAnyOrderEntriesOf(expectedMeta)
									;
							})
							;

						// 2nd replaced
						and.then(l).element(2)
							.as("Replaced name is serial number")
							.returns("6E140000-01", from(CloudDataValue::getName))
							.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
							.satisfies(m -> {
								var expectedMeta = new HashMap<String, Object>(16);
								expectedMeta.putAll(Map.of(
										CloudDataValue.REPLACED_BY_METADATA, "/%s/inv/%s".formatted(siteId, inverterComponentId),
										CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, "6E140000-01"
										));
								and.then(m)
									.as("Metadata extracted")
									.containsExactlyInAnyOrderEntriesOf(expectedMeta)
									;
							})
							;
					})
					;
			})
			;
		// @formatter:on
	}

}
