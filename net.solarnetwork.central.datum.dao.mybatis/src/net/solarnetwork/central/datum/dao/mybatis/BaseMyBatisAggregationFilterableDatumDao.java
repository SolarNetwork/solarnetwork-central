/* ==================================================================
 * BaseMyBatisAggregationFilterableDatumDao.java - Nov 13, 2014 6:22:32 AM
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

package net.solarnetwork.central.datum.dao.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.AggregationFilterableDao;
import net.solarnetwork.central.dao.mybatis.support.SqlSessionCallback;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.AggregationFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for MyBatis {@link AggregationFilterableDao} implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseMyBatisAggregationFilterableDatumDao<T extends Datum, M extends FilterMatch<Long>, F extends Filter, AM, AF extends AggregationFilter>
		extends BaseMyBatisFilterableDatumDao<T, M, F> implements AggregationFilterableDao<AM, AF> {

	private final Class<? extends AM> aggregationFilterResultClass;

	/**
	 * Constructor.
	 * 
	 * @param domainClass
	 *        the domain class
	 * @param filterResultClass
	 *        the filter results class
	 * @param aggregationFilterResultClass
	 *        the aggregation filter result class
	 */
	public BaseMyBatisAggregationFilterableDatumDao(Class<? extends T> domainClass,
			Class<? extends M> filterResultClass, Class<? extends AM> aggregationFilterResultClass) {
		super(domainClass, filterResultClass);
		this.aggregationFilterResultClass = aggregationFilterResultClass;
	}

	/**
	 * Get the aggregation filter query name for a given domain.
	 * 
	 * @param filterDomain
	 *        the domain
	 * @return query name
	 */
	protected String getAggregationFilteredQuery(String filterDomain, AF filter) {
		Aggregation aggregation = filter.getAggregation();
		if ( aggregation == null ) {
			aggregation = Aggregation.Day;
		}
		return getQueryForAll() + "-" + filterDomain + "-" + aggregation.toString();
	}

	/**
	 * Callback to alter the default SQL properties set up by
	 * {@link #findAggregationFiltered(AggregationFilter, List, Integer, Integer)}
	 * .
	 * 
	 * @param filter
	 *        the current filter
	 * @param sqlProps
	 *        the properties
	 */
	protected void postProcessAggregationFilterProperties(AF filter, Map<String, Object> sqlProps) {
		// nothing here, extending classes can implement
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public FilterResults<AM> findAggregationFiltered(final AF filter,
			final List<SortDescriptor> sortDescriptors, final Integer offset, final Integer max) {
		final String filterDomain = getMemberDomainKey(aggregationFilterResultClass);
		final String query = getAggregationFilteredQuery(filterDomain, filter);
		final Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(FILTER_PROPERTY, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		postProcessAggregationFilterProperties(filter, sqlProps);

		// attempt count first, if max NOT specified as -1
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 ) {
			final String countQuery = query + "-count";
			Number n = execute(new SqlSessionCallback<Number>() {

				@Override
				public Number doWithSqlSession(SqlSession session) {
					return session.selectOne(countQuery, sqlProps);
				}
			}, filter);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<AM> rows = execute(new SqlSessionCallback<List<AM>>() {

			@Override
			public List<AM> doWithSqlSession(SqlSession session) {
				List<AM> rows = null;
				if ( offset != null && offset >= 0 && max != null && max > 0 ) {
					rows = session.selectList(query, sqlProps, new RowBounds(offset, max));
				} else {
					rows = session.selectList(query, sqlProps);
				}
				return rows;
			}
		}, filter);

		rows = postProcessAggregationFilterQuery(filter, rows);

		BasicFilterResults<AM> results = new BasicFilterResults<AM>(rows,
				(totalCount != null ? totalCount : Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	/**
	 * Post-process aggregation filter query results.
	 * 
	 * <p>
	 * This implementation simply returns the passed on {@code rows}; subclasses
	 * can override this to process the filter results as needed.
	 * </p>
	 * 
	 * @param filter
	 *        the query filter
	 * @param rows
	 *        the result rows (never <em>null</em>)
	 * @return the processed rows
	 */
	protected List<AM> postProcessAggregationFilterQuery(AF filter, List<AM> rows) {
		// subclasses can override
		return rows;
	}

}
