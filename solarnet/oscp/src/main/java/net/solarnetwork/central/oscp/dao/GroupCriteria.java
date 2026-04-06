/* ==================================================================
 * GroupCriteria.java - 12/08/2022 4:44:36 pm
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

import org.jspecify.annotations.Nullable;

/**
 * Criteria API for a grouped entities.
 *
 * @author matt
 * @version 1.1
 */
public interface GroupCriteria {

	/**
	 * Test if any group criteria exists.
	 *
	 * @return {@literal true} if a group criteria exists
	 */
	default boolean hasGroupCriteria() {
		Long id = getGroupId();
		return (id != null);
	}

	/**
	 * Get an array of group IDs.
	 *
	 * @return array of IDs (may be {@code null})
	 */
	Long @Nullable [] getGroupIds();

	/**
	 * Get the first group ID.
	 *
	 * <p>
	 * This returns the first available ID from the {@link #getGroupIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 *
	 * @return the group ID, or {@code null} if not available
	 */
	default @Nullable Long getGroupId() {
		Long[] ids = getGroupIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Get the first group ID.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasGroupCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the first group ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long groupId() {
		return getGroupId();
	}

	/**
	 * Get an array of group IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasGroupCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of group IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long[] groupIds() {
		return getGroupIds();
	}

}
