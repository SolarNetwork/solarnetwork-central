/* ==================================================================
 * CommonFilter.java - 22/04/2018 9:08:34 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.central.domain.OptimizedQueryFilter;

/**
 * API for common filter properties.
 * 
 * @author matt
 * @version 1.3
 * @since 1.22
 */
public interface CommonFilter extends Filter, DateRangeFilter, LocalDateRangeFilter, SourceFilter,
		OptimizedQueryFilter, MostRecentFilter {

	/**
	 * Get a bean object path to a specific data value key to extract and return
	 * from the results, instead of all data. For example a path like
	 * {@code i.watts} might return a power value.
	 * 
	 * @return bean object path to extract
	 */
	String getDataPath();

	/**
	 * Get the {@link #getDataPath()} value split into bean path elements. For
	 * example a path like {@code i.watts} would return an array like
	 * {@code ["i", "watts"]}.
	 * 
	 * @return the data path elements, or <em>null</em>
	 */
	String[] getDataPathElements();

}
