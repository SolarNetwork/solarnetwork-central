/* ==================================================================
 * GenericCompositeKey3Dao.java - 7/08/2023 6:43:30 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.central.domain.CompositeKey3;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API using a composite key of 3 components.
 * 
 * @param <T>
 *        the entity type managed by this DAO
 * @param <K>
 *        the entity composite primary key type
 * @param <K1>
 *        the primary key's first component
 * @param <K2>
 *        the primary key's second component
 * @param <K3>
 *        the primary key's third component
 * 
 * @author matt
 * @version 2.0
 */
public interface GenericCompositeKey3Dao<T extends Entity<K>, K extends Comparable<K> & Serializable & CompositeKey3<K1, K2, K3>, K1, K2, K3>
		extends GenericDao<T, K>, GenericCompositeKeyFilterableDao<T, K> {

	/**
	 * Create an entity using a specific first key component, assuming the
	 * second component is generated by the database.
	 * 
	 * @param keyComponent1
	 *        the assigned first key component
	 * @param keyComponent2
	 *        the assigned second key component
	 * @param entity
	 *        the domain object so store; the {@link Entity#getId()} of the
	 *        entity will be ignored
	 * @return the primary key of the stored object
	 */
	K create(K1 keyComponent1, K2 keyComponent2, T entity);

	/**
	 * Find all persisted entities available matching the first key component,
	 * optionally sorted in some way.
	 * 
	 * <p>
	 * The {@code sortDescriptors} parameter can be {@literal null}, in which
	 * case the sort order is not defined and implementation specific.
	 * </p>
	 * 
	 * @param keyComponent1
	 *        the first key component to restrict the results to
	 * @param keyComponent2
	 *        the second key component to restrict the results to
	 * @param sorts
	 *        list of sort descriptors to sort the results by
	 * @return list of all persisted entities, or empty list if none available
	 */
	Collection<T> findAll(K1 keyComponent1, K2 keyComponent2, List<SortDescriptor> sorts);

	@Override
	default Collection<T> findAllForKey(K filter, List<SortDescriptor> sorts) {
		return findAll(filter.keyComponent1(), filter.keyComponent2(), sorts);
	}

}
