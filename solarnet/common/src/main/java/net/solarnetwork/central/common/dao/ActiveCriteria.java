/* ==================================================================
 * ActiveCriteria.java - 2/04/2025 8:43:03 am
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

/**
 * Criteria API for active status entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ActiveCriteria {

	/**
	 * Test if an active criteria exists.
	 * 
	 * @return {@literal true} if an active criteria exists
	 */
	default boolean hasActiveCriteria() {
		Boolean a = getActive();
		return (a != null);
	}

	/**
	 * Get the active criteria.
	 * 
	 * @return the active criteria (may be {@code null})
	 */
	Boolean getActive();

	/**
	 * Get the active criteria as a primitive boolean.
	 * 
	 * <p>
	 * This returns the {@link #getActive()} value if not {@code null} and
	 * {@code false} otherwise
	 * </p>
	 * 
	 * @return the active criteria, or {@code false} if not available
	 */
	default boolean active() {
		Boolean a = getActive();
		return (a != null ? a : false);
	}

}
