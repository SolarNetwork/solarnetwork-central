/* ==================================================================
 * EGaugeCloudDatumStreamServiceTests.java - 26/10/2024 8:18:11 am
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
import static net.solarnetwork.central.c2c.biz.impl.CloudIntegrationsUtils.SECS_PER_HOUR;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.EgaugeCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.EgaugeCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SpelCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link EGaugeCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class EGaugeCloudDatumStreamServiceTests {

	private static final Long TEST_USER_ID = randomLong();

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
	private ClientAccessTokenDao clientAccessTokenDao;

	@Captor
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private SimpleCache<String, CloudDataValue[]> deviceRegistersCache;
	private EgaugeCloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.newObjectMapper();

		expressionService = new SpelCloudIntegrationsExpressionService();
		service = new EgaugeCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao, restOps,
				clock, new SecureRandom(), clientAccessTokenDao);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolarEdgeV1CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		deviceRegistersCache = new SimpleCache<>("Device Registers");
		deviceRegistersCache.setTimeUnit(TimeUnit.MINUTES);
		deviceRegistersCache.setTtl(60);
		service.setDeviceRegistersCache(deviceRegistersCache);
	}

	private static String componentValueRef(Object deviceId, String registerName) {
		return "/%s/%s".formatted(deviceId, registerName);
	}

	@Test
	public void datum_websiteExample() throws IOException {
		// GIVEN
		final String deviceId = randomString();

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
		c1p1.setPropertyName("temp");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(componentValueRef(deviceId, "temp"));

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
				EgaugeCloudDatumStreamService.DEVICE_ID_FILTER, deviceId,
				CloudIntegrationService.USERNAME_SETTING, "owner",
				CloudIntegrationService.PASSWORD_SETTING, "Secret123",
				EgaugeCloudDatumStreamService.GRANULARITY_SETTING, "60"
		));
		// @formatter:on

		// pre-populate device register cache to avoid HTTP request
		CloudDataValue tempReg = dataValue(List.of(deviceId, "temp"), "temp",
				Map.of("type", "T", "idx", 8));
		deviceRegistersCache.put(deviceId, new CloudDataValue[] { tempReg });

		// make access token available
		ClientAccessTokenEntity accessToken = new ClientAccessTokenEntity(TEST_USER_ID,
				userIdSystemIdentifier(TEST_USER_ID, CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, deviceId),
				"owner", Instant.now());
		accessToken.setAccessTokenType("Bearer");
		accessToken.setAccessToken("TOKEN".getBytes(StandardCharsets.UTF_8));
		accessToken.setAccessTokenIssuedAt(clock.instant().minus(5L, ChronoUnit.MINUTES));
		accessToken.setAccessTokenExpiresAt(clock.instant().plus(1, ChronoUnit.HOURS));
		given(clientAccessTokenDao.get(accessToken.getId())).willReturn(accessToken);

		final Instant endDate = clock.instant();
		final Instant startDate = endDate.minusSeconds(60);

		// request register data
		final URI registerDataUri = fromUriString(EgaugeCloudIntegrationService.BASE_URI_TEMPLATE)
				.path(EgaugeCloudDatumStreamService.REGISTER_URL_PATH).queryParam("raw")
				.queryParam("virtual", "value").queryParam("reg", "8")
				.queryParam("time",
						"%s:60:%s".formatted(startDate.getEpochSecond(), endDate.getEpochSecond()))
				.buildAndExpand(deviceId).toUri();
		final JsonNode registerDataJson = objectMapper
				.readTree(utf8StringResource("egauge-register-data-02.json", getClass()));
		final var registerDataRes = new ResponseEntity<JsonNode>(registerDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(registerDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(registerDataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(1)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), Datum::getSourceId)
					;
			})
			.satisfies(list -> {
				// inverter
				and.then(list).element(0)
					.as("Timestamp from register data delta difference, leading value")
					.returns(Instant.ofEpochSecond(1678391100L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"temp", new BigDecimal("5.335")
							),null , null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void datum_powerAndEnergy() throws IOException {
		// GIVEN
		final String deviceId = randomString();

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
				TEST_USER_ID, mapping.getConfigId(), 0, now());
		c1p1.setEnabled(true);
		c1p1.setPropertyType(DatumSamplesType.Instantaneous);
		c1p1.setPropertyName("watts");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(componentValueRef(deviceId, "use"));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 1, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Accumulating);
		c1p2.setPropertyName("wattHours");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(componentValueRef(deviceId, "use"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(List.of(c1p1, c1p2));

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
				EgaugeCloudDatumStreamService.DEVICE_ID_FILTER, deviceId,
				CloudIntegrationService.USERNAME_SETTING, "owner",
				CloudIntegrationService.PASSWORD_SETTING, "Secret123",
				EgaugeCloudDatumStreamService.GRANULARITY_SETTING, "60"
		));
		// @formatter:on

		// pre-populate device register cache to avoid HTTP request
		CloudDataValue useReg = dataValue(List.of(deviceId, "use"), "use",
				Map.of("type", "P", "idx", 0));
		deviceRegistersCache.put(deviceId, new CloudDataValue[] { useReg });

		// make access token available
		ClientAccessTokenEntity accessToken = new ClientAccessTokenEntity(TEST_USER_ID,
				userIdSystemIdentifier(TEST_USER_ID, CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, deviceId),
				"owner", Instant.now());
		accessToken.setAccessTokenType("Bearer");
		accessToken.setAccessToken("TOKEN".getBytes(StandardCharsets.UTF_8));
		accessToken.setAccessTokenIssuedAt(clock.instant().minus(5L, ChronoUnit.MINUTES));
		accessToken.setAccessTokenExpiresAt(clock.instant().plus(1, ChronoUnit.HOURS));
		given(clientAccessTokenDao.get(accessToken.getId())).willReturn(accessToken);

		final Instant endDate = clock.instant();
		final Instant startDate = endDate.minusSeconds(60);

		// request register data
		final URI registerDataUri = fromUriString(EgaugeCloudIntegrationService.BASE_URI_TEMPLATE)
				.path(EgaugeCloudDatumStreamService.REGISTER_URL_PATH).queryParam("raw")
				.queryParam("virtual", "value").queryParam("reg", "0")
				.queryParam("time",
						"%s:60:%s".formatted(startDate.getEpochSecond(), endDate.getEpochSecond()))
				.buildAndExpand(deviceId).toUri();
		final JsonNode registerDataJson = objectMapper
				.readTree(utf8StringResource("egauge-register-data-01.json", getClass()));
		final var registerDataRes = new ResponseEntity<JsonNode>(registerDataJson, HttpStatus.OK);
		given(restOps.exchange(eq(registerDataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(registerDataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off

		// 10044744826787 - 10044736304528

		and.then(result)
			.as("Datum parsed from HTTP response")
			.hasSize(1)
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), Datum::getKind)
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), Datum::getObjectId)
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), Datum::getSourceId)
					;
			})
			.satisfies(list -> {
				// inverter
				and.then(list).element(0)
					.as("Timestamp from register data delta difference, leading value")
					.returns(Instant.ofEpochSecond(1729883700L - 60L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"watts", new BigDecimal(10044744826787L - 10044736304528L)
											.divide(new BigDecimal(60), RoundingMode.DOWN)
							), Map.of(
								"wattHours", new BigDecimal("10044736304528")
											.divide(SECS_PER_HOUR, RoundingMode.DOWN)
							), null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
