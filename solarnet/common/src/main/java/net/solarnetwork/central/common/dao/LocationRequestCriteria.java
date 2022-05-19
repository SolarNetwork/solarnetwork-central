/* ==================================================================
 * LocationRequestCriteria.java - 19/05/2022 2:07:21 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import java.util.Set;
import net.solarnetwork.central.domain.LocationRequestStatus;

/**
 * Criteria API for location requests.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface LocationRequestCriteria extends LocationCriteria, UserCriteria {

	/**
	 * Get the location request statuses.
	 * 
	 * @return the request statuses
	 */
	Set<LocationRequestStatus> getRequestStatuses();

	/**
	 * Get the first request status.
	 * 
	 * <p>
	 * This returns the first available status from the
	 * {@link #getRequestStatuses()} set in iteration order, or {@literal null}
	 * if not available.
	 * </p>
	 * 
	 * @return the first status, or {@literal null} if not available
	 */
	default LocationRequestStatus getRequestStatus() {
		Set<LocationRequestStatus> s = getRequestStatuses();
		return (s != null && !s.isEmpty() ? s.iterator().next() : null);
	}

	/**
	 * Test if this criteria has any request status criteria.
	 * 
	 * @return {@literal true} if the request status set is not empty
	 */
	default boolean hasRequestStatusCriteria() {
		Set<LocationRequestStatus> s = getRequestStatuses();
		return (s != null && !s.isEmpty());
	}

}
