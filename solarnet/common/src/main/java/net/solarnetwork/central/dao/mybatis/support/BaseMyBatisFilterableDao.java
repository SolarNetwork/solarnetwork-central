/* ==================================================================
 * BaseMyBatisFilterableDao.java - Nov 10, 2014 7:26:27 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base MyBatis {@link FilterableDao} implementation.
 *
 * @author matt
 * @version 1.4
 */
public abstract class BaseMyBatisFilterableDao<T extends Entity<PK>, M extends FilterMatch<PK>, F extends Filter, PK extends Serializable>
		extends BaseMyBatisGenericDao<T, PK> implements FilterableDao<M, PK, F> {

	/** A query property for a general Filter object value. */
	public static final String FILTER_PROPERTY = "filter";

	private final Class<? extends M> filterResultClass;

	/**
	 * Constructor.
	 *
	 * @param domainClass
	 *        the domain class
	 * @param pkClass
	 *        the primary key class
	 * @param filterResultClass
	 *        the filter result class
	 */
	public BaseMyBatisFilterableDao(Class<? extends T> domainClass, Class<? extends PK> pkClass,
			Class<? extends M> filterResultClass) {
		super(domainClass, pkClass);
		this.filterResultClass = filterResultClass;
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
	 * {@link #findFiltered(Filter, List, Long, Integer)}.
	 *
	 * @param filter
	 *        the current filter
	 * @param sqlProps
	 *        the properties
	 */
	protected void postProcessFilterProperties(F filter, Map<String, Object> sqlProps) {
		// nothing here, extending classes can implement
	}

	@Override
	public FilterResults<M, PK> findFiltered(F filter, List<SortDescriptor> sortDescriptors, Long offset,
			Integer max) {
		final String filterDomain = getMemberDomainKey(filterResultClass);
		final String query = getFilteredQuery(filterDomain, filter);
		Map<String, Object> sqlProps = new HashMap<>(1);
		sqlProps.put(FILTER_PROPERTY, filter);
		if ( sortDescriptors != null && !sortDescriptors.isEmpty() ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max != -1 ) {
			Long n = executeFilterCountQuery(query + "-count", filter, sqlProps);
			if ( n != null ) {
				totalCount = n;
			}
		}

		List<M> rows = selectList(query, sqlProps, offset, max);

		return new BasicFilterResults<M, PK>(rows,
				(totalCount != null ? totalCount : Long.valueOf(rows.size())),
				offset != null ? offset : 0L, rows.size());
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
