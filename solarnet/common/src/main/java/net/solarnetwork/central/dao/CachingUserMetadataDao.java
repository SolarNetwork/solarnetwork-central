/* ==================================================================
 * CachingUserMetadataDao.java - 5/04/2024 9:17:12 am
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

package net.solarnetwork.central.dao;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.CachingGenericDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Caching implementation of {@link UserMetadataDao}.
 *
 * @author matt
 * @version 2.0
 */
public class CachingUserMetadataDao extends CachingGenericDao<UserMetadataEntity, Long, UserMetadataDao>
		implements UserMetadataDao {

	private final Cache<UserStringCompositePK, String> metadataPathCache;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate DAO
	 * @param cache
	 *        the cache
	 * @param executor
	 *        task executor
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CachingUserMetadataDao(UserMetadataDao delegate, Cache<Long, UserMetadataEntity> cache,
			Executor executor, Cache<UserStringCompositePK, String> metadataPathCache) {
		super(delegate, cache, executor);
		this.metadataPathCache = requireNonNullArgument(metadataPathCache, "metadataPathCache");
	}

	@Override
	public FilterResults<UserMetadataEntity, Long> findFiltered(UserMetadataFilter filter,
			@Nullable List<SortDescriptor> sorts, @Nullable Long offset, @Nullable Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public @Nullable String jsonMetadataAtPath(UserMetadataFilter filter, String path) {
		final Long userId = requireNonNullArgument(requireNonNullArgument(filter, "filter").getUserId(),
				"filter.userId");
		final UserStringCompositePK key = cacheKey(userId, filter, path);

		String result = null;
		if ( key != null ) {
			result = metadataPathCache.get(key);
		}
		if ( result == null ) {
			result = delegate.jsonMetadataAtPath(filter, path);
			if ( result != null ) {
				metadataPathCache.put(key, result);
			}
		}

		return result;
	}

	/**
	 * Test if we can use the cache for a given filter.
	 * 
	 * <p>
	 * Will return the cache key to use when looking for single user ID, without
	 * any metadata search filter. If the current actor has a
	 * {@code userMetadataPaths} constraint in their security policy, those
	 * paths are included in the returned key.
	 * </p>
	 * 
	 * <p>
	 * The returned key is a hex-encoded MD5 digest of the given path
	 * concatenated with a sorted list of any {@code userMetadataPaths} security
	 * policy constraints. All strings are treated as {@code UTF-8}.
	 * </p>
	 * 
	 * 
	 * @param userId
	 *        the user ID
	 * @param filter
	 *        the filter to test
	 * @return a cache key to use, or {@code null} if caching should not be used
	 * @since 2.0
	 */
	private static @Nullable UserStringCompositePK cacheKey(Long userId, UserMetadataFilter filter,
			String path) {
		if ( !filter.hasSearchFilterCriteria() ) {
			var digest = DigestUtils.getMd5Digest();
			digest.update(path.getBytes(UTF_8));
			SecurityPolicy policy = SecurityUtils.getActiveSecurityPolicy();
			if ( policy != null && policy.getUserMetadataPaths() != null ) {
				String[] policyPaths = policy.getUserMetadataPaths().toArray(String[]::new);
				Arrays.sort(policyPaths);
				for ( String policyPath : policyPaths ) {
					digest.update(policyPath.getBytes(UTF_8));
				}
			}
			return new UserStringCompositePK(userId, HexFormat.of().formatHex(digest.digest()));
		}
		return null;
	}

}
