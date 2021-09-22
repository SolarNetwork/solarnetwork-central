/* ==================================================================
 * LocalDateRangeFilter.java - 4/02/2019 7:37:34 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.time.LocalDateTime;

/**
 * Filter API for a local date range.
 * 
 * <p>
 * This API is meant to be used as an alternative to {@link DateRangeFilter}.
 * The <em>local</em> reference is specific to the type of data being queried.
 * For example, when querying nodes, local time might refer to the local times
 * of the nodes.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.46
 */
public interface LocalDateRangeFilter extends Filter {

	/**
	 * Get a start date in local time.
	 * 
	 * @return the local start date
	 */
	LocalDateTime getLocalStartDate();

	/**
	 * Get an end date in local time.
	 * 
	 * @return the local end date
	 */
	LocalDateTime getLocalEndDate();

}
