/* ==================================================================
 * SolcastIrradianceCloudDatumStreamServiceTests.java - 30/10/2024 1:34:36 pm
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
import static net.solarnetwork.central.c2c.biz.impl.SolcastIrradianceType.GHI;
import static net.solarnetwork.central.c2c.biz.impl.SolcastIrradianceType.Temp;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
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
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseSolcastCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SolcastCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.SolcastIrradianceCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolcastIrradianceType;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SolcastIrradianceCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SolcastIrradianceCloudDatumStreamServiceTests {

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

	private SolcastIrradianceCloudDatumStreamService service;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.newObjectMapper();

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new SolcastIrradianceCloudDatumStreamService(userEventAppenderBiz, encryptor,
				expressionService, integrationDao, datumStreamDao, datumStreamMappingDao,
				datumStreamPropertyDao, restOps, clock);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolcastIrradianceCloudDatumStreamServiceTests.class.getName(),
				BaseSolcastCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

	}

	private static String componentValueRef(SolcastIrradianceType type) {
		return "/%s".formatted(type.name());
	}

	@Test
	public void datum() throws IOException {
		// GIVEN
		final String apiKey = randomString();
		final BigDecimal lat = new BigDecimal(randomLong());
		final BigDecimal lon = new BigDecimal(randomLong());

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				SolcastCloudIntegrationService.API_KEY_SETTING, apiKey
		));
		// @formatter:on
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
		c1p1.setPropertyName("irradiance");
		c1p1.setValueType(CloudDatumStreamValueType.Reference);
		c1p1.setValueReference(componentValueRef(GHI));

		final CloudDatumStreamPropertyConfiguration c1p2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, mapping.getConfigId(), 2, now());
		c1p2.setEnabled(true);
		c1p2.setPropertyType(DatumSamplesType.Instantaneous);
		c1p2.setPropertyName("temp");
		c1p2.setValueType(CloudDatumStreamValueType.Reference);
		c1p2.setValueReference(componentValueRef(Temp));

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
				BaseSolcastCloudDatumStreamService.LATITUDE_SETTING, lat.toPlainString(),
				BaseSolcastCloudDatumStreamService.LONGITUDE_SETTING, lon.toPlainString(),
				BaseSolcastCloudDatumStreamService.RESOLUTION_SETTING, "1800"
		));
		// @formatter:on

		// request register data
		final URI dataUri = UriComponentsBuilder
				.fromUri(resolveBaseUrl(integration, SolcastCloudIntegrationService.BASE_URI))
				.path(SolcastCloudIntegrationService.LIVE_RADIATION_URL_PATH)
				.queryParam(SolcastCloudIntegrationService.LATITUDE_PARAM, lat.toPlainString())
				.queryParam(SolcastCloudIntegrationService.LONGITUDE_PARAM, lon.toPlainString())
				.queryParam(SolcastCloudIntegrationService.HOURS_PARAM, 1)
				.queryParam(SolcastCloudIntegrationService.PERIOD_PARAM, Duration.ofSeconds(1800))
				.queryParam(SolcastCloudIntegrationService.OUTPUT_PARAMETERS_PARAM,
						"%s,%s".formatted(GHI.getKey(), Temp.getKey()))
				.buildAndExpand().toUri();
		final JsonNode dataJson = objectMapper
				.readTree(utf8StringResource("solcast-irradiance-data-01.json", getClass()));
		final var dataRes = new ResponseEntity<JsonNode>(dataJson, HttpStatus.OK);
		given(restOps.exchange(eq(dataUri), eq(HttpMethod.GET), any(), eq(JsonNode.class)))
				.willReturn(dataRes);

		// WHEN
		final Instant endDate = Instant.parse("2024-10-29T20:00:00Z");
		final Instant startDate = Instant.parse("2024-10-29T19:00:00Z");

		BasicQueryFilter filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		Iterable<Datum> result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off

		and.then(result)
			.as("Datum parsed from HTTP response (ignoring datum outside date range)")
			.hasSize(2)
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
					.as("Timestamp from data delta difference, leading value")
					.returns(startDate, from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								  "irradiance", 13
								, "temp", new BigDecimal("16.1")
							),null , null),
						Datum::asSampleOperations)
					;
				and.then(list).element(1)
					.as("Timestamp from data delta difference, leading value")
					.returns(startDate.plus(30L, ChronoUnit.MINUTES), from(Datum::getTimestamp))
					.as("Datum samples from register data")
					.returns(new DatumSamples(Map.of(
								  "irradiance", 61
								, "temp", 17
							),null , null),
						Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
