/* ==================================================================
 * DrasIbatisFilterableDaoSupport.java - Jun 8, 2011 6:44:15 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.Entity;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * Abstract base Ibatis FilterableDao for DRAS support.
 * 
 * @author matt
 * @version $Revision$
 */
public abstract class DrasIbatisFilterableDaoSupport<T 
extends Entity<Long>, M extends FilterMatch<Long>, F extends Filter> 
extends DrasIbatisGenericDaoSupport<T> implements FilterableDao<M, Long, F> {

	/** A query property for a general Filter object value. */
	public static final String FILTER_PROPERTY = "filter";
	
	private final Class<? extends M> filterResultClass;
	
	/**
	 * Constructor.
	 * 
	 * @param domainClass the domain class
	 */
	public DrasIbatisFilterableDaoSupport(Class<? extends T> domainClass, 
			Class<? extends M> filterResultClass) {
		super(domainClass);
		this.filterResultClass = filterResultClass;
	}
	
	/**
	 * Append to a space-delimited string buffer.
	 * 
	 * <p>This is designed with full-text search in mind, for building
	 * up a query string.</p>
	 * 
	 * @param value the value to append if not empty
	 * @param buf the buffer to append to
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
	 * @param filterDomain the domain
	 * @return query name
	 */
	protected String getFilteredQuery(String filterDomain, F filter) {
		return getQueryForAll()+"-"+filterDomain;
	}
	
	/**
	 * Callback to alter the default SQL properties set up by 
	 * {@link #findFiltered(Filter, List, Integer, Integer)}.
	 * 
	 * @param filter the current filter
	 * @param sqlProps the properties
	 */
	protected void postProcessFilterProperties(F filter, Map<String, Object> sqlProps) {
		// nothing here, extending classes can implement
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public FilterResults<M> findFiltered(F filter, List<SortDescriptor> sortDescriptors, 
			Integer offset, Integer max) {
		final String filterDomain = getMemberDomainKey(filterResultClass);
		final String query = getFilteredQuery(filterDomain, filter);
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(FILTER_PROPERTY, filter);
		if ( sortDescriptors != null && sortDescriptors.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sortDescriptors);
		}
		postProcessFilterProperties(filter, sqlProps);
		List<M> results = null;
		if ( offset != null && offset >= 0 && max != null && max > 0 ) {
			results = getSqlMapClientTemplate().queryForList(query, sqlProps, offset, max);
		} else {
			results = getSqlMapClientTemplate().queryForList(query, sqlProps);
		}
		
		return new BasicFilterResults<M>(results, 
				Long.valueOf(results.size()),
				offset, results.size());
	}
	
}
