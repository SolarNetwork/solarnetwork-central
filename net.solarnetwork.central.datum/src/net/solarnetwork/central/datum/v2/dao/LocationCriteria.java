/* ==================================================================
 * LocationCriteria.java - 26/10/2020 9:32:02 pm
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

import net.solarnetwork.domain.Location;

/**
 * Search criteria for location related data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface LocationCriteria {

	/**
	 * Get the first location ID.
	 * 
	 * <p>
	 * This returns the first available location ID from the
	 * {@link #getLocationIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the location ID, or {@literal null} if not available
	 */
	Long getLocationId();

	/**
	 * Get an array of location IDs.
	 * 
	 * @return array of locations IDs (may be {@literal null})
	 */
	Long[] getLocationIds();

	/**
	 * Get a location to use as geographic criteria.
	 * 
	 * @return the location whose properties represent geographic search
	 *         criteria
	 */
	Location getLocation();

	/**
	 * Test if a {@link Location} is present and any of its properties have a
	 * non-empty value.
	 * 
	 * @return {@literal true} if some property is not empty on the location
	 */
	default boolean hasLocationCriteria() {
		return (getLocation() != null && getLocation().hasLocationCriteria());
	}

}
