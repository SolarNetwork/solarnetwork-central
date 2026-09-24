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

package net.solarnetwork.central.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.BDDAssertions.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import java.util.concurrent.Executors;
import javax.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.dao.CachingUserMetadataDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.domain.UserStringCompositePK;

/**
 * Test cases for the {@link CachingUserMetadataDao}.
 * 
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CachingUserMetadataDaoTests {

	@Mock
	private Cache<Long, UserMetadataEntity> entityCache;

	@Mock
	private Cache<UserStringCompositePK, String> metadataCache;

	@Mock
	private UserMetadataDao delegate;

	@Captor
	public ArgumentCaptor<UserMetadataFilter> filterCaptor;

	@Captor
	public ArgumentCaptor<UserStringCompositePK> keyCaptor;

	private CachingUserMetadataDao dao;

	@BeforeEach
	public void setup() {
		dao = new CachingUserMetadataDao(delegate, entityCache,
				Executors.newVirtualThreadPerTaskExecutor(), metadataCache);
	}



	/**
	 * The metadata returned by the delegate is restricted to the token policy's
	 * paths, so the cache key must vary with the token or one token would serve
	 * another token's view.
	 */
	@Test
	public void metadata_cacheKey_includesTokenId() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();
		final String tokenId = randomString(20);

		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenId(tokenId);
		String result = dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		// the digest is over the path plus the filter token ID
		final UserStringCompositePK expectedKey = new UserStringCompositePK(userId,
				md5Hex((metadataPath + tokenId).getBytes(UTF_8)));

		// @formatter:off
		then(metadataCache).should().get(expectedKey);
		then(metadataCache).should().put(expectedKey, metadata);

		and.then(result)
			.as("Result from delegate")
			.isSameAs(metadata)
			;
		// @formatter:on
	}

	@Test
	public void metadata_cacheKey_differsByToken() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();
		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);

		// WHEN
		filter.setTokenId(randomString(20));
		dao.jsonMetadataAtPath(filter, metadataPath);

		filter.setTokenId(randomString(20));
		dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		// @formatter:off
		then(metadataCache).should(times(2)).get(keyCaptor.capture());

		and.then(keyCaptor.getAllValues())
			.as("A different token produces a different cache key, so one token"
					+ " cannot serve another token's restricted view")
			.doesNotHaveDuplicates()
			;
		// @formatter:on
	}

	@Test
	public void metadata_cacheKey_differsWithAndWithoutToken() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();
		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);

		// WHEN
		dao.jsonMetadataAtPath(filter, metadataPath);

		filter.setTokenId(randomString(20));
		dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		// @formatter:off
		then(metadataCache).should(times(2)).get(keyCaptor.capture());

		and.then(keyCaptor.getAllValues())
			.as("The unrestricted view is cached apart from any token's restricted view")
			.doesNotHaveDuplicates()
			;
		// @formatter:on
	}

	@Test
	public void metadata_cacheKey_sameForSameToken() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();
		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setTokenId(randomString(20));

		// WHEN
		dao.jsonMetadataAtPath(filter, metadataPath);
		dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		// @formatter:off
		then(metadataCache).should(times(2)).get(keyCaptor.capture());

		and.then(keyCaptor.getAllValues())
			.as("The same token reuses one cache key")
			.containsExactly(keyCaptor.getAllValues().getFirst(), keyCaptor.getAllValues().getFirst())
			;
		// @formatter:on
	}

	/**
	 * A metadata search filter makes the result depend on more than the path,
	 * so the cache must not be used at all.
	 */
	@Test
	public void metadata_searchFilter_bypassesCache() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();
		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		filter.setSearchFilter("(/m/foo=bar)");

		// WHEN
		String result = dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		// @formatter:off
		then(metadataCache).shouldHaveNoInteractions();

		and.then(result)
			.as("Result from delegate")
			.isSameAs(metadata)
			;
		// @formatter:on
	}

	@Test
	public void metadata_cacheMiss() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();

		final UserStringCompositePK key = new UserStringCompositePK(userId, md5Hex(metadataPath));
		given(metadataCache.get(key)).willReturn(null);

		final String metadata = randomString();
		given(delegate.jsonMetadataAtPath(any(), eq(metadataPath))).willReturn(metadata);

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		then(delegate).should().jsonMetadataAtPath(filterCaptor.capture(), eq(metadataPath));
		and.then(filterCaptor.getValue()).as("Filter passed to delegate").isSameAs(filter);
		and.then(result).as("Result from delegate").isSameAs(metadata);
	}

	@Test
	public void metadata_cacheHit() {
		// GIVEN
		final Long userId = randomLong();
		final String metadataPath = randomString();

		final UserStringCompositePK key = new UserStringCompositePK(userId, md5Hex(metadataPath));
		final String metadata = randomString();
		given(metadataCache.get(key)).willReturn(metadata);

		// WHEN
		final BasicUserMetadataFilter filter = new BasicUserMetadataFilter();
		filter.setUserId(userId);
		String result = dao.jsonMetadataAtPath(filter, metadataPath);

		// THEN
		then(delegate).shouldHaveNoInteractions();
		and.then(result).as("Result from cache").isSameAs(metadata);
	}

}
