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

import java.util.List;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import net.solarnetwork.central.common.dao.CachingGenericDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.ObjectUtils;

/**
 * Caching implementation of {@link UserMetadataDao}.
 * 
 * @author matt
 * @version 1.0
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
	 *         if any argument is {@literal null}
	 */
	public CachingUserMetadataDao(UserMetadataDao delegate, Cache<Long, UserMetadataEntity> cache,
			Executor executor, Cache<UserStringCompositePK, String> metadataPathCache) {
		super(delegate, cache, executor);
		this.metadataPathCache = ObjectUtils.requireNonNullArgument(metadataPathCache,
				"metadataPathCache");
	}

	@Override
	public FilterResults<UserMetadataEntity, Long> findFiltered(UserMetadataFilter filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public String jsonMetadataAtPath(Long userId, String path) {
		final UserStringCompositePK key = new UserStringCompositePK(userId, path);
		String result = metadataPathCache.get(key);
		if ( result == null ) {
			result = delegate.jsonMetadataAtPath(userId, path);
			if ( result != null ) {
				metadataPathCache.put(key, result);
			}
		}
		return result;
	}

}
