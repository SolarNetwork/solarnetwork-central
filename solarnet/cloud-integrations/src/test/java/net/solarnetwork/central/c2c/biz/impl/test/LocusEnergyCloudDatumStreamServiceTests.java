/* ==================================================================
 * LocusEnergyCloudDatumStreamServiceTests.java - 9/10/2024 8:13:00 am
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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService.BASE_URI;
import static net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService.V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.CommonTestUtils.utf8StringResource;
import static net.solarnetwork.codec.JsonUtils.getObjectFromJSON;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.web.client.RestOperations;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyGranularity;
import net.solarnetwork.central.c2c.biz.impl.SpelCloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamValueType;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link LocusEnergyCloudDatumStreamService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class LocusEnergyCloudDatumStreamServiceTests {

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
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;

	@Captor
	private ArgumentCaptor<URI> uriCaptor;

	@Captor
	private ArgumentCaptor<HttpEntity<?>> httpEntityCaptor;

	private CloudIntegrationsExpressionService expressionService;

	private LocusEnergyCloudDatumStreamService service;

	@BeforeEach
	public void setup() {
		ExecutorService executor = new CallingThreadExecutorService();
		expressionService = new SpelCloudIntegrationsExpressionService();
		service = new LocusEnergyCloudDatumStreamService(new TaskExecutorAdapter(executor),
				userEventAppenderBiz, encryptor, expressionService, integrationDao, datumStreamDao,
				datumStreamPropertyDao, restOps, oauthClientManager);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasename(LocusEnergyCloudIntegrationService.class.getName());
		service.setMessageSource(msg);
	}

	private static String componentValueRef(Long componentId, String fieldName) {
		final int sep = fieldName.lastIndexOf('_');
		final String baseName = fieldName.substring(0, sep);
		return "/123/%d/%s/%s".formatted(componentId, baseName, fieldName);
	}

	@Test
	public void requestLatest_singleComponent() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		// request data
		final ObjectNode resJson = getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-01.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<ObjectNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res);

		// WHEN
		Datum result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId.toString())
						+ "?gran=latest&tz=UTC&fields=W_avg,TotWhExp_max"))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("23.717"));
		expectedSamples.putAccumulatingSampleValue("wattHours", 5936);
		and.then(result)
			.as("Datum parsed from HTTP response")
			.isNotNull()
			.as("Datum kind is from DatumStream configuration")
			.returns(datumStream.getKind(), from(Datum::getKind))
			.as("Datum object ID is from DatumStream configuration")
			.returns(datumStream.getObjectId(), from(Datum::getObjectId))
			.as("Datum source ID is from DatumStream configuration")
			.returns(datumStream.getSourceId(), from(Datum::getSourceId))
			.as("Datum timestamp from JSON response")
			.returns(Instant.parse("2014-04-01T12:00:00Z"), from(Datum::getTimestamp))
			.as("Datum samples from JSON response")
			.returns(expectedSamples, Datum::asSampleOperations)
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_multipleComponents() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId1 = randomLong();
		final Long componentId2 = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId1, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId2, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient)
				.willReturn(oauthAuthClient);

		// request data
		final var res1 = new ResponseEntity<ObjectNode>(getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-02.json", getClass()),
				ObjectNode.class), HttpStatus.OK);
		final var res2 = new ResponseEntity<ObjectNode>(getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-03.json", getClass()),
				ObjectNode.class), HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res1)
				.willReturn(res2);

		// WHEN
		Datum result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getAllValues())
			.as("Made 2 HTTP requests, one for each component")
			.hasSize(2)
			.containsOnly(
					BASE_URI.resolve(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId1.toString())
							+ "?gran=latest&tz=UTC&fields=W_avg"),
					BASE_URI.resolve(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId2.toString())
							+ "?gran=latest&tz=UTC&fields=TotWhExp_max")
			);

		and.then(httpEntityCaptor.getAllValues()).extracting(HttpEntity::getHeaders)
			.as("HTTP request includes OAuth Authorization header")
			.allMatch(headers -> headers.getFirst(AUTHORIZATION).equals("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("23.717"));
		expectedSamples.putAccumulatingSampleValue("wattHours", 5936);
		and.then(result)
			.as("Datum parsed from multiple HTTP responses are merged based on timestamps")
			.isNotNull()
			.as("Datum kind is from DatumStream configuration")
			.returns(datumStream.getKind(), from(Datum::getKind))
			.as("Datum object ID is from DatumStream configuration")
			.returns(datumStream.getObjectId(), from(Datum::getObjectId))
			.as("Datum source ID is from DatumStream configuration")
			.returns(datumStream.getSourceId(), from(Datum::getSourceId))
			.as("Datum timestamp from JSON response")
			.returns(Instant.parse("2014-04-01T12:00:00Z"), from(Datum::getTimestamp))
			.as("Datum samples from JSON response")
			.returns(expectedSamples, Datum::asSampleOperations)
			;
		// @formatter:on
	}

	@Test
	public void requestLatest_multipleComponents_multipleTimestamps() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId1 = randomLong();
		final Long componentId2 = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId1, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId2, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient)
				.willReturn(oauthAuthClient);

		// request data
		final var res1 = new ResponseEntity<ObjectNode>(getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-02.json", getClass()),
				ObjectNode.class), HttpStatus.OK);
		final var res2 = new ResponseEntity<ObjectNode>(getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-04.json", getClass()),
				ObjectNode.class), HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res1)
				.willReturn(res2);

		// WHEN
		Datum result = service.latestDatum(datumStream);

		// THEN
		// @formatter:off
		then(restOps).should(times(2)).exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getAllValues())
			.as("Made 2 HTTP requests, one for each component")
			.hasSize(2)
			.containsOnly(
					BASE_URI.resolve(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId1.toString())
							+ "?gran=latest&tz=UTC&fields=W_avg"),
					BASE_URI.resolve(V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId2.toString())
							+ "?gran=latest&tz=UTC&fields=TotWhExp_max")
			);

		and.then(httpEntityCaptor.getAllValues()).extracting(HttpEntity::getHeaders)
			.as("HTTP request includes OAuth Authorization header")
			.allMatch(headers -> headers.getFirst(AUTHORIZATION).equals("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		DatumSamples expectedSamples = new DatumSamples();
		expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("23.717"));
		and.then(result)
			.as("Datum parsed from multiple HTTP responses with different timestamps returns datum with highest timestamp")
			.isNotNull()
			.as("Datum kind is from DatumStream configuration")
			.returns(datumStream.getKind(), from(Datum::getKind))
			.as("Datum object ID is from DatumStream configuration")
			.returns(datumStream.getObjectId(), from(Datum::getObjectId))
			.as("Datum source ID is from DatumStream configuration")
			.returns(datumStream.getSourceId(), from(Datum::getSourceId))
			.as("Datum timestamp from JSON response")
			.returns(Instant.parse("2014-04-01T12:00:00Z"), from(Datum::getTimestamp))
			.as("Datum samples from JSON response")
			.returns(expectedSamples, Datum::asSampleOperations)
			;
		// @formatter:on
	}

	@Test
	public void requestList_singleComponent() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		// request data
		final ObjectNode resJson = getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-05.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<ObjectNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res);

		// WHEN
		final Instant startDate = Instant.parse("2024-01-01T00:00:00Z");
		final Instant endDate = Instant.parse("2024-01-01T01:00:00Z");
		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(
					V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId.toString())
					+ "?gran=5min&tz=UTC&fields=W_avg,TotWhExp_max&start=%s&end=%s".formatted(
							ISO_LOCAL_DATE_TIME.format(startDate.atOffset(UTC)),
							ISO_LOCAL_DATE_TIME.format(endDate.atOffset(UTC))
					)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(result)
			.as("Datum list parsed from HTTP response")
			.hasSize(2)
			.satisfies(r -> {
				and.then(result.getNextQueryFilter())
					.as("Next filter not provided")
					.isNull()
					;
			})
			.as("All datum have properties taken from DatumStream configuration")
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final DatumSamples expectedSamples = new DatumSamples();
				expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("23.717"));
				expectedSamples.putAccumulatingSampleValue("wattHours", 5936);
				and.then(list)
					.element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2014-04-01T12:00:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples, Datum::asSampleOperations)
					;

				expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("24.717"));
				expectedSamples.putAccumulatingSampleValue("wattHours", 5937);
				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2014-04-01T12:05:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples, Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestList_singleComponent_partialResults() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		// request data
		final ObjectNode resJson = getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-05.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<ObjectNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res);

		// WHEN
		final Instant startDate = Instant.parse("2024-01-01T00:00:00Z");
		final Instant endDate = Instant.parse("2025-01-01T00:00:00Z");
		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI has end date truncated to 5min granularity constraint")
			.isEqualTo(BASE_URI.resolve(
					V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId.toString())
					+ "?gran=5min&tz=UTC&fields=W_avg,TotWhExp_max&start=%s&end=%s".formatted(
							ISO_LOCAL_DATE_TIME.format(startDate.atOffset(UTC)),
							ISO_LOCAL_DATE_TIME.format(startDate.plus(
									LocusEnergyGranularity.FiveMinute.getConstraint()).atOffset(UTC))
					)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(result)
			.as("Datum list parsed from HTTP response")
			.hasSize(2)
			.satisfies(r -> {
				and.then(result.getNextQueryFilter())
					.as("Next filter provided")
					.isNotNull()
					.as("Next start date is granularity constraint offset")
					.returns(startDate.plus(LocusEnergyGranularity.FiveMinute.getConstraint()),
							from(CloudDatumStreamQueryFilter::getStartDate))
					.as("Next end date is next start date plus constraint offset")
					.returns(startDate
							.plus(LocusEnergyGranularity.FiveMinute.getConstraint())
							.plus(LocusEnergyGranularity.FiveMinute.getConstraint()),
							from(CloudDatumStreamQueryFilter::getEndDate))
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestList_singleComponent_lastPagePartialResults() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		// request data
		final ObjectNode resJson = getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-05.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<ObjectNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res);

		// WHEN
		final Instant startDate = Instant.parse("2024-12-30T00:00:00Z");
		final Instant endDate = Instant.parse("2025-01-01T00:00:00Z");
		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI")
			.isEqualTo(BASE_URI.resolve(
					V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId.toString())
					+ "?gran=5min&tz=UTC&fields=W_avg,TotWhExp_max&start=%s&end=%s".formatted(
							ISO_LOCAL_DATE_TIME.format(startDate.atOffset(UTC)),
							ISO_LOCAL_DATE_TIME.format(endDate.atOffset(UTC))
					)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(result)
			.as("Datum list parsed from HTTP response")
			.hasSize(2)
			.satisfies(r -> {
				and.then(result.getNextQueryFilter())
					.as("Next filter not provided because no more date-based pages")
					.isNull()
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void requestList_singleComponent_overrideGranularity() {
		// GIVEN
		final String tokenUri = "https://example.com/oauth/token";
		final Long partnerId = randomLong();
		final String clientId = randomString();
		final String clientSecret = randomString();
		final String username = randomString();
		final String password = randomString();
		final Long componentId = randomLong();

		// configure integration
		final CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(TEST_USER_ID,
				randomLong(), now());
		// @formatter:off
		integration.setServiceProps(Map.of(
				LocusEnergyCloudIntegrationService.PARTNER_ID_SETTING, partnerId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_ID_SETTING, clientId,
				LocusEnergyCloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING, clientSecret,
				LocusEnergyCloudIntegrationService.USERNAME_SETTING, username,
				LocusEnergyCloudIntegrationService.PASSWORD_SETTING, password
			));
		// @formatter:on
		given(integrationDao.get(integration.getId())).willReturn(integration);

		// configure datum stream
		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final CloudDatumStreamConfiguration datumStream = new CloudDatumStreamConfiguration(TEST_USER_ID,
				randomLong(), now());
		datumStream.setIntegrationId(integration.getConfigId());
		datumStream.setKind(ObjectDatumKind.Node);
		datumStream.setObjectId(nodeId);
		datumStream.setSourceId(sourceId);
		datumStream.setServiceProps(
				Map.of(LocusEnergyCloudDatumStreamService.GRANULARITY_SETTING, "daily"));

		// configure datum stream properties
		final CloudDatumStreamPropertyConfiguration prop1 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 1, now());
		prop1.setEnabled(true);
		prop1.setPropertyType(DatumSamplesType.Instantaneous);
		prop1.setPropertyName("watts");
		prop1.setValueType(CloudDatumStreamValueType.Reference);
		prop1.setValueReference(componentValueRef(componentId, "W_avg"));

		final CloudDatumStreamPropertyConfiguration prop2 = new CloudDatumStreamPropertyConfiguration(
				TEST_USER_ID, datumStream.getConfigId(), 2, now());
		prop2.setEnabled(true);
		prop2.setPropertyType(DatumSamplesType.Accumulating);
		prop2.setPropertyName("wattHours");
		prop2.setValueType(CloudDatumStreamValueType.Reference);
		prop2.setValueReference(componentValueRef(componentId, "TotWhExp_max"));

		given(datumStreamPropertyDao.findAll(TEST_USER_ID, datumStream.getConfigId(), null))
				.willReturn(List.of(prop1, prop2));

		// @formatter:off
		@SuppressWarnings("deprecation")
		final ClientRegistration oauthClientReg = ClientRegistration.withRegistrationId("test")
				.authorizationGrantType(AuthorizationGrantType.PASSWORD)
				.clientId(randomString())
				.clientSecret(randomString())
				.tokenUri(tokenUri)
				.build();
		// @formatter:on

		final OAuth2AccessToken oauthAccessToken = new OAuth2AccessToken(TokenType.BEARER,
				randomString(), now(), now().plusSeconds(60));

		final OAuth2AuthorizedClient oauthAuthClient = new OAuth2AuthorizedClient(oauthClientReg, "Test",
				oauthAccessToken);

		given(oauthClientManager.authorize(any())).willReturn(oauthAuthClient);

		// request data
		final ObjectNode resJson = getObjectFromJSON(
				utf8StringResource("locus-energy-data-for-component-05.json", getClass()),
				ObjectNode.class);
		final var res = new ResponseEntity<ObjectNode>(resJson, HttpStatus.OK);
		given(restOps.exchange(any(), eq(HttpMethod.GET), any(), eq(ObjectNode.class))).willReturn(res);

		// WHEN
		final Instant startDate = Instant.parse("2024-01-01T00:00:00Z");
		final Instant endDate = Instant.parse("2024-01-01T01:00:00Z");
		final var filter = new BasicQueryFilter();
		filter.setStartDate(startDate);
		filter.setEndDate(endDate);
		filter.setParameters(Map.of(LocusEnergyCloudDatumStreamService.GRANULARITY_SETTING, "monthly"));
		CloudDatumStreamQueryResult result = service.datum(datumStream, filter);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(uriCaptor.capture(), eq(HttpMethod.GET), httpEntityCaptor.capture(), eq(ObjectNode.class));

		and.then(uriCaptor.getValue())
			.as("Request URI uses 'monthly' granularity from query filter, overriding datum stream setting")
			.isEqualTo(BASE_URI.resolve(
					V3_DATA_FOR_COMPOENNT_ID_URL_TEMPLATE.replace("{componentId}", componentId.toString())
					+ "?gran=monthly&tz=UTC&fields=W_avg,TotWhExp_max&start=%s&end=%s".formatted(
							ISO_LOCAL_DATE_TIME.format(startDate.atOffset(UTC)),
							ISO_LOCAL_DATE_TIME.format(endDate.atOffset(UTC))
					)))
			;

		and.then(httpEntityCaptor.getValue().getHeaders())
			.as("HTTP request includes OAuth Authorization header")
			.containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer %s".formatted(oauthAccessToken.getTokenValue())))
			;

		and.then(result)
			.as("Datum list parsed from HTTP response")
			.hasSize(2)
			.as("All datum have properties taken from DatumStream configuration")
			.allSatisfy(d -> {
				and.then(d)
					.as("Datum kind is from DatumStream configuration")
					.returns(datumStream.getKind(), from(Datum::getKind))
					.as("Datum object ID is from DatumStream configuration")
					.returns(datumStream.getObjectId(), from(Datum::getObjectId))
					.as("Datum source ID is from DatumStream configuration")
					.returns(datumStream.getSourceId(), from(Datum::getSourceId))
					;
			})
			.satisfies(list -> {
				final DatumSamples expectedSamples = new DatumSamples();
				expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("23.717"));
				expectedSamples.putAccumulatingSampleValue("wattHours", 5936);
				and.then(list)
					.element(0)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2014-04-01T12:00:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples, Datum::asSampleOperations)
					;

				expectedSamples.putInstantaneousSampleValue("watts", new BigDecimal("24.717"));
				expectedSamples.putAccumulatingSampleValue("wattHours", 5937);
				and.then(list)
					.element(1)
					.as("Datum timestamp from JSON response")
					.returns(Instant.parse("2014-04-01T12:05:00Z"), from(Datum::getTimestamp))
					.as("Datum samples from JSON response")
					.returns(expectedSamples, Datum::asSampleOperations)
					;
			})
			;
		// @formatter:on
	}

}
