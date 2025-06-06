/* ==================================================================
 * ObjectSearchFilters.java - Aug 8, 2010 8:15:35 PM
 *
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.domain.Filter;

/**
 * Collection of object search filters.
 *
 * @param <T>
 *        the object to filter on
 * @author matt
 * @version 1.1
 */
public class ObjectSearchFilters<T extends Filter> implements Cloneable, ObjectCriteria<T> {

	private JoinType joinType;
	private List<ObjectSearchFilter<T>> filters = new ArrayList<>(2);
	private List<ObjectSearchFilters<T>> nestedFilters = new ArrayList<>(2);
	private Integer resultOffset;
	private Integer resultMax;

	/**
	 * Construct a new ObjectSearchFilters object with a join type.
	 *
	 * @param joinType
	 *        the logical join type
	 */
	public ObjectSearchFilters(JoinType joinType) {
		this.joinType = joinType;
	}

	/**
	 * Construct a search filters object out of a single filter, using
	 * {@link JoinType#AND} and {@link MatchType#EQUAL}.
	 *
	 * @param filter
	 *        the object to filter on
	 */
	public ObjectSearchFilters(T filter) {
		this(JoinType.AND, filter);
	}

	/**
	 * Construct a search filters object out of a single filter, using
	 * {@link MatchType#EQUAL}.
	 *
	 * @param joinType
	 *        the logical join type, used for nested join and the given filter
	 * @param filter
	 *        the object to filter on
	 */
	public ObjectSearchFilters(JoinType joinType, T filter) {
		this(joinType, filter, joinType);
	}

	/**
	 * Construct a search filters object out of a single filter, using
	 * {@link MatchType#EQUAL}.
	 *
	 * @param joinType
	 *        the logical join type
	 * @param filter
	 *        the object to filter on
	 * @param filterJoinType
	 *        the filter object join type
	 */
	public ObjectSearchFilters(JoinType joinType, T filter, JoinType filterJoinType) {
		this.joinType = joinType;
		filters.add(new ObjectSearchFilter<>(filter, MatchType.EQUAL, filterJoinType));
	}

	/**
	 * Add a new nested ObjectSearchFilters object to this one.
	 *
	 * <p>
	 * This allows for creating complex logical filters. See the class
	 * description for an example of this.
	 * </p>
	 *
	 * @param nestedJoinType
	 *        the logical join type
	 * @return the new nested ObjectSearchFilters object
	 */
	public ObjectSearchFilters<T> addNestedFilters(JoinType nestedJoinType) {
		ObjectSearchFilters<T> sf = new ObjectSearchFilters<>(nestedJoinType);
		nestedFilters.add(sf);
		return sf;
	}

	/**
	 * Add a new ObjectSearchFilter to this object.
	 *
	 * @param filter
	 *        the filter
	 */
	public void addObjectSearchFilter(ObjectSearchFilter<T> filter) {
		filters.add(filter);
	}

	/**
	 * Add a new ObjectSearchFilter to this object.
	 *
	 * @param filter
	 *        the filter to add
	 * @param mode
	 *        the search filter mode
	 * @param joinType
	 *        the join type
	 */
	public void addObjectSearchFilter(T filter, MatchType mode, JoinType joinType) {
		ObjectSearchFilter<T> osf = new ObjectSearchFilter<>(filter, mode, joinType);
		filters.add(osf);
	}

	/**
	 * Generate a complete search filter string of this object into a
	 * StringBuffer.
	 *
	 * @param buf
	 *        buffer to append to
	 */
	public void appendLdapSearchFilter(StringBuilder buf) {
		buf.append("(");
		if ( filters == null && nestedFilters == null ) {
			buf.append("objectClass=*");
		} else if ( filters != null ) {
			buf.append(joinType);
		}
		if ( filters != null ) {
			// for each search filter, append value
			for ( ObjectSearchFilter<T> filter : filters ) {
				filter.appendLdapSearchFilter(buf);
			}
		}
		if ( nestedFilters != null ) {
			// for each nested search filter, append value
			for ( ObjectSearchFilters<T> oneNestedFilter : nestedFilters ) {
				oneNestedFilter.appendLdapSearchFilter(buf);
			}
		}
		buf.append(")");
	}

	/**
	 * Generate a complete LDAP search filter string of this object.
	 *
	 * @return String
	 */
	public String asLdapSearchFilterString() {
		StringBuilder buf = new StringBuilder();
		appendLdapSearchFilter(buf);
		return buf.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ObjectSearchFilters<T> clone() {
		try {
			ObjectSearchFilters<T> clone = (ObjectSearchFilters<T>) super.clone();
			clone.filters = new ArrayList<>(filters.size());
			for ( ObjectSearchFilter<T> aFilter : filters ) {
				clone.filters.add(aFilter.clone());
			}
			clone.nestedFilters = new ArrayList<>(filters.size());
			for ( ObjectSearchFilters<T> aFilter : nestedFilters ) {
				clone.nestedFilters.add(aFilter.clone());
			}
			return clone;
		} catch ( CloneNotSupportedException e ) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return an LDAP search filter string.
	 *
	 * <p>
	 * This simply calls {@link #asLdapSearchFilterString()}.
	 * </p>
	 *
	 * @return String
	 */
	@Override
	public String toString() {
		return asLdapSearchFilterString();
	}

	private ObjectSearchFilter<T> getSimpleSearchFilter() {
		if ( filters == null || filters.isEmpty() ) {
			return null;
		}
		return filters.getFirst();
	}

	@Override
	public T getSimpleFilter() {
		ObjectSearchFilter<T> filter = getSimpleSearchFilter();
		if ( filter == null ) {
			return null;
		}
		return filter.getFilter();
	}

	@Override
	public ObjectCriteria.JoinType getSimpleJoinType() {
		ObjectSearchFilter<T> filter = getSimpleSearchFilter();
		if ( filter == null ) {
			return null;
		}
		return filter.getJoinType();
	}

	@Override
	public ObjectCriteria.MatchType getSimpleMatchType() {
		ObjectSearchFilter<T> filter = getSimpleSearchFilter();
		if ( filter == null ) {
			return null;
		}
		return filter.getMode();
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public List<ObjectSearchFilters<T>> getNestedFilters() {
		return nestedFilters;
	}

	public void setFilters(List<ObjectSearchFilter<T>> newFilters) {
		filters = newFilters;
	}

	public void setJoinType(JoinType newJoinType) {
		joinType = newJoinType;
	}

	public void setNestedFilters(List<ObjectSearchFilters<T>> newNestedLdapSearchFilters) {
		nestedFilters = newNestedLdapSearchFilters;
	}

	@Override
	public Integer getResultOffset() {
		return resultOffset;
	}

	public void setResultOffset(Integer resultOffset) {
		this.resultOffset = resultOffset;
	}

	@Override
	public Integer getResultMax() {
		return resultMax;
	}

	public void setResultMax(Integer resultMax) {
		this.resultMax = resultMax;
	}

}
