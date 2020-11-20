/* ==================================================================
 * DatumStreamCriteria.java - 20/11/2020 10:44:40 am
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

import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.RecentCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Search criteria for datum streams.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumStreamCriteria extends DateRangeCriteria, LocalDateRangeCriteria,
		NodeMetadataCriteria, LocationMetadataCriteria, UserCriteria, AggregationCriteria,
		RecentCriteria, OptimizedQueryCriteria, PaginationCriteria, SortCriteria {

	/**
	 * Test if this filter has any location, node, or source criteria.
	 * 
	 * @return {@literal true} if a location, node, or source ID is non-null
	 */
	default boolean hasDatumMetadataCriteria() {
		return (getLocationId() != null || getNodeId() != null || getSourceId() != null);
	}

	/**
	 * Test if the filter as a local date range specified.
	 * 
	 * @return {@literal true} if both a local start and end date are non-null
	 */
	default boolean hasLocalDateRange() {
		return (getLocalStartDate() != null && getLocalEndDate() != null);
	}

	/**
	 * Test if the filter as a local start date specified.
	 * 
	 * @return {@literal true} if the local start date is non-null
	 */
	default boolean hasLocalStartDate() {
		return getLocalStartDate() != null;
	}

}
