/* ==================================================================
 * SearchFilterCriteria.java - 11/12/2020 5:13:18 pm
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

package net.solarnetwork.central.datum.v2.dao;

import net.solarnetwork.support.SearchFilter;

/**
 * Search criteria using a search filter.
 * 
 * <p>
 * See {@link net.solarnetwork.support.SearchFilter} for an LDAP-inspired search
 * filter style.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface SearchFilterCriteria {

	/**
	 * Get the search filter.
	 * 
	 * <p>
	 * The actual syntax of this search filter is not defined by this API, and
	 * is implementation and context specific.
	 * </p>
	 * 
	 * @return the search filter
	 */
	String getSearchFilter();

	/**
	 * Test if a search filter value is present.
	 * 
	 * @return {@literal true} if the search filter is not empty
	 */
	default boolean hasSearchFilterCriteria() {
		return (getSearchFilter() != null && !getSearchFilter().isEmpty());
	}

	/**
	 * Parse the search filter string into a {@link SearchFilter} instance.
	 * 
	 * @return the instance, or {@literal null} if no search filter is defined
	 *         or the filter has an invalid syntax
	 */
	default SearchFilter searchFilter() {
		return SearchFilter.forLDAPSearchFilterString(getSearchFilter());
	}

}
