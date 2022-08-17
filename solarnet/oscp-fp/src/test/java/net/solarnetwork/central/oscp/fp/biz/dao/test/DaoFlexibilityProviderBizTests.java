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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.fp.biz.dao.DaoFlexibilityProviderBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoFlexibilityProviderBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoFlexibilityProviderBizTests {

	@Mock
	private FlexibilityProviderDao flexibilityProviderDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Captor
	private ArgumentCaptor<ConfigurationFilter> cpFilterCaptor;

	@Captor
	private ArgumentCaptor<ConfigurationFilter> coFilterCaptor;

	@Captor
	private ArgumentCaptor<String> tokenCaptor;

	private DaoFlexibilityProviderBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoFlexibilityProviderBiz(flexibilityProviderDao, capacityProviderDao,
				capacityOptimizerDao);
	}

	@Test
	public void register_cp_configurationNotFound() {
		// GIVEN
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(randomUUID().getMostSignificantBits(),
						randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final String sysToken = randomUUID().toString();

		final FilterResults<CapacityProviderConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				emptyList());
		given(capacityProviderDao.findFiltered(any())).willReturn(cpResults);

		// WHEN
		AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
			biz.register(authInfo, sysToken);
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

		final FilterResults<CapacityOptimizerConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				emptyList());
		given(capacityOptimizerDao.findFiltered(any())).willReturn(cpResults);

		// WHEN
		AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
			biz.register(authInfo, sysToken);
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
	public void register_cp() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityProvider);
		final String sysToken = randomUUID().toString();

		final CapacityProviderConfiguration cp = new CapacityProviderConfiguration(userId,
				randomUUID().getMostSignificantBits(), Instant.now());
		cp.setRegistrationStatus(RegistrationStatus.Pending);
		cp.setFlexibilityProviderId(randomUUID().getMostSignificantBits());
		final FilterResults<CapacityProviderConfiguration, UserLongCompositePK> cpResults = new BasicFilterResults<>(
				singleton(cp));
		given(capacityProviderDao.findFiltered(any())).willReturn(cpResults);

		// save reg status
		given(capacityProviderDao.save(same(cp))).willReturn(cp.getId());

		// generate new auth token
		final UserLongCompositePK fpId = new UserLongCompositePK(userId, cp.getFlexibilityProviderId());
		given(flexibilityProviderDao.createAuthToken(fpId)).willReturn(randomUUID().toString());

		// WHEN
		String result = biz.register(authInfo, sysToken);

		// THEN
		then(capacityProviderDao).should().findFiltered(cpFilterCaptor.capture());
		ConfigurationFilter cpFilter = cpFilterCaptor.getValue();
		assertThat("CP filter included user criteria from auth info", cpFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CP filter included conf criteria from auth info", cpFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		assertThat("CP reg status changed to Registered", cp.getRegistrationStatus(),
				is(equalTo(RegistrationStatus.Registered)));
		assertThat("New auth token returned", result, is(notNullValue()));

		then(capacityProviderDao).should().saveExternalSystemAuthToken(eq(cp.getId()), tokenCaptor.capture());
		assertThat("System token saved to database", tokenCaptor.getValue(), is(equalTo(sysToken)));
	}

	@Test
	public void register_co() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();
		final AuthRoleInfo authInfo = new AuthRoleInfo(
				new UserLongCompositePK(userId, randomUUID().getMostSignificantBits()),
				OscpRole.CapacityOptimizer);
		final String sysToken = randomUUID().toString();

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
		given(flexibilityProviderDao.createAuthToken(fpId)).willReturn(randomUUID().toString());

		// WHEN
		String result = biz.register(authInfo, sysToken);

		// THEN
		then(capacityOptimizerDao).should().findFiltered(coFilterCaptor.capture());
		ConfigurationFilter coFilter = coFilterCaptor.getValue();
		assertThat("CO filter included user criteria from auth info", coFilter.getUserId(),
				is(equalTo(authInfo.id().getUserId())));
		assertThat("CO filter included conf criteria from auth info", coFilter.getConfigurationId(),
				is(equalTo(authInfo.id().getEntityId())));

		assertThat("CO reg status changed to Registered", co.getRegistrationStatus(),
				is(equalTo(RegistrationStatus.Registered)));
		assertThat("New auth token returned and different from input token", result, is(notNullValue()));

		then(capacityOptimizerDao).should().saveExternalSystemAuthToken(eq(co.getId()), tokenCaptor.capture());
		assertThat("System token saved to database", tokenCaptor.getValue(), is(equalTo(sysToken)));
	}

}
