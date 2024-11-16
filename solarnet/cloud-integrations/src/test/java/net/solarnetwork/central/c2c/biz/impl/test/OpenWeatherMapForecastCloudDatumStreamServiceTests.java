/* ==================================================================
 * OpenWeatherMapForecastCloudDatumStreamServiceTests.java - 31/10/2024 4:24:04 pm
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
import java.math.BigDecimal;
import java.net.URI;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseOpenWeatherMapCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapForecastCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link OpenWeatherMapForecastCloudDatumStreamService}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class OpenWeatherMapForecastCloudDatumStreamServiceTests {

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
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private OpenWeatherMapForecastCloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.newObjectMapper();

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new OpenWeatherMapForecastCloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(OpenWeatherMapForecastCloudDatumStreamService.class.getName(),
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
		final URI dataUri = UriComponentsBuilder
				.fromUri(resolveBaseUrl(integration, OpenWeatherMapCloudIntegrationService.BASE_URI))
				.path(OpenWeatherMapForecastCloudDatumStreamService.FORECAST_URL_PATH)
				.queryParam(UNITS_PARAM, UNITS_METRIC_VALUE)
				.queryParam(OpenWeatherMapCloudIntegrationService.LATITUDE_PARAM, lat.toPlainString())
				.queryParam(OpenWeatherMapCloudIntegrationService.LONGITUDE_PARAM, lon.toPlainString())
				.queryParam(OpenWeatherMapCloudIntegrationService.APPID_PARAM, apiKey).buildAndExpand()
				.toUri();
		final JsonNode dataJson = objectMapper
				.readTree(utf8StringResource("openweathermap-forecast-01.json", getClass()));
		final var dataRes = new ResponseEntity<JsonNode>(dataJson, HttpStatus.OK);
		given(restOps.exchange(eq(dataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(dataRes);

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off

		and.then(result)
			.as("Latest datum parsed from HTTP response ")
			.hasSize(40)
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
					.returns(Instant.ofEpochSecond(1730419200L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"temp", new BigDecimal("16.06"),
								"tempMin", new BigDecimal("14.33"),
								"tempMax", new BigDecimal("16.06"),
								"atm", 102000,
								"humidity", 68,
								"visibility", 10000,
								"wdir", 281,
								"wspeed", new BigDecimal("5.49"),
								"wgust", new BigDecimal("7.26"),
								"cloudiness", 41
							), null , Map.of(
								"sky", "Clouds",
								"iconId", "03d"
							)),
						Datum::asSampleOperations)
					;
				and.then(list).element(2)
					.as("Timestamp from data")
					.returns(Instant.ofEpochSecond(1730440800L), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								"temp", new BigDecimal("12.47"),
								"tempMin", new BigDecimal("12.47"),
								"tempMax", new BigDecimal("12.47"),
								"atm", 101900,
								"humidity", 80,
								"visibility", 10000,
								"wdir", 304,
								"wspeed", new BigDecimal("3.59"),
								"wgust", new BigDecimal("6.33"),
								"cloudiness", 94
							), null , Map.of(
								"sky", "Clouds",
								"iconId", "04n"
							)),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
