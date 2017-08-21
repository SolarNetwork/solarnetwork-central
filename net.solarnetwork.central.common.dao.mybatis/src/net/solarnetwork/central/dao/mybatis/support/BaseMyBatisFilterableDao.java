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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Base MyBatis {@link FilterableDao} implementation.
 * 
 * @author matt
 * @version 1.1
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

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<M> findFiltered(F filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		final String filterDomain = getMemberDomainKey(filterResultClass);
		final String query = getFilteredQuery(filterDomain, filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(FILTER_PROPERTY, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		postProcessFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			Number n = selectLong(query + "-count", sqlProps);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<M> rows = selectList(query, sqlProps, offset, max);

		BasicFilterResults<M> results = new BasicFilterResults<M>(rows,
				(totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

}
