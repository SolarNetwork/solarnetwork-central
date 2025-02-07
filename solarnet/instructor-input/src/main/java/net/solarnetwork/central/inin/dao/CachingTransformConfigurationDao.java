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

package net.solarnetwork.central.inin.dao;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import net.solarnetwork.central.common.dao.CachingGenericDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Caching proxy for {@link TransformConfigurationDao}.
 *
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.1
 */
public class CachingTransformConfigurationDao<C extends TransformConfiguration<C>>
		extends CachingGenericDao<C, UserLongCompositePK, TransformConfigurationDao<C>>
		implements TransformConfigurationDao<C> {

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
	public CachingTransformConfigurationDao(TransformConfigurationDao<C> delegate,
			Cache<UserLongCompositePK, C> cache, Executor executor) {
		super(delegate, cache, executor);
	}

	@Override
	public FilterResults<C, UserLongCompositePK> findFiltered(TransformFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		return delegate.findFiltered(filter, sorts, offset, max);
	}

	@Override
	public UserLongCompositePK create(Long keyComponent1, C entity) {
		return delegate.create(keyComponent1, entity);
	}

	@Override
	public Collection<C> findAll(Long keyComponent1, List<SortDescriptor> sorts) {
		return delegate.findAll(keyComponent1, sorts);
	}

}
