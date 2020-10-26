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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
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
public abstract class BaseMyBatisFilterableDaoSupport<T extends Entity<K>, K, M extends Identity<K>, F>
		extends BaseMyBatisGenericDaoSupport<T, K> implements FilterableDao<M, K, F> {

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
	 * Create a results instance from query results.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sqlProps
	 *        the SQL parameters
	 * @param rows
	 *        the resulting rows
	 * @param totalCount
	 *        the total count
	 * @param offset
	 *        the offset of the first result
	 * @param returnedCount
	 *        the maximum number of results
	 * @return the result instance
	 */
	protected FilterResults<M, K> createResults(F filter, Map<String, Object> sqlProps, Iterable<M> rows,
			Long totalCount, Integer offset, Integer returnedCount) {
		return new BasicFilterResults<M, K>(rows,
				(totalCount != null ? totalCount : returnedCount.longValue()), offset, returnedCount);
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * <p>
	 * The following steps are taken:
	 * </p>
	 * 
	 * <ol>
	 * <li>Create a SQL parameters map with a
	 * {@link BaseMyBatisGenericDaoSupport#FILTER_PROPERTY} key and {@code f}
	 * value.</li>
	 * <li>Call {@link #postProcessFilterProperties(Object, Map)}</li>
	 * <li>If {@code max} is not {@literal null} then call
	 * {@link #executeFilterCountQuery(String, Object, Map)}</li>
	 * <li>Call
	 * {@link net.solarnetwork.central.dao.mybatis.support.BaseMyBatisDao.selectList(String,
	 * Object, Integer, Integer)}</li>
	 * <li>Call
	 * {@link #createResults(Object, Map, Iterable, Long, Integer, Integer)} and
	 * return the result.</li>
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
	 */
	protected FilterResults<M, K> doFindFiltered(F filter, List<SortDescriptor> sorts, Integer offset,
			Integer max) {
		final String filterDomain = matchType.getSimpleName();
		final String query = getFilteredQuery(filterDomain, filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(FILTER_PROPERTY, filter);
		if ( sorts != null && sorts.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sorts);
		}
		postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			Long n = executeFilterCountQuery(query + "-count", filter, sqlProps);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<M> rows = selectList(query, sqlProps, offset, max);

		FilterResults<M, K> results = createResults(filter, sqlProps, rows,
				(totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	/**
	 * Execute a count query for a filter.
	 * 
	 * <p>
	 * If the query throws an {@link IllegalArgumentException} this method
	 * assumes that means the query name was not found, and will simply return
	 * {@literal null}.
	 * </p>
	 * 
	 * @param countQueryName
	 *        the query name
	 * @param filter
	 *        the filter
	 * @param sqlProps
	 *        the SQL properties
	 * @return the count
	 */
	protected Long executeFilterCountQuery(final String countQueryName, F filter,
			final Map<String, ?> sqlProps) {
		try {
			return selectLong(countQueryName, sqlProps);
		} catch ( RuntimeException e ) {
			Throwable cause = e;
			while ( cause.getCause() != null ) {
				cause = cause.getCause();
			}
			if ( cause instanceof IllegalArgumentException ) {
				log.warn("Count query not supported: {}", countQueryName, e);
			} else {
				throw e;
			}
		}
		return null;
	}

}
