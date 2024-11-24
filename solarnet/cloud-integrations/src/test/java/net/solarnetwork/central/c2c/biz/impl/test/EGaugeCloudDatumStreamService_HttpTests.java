/* ==================================================================
 * EGaugeCloudDatumStreamService_HttpTests.java - 31/10/2024 6:57:41 am
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
import static net.solarnetwork.central.c2c.biz.impl.EgaugeCloudDatumStreamService.REGISTER_URL_PATH;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeRestOperationsHelper.AUTH_LOGIN_PATH;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeRestOperationsHelper.AUTH_UNAUTHORIZED_PATH;
import static net.solarnetwork.central.c2c.biz.impl.EgaugeRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER;
import static net.solarnetwork.central.c2c.domain.CloudDataValue.dataValue;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.EgaugeCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService;
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
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.RemoteServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * HTTP integration test cases for the {@link EGaugeCloudDatumStreamService}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class EGaugeCloudDatumStreamService_HttpTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

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

	@Captor
	private ArgumentCaptor<ClientAccessTokenEntity> clientAccessTokenCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private MutableClock clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.DAYS), UTC);

	private SimpleCache<String, CloudDataValue[]> deviceRegistersCache;
	private EgaugeCloudDatumStreamService service;

	private MockWebServer server;

	@BeforeEach
	public void setup() {
		server = new MockWebServer();

		expressionService = new BasicCloudIntegrationsExpressionService(nodeOwnershipDao);
		service = new EgaugeCloudDatumStreamService(userEventAppenderBiz, encryptor, expressionService,
				integrationDao, datumStreamDao, datumStreamMappingDao, datumStreamPropertyDao,
				new RestTemplate(), clock, new SecureRandom(), clientAccessTokenDao);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SolarEdgeV1CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msg);

		deviceRegistersCache = new SimpleCache<>("Device Registers");
		deviceRegistersCache.setTimeUnit(TimeUnit.MINUTES);
		deviceRegistersCache.setTtl(60);
		service.setDeviceRegistersCache(deviceRegistersCache);
	}

	@AfterEach
	public void teardown() throws IOException {
		server.close();
	}

	private static String componentValueRef(Object deviceId, String registerName) {
		return "/%s/%s".formatted(deviceId, registerName);
	}

	private String urlPath(String deviceId, String path) {
		return "/" + deviceId + path;
	}

	@Test
	public void datum_auth404() throws Exception {
		// GIVEN
		final String deviceId = randomString();

		final String baseUrl = server.url("").toString() + "/{deviceId}";

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				CloudIntegrationService.BASE_URL_SETTING, baseUrl
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

		given(integrationDao.integrationForDatumStream(datumStream.getId())).willReturn(integration);

		// pre-populate device register cache to avoid HTTP request
		CloudDataValue tempReg = dataValue(List.of(deviceId, "temp"), "temp",
				Map.of("type", "T", "idx", 8));
		deviceRegistersCache.put(deviceId, new CloudDataValue[] { tempReg });

		// no access token available, so must authenticate to /api/auth/unauthorized
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value())
				.setHeader(CONTENT_TYPE, TEXT_HTML_VALUE).setBody("""
						<html>
						This should have been JSON, but eGauge returns HTML.
						</html>
						"""));

		// WHEN
		// @formatter:off
		and.thenThrownBy(() -> {
				service.latestDatum(datumStream);
			}, "Should throw RemoteServiceException")
			.isInstanceOf(RemoteServiceException.class)
			.cause()
			.as("Cause is a HttpClientErrorException")
			.asInstanceOf(type( HttpClientErrorException.class))
			.returns(HttpStatus.NOT_FOUND, from(HttpClientErrorException::getStatusCode))
			;

		// THEN
		and.then(server.getRequestCount())
			.as("Single HTTP request made")
			.isEqualTo(1)
			;

		and.then(server.takeRequest())
			.isNotNull()
			.returns(urlPath(deviceId, AUTH_UNAUTHORIZED_PATH), from(RecordedRequest::getPath))
			;
		// @formatter:on
	}

	@Test
	public void datum_authExpired() throws Exception {
		// GIVEN
		final String deviceId = randomString();

		final String baseUrl = server.url("").toString() + "/{deviceId}";

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				CloudIntegrationService.BASE_URL_SETTING, baseUrl
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

		given(integrationDao.integrationForDatumStream(datumStream.getId())).willReturn(integration);

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

		// list registers fails with 401
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value())
				.setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).setBody("""
						{"rlm":"eGauge Administration","nnc":"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.\
						eyJubmMiOiJmMjUzMTUwM2FmOTE4MTA2IiwiYmVnIjoxNzMwMzE1Mjg0LCJsdG0iOjYwfQ.kFq3\
						9D6E9qZfmoidLXrm2lHhqh7Rv-FfpZd6lmJGEKo","error":"Authentication required."}
						"""));

		// and so login and get new token
		final String jwt = """
				eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJybG0iOiJlR2F1Z2UgQWRtaW5pc3RyYXRpb24iLCJ1c\
				3IiOiJvd25lciIsImJlZyI6MTczMDMxNTI4NCwibHRtIjo2MDAsImdlbiI6MH0.BKuK9KGnXfhL7y8wv1u\
				-TTKd_bsnzZjpdWvFopznT3s""";
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).setBody("""
						{"jwt":"%s","rights":["save"]}
						""".formatted(jwt)));

		// and finally request the register data again
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.setBody(utf8StringResource("egauge-register-data-02.json", getClass())));

		// then save the new token
		given(clientAccessTokenDao.store(any())).willReturn(accessToken.getId());

		// WHEN
		Iterable<Datum> result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(clientAccessTokenDao).should().store(clientAccessTokenCaptor.capture());
		and.then(clientAccessTokenCaptor.getValue())
			.as("Access token saved for client")
			.isEqualTo(accessToken)
			.as("JWT from login response saved as token")
			.returns(jwt, from(ClientAccessTokenEntity::getAccessTokenValue))
			;

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
								"temp", 5.335f
							),null , null),
						Datum::asSampleOperations)
					;
			})
			;

		and.then(server.getRequestCount())
			.as("HTTP requests made")
			.isEqualTo(3)
			;

		final Instant endDate = clock.instant();
		final Instant startDate = endDate.minusSeconds(60);
		final URI registerDataUri = fromUriString(baseUrl)
			.path(REGISTER_URL_PATH)
			.queryParam("raw")
			.queryParam("virtual", "value")
			.queryParam("reg", "8")
			.queryParam("time", "%s:60:%s".formatted(startDate.getEpochSecond(), endDate.getEpochSecond()))
			.buildAndExpand(deviceId)
			.toUri();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request to list registers for 'most recent' time period")
			.returns(registerDataUri, from(req -> req.getRequestUrl().uri()))
			;

		final URI loginUri = fromUriString(baseUrl)
			.path(AUTH_LOGIN_PATH)
			.buildAndExpand(deviceId)
			.toUri();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request to get token, using nonce from previous response")
			.returns(loginUri, from(req -> req.getRequestUrl().uri()))
			// TODO verify computed credentials
			;

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request to retry list registers")
			.returns(registerDataUri, from(req -> req.getRequestUrl().uri()))
			.as("Includes bearer JWT token Authorization header")
			.returns("Bearer " +jwt, from(req -> req.getHeader(HttpHeaders.AUTHORIZATION)))
			;

		// @formatter:on
	}

	@Test
	public void datum_registers404() throws Exception {
		// GIVEN
		final String deviceId = randomString();

		final String baseUrl = server.url("").toString() + "/{deviceId}";

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				CloudIntegrationService.BASE_URL_SETTING, baseUrl
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

		// list registers fails with 404
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value())
				.setHeader(CONTENT_TYPE, TEXT_HTML_VALUE).setBody("""
						<html>
						This should have been JSON, but eGauge returns HTML.
						</html>
						"""));

		// WHEN
		// @formatter:off
		and.thenThrownBy(() -> {
				service.latestDatum(datumStream);
			}, "Should throw RemoteServiceException")
			.isInstanceOf(RemoteServiceException.class)
			.cause()
			.as("Cause is a HttpClientErrorException")
			.asInstanceOf(type( HttpClientErrorException.class))
			.returns(HttpStatus.NOT_FOUND, from(HttpClientErrorException::getStatusCode))
			;

		// THEN
		and.then(server.getRequestCount())
			.as("Single HTTP request made")
			.isEqualTo(1)
			;

		final Instant endDate = clock.instant();
		final Instant startDate = endDate.minusSeconds(60);
		final URI registerDataUri = fromUriString(baseUrl)
			.path(REGISTER_URL_PATH)
			.queryParam("raw")
			.queryParam("virtual", "value")
			.queryParam("reg", "8")
			.queryParam("time", "%s:60:%s".formatted(startDate.getEpochSecond(), endDate.getEpochSecond()))
			.buildAndExpand(deviceId)
			.toUri();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request to list registers for 'most recent' time period")
			.returns(registerDataUri, from(req -> req.getRequestUrl().uri()))
			;
		// @formatter:on
	}

}
