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

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for cloud integration related data.
 *
 * @author matt
 * @version 1.1
 */
public interface CloudIntegrationCriteria {

	/**
	 * Get the first integration ID.
	 *
	 * <p>
	 * This returns the first available integration ID from the
	 * {@link #getIntegrationIds()} array, or {@code null} if not available.
	 * </p>
	 *
	 * @return the first integration ID, or {@code null} if not available
	 */
	default @Nullable Long getIntegrationId() {
		final Long[] array = getIntegrationIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of integration IDs.
	 *
	 * @return array of integration IDs (may be {@code null})
	 */
	Long @Nullable [] getIntegrationIds();

	/**
	 * Test if this filter has any integration criteria.
	 *
	 * @return {@literal true} if the integration ID is non-null
	 */
	default boolean hasIntegrationCriteria() {
		return getIntegrationId() != null;
	}

	/**
	 * Get the first cloud integration ID.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasIntegrationCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the first cloud integration ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long integrationId() {
		return getIntegrationId();
	}

	/**
	 * Get an array of cloud integration IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasIntegrationCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of cloud integration IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long[] integrationIds() {
		return getIntegrationIds();
	}

}
