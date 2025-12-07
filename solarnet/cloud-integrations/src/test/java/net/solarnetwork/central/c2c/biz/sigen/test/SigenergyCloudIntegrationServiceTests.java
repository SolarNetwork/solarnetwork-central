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
import static net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper.BASE_URI_TEMPLATE;
import static net.solarnetwork.central.domain.UserIdentifiableSystem.userIdSystemIdentifier;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
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
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.Result.ErrorDetail;
import net.solarnetwork.service.StaticOptionalService;

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
				serviceIdentifier -> SigenergyCloudIntegrationService.SECURE_SETTINGS, clock,
				JsonUtils.newObjectMapper(), clientAccessTokenDao, new StaticOptionalService<>(null));

		service = new SigenergyCloudIntegrationService(List.of(datumStreamService), List.of(),
				userEventAppenderBiz, encryptor, restOpsHelper);

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

}
