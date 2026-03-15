/* ==================================================================
 * ControlCriteria.java - 23/10/2025 9:19:11 pm
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
 * Search criteria for control related data.
 * 
 * @author matt
 * @version 1.1
 */
public interface ControlCriteria {

	/**
	 * Get the first control ID.
	 * 
	 * <p>
	 * This returns the first available control ID from the
	 * {@link #getControlIds()} array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first control ID, or {@code null} if not available
	 */
	default @Nullable String getControlId() {
		final String[] array = getControlIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of control IDs.
	 * 
	 * @return array of control IDs (may be {@code null})
	 */
	String @Nullable [] getControlIds();

	/**
	 * Test if this filter has any control criteria.
	 * 
	 * @return {@literal true} if the control ID is non-null
	 * @since 1.1
	 */
	default boolean hasControlCriteria() {
		return getControlId() != null;
	}

	/**
	 * Get the first control ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasControlCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first control ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String controlId() {
		return getControlId();
	}

	/**
	 * Get an array of control IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasControlCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of control IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String[] controlIds() {
		return getControlIds();
	}

}
