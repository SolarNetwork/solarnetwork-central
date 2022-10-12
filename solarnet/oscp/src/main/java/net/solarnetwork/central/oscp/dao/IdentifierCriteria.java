/* ==================================================================
 * IdentifierCriteria.java - 12/08/2022 4:44:36 pm
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

/**
 * Criteria API for a identified entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface IdentifierCriteria {

	/**
	 * Test if any identifier criteria exists.
	 * 
	 * @return {@literal true} if an identifier criteria exists
	 */
	default boolean hasIdentifierCriteria() {
		String id = getIdentifier();
		return (id != null);
	}

	/**
	 * Get an array of identifiers.
	 * 
	 * @return array of identifiers (may be {@literal null})
	 */
	String[] getIdentifiers();

	/**
	 * Get the first group ID.
	 * 
	 * <p>
	 * This returns the first available value from the {@link #getIdentifiers()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the identifier, or {@literal null} if not available
	 */
	default String getIdentifier() {
		String[] ids = getIdentifiers();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

}
