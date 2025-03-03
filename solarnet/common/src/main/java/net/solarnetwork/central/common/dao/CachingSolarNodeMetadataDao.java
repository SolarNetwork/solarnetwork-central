/* ==================================================================
 * CachingSolarNodeMetadataDao.java - 13/11/2024 7:39:12 am
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

package net.solarnetwork.central.common.dao;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Caching implementation of {@link SolarNodeMetadataDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class CachingSolarNodeMetadataDao
		extends CachingGenericDao<SolarNodeMetadata, Long, SolarNodeMetadataDao>
		implements SolarNodeMetadataDao {

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
	public CachingSolarNodeMetadataDao(SolarNodeMetadataDao delegate,
			Cache<Long, SolarNodeMetadata> cache, Executor executor) {
		super(delegate, cache, executor);
	}

	@Override
	public FilterResults<SolarNodeMetadata, Long> findFiltered(SolarNodeMetadataFilter filter,
			List<SortDescriptor> sorts, Long offset, Integer max) {
		if ( filter.hasNodeCriteria() && filter.getNodeIds().length == 1 ) {
			// use cache when looking for single node ID
			SolarNodeMetadata meta = get(filter.getNodeId());
			return new BasicFilterResults<>(meta != null ? List.of(meta) : Collections.emptyList());
		}
		return delegate.findFiltered(filter, sorts, offset, max);
	}

}
