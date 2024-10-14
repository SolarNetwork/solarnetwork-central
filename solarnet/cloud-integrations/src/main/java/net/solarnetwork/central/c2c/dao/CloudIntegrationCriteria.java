/* ==================================================================
 * CloudIntegrationCriteria.java - 1/10/2024 7:48:21 am
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

package net.solarnetwork.central.c2c.dao;

/**
 * Search criteria for cloud integration related data.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudIntegrationCriteria {

	/**
	 * Get the first integration ID.
	 *
	 * <p>
	 * This returns the first available integration ID from the
	 * {@link #getIntegrationIds()} array, or {@literal null} if not available.
	 * </p>
	 *
	 * @return the first integration ID, or {@literal null} if not available
	 */
	default Long getIntegrationId() {
		final Long[] array = getIntegrationIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of integration IDs.
	 *
	 * @return array of integration IDs (may be {@literal null})
	 */
	Long[] getIntegrationIds();

	/**
	 * Test if this filter has any integration criteria.
	 *
	 * @return {@literal true} if the integration ID is non-null
	 */
	default boolean hasIntegrationCriteria() {
		return getIntegrationId() != null;
	}

}
