/* ==================================================================
 * UserCriteria.java - 15/11/2020 12:52:35 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for user related data.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public interface UserCriteria {

	/**
	 * Get the first user ID.
	 * 
	 * <p>
	 * This returns the first available user ID from the {@link #getUserIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first user ID, or {@code null} if not available
	 */
	@Nullable
	default Long getUserId() {
		final var a = getUserIds();
		return (a != null && a.length > 0 ? a[0] : null);
	}

	/**
	 * Get an array of user IDs.
	 * 
	 * @return array of user IDs (may be {@code null})
	 */
	Long @Nullable [] getUserIds();

	/**
	 * Test if this filter has any user criteria.
	 * 
	 * @return {@literal true} if the user ID is non-null
	 */
	default boolean hasUserCriteria() {
		return getUserId() != null;
	}

	/**
	 * Get the first user ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasUserCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first user ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long userId() {
		return getUserId();
	}

	/**
	 * Get an array of user IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasUserCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of user IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long[] userIds() {
		return getUserIds();
	}

}
