/* ==================================================================
 * CachingFluxPublishSettingsDaoTests.java - 26/06/2024 2:35:12 pm
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

package net.solarnetwork.central.datum.flux.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.flux.dao.CachingFluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.dao.FluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettingsInfo;
import net.solarnetwork.central.domain.UserLongStringCompositePK;

/**
 * Test cases for the {@link CachingFluxPublishSettingsDao} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CachingFluxPublishSettingsDaoTests {

	@Mock
	private FluxPublishSettingsDao delegate;

	@Mock
	private Cache<UserLongStringCompositePK, FluxPublishSettings> cache;

	private CachingFluxPublishSettingsDao dao;

	@BeforeEach
	public void setup() {
		dao = new CachingFluxPublishSettingsDao(delegate, cache);
	}

	@Test
	public void cacheMiss() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		// cache miss
		final UserLongStringCompositePK pk = new UserLongStringCompositePK(userId, nodeId, sourceId);
		given(cache.get(pk)).willReturn(null);

		var settings = new FluxPublishSettingsInfo(false, false);
		given(delegate.nodeSourcePublishConfiguration(userId, nodeId, sourceId)).willReturn(settings);

		given(cache.putIfAbsent(pk, settings)).willReturn(true);

		// WHEN
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		and.then(result).as("DAO result returned").isSameAs(settings);
	}

	@Test
	public void cacheHit() {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		// cache miss
		final UserLongStringCompositePK pk = new UserLongStringCompositePK(userId, nodeId, sourceId);
		var settings = new FluxPublishSettingsInfo(false, false);
		given(cache.get(pk)).willReturn(settings);

		// WHEN
		FluxPublishSettings result = dao.nodeSourcePublishConfiguration(userId, nodeId, sourceId);

		// THEN
		then(cache).shouldHaveNoMoreInteractions();
		then(delegate).shouldHaveNoInteractions();
		and.then(result).as("Cache result returned").isSameAs(settings);
	}

}
