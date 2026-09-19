/* ==================================================================
 * NameCriteria.java - 17/07/2026 7:02:53 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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
 * Criteria API for named entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface NameCriteria {

	/**
	 * Get the first name.
	 *
	 * <p>
	 * This returns the first available name from the {@link #getNames()} array,
	 * or {@code null} if not available.
	 * </p>
	 *
	 * @return the first name, or {@code null} if not available
	 */
	default @Nullable String getName() {
		final String[] names = getNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of names.
	 *
	 * @return array of names (may be {@code null})
	 */
	String @Nullable [] getNames();

	/**
	 * Test if the filter has a name criteria specified.
	 *
	 * @return {@literal true} if {@link #getName()} is non-null and not empty
	 */
	default boolean hasNameCriteria() {
		return getName() != null && !getName().isEmpty();
	}

	/**
	 * Get the names.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasNameCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the names (presumed non-null)
	 */
	@SuppressWarnings("NullAway")
	default String[] names() {
		return getNames();
	}

}
