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
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Battery;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType.Meter;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeResolution.FifteenMinute;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.util.DateUtils.ISO_DATE_OPT_TIME_ALT;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeDeviceType;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeResolution;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SolarEdgeV1CloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SolarEdgeV1CloudDatumStreamServiceTests {

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

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private SolarEdgeV1CloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.newObjectMapper();

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SolarEdgeV1CloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolarEdgeV1CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	private static String componentValueRef(Object siteId, SolarEdgeDeviceType deviceType,
			Object componentId, String fieldName) {
		return "/%s/%s/%s/%s".formatted(siteId, deviceType.getKey(), componentId, fieldName);
	}

	@Test
	public void requestLatest() throws IOException {
		// GIVEN
		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final String meterComponentId = "Production";
		final String batteryComponentId = "11111111111111111111111";
		final ZoneId siteTimeZone = ZoneId.of("America/New_York");

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());

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
		final URI siteDetailsUri = fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
				.buildAndExpand(siteId).toUri();
		final JsonNode siteDetailsJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-site-details-01.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);
		given(restOps.exchange(eq(siteDetailsUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(siteDetailsRes);

		// expected date range is clock-aligned
		final ZonedDateTime expectedEndDate = clock.instant().atZone(siteTimeZone);
		final ZonedDateTime expectedStartDate = expectedEndDate.minus(FifteenMinute.getTickDuration());
		final DateTimeFormatter timestampFmt = ISO_DATE_OPT_TIME_ALT.withZone(siteTimeZone);

		// request inverter data
		final URI inverterDataUri = fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.EQUIPMENT_DATA_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.buildAndExpand(siteId, inverterComponentId).toUri();
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-inverter-data-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(inverterDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(inverterDataRes);

		// request meter power data
		final URI meterPowerDataUri = fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.POWER_DETAILS_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
				.buildAndExpand(siteId).toUri();
		final JsonNode meterPowerDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-power-data-01.json", getClass()));
		final var meterPowerDataRes = new ResponseEntity<JsonNode>(meterPowerDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(meterPowerDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(meterPowerDataRes);

		// request meter energy data
		final URI meterEnergyDataUri = fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.METERS_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.queryParam("timeUnit", SolarEdgeResolution.FifteenMinute.getKey())
				.buildAndExpand(siteId).toUri();
		final JsonNode meterEnergyDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-meter-energy-data-01.json", getClass()));
		final var meterEnergyDataRes = new ResponseEntity<JsonNode>(meterEnergyDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(meterEnergyDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(meterEnergyDataRes);

		// request battery data
		final URI batteryDataUri = fromUri(BASE_URI)
				.path(SolarEdgeV1CloudDatumStreamService.STORAGE_DATA_URL_TEMPLATE)
				.queryParam("startTime", timestampFmt.format(expectedStartDate.toLocalDateTime()))
				.queryParam("endTime", timestampFmt.format(expectedEndDate.toLocalDateTime()))
				.buildAndExpand(siteId).toUri();
		final JsonNode batteryDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v1-storage-data-01.json", getClass()));
		final var storageDataRes = new ResponseEntity<JsonNode>(batteryDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(batteryDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(storageDataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off

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
				// inverter
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
				// meter
				and.then(list).element(4)
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
				// battery
				and.then(list).element(6)
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
			})
			;
		// @formatter:on
	}

}
