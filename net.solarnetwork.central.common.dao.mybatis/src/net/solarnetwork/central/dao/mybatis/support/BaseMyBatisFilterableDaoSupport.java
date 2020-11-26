/* ==================================================================
 * BaseMyBatisFilterableDaoSupport.java - 26/10/2020 1:04:30 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis.support;

import java.util.List;
import java.util.Map;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base implementation of {@link FilterableDao} using MyBatis via
 * {@link SqlSessionDaoSupport}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.7
 */
public abstract class BaseMyBatisFilterableDaoSupport<T extends Entity<K>, K, M extends Identity<K>, F extends PaginationCriteria>
		extends BaseMyBatisGenericDaoSupport<T, K>
		implements FilterableDao<M, K, F>, FilterResultsFactory<M, K, F> {

	private final Class<? extends M> matchType;

	/**
	 * Constructor.
	 * 
	 * @param objectType
	 *        the object type
	 * @param keyType
	 *        the key type
	 * @param matchType
	 *        the match type
	 */
	public BaseMyBatisFilterableDaoSupport(Class<? extends T> objectType, Class<? extends K> keyType,
			Class<? extends M> matchType) {
		super(objectType, keyType);
		this.matchType = matchType;
	}

	/**
	 * Get the filter query name for a given domain.
	 * 
	 * @param filterDomain
	 *        the domain
	 * @param filter
	 *        the filter
	 * @return query name
	 */
	protected String getFilteredQuery(String filterDomain, F filter) {
		return getQueryForAll() + "-" + filterDomain;
	}

	/**
	 * Callback to alter the default SQL properties set up by
	 * {@link #findFiltered(Filter, List, Integer, Integer)}.
	 * 
	 * @param filter
	 *        the current filter
	 * @param sqlProps
	 *        the properties
	 */
	protected void postProcessFilterProperties(F filter, Map<String, Object> sqlProps) {
		// nothing here, extending classes can implement
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * <p>
	 * The following steps are taken:
	 * </p>
	 * 
	 * <ol>
	 * <li>Compute the MyBatis query name by calling
	 * {@link #getFilteredQuery(String, Object)}.</li>
	 * <li>Call
	 * {@link BaseMyBatisDao#selectFiltered(String, Object, List, Integer, Integer, java.util.function.BiConsumer)}.</li>
	 * </ol>
	 * 
	 * @param filter
	 *        the search filter
	 * @param sorts
	 *        the sort descriptors
	 * @param offset
	 *        the starting result offset
	 * @param max
	 *        the maximum number of results
	 * @return the results
	 * @see BaseMyBatisDao#selectFiltered(String, Object, List, Integer,
	 *      Integer, java.util.function.BiConsumer)
	 */
	protected FilterResults<M, K> doFindFiltered(F filter, List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		final String filterDomain = matchType.getSimpleName();
		final String query = getFilteredQuery(filterDomain, filter);
		return selectFiltered(query, filter, sorts, offset, max, this::postProcessFilterProperties,
				this);
	}

	@Override
	public FilterResults<M, K> createFilterResults(F filter, Map<String, Object> sqlProps,
			Iterable<M> rows, Long totalCount, Integer offset, Integer returnedCount) {
		return BasicFilterResults.filterResults(rows, filter, totalCount,
				(returnedCount != null ? returnedCount : 0));
	}

}
