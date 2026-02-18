/* ==================================================================
 * OpenWeatherMapWeatherCloudDatumStreamServiceTests.java - 31/10/2024 4:24:04 pm
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
import static net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService.resolveBaseUrl;
import static net.solarnetwork.central.c2c.biz.impl.BaseOpenWeatherMapCloudDatumStreamService.UNITS_METRIC_VALUE;
import static net.solarnetwork.central.c2c.biz.impl.BaseOpenWeatherMapCloudDatumStreamService.UNITS_PARAM;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseOpenWeatherMapCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapWeatherCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link OpenWeatherMapWeatherCloudDatumStreamService}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class OpenWeatherMapWeatherCloudDatumStreamServiceTests {

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

	@Mock
	private ClientAccessTokenDao clientAccessTokenDao;

	@Captor
	private ArgumentCaptor<RequestEntity<JsonNode>> httpRequestCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private OpenWeatherMapWeatherCloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.JSON_OBJECT_MAPPER;

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new OpenWeatherMapWeatherCloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(OpenWeatherMapWeatherCloudDatumStreamService.class.getName(),
				BaseOpenWeatherMapCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void latestDatum() throws Exception {
		// GIVEN
		final String apiKey = randomString();
		final BigDecimal lat = new BigDecimal(randomLong());
		final BigDecimal lon = new BigDecimal(randomLong());

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OpenWeatherMapCloudIntegrationService.API_KEY_SETTING, apiKey
		));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(Collections.emptyList());

		// configure datum stream
		final Long locationId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Location);
		datumStream.setObjectId(locationId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				BaseOpenWeatherMapCloudDatumStreamService.LATITUDE_SETTING, lat.toPlainString(),
				BaseOpenWeatherMapCloudDatumStreamService.LONGITUDE_SETTING, lon.toPlainString()
		));
		// @formatter:on

		// request data
		final JsonNode dataJson = objectMapper
				.readTree(utf8StringResource("openweathermap-weather-01.json", getClass()));
		final var dataRes = new ResponseEntity<JsonNode>(dataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(dataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(UriComponentsBuilder
					.fromUri(resolveBaseUrl(integration, OpenWeatherMapCloudIntegrationService.BASE_URI))
					.path(OpenWeatherMapCloudIntegrationService.WEATHER_URL_PATH)
					.queryParam(UNITS_PARAM, UNITS_METRIC_VALUE)
					.queryParam(OpenWeatherMapCloudIntegrationService.LATITUDE_PARAM, lat.toPlainString())
					.queryParam(OpenWeatherMapCloudIntegrationService.LONGITUDE_PARAM, lon.toPlainString())
					.queryParam(OpenWeatherMapCloudIntegrationService.APPID_PARAM, apiKey).buildAndExpand()
					.toUri(), from(RequestEntity::getUrl))
			;


		and.then(result)
			.as("Latest datum parsed from HTTP response ")
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
				and.then(list).element(0)
					.as("Timestamp from data")
					.returns(Instant.ofEpochSecond(1537138800L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"temp", 14,
								"atm", 101600,
								"humidity", 87,
								"visibility", 10000,
								"wdir", 350,
								"wspeed", new BigDecimal("9.8"),
								"wgust", new BigDecimal("14.9"),
								"cloudiness", 44
							), null , Map.of(
								"sky", "Rain",
								"iconId", "10n",
								"sky_night", "Rain",
								"iconId_night", "10n"
							)),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void latestDatum_daytime() throws Exception {
		// GIVEN
		final String apiKey = randomString();
		final BigDecimal lat = new BigDecimal(randomLong());
		final BigDecimal lon = new BigDecimal(randomLong());

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				OpenWeatherMapCloudIntegrationService.API_KEY_SETTING, apiKey
		));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream mapping
		final CloudDatumStreamMappingConfiguration mapping = new CloudDatumStreamMappingConfiguration(
				TEST_USER_ID, randomLong(), now());
		mapping.setIntegrationId(integration.getConfigId());

		given(datumStreamMappingDao.get(mapping.getId())).willReturn(mapping);

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, mapping.getConfigId(), null))
				.willReturn(Collections.emptyList());

		// configure datum stream
		final Long locationId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setDatumStreamMappingId(mapping.getConfigId());
		datumStream.setKind(ObjectDatumKind.Location);
		datumStream.setObjectId(locationId);
		datumStream.setSourceId(sourceId);
		// @formatter:off
		datumStream.setServiceProps(Map.of(
				BaseOpenWeatherMapCloudDatumStreamService.LATITUDE_SETTING, lat.toPlainString(),
				BaseOpenWeatherMapCloudDatumStreamService.LONGITUDE_SETTING, lon.toPlainString()
		));
		// @formatter:on

		// request data
		final JsonNode dataJson = objectMapper
				.readTree(utf8StringResource("openweathermap-weather-02.json", getClass()));
		final var dataRes = new ResponseEntity<JsonNode>(dataJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(dataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for data")
			.returns(UriComponentsBuilder
					.fromUri(resolveBaseUrl(integration, OpenWeatherMapCloudIntegrationService.BASE_URI))
					.path(OpenWeatherMapCloudIntegrationService.WEATHER_URL_PATH)
					.queryParam(UNITS_PARAM, UNITS_METRIC_VALUE)
					.queryParam(OpenWeatherMapCloudIntegrationService.LATITUDE_PARAM, lat.toPlainString())
					.queryParam(OpenWeatherMapCloudIntegrationService.LONGITUDE_PARAM, lon.toPlainString())
					.queryParam(OpenWeatherMapCloudIntegrationService.APPID_PARAM, apiKey).buildAndExpand()
					.toUri(), from(RequestEntity::getUrl))
			;


		and.then(result)
			.as("Latest datum parsed from HTTP response ")
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
				and.then(list).element(0)
					.as("Timestamp from data")
					.returns(Instant.ofEpochSecond(1730400000L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"temp", 14,
								"atm", 101600,
								"humidity", 87,
								"visibility", 10000,
								"wdir", 350,
								"wspeed", new BigDecimal("9.8"),
								"wgust", new BigDecimal("14.9"),
								"cloudiness", 44
							), null , Map.of(
								"sky", "Rain",
								"iconId", "10n",
								"sky_day", "Rain",
								"iconId_day", "10n"
							)),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
