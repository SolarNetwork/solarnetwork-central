/* ==================================================================
 * ChargeSessionCriteria.java - 10/12/2022 11:00:46 am
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

package net.solarnetwork.central.ocpp.dao;

import java.util.UUID;

/**
 * Search criteria for charge session related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionCriteria {

	/**
	 * Get the first charge session ID.
	 * 
	 * <p>
	 * This returns the first available charge session ID from the
	 * {@link #getChargeSessionIds()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the first charge session ID, or {@literal null} if not available
	 */
	UUID getChargeSessionId();

	/**
	 * Get an array of charge session IDs.
	 * 
	 * @return array of charge session IDs (may be {@literal null})
	 */
	UUID[] getChargeSessionIds();

	/**
	 * Test if this filter has any charge session criteria.
	 * 
	 * @return {@literal true} if the charge session ID is non-null
	 */
	default boolean hasChargeSessionCriteria() {
		return getChargeSessionId() != null;
	}

	/**
	 * Filter on active (not ended) sessions.
	 * 
	 * @return {@literal true} to only include sessions that have not ended
	 */
	Boolean getActive();

}
