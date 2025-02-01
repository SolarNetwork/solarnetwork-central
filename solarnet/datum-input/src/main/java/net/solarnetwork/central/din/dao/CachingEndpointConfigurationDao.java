/* ==================================================================
 * CachingEndpointConfigurationDao.java - 24/02/2024 3:20:44 pm
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

package net.solarnetwork.central.din.dao;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import net.solarnetwork.central.common.dao.CachingGenericDao;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Caching proxy for {@link EndpointConfigurationDao}.
 *
 * @author matt
 * @version 1.1
 */
public class CachingEndpointConfigurationDao
		extends CachingGenericDao<EndpointConfiguration, UserUuidPK, EndpointConfigurationDao>
		implements EndpointConfigurationDao {

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
	public CachingEndpointConfigurationDao(EndpointConfigurationDao delegate,
			Cache<UserUuidPK, EndpointConfiguration> cache, Executor executor) {
		super(delegate, cache, executor);
	}

	@Override
	public FilterResults<EndpointConfiguration, UserUuidPK> findFiltered(EndpointFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public UserUuidPK create(Long keyComponent1, EndpointConfiguration entity) {
		return delegate.create(keyComponent1, entity);
	}

	@Override
	public Collection<EndpointConfiguration> findAll(Long keyComponent1, List<SortDescriptor> sorts) {
		return delegate.findAll(keyComponent1, sorts);
	}

	private static UserUuidPK endpointOnlyKey(UUID endpointId) {
		return new UserUuidPK(UserUuidPK.UNASSIGNED_USER_ID, endpointId);
	}

	@Override
	public EndpointConfiguration getForEndpointId(UUID endpointId) {
		UserUuidPK id = endpointOnlyKey(endpointId);
		EndpointConfiguration result = cache.get(id);
		if ( result == null ) {
			result = delegate.getForEndpointId(endpointId);
			if ( result != null ) {
				cache.put(id, result);
				cache.put(result.getId(), result);
			}
		}
		return result;
	}

	@Override
	public UserUuidPK save(EndpointConfiguration entity) {
		UserUuidPK result = super.save(entity);
		if ( result != null ) {
			UserUuidPK id = endpointOnlyKey(result.getUuid());
			cache.remove(id);
		}
		return result;
	}

	@Override
	public int updateEnabledStatus(Long userId, EndpointFilter filter, boolean enabled) {
		int result = delegate.updateEnabledStatus(userId, filter, enabled);
		evictKeysMatching((id) -> {
			if ( !userId.equals(id.getUserId()) ) {
				return false;
			}
			if ( filter != null && filter.hasEndpointCriteria() ) {
				for ( UUID endpointId : filter.getEndpointIds() ) {
					if ( endpointId.equals(id.getUuid()) ) {
						return true;
					}
				}
			}
			return false;
		});
		return result;
	}

}
