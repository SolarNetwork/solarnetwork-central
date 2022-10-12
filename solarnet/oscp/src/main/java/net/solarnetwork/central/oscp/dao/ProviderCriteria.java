/* ==================================================================
 * ProviderCriteria.java - 16/08/2022 9:40:43 pm
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

package net.solarnetwork.central.oscp.dao;

/**
 * Criteria API for a provider entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ProviderCriteria {

	/**
	 * Test if any provider criteria exists.
	 * 
	 * @return {@literal true} if a provider criteria exists
	 */
	default boolean hasProviderCriteria() {
		Long id = getProviderId();
		return (id != null);
	}

	/**
	 * Get an array of provider IDs.
	 * 
	 * @return array of IDs (may be {@literal null})
	 */
	Long[] getProviderIds();

	/**
	 * Get the first provider ID.
	 * 
	 * <p>
	 * This returns the first available ID from the {@link #getProviderIds()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the provider ID, or {@literal null} if not available
	 */
	default Long getProviderId() {
		Long[] ids = getProviderIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

}
