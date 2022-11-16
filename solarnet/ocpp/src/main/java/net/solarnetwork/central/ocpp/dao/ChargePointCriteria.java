/* ==================================================================
 * ChargePointCriteria.java - 16/11/2022 5:34:39 pm
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

/**
 * Search criteria for charge point related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargePointCriteria {

	/**
	 * Get the first charge point ID.
	 * 
	 * <p>
	 * This returns the first available charge point ID from the
	 * {@link #getChargePointIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first charge point ID, or {@literal null} if not available
	 */
	Long getChargePointId();

	/**
	 * Get an array of charge point IDs.
	 * 
	 * @return array of charge point IDs (may be {@literal null})
	 */
	Long[] getChargePointIds();

	/**
	 * Test if this filter has any charge point criteria.
	 * 
	 * @return {@literal true} if the charge point ID is non-null
	 */
	default boolean hasChargePointCriteria() {
		return getChargePointId() != null;
	}

}
