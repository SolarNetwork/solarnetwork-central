/* ==================================================================
 * EnabledCriteria.java - 5/08/2023 2:45:01 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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
 * Search criteria for enabled-state data.
 * 
 * @author matt
 * @version 1.1
 */
public interface EnabledCriteria {

	/**
	 * Get the enabled flag.
	 * 
	 * @return the {@literal true} or {@literal false} to filter by that state,
	 *         or {@code null} to not filter
	 */
	@Nullable
	Boolean getEnabled();

	/**
	 * Test if this filter has any enabled criteria.
	 * 
	 * @return {@literal true} if the enabled is non-null
	 */
	default boolean hasEnabledCriteria() {
		return getEnabled() != null;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasEnabledCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the {@literal true} or {@literal false} to filter by that state
	 *         (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Boolean enabled() {
		return getEnabled();
	}

}
