/* ==================================================================
 * GenericCompositeKeyFilterableDao.java - 25/02/2024 4:56:13 pm
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

import java.util.Collection;
import java.util.List;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for a DAO that can filter by composite key components.
 * 
 * @author matt
 * @version 1.0
 */
public interface GenericCompositeKeyFilterableDao<T extends Entity<K>, K extends CompositeKey>
		extends GenericDao<T, K> {

	/**
	 * Find all persisted entities available matching the components of a
	 * composite key, optionally sorted in some way.
	 * 
	 * <p>
	 * Only key components that are assigned are used to filter the result. The
	 * {@code sortDescriptors} parameter can be {@literal null}, in which case
	 * the sort order is not defined and implementation specific.
	 * </p>
	 * 
	 * @param filter
	 *        the key that acts like a filter to restrict the results to
	 * @param sorts
	 *        list of sort descriptors to sort the results by
	 * @return list of all persisted entities, or empty list if none available
	 */
	Collection<T> findAllForKey(K filter, List<SortDescriptor> sorts);

}
