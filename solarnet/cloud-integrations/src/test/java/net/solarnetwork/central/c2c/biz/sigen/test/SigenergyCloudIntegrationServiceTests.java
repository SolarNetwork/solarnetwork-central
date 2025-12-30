/* ==================================================================
 * SigenergyCloudIntegrationServiceTests.java - 6/12/2025 1:46:51 pm
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
import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.APP_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.OFFBOARD_CONFIGURATION_TOPIC;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.ONBOARD_CONFIGURATION_TOPIC;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService.SYSTEM_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRegion;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationTopicConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.service.StaticOptionalService;
import tools.jackson.databind.JsonNode;

/**
 * Test cases for the {@link SigenergyCloudIntegrationService}.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class SigenergyCloudIntegrationServiceTests {

	private static final Long TEST_USER_ID = randomLong();

	@Mock
	private CloudDatumStreamService datumStreamService;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private ClientAccessTokenDao clientAccessTokenDao;

	@Captor
	private ArgumentCaptor<RequestEntity<String>> httpRequestCaptor;

	private MutableClock clock;

	private SigenergyCloudIntegrationService service;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now().truncatedTo(ChronoUnit.HOURS), UTC);

		var restOpsHelper = new SigenergyRestOperationsHelper(
				LoggerFactory.getLogger(SigenergyCloudIntegrationService.class), userEventAppenderBiz,
				restOps, CloudIntegrationsUserEvents.INTEGRATION_HTTP_ERROR_TAGS, encryptor,
				_ -> SigenergyCloudIntegrationService.SECURE_SETTINGS, clock,
				JsonUtils.JSON_OBJECT_MAPPER, clientAccessTokenDao, new StaticOptionalService<>(null));

		service = new SigenergyCloudIntegrationService(List.of(datumStreamService), List.of(),
				userEventAppenderBiz, encryptor, restOpsHelper, JsonUtils.JSON_OBJECT_MAPPER);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(SigenergyCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msg);
	}

	@Test
	public void validate_missingAuthSettings() {
		// GIVEN
		final CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		conf.setServiceProps(Map.of(
				"foo", "bar"
			));
		// @formatter:on

		// WHEN
		Result<Void> result = service.validate(conf, Locale.getDefault());

		// THEN
		// @formatter:off
		then(restOps).shouldHaveNoInteractions();

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is NOT success")
			.returns(false, from(Result::getSuccess))
			.satisfies(r -> {
				and.then(r.getErrors())
					.as("Error details provided for missing API keys")
					.hasSize(2)
					.satisfies(errors -> {
						and.then(errors)
							.as("Error detail")
							.element(0)
							.as("App key flagged")
							.returns(APP_KEY_SETTING, from(ErrorDetail::getLocation))
							;
						and.then(errors)
							.as("Error detail")
							.element(1)
							.as("App secret flagged")
							.returns(APP_SECRET_SETTING, from(ErrorDetail::getLocation))
							;
					})
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void validate_ok() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final ResponseEntity<String> res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(any(), eq(String.class))).willReturn(res);

		// WHEN

		Result<Void> result = service.validate(config, Locale.getDefault());

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(String.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is GET")
			.returns(HttpMethod.GET, from(RequestEntity::getMethod))
			.as("Request URI for system list")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.SYSTEM_LIST_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void onboard_single() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-01.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				ONBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, systemId)));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void onboard_single_fail() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-02.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				ONBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, systemId)));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is not success")
			.returns(false, from(Result::getSuccess))
			.as("Code returned")
			.returns("SGCI.0003", from(Result::getCode))
			.as("No data returned")
			.returns(null, from(Result::getData))
			.extracting(Result::getErrors, list(ErrorDetail.class))
			.as("One erorr returned for failed onboard")
			.hasSize(1)
			.element(0)
			.as("Single code returned as error code")
			.returns("123", from(ErrorDetail::getCode))
			.as("System ID returned as rejected value")
			.returns("ABC123", from(ErrorDetail::getRejectedValue))
			;
		// @formatter:on
	}

	@Test
	public void onboard_multiple_delimitedString() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId1 = randomString();
		final String systemId2 = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-03.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config,
				new CloudIntegrationTopicConfiguration(ONBOARD_CONFIGURATION_TOPIC,
						Map.of(SYSTEM_ID_SETTING,
								commaDelimitedStringFromCollection(List.of(systemId1, systemId2)))));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId1, systemId2}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void onboard_multiple_array() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId1 = randomString();
		final String systemId2 = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-03.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config,
				new CloudIntegrationTopicConfiguration(ONBOARD_CONFIGURATION_TOPIC,
						Map.of(SYSTEM_ID_SETTING, new String[] { systemId1, systemId2 })));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId1, systemId2}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void onboard_multiple_list() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId1 = randomString();
		final String systemId2 = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-03.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				ONBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, List.of(systemId1, systemId2))));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId1, systemId2}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void onboard_multiple_failSome() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId1 = randomString();
		final String systemId2 = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-04.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				ONBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, List.of(systemId1, systemId2))));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId1, systemId2}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is not success")
			.returns(false, from(Result::getSuccess))
			.as("Code returned")
			.returns("SGCI.0003", from(Result::getCode))
			.as("No data returned")
			.returns(null, from(Result::getData))
			.extracting(Result::getErrors, list(ErrorDetail.class))
			.as("One erorr returned for failed onboard")
			.hasSize(1)
			.element(0)
			.as("Single code returned as error code")
			.returns("123", from(ErrorDetail::getCode))
			.as("System ID returned as rejected value")
			.returns("DEF234", from(ErrorDetail::getRejectedValue))
			;
		// @formatter:on
	}

	@Test
	public void onboard_multiple_failAll() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId1 = randomString();
		final String systemId2 = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-onboard-05.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				ONBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, List.of(systemId1, systemId2))));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId1, systemId2}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.ONBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is not success")
			.returns(false, from(Result::getSuccess))
			.as("Code returned")
			.returns("SGCI.0003", from(Result::getCode))
			.as("No data returned")
			.returns(null, from(Result::getData))
			.extracting(Result::getErrors, list(ErrorDetail.class))
			.as("Two erorrs returned for failed onboards")
			.hasSize(2)
			.satisfies(errors -> {
				and.then(errors).element(0)
					.as("Single code returned as error code")
					.returns("123", from(ErrorDetail::getCode))
					.as("System ID returned as rejected value")
					.returns("ABC123", from(ErrorDetail::getRejectedValue))
					;
				and.then(errors).element(1)
					.as("Single code returned as error code")
					.returns("321", from(ErrorDetail::getCode))
					.as("System ID returned as rejected value")
					.returns("DEF234", from(ErrorDetail::getRejectedValue))
					;
			})
			;
		// @formatter:on
	}

	public void offboard_single() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-offboard-01.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				OFFBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, systemId)));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.OFFBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void offboard_single_fail() {
		// GIVEN
		final String appKey = randomString();
		final String appSecret = randomString();
		final String systemId = randomString();

		final CloudIntegrationConfiguration config = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		config.setServiceProps(Map.of(
				APP_KEY_SETTING, appKey,
				APP_SECRET_SETTING, appSecret
		));
		// @formatter:on

		// get access token
		final String authToken = randomString();
		final String registrationId = userIdSystemIdentifier(config.getUserId(),
				SigenergyRestOperationsHelper.CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, config.getConfigId());
		ClientAccessTokenEntity tokenEntity = new ClientAccessTokenEntity(TEST_USER_ID, registrationId,
				appKey, clock.instant());
		tokenEntity.setAccessTokenIssuedAt(clock.instant());
		tokenEntity.setAccessTokenExpiresAt(clock.instant().plus(1L, ChronoUnit.HOURS));
		tokenEntity.setAccessToken(authToken.getBytes(UTF_8));

		given(clientAccessTokenDao.get(tokenEntity.getId())).willReturn(tokenEntity);

		// get data
		final JsonNode onboardResJson = JsonUtils.getObjectFromJSON(
				utf8StringResource("sigen-offboard-02.json", getClass()), JsonNode.class);
		final ResponseEntity<JsonNode> res = new ResponseEntity<JsonNode>(onboardResJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(JsonNode.class))).willReturn(res);

		// WHEN

		Result<?> result = service.configure(config, new CloudIntegrationTopicConfiguration(
				OFFBOARD_CONFIGURATION_TOPIC, Map.of(SYSTEM_ID_SETTING, systemId)));

		// THEN
		// @formatter:off
		then(restOps).should().exchange(httpRequestCaptor.capture(), eq(JsonNode.class));

		and.then(httpRequestCaptor.getValue())
			.as("HTTP method is POST")
			.returns(HttpMethod.POST, from(RequestEntity::getMethod))
			.as("HTTP body is string array")
			.returns(new String[] {systemId}, from(RequestEntity::getBody))
			.as("Request URI for system onboard")
			.returns(UriComponentsBuilder.fromUriString(BASE_URI_TEMPLATE)
					.buildAndExpand(SigenergyRegion.AustraliaNewZealand.getKey()).toUri()
					.resolve(SigenergyRestOperationsHelper.OFFBOARD_PATH), from(RequestEntity::getUrl))
			.extracting(RequestEntity::getHeaders, map(String.class, List.class))
			.as("Request headers contains Bearer authentication")
			.containsEntry(AUTHORIZATION, List.of("Bearer " +authToken))
			;

		and.then(result)
			.as("Result generated")
			.isNotNull()
			.as("Result is not success")
			.returns(false, from(Result::getSuccess))
			.as("Code returned")
			.returns("SGCI.0005", from(Result::getCode))
			.as("No data returned")
			.returns(null, from(Result::getData))
			.extracting(Result::getErrors, list(ErrorDetail.class))
			.as("One erorr returned for failed onboard")
			.hasSize(1)
			.element(0)
			.as("Single code returned as error code")
			.returns("123", from(ErrorDetail::getCode))
			.as("System ID returned as rejected value")
			.returns("ABC123", from(ErrorDetail::getRejectedValue))
			;
		// @formatter:on
	}

}
