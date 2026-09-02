/* ==================================================================
 * SolarEdgeV2CloudDatumStreamServiceTests.java - 14 Aug 2026 9:36:29 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.solaredge.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.test.CloudIntegrationTestUtils.timeGapValidationMetadata;
import static net.solarnetwork.central.c2c.biz.impl.test.CloudIntegrationTestUtils.timeGapValidationPropertyMetadata;
import static net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeDeviceType.Inverter;
import static net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.Reference;
import static net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType.SpelExpression;
import static net.solarnetwork.central.datum.domain.DatumValidationType.TIME_GAP_VALIDATION_TYPE;
import static net.solarnetwork.central.datum.support.QueryingDatumStreamsAccessor.DEFAULT_MAX_START_DATE_DURATION;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.jackson.JsonUtils.getObjectFromJSON;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.security.crypto.encrypt.Encryptors.noOpText;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeDeviceType;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeInverterTelemtry;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeResolution;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeTelemetryType;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudIntegrationService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
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
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.support.RetrySettings;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test cases for the {@link SolarEdgeV2CloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SolarEdgeV2CloudDatumStreamServiceTests implements CloudIntegrationsUserEvents {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

	private TextEncryptor encryptor = noOpText();

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
	private Cache<Long, ZoneId> siteTimeZoneCache;

	@Mock
	private Cache<Long, CloudDataValue[]> siteInventoryCache;

	@Captor
	private ArgumentCaptor<DatumCriteria> datumCriteriaCaptor;

	@Mock
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	@Captor
	private ArgumentCaptor<DatumCriteria> criteriaCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now(), UTC);

	private SolarEdgeV2CloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.JSON_OBJECT_MAPPER;

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SolarEdgeV2CloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolarEdgeV2CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		service.setDatumStreamMetadataDao(datumStreamMetadataDao);

		service.setSiteTimeZoneCache(siteTimeZoneCache);
		service.setSiteInventoryCache(siteInventoryCache);

		clock.setInstant(Instant.now().truncatedTo(ChronoUnit.DAYS));
	}

	private static String componentValueRef(Object siteId, SolarEdgeDeviceType deviceType,
			Object componentId, String fieldName) {
		return "/%s/%s/%s/%s".formatted(siteId, deviceType.getKey(), componentId, fieldName);
	}

	private static String placeholderComponentValueRef(SolarEdgeDeviceType deviceType,
			SolarEdgeTelemetryType field) {
		return "/{siteId}/%s/{componentId}/%s".formatted(deviceType.getKey(), field.name());
	}

	@Test
	public void dataValues_root() {
		// GIVEN
		final String apiKey = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("solaredge-v2-sites-01.json", getClass()), ObjectNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		final Iterable<CloudDataValue> results = service.dataValues(integration.getId(), Map.of());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list sites")
			.returns(fromUri(SolarEdgeV2CloudIntegrationService.BASE_URI)
					.path(SolarEdgeV2CloudIntegrationService.SITES_LIST_URL)
					.queryParam(SolarEdgeV2CloudDatumStreamService.SITES_MAX_RESULTS_PARAM, 1000)
					.buildAndExpand().toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes API key header")
			.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
			;

		and.then(results)
			.as("Result generated")
			.hasSize(2)
			.satisfies(l -> {
				and.then(l).element(0)
					.as("System name parsed")
					.returns("Smith, John CRM1234", from(CloudDataValue::getName))
					.as("System ID parsed")
					.returns(List.of("93082"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								CloudDataValue.STREET_ADDRESS_METADATA, "2888 Main St",
								CloudDataValue.LOCALITY_METADATA, "Green Bay",
								CloudDataValue.STATE_PROVINCE_METADATA, "Wisconsin",
								CloudDataValue.POSTAL_CODE_METADATA, "54311",
								CloudDataValue.COUNTRY_METADATA, "United States",
								CloudDataValue.START_DATE_METADATA, Instant.parse("2022-11-10T00:00:00Z"),
								CloudDataValue.RATED_POWER_METADATA, 6140,
								CloudDataValue.ACTIVE_METADATA, true
								));
						expectedMeta.putAll(Map.of(
								"activationStatus", "ACTIVE",
						        "notes", "Created via API, triggered from CRM"
								));
						and.then(m)
							.as("Metadata extracted")
							.containsExactlyInAnyOrderEntriesOf(expectedMeta)
							;
					})
					;
				and.then(l).element(1)
					.as("Site name parsed")
					.returns("American Solar Farm", from(CloudDataValue::getName))
					.as("Site ID parsed")
					.returns(List.of("1000000"), from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								CloudDataValue.STREET_ADDRESS_METADATA, "123 West Main Avenue",
								CloudDataValue.LOCALITY_METADATA, "Anytown",
								CloudDataValue.STATE_PROVINCE_METADATA, "New Jersey",
								CloudDataValue.POSTAL_CODE_METADATA, "07712",
								CloudDataValue.COUNTRY_METADATA, "United States",
								CloudDataValue.START_DATE_METADATA, Instant.parse("2025-01-30T01:02:03Z"),
								CloudDataValue.RATED_POWER_METADATA, 636320,
								CloudDataValue.ACTIVE_METADATA, false
								));
						expectedMeta.putAll(Map.of(
								"activationStatus", "PENDING"
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
	public void dataValues_site() {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		final JsonNode resJson = getObjectFromJSON(
				utf8StringResource("solaredge-v2-site-inventory-01.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN
		final Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SolarEdgeV2CloudDatumStreamService.SITE_ID_FILTER, siteId));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("URL is list site inventory")
			.returns(fromUri(SolarEdgeV2CloudIntegrationService.BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.SITE_INVENTORY_URL_TEMPLATE)
					.queryParam(SolarEdgeV2CloudDatumStreamService.TYPES_PARAM, SolarEdgeDeviceType.ALL_API_TYPES)
					.buildAndExpand(siteId).toUri(), from(RequestEntity::getUrl))
			.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
			.as("HTTP request includes API key header")
			.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
			;

		and.then(results)
			.as("Result generated with 1 'inv' group")
			.hasSize(1)
			.element(0)
			.as("Inverter group parsed")
			.returns(SolarEdgeDeviceType.Inverter.getGroupKey(), from(CloudDataValue::getName))
			.as("Identifier is site, group")
			.returns(List.of(siteId.toString(), SolarEdgeDeviceType.Inverter.getKey()),
					from(CloudDataValue::getIdentifiers))
			.as("Reference not returned for group value")
			.returns(null, from(CloudDataValue::getReference))
			.as("No metadata provided")
			.returns(null, from(CloudDataValue::getMetadata))
			.extracting(CloudDataValue::getChildren, list(CloudDataValue.class))
			.as("Inverter devices provided as children")
			.hasSize(18)
			.satisfies(l -> {
				and.then(l)
					.extracting(d -> d.getName() + ' ' + d.getIdentifiers().getLast())
					.containsExactly(
							"Inverter 1 AAAA86EB-06",
						    "Inverter 2 AAAA8AD0-E0",
						    "Inverter 3 AAAA8ACB-DB",
						    "Inverter 4 AAAA8DED-00",
						    "Inverter 5 AAAA8AB3-C3",
						    "Inverter 6 AAAA8C13-25",
						    "Inverter 7 AAAA8702-1E",
						    "Inverter 8 AAAA1E4B-F5",
						    "Inverter 8 AAAA86DD-F8",
						    "Inverter 9 AAAA8AB4-C4",
						    "Inverter 10 AAAA8DEA-FD",
						    "Inverter 10 AAAAD9D0-31",
						    "Inverter 11 AAAA8C07-19",
						    "Inverter 12 AAAA8BE7-F8",
						    "Inverter 13 AAAA8BF7-08",
						    "Inverter 14 AAAA31B0-AF",
						    "Inverter 14 AAAA8C02-14",
						    "Inverter 15 AAAA8BFE-0F"
					)
					;
				and.then(l).element(0)
					.as("Device name parsed")
					.returns("Inverter 1", from(CloudDataValue::getName))
					.as("Identifier is site, group, serial")
					.returns(List.of(siteId.toString(), SolarEdgeDeviceType.Inverter.getKey(), "AAAA86EB-06"),
							from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								  CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, "AAAA86EB-06"
								, CloudDataValue.MANUFACTURER_METADATA, "SolarEdge"
								, CloudDataValue.DEVICE_MODEL_METADATA, "SE33.3K-USR48BNU4"
								, CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA, "4.23.530"
								, CloudDataValue.ACTIVE_METADATA, true
								, CloudDataValue.ACTIVATED_AT_METADATA, Instant.parse("2023-07-27T13:10:11-04:00")
								, CloudDataValue.RATED_POWER_METADATA, 33300
								));
						expectedMeta.putAll(Map.of(
								  "communicationType", "ETHERNET"
						        , "connectedOptimizers", 55
								));
						and.then(m)
							.as("Metadata extracted")
							.containsExactlyInAnyOrderEntriesOf(expectedMeta)
							;
					})
					;
				and.then(l).element(11)
					.as("Device name parsed")
					.returns("Inverter 10", from(CloudDataValue::getName))
					.as("Identifier is site, group, serial")
					.returns(List.of(siteId.toString(), SolarEdgeDeviceType.Inverter.getKey(), "AAAAD9D0-31"),
							from(CloudDataValue::getIdentifiers))
					.as("Reference not returned for intermediate value")
					.returns(null, from(CloudDataValue::getReference))
					.as("No children provided")
					.returns(null, from(CloudDataValue::getChildren))
					.extracting(CloudDataValue::getMetadata, map(String.class, Object.class))
					.satisfies(m -> {
						var expectedMeta = new HashMap<String, Object>(16);
						expectedMeta.putAll(Map.of(
								  CloudDataValue.DEVICE_SERIAL_NUMBER_METADATA, "AAAAD9D0-31"
								, CloudDataValue.MANUFACTURER_METADATA, "SolarEdge"
								, CloudDataValue.DEVICE_MODEL_METADATA, "SE33.3K-USR8IBNZ4"
								, CloudDataValue.DEVICE_FIRMWARE_VERSION_METADATA, "4.24.518"
								, CloudDataValue.ACTIVE_METADATA, true
								, CloudDataValue.ACTIVATED_AT_METADATA, Instant.parse("2025-08-02T08:36:28-04:00")
								, CloudDataValue.RATED_POWER_METADATA, 33300
								, CloudDataValue.RELATED_IDENTIFIER_METADATA, "AAAA86EB-06"
								));
						expectedMeta.putAll(Map.of(
								  "communicationType", "RS485"
						        , "connectedOptimizers", 58
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
	public void dataValues_component_inverter() {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();
		final SolarEdgeDeviceType type = SolarEdgeDeviceType.Inverter;
		final String componentId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// WHEN
		final Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SolarEdgeV2CloudDatumStreamService.SITE_ID_FILTER, siteId,
						SolarEdgeV2CloudDatumStreamService.DEVICE_TYPE_FILTER, type.getKey(),
						SolarEdgeV2CloudDatumStreamService.COMPONENT_ID_FILTER, componentId));

		// THEN
		// @formatter:off
		then(restOps).shouldHaveNoInteractions();

		and.then(results)
			.as("Result generated fixed inverter properties")
			.hasSize(7)
			.allSatisfy(item -> {
				and.then(item)
					.as("Has 4 identifiers for site, type, component, property")
					.returns(4, d -> d.getIdentifiers().size())
					.as("First identifier is site ID")
					.returns(siteId.toString(), from(d -> d.getIdentifiers().get(0)))
					.as("Second identifier is device type")
					.returns(type.getKey(), from(d -> d.getIdentifiers().get(1)))
					.as("Third identifier is site ID")
					.returns(componentId, from(d -> d.getIdentifiers().get(2)))
					.as("Reference provided")
					.returns(componentValueRef(siteId, type, componentId, item.getIdentifiers().getLast()),
							from(CloudDataValue::getReference))
					;
			})
			.extracting(d -> d.getIdentifiers().getLast())
			.containsExactly(
					"Edel",
				    "Erec",
				    "Hz",
				    "I",
				    "Pdel",
				    "Prec",
				    "V"
			)
			;
		// @formatter:on
	}

	@Test
	public void dataValues_component_meter() {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();
		final SolarEdgeDeviceType type = SolarEdgeDeviceType.Meter;
		final String componentId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// WHEN
		final Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SolarEdgeV2CloudDatumStreamService.SITE_ID_FILTER, siteId,
						SolarEdgeV2CloudDatumStreamService.DEVICE_TYPE_FILTER, type.getKey(),
						SolarEdgeV2CloudDatumStreamService.COMPONENT_ID_FILTER, componentId));

		// THEN
		// @formatter:off
		then(restOps).shouldHaveNoInteractions();

		and.then(results)
			.as("Result generated fixed inverter properties")
			.hasSize(8)
			.allSatisfy(item -> {
				and.then(item)
					.as("Has 4 identifiers for site, type, component, property")
					.returns(4, d -> d.getIdentifiers().size())
					.as("First identifier is site ID")
					.returns(siteId.toString(), from(d -> d.getIdentifiers().get(0)))
					.as("Second identifier is device type")
					.returns(type.getKey(), from(d -> d.getIdentifiers().get(1)))
					.as("Third identifier is site ID")
					.returns(componentId, from(d -> d.getIdentifiers().get(2)))
					.as("Reference provided")
					.returns(componentValueRef(siteId, type, componentId, item.getIdentifiers().getLast()),
							from(CloudDataValue::getReference))
					;
			})
			.extracting(d -> d.getIdentifiers().getLast())
			.containsExactly(
					"Econ",
					"Edel",
					"Egen",
					"Erec",
					"Pcon",
					"Pdel",
					"Pgen",
					"Prec"
			)
			;
		// @formatter:on
	}

	@Test
	public void dataValues_component_battery() {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();
		final SolarEdgeDeviceType type = SolarEdgeDeviceType.Battery;
		final String componentId = randomString();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		// @formatter:off
		integration.setServiceProps(Map.of(
				API_KEY_SETTING, apiKey
			));
		// @formatter:on

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// WHEN
		final Iterable<CloudDataValue> results = service.dataValues(integration.getId(),
				Map.of(SolarEdgeV2CloudDatumStreamService.SITE_ID_FILTER, siteId,
						SolarEdgeV2CloudDatumStreamService.DEVICE_TYPE_FILTER, type.getKey(),
						SolarEdgeV2CloudDatumStreamService.COMPONENT_ID_FILTER, componentId));

		// THEN
		// @formatter:off
		then(restOps).shouldHaveNoInteractions();

		and.then(results)
			.as("Result generated fixed inverter properties")
			.hasSize(6)
			.allSatisfy(item -> {
				and.then(item)
					.as("Has 4 identifiers for site, type, component, property")
					.returns(4, d -> d.getIdentifiers().size())
					.as("First identifier is site ID")
					.returns(siteId.toString(), from(d -> d.getIdentifiers().get(0)))
					.as("Second identifier is device type")
					.returns(type.getKey(), from(d -> d.getIdentifiers().get(1)))
					.as("Third identifier is site ID")
					.returns(componentId, from(d -> d.getIdentifiers().get(2)))
					.as("Reference provided")
					.returns(componentValueRef(siteId, type, componentId, item.getIdentifiers().getLast()),
							from(CloudDataValue::getReference))
					;
			})
			.extracting(d -> d.getIdentifiers().getLast())
			.containsExactly(
					"Eavail",
					"Edel",
					"Erec",
					"Pdel",
					"Prec",
					"SOC"
			)
			;
		// @formatter:on
	}

	@Test
	public void datum_inverters() throws IOException {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef("{siteId}", Inverter, "{componentId}",
						SolarEdgeInverterTelemtry.Pdel.name()));
		c1p1.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef("{siteId}", Inverter, "{componentId}",
						SolarEdgeInverterTelemtry.Edel.name()));
		c1p2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1), "INV/1",
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2), "INV/2"
				)
		));
		// @formatter:on

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		/*- TODO: data validation
		// get system devices for data validation (cached)
		given(siteInventoryCache.get(siteId)).willReturn(parseSiteInventory(siteId.toString(),
				getObjectFromJSON(utf8StringResource("solaredge-v2-site-inventory-01.json", getClass()),
						JsonNode.class)).toArray(CloudDataValue[]::new));
		*/

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(clock.instant());
		filter.setEndDate(filter.getStartDate().plus(1, HOURS));

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		final CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// expected date range is clock-aligned
		final Instant expectedStartDate = SolarEdgeResolution.FifteenMinute
				.tickStart(filter.getStartDate(), UTC);
		final Instant expectedEndDate = SolarEdgeResolution.FifteenMinute.tickStart(filter.getEndDate(),
				UTC);

		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", expectedStartDate)
					.queryParam("to", expectedEndDate)
					.buildAndExpand(
						siteId,
						SolarEdgeDeviceType.Inverter.getTelemetryType(),
						// component IDs will be sorted
						commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2))
					)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(14)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			.satisfies(list -> {
				// inv/1
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp is first with non-null value")
					.returns(Instant.parse("2026-08-10T10:15:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from telemetry data")
					.returns(new DatumSamples(Map.of(
								"watts", 0
							), Map.of(
								"wattHours", 0
							), null),
						Datum::asSampleOperations)
					;
				and.then(list).element(6)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					.as("Timestamp is parsed")
					.returns(Instant.parse("2026-08-10T11:45:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from telemetry data")
					.returns(new DatumSamples(Map.of(
								"watts", 2943.3333f
							), Map.of(
								"wattHours", 719
							), null),
						Datum::asSampleOperations)
					;

			})
			;
		// @formatter:on
	}

	@Test
	public void datum_inverters_latest() throws IOException {
		// GIVEN
		final String apiKey = randomString();
		final Long siteId = randomLong();
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef("{siteId}", Inverter, "{componentId}",
						SolarEdgeInverterTelemtry.Pdel.name()));
		c1p1.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				componentValueRef("{siteId}", Inverter, "{componentId}",
						SolarEdgeInverterTelemtry.Edel.name()));
		c1p2.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1), "INV/1",
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2), "INV/2"
				)
		));
		// @formatter:on

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// WHEN
		final Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// expected date range is clock-aligned
		final Instant expectedEndDate = SolarEdgeResolution.FifteenMinute.tickStart(clock.instant(),
				UTC);
		final Instant expectedStartDate = SolarEdgeResolution.FifteenMinute
				.prevTickStart(expectedEndDate, UTC);

		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", expectedStartDate)
					.queryParam("to", expectedEndDate)
					.buildAndExpand(
						siteId,
						SolarEdgeDeviceType.Inverter.getTelemetryType(),
						// component IDs will be sorted
						commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2))
					)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(14)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void withRetry_handleHttp403() throws IOException {
		// GIVEN
		// add retry config
		final var retrySettings = new RetrySettings();
		retrySettings.setMaxRetries(1L);
		service.setRetryOps(
				SolarNetCloudIntegrationsConfiguration.cloudDatumStreamRetryTemplate(retrySettings));

		final Instant endAt = Instant.parse("2025-02-28T02:00:38.696382784Z");
		final Instant startAt = Instant.parse("2025-02-28T01:30:00Z");
		clock.setInstant(endAt);

		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, Inverter, inverterComponentId, "W"));
		c1p1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId), "INV/1"
				)
		));
		// @formatter:on

		// request site time zone info; denied
		final var forbiddenEx = HttpClientErrorException.create("Access Denied", HttpStatus.FORBIDDEN,
				"403 FORBIDDEN", new HttpHeaders(), "Access Denied".getBytes(UTF_8), UTF_8);
		given(restOps.exchange(any(), eq(JsonNode.class))).willThrow(forbiddenEx);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startAt);
		filter.setEndDate(endAt);

		thenExceptionOfType(RemoteServiceException.class).isThrownBy(() -> {
			service.datum(datumStream, filter);
		}).havingRootCause().as("Cause is HTTP 403").isSameAs(forbiddenEx);

		// THEN
		// @formatter:off
		then(restOps).should(times(1)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// site details
					fromUri(BASE_URI)
						.path(SolarEdgeV2CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
						.buildAndExpand(siteId)
						.toUri()
			)
			;
		// @formatter:on
	}

	@Test
	public void datum_dateRangeTickAligned() throws IOException {
		// GIVEN
		final Instant endAt = Instant.parse("2025-02-28T02:00:38.696382784Z");
		final Instant startAt = Instant.parse("2025-02-28T01:30:00Z");
		clock.setInstant(endAt);

		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, Inverter, inverterComponentId,
						SolarEdgeInverterTelemtry.Pdel.name()));
		c1p1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
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
				.readTree(utf8StringResource("solaredge-v2-site-01.json", getClass()));
		final var siteDetailsRes = new ResponseEntity<JsonNode>(siteDetailsJson, HttpStatus.OK);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(siteDetailsRes)
				.willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startAt);
		filter.setEndDate(endAt);
		service.datum(datumStream, filter);

		// THEN
		// expected date range is clock-aligned
		final Instant expectedEndDate = SolarEdgeResolution.FifteenMinute
				.nextTickStart(SolarEdgeResolution.FifteenMinute.tickStart(endAt, UTC), UTC);
		final Instant expectedStartDate = SolarEdgeResolution.FifteenMinute.tickStart(startAt, UTC);

		// @formatter:off
		then(restOps).should(times(2)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		then(siteTimeZoneCache).should().put(siteId, ZoneId.of("America/Chicago"));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// site details
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.SITE_DETAILS_URL_TEMPLATE)
					.buildAndExpand(siteId)
					.toUri(),

				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", expectedStartDate)
					.queryParam("to", expectedEndDate)
					.buildAndExpand(
						siteId,
						SolarEdgeDeviceType.Inverter.getTelemetryType(),
						commaDelimitedStringFromCollection(List.of(inverterComponentId))
					)
					.toUri()
			)
			;
		// @formatter:on
	}

	@Test
	public void datum_dateRangeTruncatedToResolutionMax() throws IOException {
		// GIVEN
		final Instant endAt = Instant.parse("2025-02-28T02:00:38.696382784Z");
		final Instant startAt = Instant.parse("2025-02-26T01:30:00Z");
		clock.setInstant(endAt);

		final Long siteId = randomLong();
		final String inverterComponentId = randomString();
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration c1p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				componentValueRef(siteId, Inverter, inverterComponentId,
						SolarEdgeInverterTelemtry.Pdel.name()));
		c1p1.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				CloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId), "INV/1"
				)
		));
		// @formatter:on

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(ZoneId.of("America/Chicago"));

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-01.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);

		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startAt);
		filter.setEndDate(endAt);
		service.datum(datumStream, filter);

		// THEN
		// expected date range is clock-aligned and truncated to query max
		final Instant expectedStartDate = SolarEdgeResolution.FifteenMinute.tickStart(startAt, UTC);
		final Instant expectedEndDate = expectedStartDate
				.plus(SolarEdgeResolution.FifteenMinute.getQueryMax());

		// @formatter:off
		then(restOps).should(times(1)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		then(siteTimeZoneCache).shouldHaveNoMoreInteractions();

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", expectedStartDate)
					.queryParam("to", expectedEndDate)
					.buildAndExpand(
						siteId,
						SolarEdgeDeviceType.Inverter.getTelemetryType(),
						commaDelimitedStringFromCollection(List.of(inverterComponentId))
					)
					.toUri()
			)
			;
		// @formatter:on
	}

	@Test
	public void simulation_inverterSumExpression() throws IOException {
		// GIVEN
		service.setDatumDao(datumDao);

		final Long siteId = randomLong();
		final String apiKey = randomString();
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration invWattsProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				"/{siteId}/inv/{componentId}/Pdel");
		invWattsProp.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration invWattHoursProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now(), Accumulating, "wattHours", Reference,
				"/{siteId}/inv/{componentId}/Edel");
		invWattHoursProp.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration meterWattsExprProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now(), Instantaneous, "wattsInvSum",
				SpelExpression,
				"""
						sourceId.contains("GEN") ? sum(latestMatching("INV/*", timestamp).![watts]) : null
						""");
		meterWattsExprProp.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration meterWattHoursExprProp = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 5, now(), Accumulating, "wattHoursInvSum",
				SpelExpression,
				"""
						sourceId.contains("GEN") ? sum(latestMatching("INV/*", timestamp).![wattHours]) : null
						""");
		meterWattHoursExprProp.setEnabled(true);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null)).willReturn(
				List.of(invWattsProp, invWattHoursProp, meterWattsExprProp, meterWattHoursExprProp));

		// configure datum stream
		final Long nodeId = randomLong();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString(), ObjectDatumKind.Node);
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId("unused");
		// @formatter:off
		datumStream.setServiceProps(JsonUtils.getStringMap("""
						{
						  "sourceIdMap": {
						    "/%s/inv/%s": "INV/1",
						    "/%1$s/inv/%s": "INV/2"
						  },
						  "virtualSourceIds": ["GEN/1"],
						  "validationIgnore": "time-gap"
						}
						""".formatted(siteId, inverterComponentId1, inverterComponentId2)));
		// @formatter:on

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		/*- TODO: data validation
		// get system devices for data validation (cached)
		given(siteInventoryCache.get(siteId)).willReturn(parseSiteInventory(siteId.toString(),
				getObjectFromJSON(utf8StringResource("solaredge-v2-site-inventory-01.json", getClass()),
						JsonNode.class)).toArray(CloudDataValue[]::new));
		*/

		// request inverter data: INV/2 has some missing data, starts after INV/1
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-02.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		final Instant queryStartDate = Instant.parse("2026-08-10T09:00:00Z");
		final Instant queryEndDate = Instant.parse("2026-08-10T12:00:00Z");
		final Instant firstDatumDate = Instant.parse("2026-08-10T10:15:00Z");
		final Instant firstInv2DatumDate = Instant.parse("2026-08-10T10:45:00Z");

		// perform datum lookup on INV/2 to satisfy latestMatching('INV/*') expressions
		var inverterDatumStreamMetadatasByComponentId = new HashMap<UUID, ObjectDatumStreamMetadata>();
		var datumDaoMock = given(datumDao.findFiltered(any()));
		var datumDaoTimestamp = firstDatumDate.minus(12, ChronoUnit.HOURS);

		var streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), siteTimeZone.getId(),
				ObjectDatumKind.Node, nodeId, "INV/2", new String[] { "watts" },
				new String[] { "wattHours" }, null);
		inverterDatumStreamMetadatasByComponentId.put(streamMeta.getStreamId(), streamMeta);
		var datumEntityWatts = new BigDecimal(1000);
		var datumEntityWattHours = new BigDecimal((1000) * 100);
		var datumEntity = new DatumEntity(streamMeta.getStreamId(), datumDaoTimestamp, null,
				propertiesOf(new BigDecimal[] { datumEntityWatts },
						new BigDecimal[] { datumEntityWattHours }, null, null));
		var filterResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				inverterDatumStreamMetadatasByComponentId, List.of(datumEntity));
		var gapFillResults = new BasicObjectDatumStreamFilterResults<net.solarnetwork.central.datum.v2.domain.Datum, DatumPK>(
				inverterDatumStreamMetadatasByComponentId, List.of());
		datumDaoMock = datumDaoMock.willReturn(filterResults).willReturn(gapFillResults);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(queryStartDate);
		filter.setEndDate(queryEndDate);
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should(times(1)).exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", queryStartDate)
					.queryParam("to", queryEndDate)
					.buildAndExpand(
						siteId,
						SolarEdgeDeviceType.Inverter.getTelemetryType(),
						commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2))
					)
					.toUri()
			)
			;

		// will invoke DAO 2x for INV/2 stream
		then(datumDao).should(times(2)).findFiltered(criteriaCaptor.capture());

		var datumCriterias = criteriaCaptor.getAllValues();
		and.then(datumCriterias)
			.as("Previous datum queried for INV/2 stream")
			.hasSize(2)
			.allSatisfy(criteria -> {
				and.then(criteria)
					.as("Search for stream node")
					.returns(nodeId, from(DatumCriteria::getNodeId))
					.as("Search for INV/2 source to find matching datum for INV/1")
					.returns("INV/2", from(DatumCriteria::getSourceId))
					.returns(List.of(
							  new SimpleSortDescriptor("stream")
							, new SimpleSortDescriptor("time", true)
						), from(DatumCriteria::getSorts))
					;

			})
			.satisfies(list -> {
				and.then(list).element(0)
					.as("Search from INV/1 first datum - max duration")
					.returns(firstDatumDate.minus(DEFAULT_MAX_START_DATE_DURATION), from(DatumCriteria::getStartDate))
					.as("Search up to INV/1 first datum (inclusive)")
					.returns(firstDatumDate.plusMillis(1), from(DatumCriteria::getEndDate))
					.as("Search for just the next oldest")
					.returns(1, from(DatumCriteria::getMax))
					;

				and.then(list).element(1)
					.as("Search up to gap-fill datum for INV/2 from found datum timestamp date")
					.returns(datumDaoTimestamp.plusMillis(1), from(DatumCriteria::getStartDate))
					.as("Search up to gap-fill datum for INV/2 to its first datum timestamp date")
					.returns(firstInv2DatumDate, from(DatumCriteria::getEndDate))
					.as("Search for gap-fill maximum")
					.returns(100, from(DatumCriteria::getMax))
					;
			})
			;

		var resultListSorted = StreamSupport.stream(result.spliterator(), false).sorted(
				Comparator.comparing(Datum::getSourceId).thenComparing(Datum::getTimestamp)).toList();

		and.then(resultListSorted)
			.as("Datum parsed from HTTP response, 7x INV/1, 5x INV/2, + 1 virtual meter (7x)")
			.hasSize(7 * 2 + 5)
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
					.as("Datum source ID is mapped from DatumStream virtual source IDs configuration")
					.returns("GEN/1", from(Datum::getSourceId))
					.as("Virtual GEN/1 timestamp from first INV/1 datum")
					.returns(firstDatumDate, from(Datum::getTimestamp))
					.as("Datum samples from merged from first INV/1 + latest INV/2 lookup")
					.returns(new DatumSamples(Map.of(
								"wattsInvSum", (0 + 1000)
							), Map.of(
								"wattHoursInvSum", (0 + 100000)
							), null),
						Datum::asSampleOperations)
					;
				and.then(list).element(2)
					.as("Datum source ID is mapped from DatumStream virtual source IDs configuration")
					.returns("GEN/1", from(Datum::getSourceId))
					.as("Virtual GEN/1 timestamp from first INV/2 datum")
					.returns(firstInv2DatumDate, from(Datum::getTimestamp))
					.as("Datum samples from merged from first INV/1 + latest INV/2 lookup")
					.returns(new DatumSamples(Map.of(
								"wattsInvSum", (358.33334 + 415.0)
							), Map.of(
								"wattHoursInvSum", (91 + 92)
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_timeJump() throws IOException {
		// GIVEN
		service.setDatumDao(datumDao);

		final Long siteId = 2883L;
		final String inverterComponentId = "AAAA1DFE-E9";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		final String mappedSourceId = "INV/1";
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId),
				mappedSourceId);

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// expected date range is clock-aligned
		final Instant expectedStartDate = Instant.parse("2026-08-10T07:15:00-04:00");
		final Instant expectedEndDate = expectedStartDate.plus(45, ChronoUnit.MINUTES);

		// request inverter 1 data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-03.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// note response order based on site details plan
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// lookup previous datum for first datum in result set
		final Instant firstDatumTs = Instant.parse("2026-08-10T07:15:00-04:00");
		final Instant prevDatumTs = firstDatumTs.minus(100, ChronoUnit.HOURS);
		final String deviceRef = "/%d/%s/%s".formatted(siteId, SolarEdgeDeviceType.Inverter.getKey(),
				inverterComponentId);
		final var prevDatum = new DatumEntity(new DatumPK(UUID.randomUUID(), prevDatumTs), null,
				new DatumProperties());
		given(datumDao
				.findFiltered(any()))
						.willReturn(
								new BasicObjectDatumStreamFilterResults<>(
										Map.of(prevDatum.streamId(),
												emptyMeta(prevDatum.streamId(), "UTC",
														datumStream.getKind(), nodeId, mappedSourceId)),
										List.of(prevDatum)));

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(expectedStartDate);
		filter.setEndDate(expectedEndDate);

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		final URI expectedInverterUri = fromUri(BASE_URI)
				.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
				.queryParam("from", expectedStartDate)
				.queryParam("to", expectedEndDate)
				.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(), inverterComponentId)
				.toUri();

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
					// inverter 1 data
					expectedInverterUri
			)
			;

		// lookup prev datum
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
			.returns(mappedSourceId, from(DatumCriteria::getSourceId))
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				// first - inverter 1
				and.then(list).element(0)
					.as("Timestamp from inverter data")
					.returns(firstDatumTs, from(Datum::getTimestamp))
					.as("Datum samples from inverter data")
					.returns(new DatumSamples(Map.of("watts", 1568.3334f), null, null), from(Datum::asSampleOperations))
					;
			})
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
					.returns(mappedSourceId, from(DatumAuxiliaryRecord::getSourceId))
					;
			})
			.satisfies(records -> {
				final Instant timeGapStartTs = prevDatumTs;
				final Instant timeGapEndTs = firstDatumTs;
				final URI expectedUri = service.getRestOpsHelper().maskedUri(expectedInverterUri);

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
									deviceRef, expectedUri, null, timeGapStartTs, timeGapEndTs, true, null))
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
									deviceRef, expectedUri, null, timeGapStartTs, timeGapEndTs, false,
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
	public void datum_multiStreamLag_withinTolerance() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				"INV/1");
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				"INV/2");

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// request inverter data: INV/2 final data one period behind INV/1
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-04.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		final Instant queryStartDate = Instant.parse("2026-08-10T09:00:00Z");
		final Instant queryEndDate = Instant.parse("2026-08-10T12:00:00Z");
		final Instant lastInv2DatumDate = Instant.parse("2026-08-10T07:15:00-04:00");

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(queryStartDate);
		filter.setEndDate(queryEndDate);

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", queryStartDate)
					.queryParam("to", queryEndDate)
					.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(),
							StringUtils.commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2)))
					.toUri()
			)
			;

		then(datumDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(12)
			.satisfies(_ -> {
				and.then(result.getNextQueryFilter())
					.as("Next query filter returned")
					.isNotNull()
					.as("INV/2 greatest value for next start date because least greatest per stream (lags INV/1)")
					.returns(lastInv2DatumDate.plus(SolarEdgeResolution.FifteenMinute.getTickAmount()),
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
			;
		// @formatter:on
	}

	@Test
	public void datum_multiStreamLag_outsideTolerance() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				"INV/1");
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				"INV/2");

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// request inverter data: INV/2 final data one period behind INV/1
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-04.json", getClass()));
		final var inverterDataRes = new ResponseEntity<>(inverterDataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		final Instant queryStartDate = Instant.parse("2026-08-10T09:00:00Z");
		final Instant queryEndDate = Instant.parse("2026-08-10T12:00:00Z");

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(queryStartDate);
		filter.setEndDate(queryEndDate);

		// setup clock to be far after end of requested data period (outside lag tolerance)
		clock.setInstant(filter.getEndDate().plus(365L, ChronoUnit.DAYS));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", queryStartDate)
					.queryParam("to", queryEndDate)
					.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(),
							StringUtils.commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2)))
					.toUri()
			)
			;

		then(datumDao).shouldHaveNoInteractions();

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(12)
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
			;
		// @formatter:on
	}

	@Test
	public void datum_kWUnit() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId = "AAAA1DFE-E9";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
		prop1.setEnabled(true);

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Accumulating, "wattHours", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Edel));
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

		// keep in order for test expectations
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		final String mappedSourceId = "INV/1";
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId),
				mappedSourceId);

		datumStream
				.setServiceProps(Map.of(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping));

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// expected date range is clock-aligned
		final Instant expectedStartDate = Instant.parse("2026-08-10T07:15:00-04:00");
		final Instant expectedEndDate = expectedStartDate.plus(45, ChronoUnit.MINUTES);

		// request inverter 1 data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-05.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// note response order based on site details plan
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// lookup previous datum for first datum in result set
		final Instant firstDatumTs = Instant.parse("2026-08-10T07:15:00-04:00");

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(expectedStartDate);
		filter.setEndDate(expectedEndDate);

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URLs called")
			.containsExactly(
				// inverter 1 data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", expectedStartDate)
					.queryParam("to", expectedEndDate)
					.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(), inverterComponentId)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(3)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns("INV/1", from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				// first - inverter 1
				and.then(list).element(0)
					.as("Timestamp from inverter data")
					.returns(firstDatumTs, from(Datum::getTimestamp))
					.as("Datum samples from inverter data, adjusted from kW unit to W")
					.returns(new DatumSamples(Map.of(
							"watts", 1568333.4f
						), Map.of(
							"wattHours", 376000
						), null), from(Datum::asSampleOperations))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_opRanges_exclude() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
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

		final Map<String, Object> datumStreamServiceProps = new LinkedHashMap<>(2);

		// mapping two components to same source ID
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		final String mappedSourceId = "INV/1";
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				mappedSourceId);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				mappedSourceId);
		datumStreamServiceProps.put(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping);

		// add date range constraint
		final Instant splitTimestamp = Instant.parse("2026-08-10T07:00:00-04:00");

		// @formatter:off
		datumStreamServiceProps.put(CloudDatumStreamService.OPERATIONAL_DATE_RANGES_SETTING, Map.of(
				// device 1 valid up to split date
				"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1), "/%s".formatted(splitTimestamp),

				// device 2 valid from split date
				"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2), "%s/".formatted(splitTimestamp)
		));
		// @formatter:on

		datumStream.setServiceProps(datumStreamServiceProps);

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-05.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// note response order based on site details plan
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.parse("2026-08-10T06:15:00-04:00"));
		filter.setEndDate(Instant.parse("2026-08-10T07:00:00-04:00"));

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult _ = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URL called: only component 1 included because of op range constraint")
			.containsExactly(
				// inverter 1 data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", filter.getStartDate())
					.queryParam("to", filter.getEndDate())
					.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(), inverterComponentId1)
					.toUri()
			)
			;
		// @formatter:on
	}

	@Test
	public void datum_opRanges_include() throws IOException {
		// GIVEN
		final Long siteId = 2883L;
		final String inverterComponentId1 = "AAAA1DFE-E9";
		final String inverterComponentId2 = "AAAA52CD-BA";
		final ZoneId siteTimeZone = ZoneId.of("America/Chicago");
		final String apiKey = randomString();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now(), randomString(), randomString());
		integration.setServiceProps(Map.of(API_KEY_SETTING, apiKey));

		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now(), randomString(), integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now(), Instantaneous, "watts", Reference,
				placeholderComponentValueRef(Inverter, SolarEdgeInverterTelemtry.Pdel));
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

		final Map<String, Object> datumStreamServiceProps = new LinkedHashMap<>(2);

		// mapping two components to same source ID
		final SequencedMap<String, String> sourceIdMapping = new LinkedHashMap<>(2);
		final String mappedSourceId = "INV/1";
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1),
				mappedSourceId);
		sourceIdMapping.put("/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2),
				mappedSourceId);
		datumStreamServiceProps.put(CloudDatumStreamService.SOURCE_ID_MAP_SETTING, sourceIdMapping);

		// add date range constraint
		final Instant splitTimestamp = Instant.parse("2026-08-10T07:00:00-04:00");

		// @formatter:off
		datumStreamServiceProps.put(CloudDatumStreamService.OPERATIONAL_DATE_RANGES_SETTING, Map.of(
				// device 1 valid up to split date
				"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId1), "/%s".formatted(splitTimestamp),

				// device 2 valid from split date
				"/%s/%s/%s".formatted(siteId, Inverter.getKey(), inverterComponentId2), "%s/".formatted(splitTimestamp)
		));
		// @formatter:on

		datumStream.setServiceProps(datumStreamServiceProps);

		// look up site time zone (cached)
		given(siteTimeZoneCache.get(siteId)).willReturn(siteTimeZone);

		// request inverter data
		final JsonNode inverterDataJson = objectMapper
				.readTree(utf8StringResource("solaredge-v2-telem-inv-05.json", getClass()));
		final var inverterDataRes = new ResponseEntity<JsonNode>(inverterDataJson, HttpStatus.OK);

		// note response order based on site details plan
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(inverterDataRes);

		// WHEN
		final BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(Instant.parse("2026-08-10T06:45:00-04:00"));
		filter.setEndDate(Instant.parse("2026-08-10T07:15:00-04:00"));

		// setup clock to be near end of requested data period (within lag tolerance)
		clock.setInstant(filter.getEndDate().plusSeconds(1));

		CloudDatumStreamQueryResult _ = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getAllValues())
			.allSatisfy(req -> {
				and.then(req)
					.as("HTTP method is GET")
					.returns(HttpMethod.GET, from(RequestEntity::getMethod))
					.extracting(r -> r.getHeaders().toSingleValueMap(), map(String.class, String.class))
					.as("HTTP request includes API key header")
					.containsEntry(SolarEdgeV2CloudIntegrationService.API_KEY_HEADER, apiKey)
					;
			})
			.extracting(RequestEntity::getUrl)
			.as("Expected URL called: component 1 and 2 included because of op range overlap")
			.containsExactly(
				// inverter 1 and 2 data
				fromUri(BASE_URI)
					.path(SolarEdgeV2CloudDatumStreamService.DEVICE_TELEMETRY_URL_TEMPLATE)
					.queryParam("from", filter.getStartDate())
					.queryParam("to", filter.getEndDate())
					.buildAndExpand(siteId, SolarEdgeDeviceType.Inverter.getTelemetryType(),
							StringUtils.commaDelimitedStringFromCollection(List.of(inverterComponentId1, inverterComponentId2)))
					.toUri()
			)
			;
		// @formatter:on
	}

}
