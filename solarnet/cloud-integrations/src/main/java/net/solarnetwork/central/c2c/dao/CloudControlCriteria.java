/* ==================================================================
 * CloudControlCriteria.java - 3/11/2025 7:56:11 am
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

package net.solarnetwork.central.c2c.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for cloud control related data.
 *
 * @author matt
 * @version 1.1
 */
public interface CloudControlCriteria {

	/**
	 * Get the first cloud control ID.
	 *
	 * <p>
	 * This returns the first available cloud control ID from the
	 * {@link #getCloudControlIds()} array, or {@code null} if not available.
	 * </p>
	 *
	 * @return the first cloud control ID, or {@code null} if not available
	 */
	default @Nullable Long getCloudControlId() {
		final Long[] array = getCloudControlIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of cloud control IDs.
	 *
	 * @return array of cloud control IDs (may be {@code null})
	 */
	Long @Nullable [] getCloudControlIds();

	/**
	 * Test if this filter has any cloud control criteria.
	 *
	 * @return {@literal true} if the cloud control ID is non-null
	 */
	default boolean hasCloudControlCriteria() {
		return getCloudControlId() != null;
	}

	/**
	 * Get the first cloud control ID.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasCloudControlCriteria()} returns {@code true}, to avoid
	 * nullness warnings.
	 * </p>
	 *
	 * @return the first cloud control ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long cloudControlId() {
		return getCloudControlId();
	}

	/**
	 * Get an array of cloud control IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasCloudControlCriteria()} returns {@code true}, to avoid
	 * nullness warnings.
	 * </p>
	 *
	 * @return array of cloud control IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long[] cloudControlIds() {
		return getCloudControlIds();
	}

}
