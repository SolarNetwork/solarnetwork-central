/* ==================================================================
 * SigenergyRestOperationsHelperTests.java - 6/12/2025 7:19:44 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz.sigen.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.MediaType;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestTemplate;
import org.threeten.extra.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRegion;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.service.StaticOptionalService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Test cases for the {@link SigenergyRestOperationsHelper} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SigenergyRestOperationsHelperTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private ClientAccessTokenDao clientAccessTokenDao;

	@Mock
	private Cache<UserLongCompositePK, Lock> lockCache;

	@Mock
	private Lock lock;

	@Captor
	private ArgumentCaptor<ClientAccessTokenEntity> clientAccessTokenCaptor;

	private MutableClock clock;
	private ObjectMapper mapper;
	private MockWebServer server;

	private SigenergyRestOperationsHelper helper;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.HOURS), UTC);
		mapper = JsonUtils.newObjectMapper();
		server = new MockWebServer();

		helper = new SigenergyRestOperationsHelper(
				LoggerFactory.getLogger(SigenergyRestOperationsHelperTests.class), userEventAppenderBiz,
				new RestTemplate(), CloudIntegrationsUserEvents.INTEGRATION_HTTP_ERROR_TAGS, encryptor,
				(serviceIdentifier) -> SigenergyCloudIntegrationService.SECURE_SETTINGS, clock, mapper,
				clientAccessTokenDao, new StaticOptionalService<>(lockCache));
		helper.setAllowLocalHosts(true);
	}

	@AfterEach
	public void teardown() throws IOException {
		server.close();
	}

	@Test
	public void acquireToken() throws Exception {
		// GIVEN
		final String baseUrl = server.url("").toString() + "{region}";
		final String appKey = randomString();
		final String appSecret = randomString();
		final SigenergyRegion region = SigenergyRegion.values()[RNG
				.nextInt(SigenergyRegion.values().length)];

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), clock.instant());
		// @formatter:off
		integration.setServiceProps(Map.of(
				CloudIntegrationService.BASE_URL_SETTING, baseUrl,
				SigenergyCloudIntegrationService.APP_KEY_SETTING, appKey,
				SigenergyCloudIntegrationService.APP_SECRET_SETTING, appSecret,
				SigenergyCloudIntegrationService.REGION_SETTING, region
				));
		// @formatter:on

		given(lockCache.get(integration.getId())).willReturn(lock);

		// request token
		final Instant tokenIssuedAt = clock.instant().minusSeconds(1);
		final int expiresIn = randomInt();
		final String tokenValue = randomString();
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).setBody(
						"""
								{
								  "code":0,
								  "msg":"success",
								  "timestamp":%d,
								  "data":"{\\"tokenType\\":\\"Bearer\\",\\"accessToken\\":\\"%s\\",\\"expiresIn\\":%d}"
								}
								"""
								.formatted(tokenIssuedAt.getEpochSecond(), tokenValue, expiresIn)));

		// save the parsed token
		given(clientAccessTokenDao.save(any())).willAnswer(inv -> {
			ClientAccessTokenEntity entity = inv.getArgument(0);
			return entity.getId();
		});

		// request given URI
		final String responseBody = randomString();
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setHeader(CONTENT_TYPE, MediaType.TEXT_PLAIN).setBody(responseBody));

		// WHEN
		final URI reqUri = fromUriString(baseUrl).path("/test").buildAndExpand(region).toUri();

		final String result = helper.httpGet("Test", integration, String.class, (headers) -> reqUri,
				ResponseEntity<String>::getBody);

		// THEN
		// @formatter:off
		then(lock).should().lock();

		final URI tokenRequestUri = fromUriString(baseUrl)
				.path(SigenergyRestOperationsHelper.KEY_TOKEN_REQUEST_PATH)
				.buildAndExpand(region.getKey())
				.toUri();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request method for token is POST")
			.returns("POST", from(req -> req.getMethod()))
			.as("HTTP request URI for token includes integration region")
			.returns(tokenRequestUri, from(req -> req.getRequestUrl().uri()))
			.as("HTTP request body for token is JSON object with key field encoded as Basic auth of appKey:appSecret")
			.returns(Map.of(
					"key", SigenergyRestOperationsHelper.encodeAuthKey(appKey, appSecret)
					), from(req -> JsonUtils.getStringMap(req.getBody().readUtf8())))
			;

		then(clientAccessTokenDao).should().save(clientAccessTokenCaptor.capture());
		and.then(clientAccessTokenCaptor.getValue())
			.as("Saved token registraiton ID based on integration ID")
			.returns(userIdSystemIdentifier(integration.getUserId(),
					SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER,
					integration.getConfigId()), from(ClientAccessTokenEntity::getRegistrationId))
			.as("Saved token username is app key")
			.returns(appKey, from(ClientAccessTokenEntity::getPrincipalName))
			.as("Saved token type from response")
			.returns("Bearer", from(ClientAccessTokenEntity::getAccessTokenType))
			.as("Saved token value from login response")
			.returns(tokenValue, from(ClientAccessTokenEntity::getAccessTokenValue))
			.as("Saved issue date from login response timestamp")
			.returns(tokenIssuedAt, from(ClientAccessTokenEntity::getAccessTokenIssuedAt))
			.as("Saved expiration date from respones timestamp + expiresIn")
			.returns(tokenIssuedAt.plusSeconds(expiresIn), from(ClientAccessTokenEntity::getAccessTokenExpiresAt))
			;

		then(lock).should().unlock();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request method for data is GET")
			.returns("GET", from(req -> req.getMethod()))
			.as("HTTP request URI for data is as given")
			.returns(reqUri, from(req -> req.getRequestUrl().uri()))
			;

		and.then(server.getRequestCount())
			.as("HTTP requests made for token followed by data")
			.isEqualTo(2)
			;

		and.then(result)
			.as("HTTP response body returned")
			.isEqualTo(responseBody)
			;
		// @formatter:on
	}

	@Test
	public void useExistingToken() throws Exception {
		// GIVEN
		final String baseUrl = server.url("").toString() + "{region}";
		final String appKey = randomString();
		final String appSecret = randomString();
		final String region = SigenergyRegion.values()[RNG.nextInt(SigenergyRegion.values().length)]
				.name();

		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), clock.instant());
		// @formatter:off
		integration.setServiceProps(Map.of(
				CloudIntegrationService.BASE_URL_SETTING, baseUrl,
				SigenergyCloudIntegrationService.APP_KEY_SETTING, appKey,
				SigenergyCloudIntegrationService.APP_SECRET_SETTING, appSecret,
				SigenergyCloudIntegrationService.REGION_SETTING, region
				));
		// @formatter:on

		final ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(
				new UserStringStringCompositePK(integration.getUserId(),
						userIdSystemIdentifier(integration.getUserId(),
								SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER,
								integration.getConfigId()),
						appKey),
				clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenType("Bearer");
		tokenEntity.setAccessToken(randomString().getBytes(UTF_8));
		tokenEntity.setEnabled(true);
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plusSeconds(60));

		given(lockCache.get(integration.getId())).willReturn(lock);

		// get the existing token
		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// request given URI
		final String responseBody = randomString();
		server.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setHeader(CONTENT_TYPE, MediaType.TEXT_PLAIN).setBody(responseBody));

		// WHEN
		final URI reqUri = fromUriString(baseUrl).path("/test").buildAndExpand(region).toUri();

		final String result = helper.httpGet("Test", integration, String.class, (headers) -> reqUri,
				ResponseEntity<String>::getBody);

		// THEN
		// @formatter:off
		then(lock).should().lock();

		then(clientAccessTokenDao).shouldHaveNoMoreInteractions();

		then(lock).should().unlock();

		and.then(server.takeRequest())
			.isNotNull()
			.as("HTTP request method for data is GET")
			.returns("GET", from(req -> req.getMethod()))
			.as("HTTP request URI for data is as given")
			.returns(reqUri, from(req -> req.getRequestUrl().uri()))
			;

		and.then(server.getRequestCount())
			.as("HTTP request made for data")
			.isEqualTo(1)
			;

		and.then(result)
			.as("HTTP response body returned")
			.isEqualTo(responseBody)
			;
		// @formatter:on
	}

	@Test
	public void authKey() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();

		// WHEN
		final String result = SigenergyRestOperationsHelper.encodeAuthKey(appKey, appSecret);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Authentiation key is HTTP basic encoding without padding")
			.isEqualTo(HttpHeaders.encodeBasicAuth(appKey, appSecret, UTF_8).replaceAll("=+$", ""))
			;
		// @formatter:on
	}

	@Test
	public void jsonObjectOrArray_object() {
		// GIVEN
		// @formatter:off
		final Map<String, Object> nestedData = Map.of(
				randomString(), randomLong()
				);
		final JsonNode json = JsonUtils.getTreeFromObject(Map.of(
				"foo", randomString(),
				"data", JsonUtils.getJSONString(nestedData)
				));
		// @formatter:off

		// WHEN
		final JsonNode result = SigenergyRestOperationsHelper.jsonObjectOrArray(mapper, json, "data");

		// THEN
		// @formatter:off
		and.then(JsonUtils.getStringMapFromTree(result))
			.as("Nested JSON decoded")
			.isEqualTo(nestedData)
			;
		// @formatter:on
	}

	@Test
	public void jsonObjectOrArray_array() throws IOException {
		// GIVEN
		// @formatter:off
		final List<String> nestedData = List.of(
				randomString(), randomString()
				);
		final JsonNode json = JsonUtils.getTreeFromObject(Map.of(
				"foo", randomString(),
				"data", JsonUtils.getJSONString(nestedData)
				));
		// @formatter:off

		// WHEN
		final JsonNode result = SigenergyRestOperationsHelper.jsonObjectOrArray(mapper, json, "data");

		// THEN
		// @formatter:off
		final String[] resultList = mapper.treeToValue(result, String[].class);
		and.then(resultList)
			.as("Nested JSON decoded")
			.containsExactlyElementsOf(nestedData)
			;
		// @formatter:on
	}

	@Test
	public void jsonObjectOrArray_alreadyObject() {
		// GIVEN
		// @formatter:off
		final Map<String, Object> nestedData = Map.of(
				randomString(), randomLong()
				);
		final JsonNode json = JsonUtils.getTreeFromObject(Map.of(
				"foo", randomString(),
				"data", nestedData
				));
		// @formatter:off

		// WHEN
		final JsonNode result = SigenergyRestOperationsHelper.jsonObjectOrArray(mapper, json, "data");

		// THEN
		// @formatter:off
		and.then(JsonUtils.getStringMapFromTree(result))
			.as("Nested object returned directly")
			.isEqualTo(nestedData)
			;
		// @formatter:on
	}

	@Test
	public void jsonObjectOrArray_alreadyArray() throws IOException {
		// GIVEN
		// @formatter:off
		final List<String> nestedData = List.of(
				randomString(), randomString()
				);
		final JsonNode json = JsonUtils.getTreeFromObject(Map.of(
				"foo", randomString(),
				"data", nestedData
				));
		// @formatter:off

		// WHEN
		final JsonNode result = SigenergyRestOperationsHelper.jsonObjectOrArray(mapper, json, "data");

		// THEN
		// @formatter:off
		final String[] resultList = mapper.treeToValue(result, String[].class);
		and.then(resultList)
			.as("Nested array returned directly")
			.containsExactlyElementsOf(nestedData)
			;
		// @formatter:on
	}

	@Test
	public void jsonObjectOrArray_notObjectOrArray() {
		// GIVEN
		// @formatter:off
		final JsonNode json = JsonUtils.getTreeFromObject(Map.of(
				"foo", randomString(),
				"data", randomString()
				));
		// @formatter:off

		// THEN
		// @formatter:off
		and.thenThrownBy(() -> {
			SigenergyRestOperationsHelper.jsonObjectOrArray(mapper, json, "data");
		}, "Exception thrown when field value is not a JSON string")
			.as("IllegalArgumentException thrown")
			.isInstanceOf(IllegalArgumentException.class)
			;
		// @formatter:on
	}

	@Test
	public void resolveRegion_noneSpecified() {
		// GIVEN
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), Instant.now());
		integration.setServiceProps(Map.of("foo", "bar"));

		// WHEN
		SigenergyRegion result = SigenergyRestOperationsHelper.resolveRegion(integration);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Default region returned")
			.isEqualTo(SigenergyRestOperationsHelper.DEFAULT_REGION)
			;
		// @formatter:on
	}

	@Test
	public void resolveRegion_notValid() {
		// GIVEN
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), Instant.now());
		integration.setServiceProps(Map.of(SigenergyCloudIntegrationService.REGION_SETTING, "bar"));

		// WHEN
		SigenergyRegion result = SigenergyRestOperationsHelper.resolveRegion(integration);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Default region returned")
			.isEqualTo(SigenergyRestOperationsHelper.DEFAULT_REGION)
			;
		// @formatter:on
	}

	@Test
	public void resolveRegion() {
		// GIVEN
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), Instant.now());

		final SigenergyRegion region = SigenergyRegion.NorthAmerica;
		integration
				.setServiceProps(Map.of(SigenergyCloudIntegrationService.REGION_SETTING, region.name()));

		// WHEN
		SigenergyRegion result = SigenergyRestOperationsHelper.resolveRegion(integration);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Explicit region returned")
			.isEqualTo(region)
			;
		// @formatter:on
	}

}
