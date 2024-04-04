/* ==================================================================
 * CachingUserMetadataDaoTests.java - 5/04/2024 9:53:41 am
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

package net.solarnetwork.central.dao.mybatis.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.util.concurrent.Executors;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.dao.CachingUserMetadataDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;

/**
 * Test cases for the {@link CachingUserMetadataDao}.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class CachingUserMetadataDaoTests {

	@Mock
	private Cache<Long, UserMetadataEntity> entityCache;

	@Mock
	private Cache<UserStringCompositePK, String> metadataCache;

	@Mock
	private UserMetadataDao delegate;

	private CachingUserMetadataDao dao;

	@BeforeEach
	public void setup() {
		dao = new CachingUserMetadataDao(delegate, entityCache,
				Executors.newVirtualThreadPerTaskExecutor(), metadataCache);
	}

	@Test
	public void metadata_cacheMiss() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();

		final UserStringCompositePK key = new UserStringCompositePK(userId, metadataPath);
		given(metadataCache.get(key)).willReturn(null);

		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(userId, metadataPath)).willReturn(metadata);

		// WHEN
		String result = dao.jsonMetadataAtPath(userId, metadataPath);

		// THEN
		then(result).as("Result from delegate").isSameAs(metadata);
	}

	@Test
	public void metadata_cacheHit() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();

		final UserStringCompositePK key = new UserStringCompositePK(userId, metadataPath);
		final String metadata = randomString();
		given(metadataCache.get(key)).willReturn(metadata);

		// WHEN
		String result = dao.jsonMetadataAtPath(userId, metadataPath);

		// THEN
		then(result).as("Result from cache").isSameAs(metadata);
	}

}
