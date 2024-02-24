/* ==================================================================
 * CachingTransformConfigurationDao.java - 24/02/2024 4:00:50 pm
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
import java.util.concurrent.Executor;
import javax.cache.Cache;
import net.solarnetwork.central.common.dao.CachingGenericDao;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Caching proxy for {@link TransformConfigurationDao}.
 *
 * @author matt
 * @version 1.0
 */
public class CachingTransformConfigurationDao
		extends CachingGenericDao<TransformConfiguration, UserLongCompositePK, TransformConfigurationDao>
		implements TransformConfigurationDao {

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
	public CachingTransformConfigurationDao(TransformConfigurationDao delegate,
			Cache<UserLongCompositePK, TransformConfiguration> cache, Executor executor) {
		super(delegate, cache, executor);
	}

	@Override
	public FilterResults<TransformConfiguration, UserLongCompositePK> findFiltered(
			TransformFilter filter, List<SortDescriptor> sorts, Integer offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public UserLongCompositePK create(Long keyComponent1, TransformConfiguration entity) {
		return delegate.create(keyComponent1, entity);
	}

	@Override
	public Collection<TransformConfiguration> findAll(Long keyComponent1, List<SortDescriptor> sorts) {
		return delegate.findAll(keyComponent1, sorts);
	}

}
