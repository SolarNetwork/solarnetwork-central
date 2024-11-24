/* ==================================================================
 * SolrenViewCloudDatumStreamServiceTests.java - 17/10/2024 1:29:43 pm
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
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_END_DATE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_PATH;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_SITE_ID_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_START_DATE_PARAM;
import static net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudIntegrationService.XML_FEED_USE_UTC_PARAM;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SolrenViewCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolrenViewGranularity;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SolrenViewCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SolrenViewCloudDatumStreamServiceTests {

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

	private SolrenViewCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SolrenViewCloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolrenViewCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	private static String componentValueRef(Object siteId, Object componentId, String fieldName) {
		return "/%s/%s/%s".formatted(siteId, componentId, fieldName);
	}

	private static String placeholderComponentValueRef(String fieldName) {
		return componentValueRef("{siteId}", "*", fieldName);
	}

	@Test
	public void requestLatest() {
		// GIVEN
		final Long siteId = randomLong();
		final String componentId1 = "1013811710134";
		final String componentId2 = "1013811710042";

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
		c1p1.setValueReference(componentValueRef(siteId, componentId1, "W"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Accumulating);
		c1p2.setPropertyName("wattHours");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(componentValueRef(siteId, componentId1, "WHL"));

		final CloudDatumStreamPropertyConfiguration c2p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c2p1.setEnabled(true);
		c2p1.setPropertyType(DatumSamplesType.Instantaneous);
		c2p1.setPropertyName("watts");
		c2p1.setValueType(CloudDatumStreamValueType.Reference);
		c2p1.setValueReference(componentValueRef(siteId, componentId2, "W"));

		final CloudDatumStreamPropertyConfiguration c2p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c2p2.setEnabled(true);
		c2p2.setPropertyType(DatumSamplesType.Accumulating);
		c2p2.setPropertyName("wattHours");
		c2p2.setValueType(CloudDatumStreamValueType.Reference);
		c2p2.setValueReference(componentValueRef(siteId, componentId2, "WHL"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2, c2p1, c2p2));

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
				SolrenViewCloudDatumStreamService.GRANULARITY_SETTING, "5min",
				SolrenViewCloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						componentId1, sourceId + "/ONE",
						componentId2, sourceId + "/TWO"
				)
		));
		// @formatter:on

		// request data
		final String resXml = utf8StringResource("solrenview-site-data-01.xml", getClass());
		final var res = new ResponseEntity<String>(resXml, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(String.class));

		// expected date range is clock-aligned
		Instant expectedEndDate = clock.instant();
		Instant expectedStartDate = expectedEndDate.minus(SolrenViewGranularity.FiveMinute.getTickDuration());

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(fromUri(BASE_URI)
					.path(XML_FEED_PATH)
					.queryParam(XML_FEED_USE_UTC_PARAM)
					.queryParam(XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM)
					.queryParam(XML_FEED_SITE_ID_PARAM, "{siteId}")
					.queryParam(XML_FEED_START_DATE_PARAM, "{startDate}")
					.queryParam(XML_FEED_END_DATE_PARAM, "{endDate}")
					.buildAndExpand(siteId, expectedStartDate, expectedEndDate)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(2)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum timestamp based on query start date")
					.returns(expectedStartDate, Datum::getTimestamp)
					;
			})
			.satisfies(list -> {
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/ONE", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
								"watts", 390
							), Map.of(
								"wattHours", 517756000
							), null),
						Datum::asSampleOperations)
					;
				and.then(list).element(1)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/TWO", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
								"watts", 425
							), Map.of(
								"wattHours", 514789000
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_placeholderRefs() {
		// GIVEN
		final Long siteId = randomLong();

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
		c1p1.setValueReference(placeholderComponentValueRef("W"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Accumulating);
		c1p2.setPropertyName("wattHours");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(placeholderComponentValueRef("WHL"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2));

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final String componentId1 = "1013811710134";
		final String componentId2 = "1013811710042";

		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				SolrenViewCloudDatumStreamService.GRANULARITY_SETTING, "5min",
				CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY, Map.of(
						SolrenViewCloudDatumStreamService.SITE_ID_FILTER, siteId
				),
				SolrenViewCloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						componentId1, sourceId + "/ONE",
						componentId2, sourceId + "/TWO"
				)
		));
		// @formatter:on

		// request data
		final String resXml = utf8StringResource("solrenview-site-data-01.xml", getClass());
		final var res = new ResponseEntity<String>(resXml, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(String.class));

		// expected date range is clock-aligned
		Instant expectedEndDate = clock.instant();
		Instant expectedStartDate = expectedEndDate.minus(SolrenViewGranularity.FiveMinute.getTickDuration());

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(fromUri(BASE_URI)
					.path(XML_FEED_PATH)
					.queryParam(XML_FEED_USE_UTC_PARAM)
					.queryParam(XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM)
					.queryParam(XML_FEED_SITE_ID_PARAM, "{siteId}")
					.queryParam(XML_FEED_START_DATE_PARAM, "{startDate}")
					.queryParam(XML_FEED_END_DATE_PARAM, "{endDate}")
					.buildAndExpand(siteId, expectedStartDate, expectedEndDate)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(2)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum timestamp based on query start date")
					.returns(expectedStartDate, Datum::getTimestamp)
					;
			})
			.satisfies(list -> {
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/ONE", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
								"watts", 390
							), Map.of(
								"wattHours", 517756000
							), null),
						Datum::asSampleOperations)
					;
				and.then(list).element(1)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/TWO", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
								"watts", 425
							), Map.of(
								"wattHours", 514789000
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_proxyUrl() {
		// GIVEN
		final Long siteId = randomLong();
		final String componentId1 = "1013811710134";
		final String componentId2 = "1013811710042";

		// configure integration
		final String proxyBaseUrl = "http://example.com:12345/proxy";
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		integration.setServiceProps(Map.of(CloudIntegrationService.BASE_URL_SETTING, proxyBaseUrl));

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
		c1p1.setValueReference(componentValueRef(siteId, componentId1, "W"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Accumulating);
		c1p2.setPropertyName("wattHours");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(componentValueRef(siteId, componentId1, "WHL"));

		final CloudDatumStreamPropertyConfiguration c2p1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c2p1.setEnabled(true);
		c2p1.setPropertyType(DatumSamplesType.Instantaneous);
		c2p1.setPropertyName("watts");
		c2p1.setValueType(CloudDatumStreamValueType.Reference);
		c2p1.setValueReference(componentValueRef(siteId, componentId2, "W"));

		final CloudDatumStreamPropertyConfiguration c2p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c2p2.setEnabled(true);
		c2p2.setPropertyType(DatumSamplesType.Accumulating);
		c2p2.setPropertyName("wattHours");
		c2p2.setValueType(CloudDatumStreamValueType.Reference);
		c2p2.setValueReference(componentValueRef(siteId, componentId2, "WHL"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2, c2p1, c2p2));

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
				SolrenViewCloudDatumStreamService.GRANULARITY_SETTING, "5min",
				SolrenViewCloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						componentId1, sourceId + "/ONE",
						componentId2, sourceId + "/TWO"
				)
		));
		// @formatter:on

		// request data
		final String resXml = utf8StringResource("solrenview-site-data-01.xml", getClass());
		final var res = new ResponseEntity<String>(resXml, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(String.class));

		// expected date range is clock-aligned
		Instant expectedEndDate = clock.instant();
		Instant expectedStartDate = expectedEndDate.minus(SolrenViewGranularity.FiveMinute.getTickDuration());

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(fromUri(URI.create(proxyBaseUrl))
					.path(XML_FEED_PATH)
					.queryParam(XML_FEED_USE_UTC_PARAM)
					.queryParam(XML_FEED_INCLUDE_LIFETIME_ENERGY_PARAM)
					.queryParam(XML_FEED_SITE_ID_PARAM, "{siteId}")
					.queryParam(XML_FEED_START_DATE_PARAM, "{startDate}")
					.queryParam(XML_FEED_END_DATE_PARAM, "{endDate}")
					.buildAndExpand(siteId, expectedStartDate, expectedEndDate)
					.toUri()
			)
			;

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(2)
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_expr() {
		// GIVEN
		final Long siteId = randomLong();
		final String componentId1 = "1013811710134";
		final String componentId2 = "1013811710042";

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
		c1p1.setPropertyName("ab");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(placeholderComponentValueRef("PPVphAB"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Instantaneous);
		c1p2.setPropertyName("bc");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(placeholderComponentValueRef("PPVphBC"));

		final CloudDatumStreamPropertyConfiguration c1p3 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 3, now());
		c1p3.setEnabled(true);
		c1p3.setPropertyType(DatumSamplesType.Instantaneous);
		c1p3.setPropertyName("ca");
		c1p3.setValueType(CloudDatumStreamValueType.Reference);
		c1p3.setValueReference(placeholderComponentValueRef("PPVphCA"));

		final CloudDatumStreamPropertyConfiguration c1p4 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 4, now());
		c1p4.setEnabled(true);
		c1p4.setPropertyType(DatumSamplesType.Instantaneous);
		c1p4.setPropertyName("voltage");
		c1p4.setValueType(CloudDatumStreamValueType.SpelExpression);
		c1p4.setValueReference("round(rms({ab, bc, ca}), 1)");

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2, c1p3, c1p4));

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
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				SolrenViewCloudDatumStreamService.GRANULARITY_SETTING, "5min",
				CloudIntegrationsConfigurationEntity.PLACEHOLDERS_SERVICE_PROPERTY, Map.of(
						SolrenViewCloudDatumStreamService.SITE_ID_FILTER, siteId
				),
				SolrenViewCloudDatumStreamService.SOURCE_ID_MAP_SETTING, Map.of(
						componentId1, sourceId + "/ONE",
						componentId2, sourceId + "/TWO"
				)
		));
		// @formatter:on
		// @formatter:on

		// request data
		final String resXml = utf8StringResource("solrenview-site-data-01.xml", getClass());
		final var res = new ResponseEntity<String>(resXml, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(res);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off

		// expected date range is clock-aligned
		Instant expectedEndDate = clock.instant();
		Instant expectedStartDate = expectedEndDate.minus(SolrenViewGranularity.FiveMinute.getTickDuration());

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(2)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum timestamp based on query start date")
					.returns(expectedStartDate, Datum::getTimestamp)
					;
			})
			.satisfies(list -> {
				and.then(list).element(0)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/ONE", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
								"ab", 478,
								"bc", 479,
								"ca", 478,
								"voltage", 478.3f
							), null, null),
						Datum::asSampleOperations)
					;
				and.then(list).element(1)
					.as("Datum source ID is mapped from DatumStream configuration")
					.returns(datumStream.getSourceId() + "/TWO", from(Datum::getSourceId))
					.as("Datum samples from XML response")
					.returns(new DatumSamples(Map.of(
							"ab", 478,
							"bc", 479,
							"ca", 477,
							"voltage", 478
						), null, null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
