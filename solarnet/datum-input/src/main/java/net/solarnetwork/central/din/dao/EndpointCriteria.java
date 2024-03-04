/* ==================================================================
 * EndpointCriteria.java - 21/02/2024 2:54:36 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.dao;

import java.util.UUID;

/**
 * Search criteria for endpoint related data.
 *
 * @author matt
 * @version 1.0
 */
public interface EndpointCriteria {

	/**
	 * Get the first endpoint ID.
	 *
	 * <p>
	 * This returns the first available endpoint ID from the
	 * {@link #getEndpointIds()} array, or {@literal null} if not available.
	 * </p>
	 *
	 * @return the first endpoint ID, or {@literal null} if not available
	 */
	default UUID getEndpointId() {
		final UUID[] array = getEndpointIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of endpoint IDs.
	 *
	 * @return array of endpoint IDs (may be {@literal null})
	 */
	UUID[] getEndpointIds();

	/**
	 * Test if this filter has any endpoint criteria.
	 *
	 * @return {@literal true} if the endpoint ID is non-null
	 */
	default boolean hasEndpointCriteria() {
		return getEndpointId() != null;
	}

}
