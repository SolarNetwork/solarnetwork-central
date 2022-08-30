/* ==================================================================
 * DaoFlexibilityProviderBizTests.java - 17/08/2022 8:26:20 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.biz.dao.test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.REGISTER_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizationHeader;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.HANDSHAKE_ACK_URL_PATH;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.UrlPaths_20.UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityForecast;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.ForecastType;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.oscp.domain.TimeBlockAmount;
import net.solarnetwork.central.oscp.fp.biz.dao.DaoFlexibilityProviderBiz;
import net.solarnetwork.central.oscp.http.RestOpsExternalSystemClient;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.test.CallingThreadExecutorService;
import net.solarnetwork.web.support.LoggingHttpRequestInterceptor;
import oscp.v20.AdjustGroupCapacityForecast;
import oscp.v20.HandshakeAcknowledge;
import oscp.v20.Register;
import oscp.v20.UpdateGroupCapacityForecast;
import oscp.v20.VersionUrl;

/**
 * Test cases for the {@link DaoFlexibilityProviderBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoFlexibilityProviderBizTests {

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private FlexibilityProviderDao flexibilityProviderDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Captor
	private ArgumentCaptor<ConfigurationFilter> cpFilterCaptor;

	@Captor
	private ArgumentCaptor<ConfigurationFilter> coFilterCaptor;

	@Captor
	private ArgumentCaptor<String> tokenCaptor;

	private ObjectMapper objectMapper;
	private CallingThreadExecutorService executor;
	private RestTemplate restTemplate;
	private MockRestServiceServer mockExternalSystem;
	private DaoFlexibilityProviderBiz biz;

	@BeforeEach
	public void setup() {
		objectMapper = JsonUtils.newObjectMapper();
		executor = new CallingThreadExecutorService();
		restTemplate = new RestTemplate(
				new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
		restTemplate.setInterceptors(Arrays.asList(new LoggingHttpRequestInterceptor()));

		mockExternalSystem = MockRestServiceServer.bindTo(restTemplate).build();
		biz = new DaoFlexibilityProviderBiz(executor,
				new RestOpsExternalSystemClient(restTemplate, userEventAppenderBiz),
				userEventAppenderBiz, flexibilityProviderDao, capacityProviderDao, capacityOptimizerDao,
				capacityGroupDao);
		biz.setTaskStartDelay(0);
		biz.setTaskStartDelayRandomness(0);
		// no biz.setTxTemplate(tt); to use test transaction
	}

	@AfterEach
	public void teardown() {
		mockExternalSystem.verify();
	}

	@Test
	public void register_cp_configurationNotFound() {
		// GIVEN
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final String sysToken = randomUUID().toString();
		final KeyValuePair versionUrl = new KeyValuePair("2.0", "http://localhost:9991/oscp/cp/2.0");

		final FilterResults<CapacityProviderConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				emptyList());
		given(capacityProviderDao.findFiltered(any())).willReturn(cpResults);

		// WHEN
		CompletableFuture<Void> sysReady = new CompletableFuture<>();
		AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
			biz.register(authInfo, sysToken, versionUrl, sysReady);
		}, "Exception thrown when CP configuration not found");

		// THEN
		assertThat("Reason is Registration Not Confirmed", ex.getReason(),
				is(equalTo(AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED)));

		then(capacityProviderDao).should().findFiltered(cpFilterCaptor.capture());
		ConfigurationFilter cpFilter = cpFilterCaptor.getValue();
		assertThat("CP filter included user criteria from auth info", cpFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CP filter included conf criteria from auth info", cpFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));
	}

	@Test
	public void register_co_configurationNotFound() {
		// GIVEN
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final String sysToken = randomUUID().toString();
		final KeyValuePair versionUrl = new KeyValuePair("2.0", "http://localhost:9991/oscp/co/2.0");

		final FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				emptyList());
		given(capacityOptimizerDao.findFiltered(any())).willReturn(cpResults);

		// WHEN
		CompletableFuture<Void> sysReady = new CompletableFuture<>();
		AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
			biz.register(authInfo, sysToken, versionUrl, sysReady);
		}, "Exception thrown when CP configuration not found");

		// THEN
		assertThat("Reason is Registration Not Confirmed", ex.getReason(),
				is(equalTo(AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED)));

		then(capacityOptimizerDao).should().findFiltered(coFilterCaptor.capture());
		ConfigurationFilter coFilter = coFilterCaptor.getValue();
		assertThat("CO filter included user criteria from auth info", coFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CO filter included conf criteria from auth info", coFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));
	}

	@Test
	public void register_cp() throws Exception {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final String sysToken = randomUUID().toString();
		final KeyValuePair sysVersionUrl = new KeyValuePair("2.0",
				"http://oscp.example.com/oscp/cp/2.0");

		final CapacityProviderConfiguration cp = new CapacityProviderConfiguration(userId,
				randomUUID().getMostSignificantBits(), Instant.now());
		cp.setRegistrationStatus(RegistrationStatus.Pending);
		cp.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		final FilterResults<CapacityProviderConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				singleton(cp));
		given(capacityProviderDao.findFiltered(any())).willReturn(cpResults);

		// save system versionUrl details
		given(capacityProviderDao.save(same(cp))).willReturn(cp.getId());

		// generate new auth token
		final UserLongCompositePK fpId = new UserLongCompositePK(userId, cp.getFlexibilityProviderId());
		final String fpToken = randomUUID().toString();
		given(flexibilityProviderDao.createAuthToken(fpId)).willReturn(fpToken);

		// the rest happens in async task (but we have forced to calling thread here)

		// get the conf for updating
		final CapacityProviderConfiguration cp2 = cp.clone();
		cp2.setOscpVersion(sysVersionUrl.getKey());
		cp2.setBaseUrl(sysVersionUrl.getValue());
		given(capacityProviderDao.getForUpdate(cp2.getId())).willReturn(cp2);

		given(capacityProviderDao.getExternalSystemAuthToken(cp2.getId())).willReturn(sysToken);

		// call out to the external system registration endpoint
		Entry<String, String> fpVersionUrl = biz.getVersionUrlMap().entrySet().iterator().next();
		Register expectedPost = new Register(fpToken, Collections
				.singletonList(new VersionUrl(fpVersionUrl.getKey(), fpVersionUrl.getValue())));
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem.expect(once(), requestTo(sysVersionUrl.getValue() + REGISTER_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// save system versionUrl details
		given(capacityProviderDao.save(same(cp2))).willReturn(cp2.getId());

		// WHEN
		CompletableFuture<Void> sysReady = CompletableFuture.completedFuture(null);
		biz.register(authInfo, sysToken, sysVersionUrl, sysReady);

		// THEN
		then(capacityProviderDao).should().findFiltered(cpFilterCaptor.capture());
		ConfigurationFilter cpFilter = cpFilterCaptor.getValue();
		assertThat("CP filter included user criteria from auth info", cpFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CP filter included conf criteria from auth info", cpFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		// save system auth token
		then(capacityProviderDao).should().saveExternalSystemAuthToken(cp.getId(), sysToken);

		assertThat("Configuration status updated", cp2.getRegistrationStatus(),
				is(equalTo(RegistrationStatus.Registered)));
	}

	@Test
	public void register_co() throws Exception {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final String sysToken = randomUUID().toString();
		final KeyValuePair sysVersionUrl = new KeyValuePair("2.0",
				"http://oscp.example.com/oscp/co/2.0");

		final CapacityOptimizerConfiguration co = new CapacityOptimizerConfiguration(userId,
				randomUUID().getMostSignificantBits(), Instant.now());
		co.setRegistrationStatus(RegistrationStatus.Pending);
		co.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		final FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> coResults = new BasicFilterResults<>(
				singleton(co));
		given(capacityOptimizerDao.findFiltered(any())).willReturn(coResults);

		// save reg status
		given(capacityOptimizerDao.save(same(co))).willReturn(co.getId());

		// generate new auth token
		final UserLongCompositePK fpId = new UserLongCompositePK(userId, co.getFlexibilityProviderId());
		final String fpToken = randomUUID().toString();
		given(flexibilityProviderDao.createAuthToken(fpId)).willReturn(fpToken);

		// the rest happens in async task (but we have forced to calling thread here)

		// get the conf for updating
		final CapacityOptimizerConfiguration co2 = co.clone();
		co2.setOscpVersion(sysVersionUrl.getKey());
		co2.setBaseUrl(sysVersionUrl.getValue());
		given(capacityOptimizerDao.getForUpdate(co2.getId())).willReturn(co2);

		given(capacityOptimizerDao.getExternalSystemAuthToken(co2.getId())).willReturn(sysToken);

		// call out to the external system registration endpoint
		Entry<String, String> fpVersionUrl = biz.getVersionUrlMap().entrySet().iterator().next();
		Register expectedPost = new Register(fpToken, Collections
				.singletonList(new VersionUrl(fpVersionUrl.getKey(), fpVersionUrl.getValue())));
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem.expect(once(), requestTo(sysVersionUrl.getValue() + REGISTER_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// save system versionUrl details
		given(capacityOptimizerDao.save(same(co2))).willReturn(co2.getId());

		// WHEN
		CompletableFuture<Void> sysReady = CompletableFuture.completedFuture(null);
		biz.register(authInfo, sysToken, sysVersionUrl, sysReady);

		// THEN
		then(capacityOptimizerDao).should().findFiltered(coFilterCaptor.capture());
		ConfigurationFilter coFilter = coFilterCaptor.getValue();
		assertThat("CO filter included user criteria from auth info", coFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CO filter included conf criteria from auth info", coFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		// save system auth token
		then(capacityOptimizerDao).should().saveExternalSystemAuthToken(co.getId(), sysToken);

		assertThat("Configuration status updated", co2.getRegistrationStatus(),
				is(equalTo(RegistrationStatus.Registered)));
	}

	@Test
	public void handshake_cp() throws Exception {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final SystemSettings settings = new SystemSettings(123, EnumSet.of(MeasurementStyle.Continuous));

		// load the configuration
		final CapacityProviderConfiguration cp = new CapacityProviderConfiguration(authInfo.id(),
				Instant.now());
		cp.setOscpVersion("2.0");
		cp.setBaseUrl("http://localhost/" + UUID.randomUUID().toString());
		cp.setRegistrationStatus(RegistrationStatus.Registered);
		cp.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		final FilterResults<CapacityProviderConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				singleton(cp));
		given(capacityProviderDao.findFiltered(any())).willReturn(cpResults);

		// the rest happens in async task (but we have forced to calling thread here)

		// get the conf, not for updating
		final CapacityProviderConfiguration cp2 = cp.clone();
		given(capacityProviderDao.get(cp2.getId())).willReturn(cp2);

		// get the system auth token
		final String sysToken = randomUUID().toString();
		given(capacityProviderDao.getExternalSystemAuthToken(cp2.getId())).willReturn(sysToken);

		// call out to the external system handshake endpoint
		HandshakeAcknowledge expectedPost = new HandshakeAcknowledge();
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem.expect(once(), requestTo(cp2.getBaseUrl() + HANDSHAKE_ACK_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// WHEN
		CompletableFuture<Void> sysReady = CompletableFuture.completedFuture(null);
		biz.handshake(authInfo, settings, sysReady);

		// THEN
		then(capacityProviderDao).should().findFiltered(cpFilterCaptor.capture());
		ConfigurationFilter cpFilter = cpFilterCaptor.getValue();
		assertThat("CP filter included user criteria from auth info", cpFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CP filter included conf criteria from auth info", cpFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		then(capacityProviderDao).should().saveSettings(eq(authInfo.id()), same(settings));
	}

	@Test
	public void handshake_co() throws Exception {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final SystemSettings settings = new SystemSettings(123, EnumSet.of(MeasurementStyle.Continuous));

		// load the configuration
		final CapacityOptimizerConfiguration cp = new CapacityOptimizerConfiguration(authInfo.id(),
				Instant.now());
		cp.setOscpVersion("2.0");
		cp.setBaseUrl("http://localhost/" + UUID.randomUUID().toString());
		cp.setRegistrationStatus(RegistrationStatus.Registered);
		cp.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		final FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				singleton(cp));
		given(capacityOptimizerDao.findFiltered(any())).willReturn(cpResults);

		// the rest happens in async task (but we have forced to calling thread here)

		// get the conf, not for updating
		final CapacityOptimizerConfiguration cp2 = cp.clone();
		given(capacityOptimizerDao.get(cp2.getId())).willReturn(cp2);

		// get the system auth token
		final String sysToken = randomUUID().toString();
		given(capacityOptimizerDao.getExternalSystemAuthToken(cp2.getId())).willReturn(sysToken);

		// call out to the external system handshake endpoint
		HandshakeAcknowledge expectedPost = new HandshakeAcknowledge();
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem.expect(once(), requestTo(cp2.getBaseUrl() + HANDSHAKE_ACK_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// WHEN
		CompletableFuture<Void> sysReady = CompletableFuture.completedFuture(null);
		biz.handshake(authInfo, settings, sysReady);

		// THEN
		then(capacityOptimizerDao).should().findFiltered(cpFilterCaptor.capture());
		ConfigurationFilter cpFilter = cpFilterCaptor.getValue();
		assertThat("CP filter included user criteria from auth info", cpFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CP filter included conf criteria from auth info", cpFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		then(capacityOptimizerDao).should().saveSettings(eq(authInfo.id()), same(settings));
	}

	@Test
	public void heartbeat_cp() {
		// GIVEN
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final Instant expires = Instant.now().plusSeconds(1);

		// WHEN
		biz.heartbeat(authInfo, expires);

		// THEN
		then(capacityProviderDao).should().updateOfflineDate(authInfo.id(), expires);
	}

	@Test
	public void heartbeat_co() {
		// GIVEN
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final Instant expires = Instant.now().plusSeconds(1);

		// WHEN
		biz.heartbeat(authInfo, expires);

		// THEN
		then(capacityOptimizerDao).should().updateOfflineDate(authInfo.id(), expires);
	}

	@Test
	public void updateGroupCapacityForecast() throws Exception {
		// GIVEN
		final String groupIdentifier = randomUUID().toString();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final Instant topOfHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
		TimeBlockAmount amount = new TimeBlockAmount(topOfHour, topOfHour.plus(1, ChronoUnit.HOURS),
				Phase.All, new BigDecimal("3.3"), MeasurementUnit.kW);
		final CapacityForecast forecast = new CapacityForecast(ForecastType.Consumption,
				Collections.singletonList(amount));

		// find the group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(authInfo.userId(),
				randomUUID().getMostSignificantBits(), Instant.now());
		group.setCapacityOptimizerId(randomUUID().getMostSignificantBits());
		group.setIdentifier(groupIdentifier);
		given(capacityGroupDao.findForCapacityProvider(authInfo.userId(), authInfo.entityId(),
				groupIdentifier)).willReturn(group);

		// get the optimizer
		CapacityOptimizerConfiguration conf = new CapacityOptimizerConfiguration(authInfo.userId(),
				group.getCapacityOptimizerId(), Instant.now());
		conf.setOscpVersion("2.0");
		conf.setBaseUrl("http://localhost/" + UUID.randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		given(capacityOptimizerDao.get(conf.getId())).willReturn(conf);

		// get the system auth token
		final String sysToken = randomUUID().toString();
		given(capacityOptimizerDao.getExternalSystemAuthToken(conf.getId())).willReturn(sysToken);

		// call out to the external system UpdateGroupCapacityForecast endpoint
		UpdateGroupCapacityForecast expectedPost = forecast
				.toOscp20UpdateGroupCapacityValue(groupIdentifier);
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem
				.expect(once(), requestTo(conf.getBaseUrl() + UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// WHEN
		biz.updateGroupCapacityForecast(authInfo, groupIdentifier, forecast);
	}

	@Test
	public void adjustGroupCapacityForecast() throws Exception {
		// GIVEN
		final String groupIdentifier = randomUUID().toString();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final Instant topOfHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
		TimeBlockAmount amount = new TimeBlockAmount(topOfHour, topOfHour.plus(1, ChronoUnit.HOURS),
				Phase.All, new BigDecimal("3.3"), MeasurementUnit.kW);
		final CapacityForecast forecast = new CapacityForecast(ForecastType.Consumption,
				Collections.singletonList(amount));

		// find the group
		CapacityGroupConfiguration group = new CapacityGroupConfiguration(authInfo.userId(),
				randomUUID().getMostSignificantBits(), Instant.now());
		group.setCapacityProviderId(randomUUID().getMostSignificantBits());
		group.setIdentifier(groupIdentifier);
		given(capacityGroupDao.findForCapacityProvider(authInfo.userId(), authInfo.entityId(),
				groupIdentifier)).willReturn(group);

		// get the optimizer
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(authInfo.userId(),
				group.getCapacityProviderId(), Instant.now());
		conf.setOscpVersion("2.0");
		conf.setBaseUrl("http://localhost/" + UUID.randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		given(capacityProviderDao.get(conf.getId())).willReturn(conf);

		// get the system auth token
		final String sysToken = randomUUID().toString();
		given(capacityProviderDao.getExternalSystemAuthToken(conf.getId())).willReturn(sysToken);

		// call out to the external system AdjustGroupCapacityForecast endpoint
		AdjustGroupCapacityForecast expectedPost = forecast
				.toOscp20AdjustGroupCapacityValue(groupIdentifier);
		String expectedPostJson = objectMapper.writeValueAsString(expectedPost);
		mockExternalSystem
				.expect(once(), requestTo(conf.getBaseUrl() + ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(AUTHORIZATION, tokenAuthorizationHeader(sysToken)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().json(expectedPostJson, false)).andRespond(withNoContent());

		// WHEN
		biz.adjustGroupCapacityForecast(authInfo, groupIdentifier, forecast);
	}

}
