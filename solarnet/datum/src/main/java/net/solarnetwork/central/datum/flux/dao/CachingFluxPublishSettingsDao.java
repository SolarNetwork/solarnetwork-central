/* ==================================================================
 * CachingFluxPublishSettingsDao.java - 26/06/2024 2:28:32 pm
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

package net.solarnetwork.central.datum.flux.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import javax.cache.Cache;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.domain.UserLongStringCompositePK;

/**
 * Caching implementation of {@link FluxPublishSettingsDao}.
 *
 * @author matt
 * @version 1.0
 */
public class CachingFluxPublishSettingsDao implements FluxPublishSettingsDao {

	private final FluxPublishSettingsDao delegate;
	private final Cache<UserLongStringCompositePK, FluxPublishSettings> cache;

	/**
	 * Constructor.
	 *
	 * @param delegate
	 *        the delegate DAO
	 * @param cache
	 *        the cache
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CachingFluxPublishSettingsDao(FluxPublishSettingsDao delegate,
			Cache<UserLongStringCompositePK, FluxPublishSettings> cache) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.cache = requireNonNullArgument(cache, "cache");
	}

	@Override
	public FluxPublishSettings nodeSourcePublishConfiguration(Long userId, Long nodeId,
			String sourceId) {
		UserLongStringCompositePK key = new UserLongStringCompositePK(userId, nodeId, sourceId);
		FluxPublishSettings result = cache.get(key);
		if ( result == null ) {
			result = delegate.nodeSourcePublishConfiguration(userId, nodeId, sourceId);
			if ( result != null ) {
				cache.putIfAbsent(key, result);
			}
		}
		return result;
	}

}
