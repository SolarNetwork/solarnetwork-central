/* ==================================================================
 * TaskCriteria.java - 21/09/2025 6:34:20 am
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

package net.solarnetwork.central.common.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for task related data.
 *
 * @author matt
 * @version 1.1
 */
public interface TaskCriteria {

	/**
	 * Get the first task ID.
	 *
	 * <p>
	 * This returns the first available task ID from the {@link #getTaskIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 *
	 * @return the first task ID, or {@code null} if not available
	 */
	default @Nullable Long getTaskId() {
		final Long[] array = getTaskIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of task IDs.
	 *
	 * @return array of task IDs (may be {@code null})
	 */
	Long @Nullable [] getTaskIds();

	/**
	 * Test if this filter has any task criteria.
	 *
	 * @return {@literal true} if the task ID is non-null
	 */
	default boolean hasTaskCriteria() {
		return getTaskId() != null;
	}

	/**
	 * Get the first task ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTaskCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first task ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long taskId() {
		return getTaskId();
	}

	/**
	 * Get an array of task IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTaskCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of task IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Long[] taskIds() {
		return getTaskIds();
	}

}
