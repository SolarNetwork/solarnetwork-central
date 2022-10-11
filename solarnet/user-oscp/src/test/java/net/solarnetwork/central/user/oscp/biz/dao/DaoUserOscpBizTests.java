/* ==================================================================
 * DaoUserOscpBizTests.java - 15/08/2022 10:41:21 am
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

package net.solarnetwork.central.user.oscp.biz.dao;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupSettingsDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.dao.UserSettingsDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.user.oscp.domain.CapacityProviderConfigurationInput;

/**
 * Test cases for the {@link DaoUserOscpBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoUserOscpBizTests {

	@Mock
	private UserSettingsDao userSettingsDao;

	@Mock
	private FlexibilityProviderDao flexibilityProviderDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Mock
	private CapacityGroupSettingsDao capacityGroupSettingsDao;

	@Mock
	private AssetConfigurationDao assetDao;

	@Captor
	private ArgumentCaptor<CapacityProviderConfiguration> cpConfCaptor;

	private DaoUserOscpBiz biz;

	@BeforeEach
	public void setup() {
		biz = new DaoUserOscpBiz(userSettingsDao, flexibilityProviderDao, capacityProviderDao,
				capacityOptimizerDao, capacityGroupDao, capacityGroupSettingsDao, assetDao);
	}

	@Test
	public void listCapacityProviders() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();

		final List<CapacityProviderConfiguration> confs = new ArrayList<>();
		given(capacityProviderDao.findAll(userId, null)).willReturn(confs);

		// WHEN
		Collection<CapacityProviderConfiguration> results = biz.capacityProvidersForUser(userId);

		// THEN
		assertThat("DAO results returned", results, is(sameInstance(confs)));
	}

	@Test
	public void listCapacityOptimizers() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();

		final List<CapacityOptimizerConfiguration> confs = new ArrayList<>();
		given(capacityOptimizerDao.findAll(userId, null)).willReturn(confs);

		// WHEN
		Collection<CapacityOptimizerConfiguration> results = biz.capacityOptimizersForUser(userId);

		// THEN
		assertThat("DAO results returned", results, is(sameInstance(confs)));
	}

	@Test
	public void listCapacityGroups() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();

		final List<CapacityGroupConfiguration> confs = new ArrayList<>();
		given(capacityGroupDao.findAll(userId, null)).willReturn(confs);

		// WHEN
		Collection<CapacityGroupConfiguration> results = biz.capacityGroupsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, is(sameInstance(confs)));
	}

	@Test
	public void listAssets() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();

		final List<AssetConfiguration> confs = new ArrayList<>();
		given(assetDao.findAll(userId, null)).willReturn(confs);

		// WHEN
		Collection<AssetConfiguration> results = biz.assetsForUser(userId);

		// THEN
		assertThat("DAO results returned", results, is(sameInstance(confs)));
	}

	@Test
	public void createCapacityProvider() {
		// GIVEN
		final Long userId = randomUUID().getMostSignificantBits();

		// create new auth token
		String newToken = randomUUID().toString();
		given(flexibilityProviderDao.createAuthToken(UserLongCompositePK.unassignedEntityIdKey(userId)))
				.willReturn(newToken);
		UserLongCompositePK authId = new UserLongCompositePK(userId,
				randomUUID().getMostSignificantBits());
		given(flexibilityProviderDao.idForToken(newToken, false)).willReturn(authId);

		final CapacityProviderConfiguration entity = new CapacityProviderConfiguration(userId,
				randomUUID().getMostSignificantBits(), Instant.now());
		given(capacityProviderDao.create(eq(userId), any())).willReturn(entity.getId());
		given(capacityProviderDao.get(entity.getId())).willReturn(entity);

		// WHEN
		final CapacityProviderConfigurationInput input = new CapacityProviderConfigurationInput();
		input.setBaseUrl(URI.create("http://example.com/oscp/cp/2.0"));
		input.setEnabled(true);
		input.setName("Test CP");
		input.setRegistrationStatus(RegistrationStatus.Pending);

		CapacityProviderConfiguration result = biz.createCapacityProvider(userId, input);

		// THEN
		assertThat("Conf returned from DAO", result, is(sameInstance(entity)));
		assertThat("Token returned", result.getToken(), is(equalTo(newToken)));

		then(capacityProviderDao).should().create(eq(userId), cpConfCaptor.capture());
		CapacityProviderConfiguration saved = cpConfCaptor.getValue();
		assertThat("Saved FP ID", saved.getFlexibilityProviderId(), is(equalTo(authId.getEntityId())));
	}

}
