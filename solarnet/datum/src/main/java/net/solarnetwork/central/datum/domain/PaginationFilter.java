/* ==================================================================
 * PaginationFilter.java - 5/02/2025 8:55:32 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.util.List;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for page-based search criteria.
 *
 * @author matt
 * @version 1.0
 */
public interface PaginationFilter {

	/**
	 * Get the sort orderings.
	 *
	 * @return the sorts
	 */
	List<SortDescriptor> getSorts();

	/**
	 * Get the desired starting offset.
	 *
	 * @return the offset, or {@literal null}
	 */
	Long getOffset();

	/**
	 * Get the maximum desired results.
	 *
	 * @return the max, or {@literal null} for all results
	 */
	Integer getMax();

	/**
	 * Test if any sort descriptors are available.
	 *
	 * @return {@literal true} if at least one sort descriptor is available
	 */
	default boolean hasSorts() {
		List<SortDescriptor> sorts = getSorts();
		return (sorts != null && !sorts.isEmpty());
	}

}
