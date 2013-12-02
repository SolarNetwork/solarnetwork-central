/* ==================================================================
 * IbatisFilterableDatumDatoSupport.java - Dec 2, 2013 4:46:59 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Base class for filterable datum DAO implementations.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class IbatisFilterableDatumDatoSupport<T extends Datum, M extends FilterMatch<Long>, F extends Filter>
		extends IbatisDatumDaoSupport<T> implements FilterableDao<M, Long, F> {

	/** A query property for a general Filter object value. */
	public static final String FILTER_PROPERTY = "filter";

	private final Class<? extends M> filterResultClass;

	public IbatisFilterableDatumDatoSupport(Class<? extends T> domainClass,
			Class<? extends M> filterResultClass) {
		super(domainClass);
		this.filterResultClass = filterResultClass;
	}

	/**
	 * Append to a space-delimited string buffer.
	 * 
	 * <p>
	 * This is designed with full-text search in mind, for building up a query
	 * string.
	 * </p>
	 * 
	 * @param value
	 *        the value to append if not empty
	 * @param buf
	 *        the buffer to append to
	 * @return <em>true</em> if {@code value} was appended to {@code buf}
	 */
	protected boolean spaceAppend(String value, StringBuilder buf) {
		if ( value == null ) {
			return false;
		}
		value = value.trim();
		if ( value.length() < 1 ) {
			return false;
		}
		if ( buf.length() > 0 ) {
			buf.append(' ');
		}
		buf.append(value);
		return true;
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

	@SuppressWarnings("unchecked")
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
			final String countQuery = query + "-count";
			Number n = null;
			n = (Number) getSqlMapClientTemplate().queryForObject(countQuery, sqlProps, Number.class);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<M> rows = null;
		if ( offset != null && offset >= 0 && max != null && max > 0 ) {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps, offset, max);
		} else {
			rows = getSqlMapClientTemplate().queryForList(query, sqlProps);
		}

		rows = postProcessFilterQuery(filter, rows);

		BasicFilterResults<M> results = new BasicFilterResults<M>(rows, (totalCount != null ? totalCount
				: Long.valueOf(rows.size())), offset, rows.size());

		return results;
	}

	/**
	 * Post-process filter query results.
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
	protected List<M> postProcessFilterQuery(F filter, List<M> rows) {
		// subclasses can override
		return rows;
	}

}
