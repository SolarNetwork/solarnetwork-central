/* ==================================================================
 * BasicLocationRequestCriteria.java - 19/05/2022 4:15:45 pm
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

import java.util.Collections;
import java.util.Set;
import net.solarnetwork.central.domain.LocationRequestStatus;

/**
 * Basic implementation of {@link LocationRequestCriteria}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class BasicLocationRequestCriteria extends BasicCoreCriteria implements LocationRequestCriteria {

	private Set<LocationRequestStatus> requestStatuses;

	/**
	 * Set a single request status.
	 * 
	 * <p>
	 * This will completely replace any previously configured statuses.
	 * </p>
	 * 
	 * @param status
	 *        the status to set, or {@literal null} to remove all statuses
	 */
	public void setRequestStatus(LocationRequestStatus status) {
		setRequestStatuses(status != null ? Collections.singleton(status) : null);
	}

	@Override
	public Set<LocationRequestStatus> getRequestStatuses() {
		return requestStatuses;
	}

	/**
	 * Set the request statuses.
	 * 
	 * @param requestStatuses
	 *        the statuses to set
	 */
	public void setRequestStatuses(Set<LocationRequestStatus> requestStatuses) {
		this.requestStatuses = requestStatuses;
	}

}
