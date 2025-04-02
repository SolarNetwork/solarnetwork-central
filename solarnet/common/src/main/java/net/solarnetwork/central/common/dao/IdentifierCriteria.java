/* ==================================================================
 * IdentifierCriteria.java - 16/11/2022 5:34:39 pm
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

package net.solarnetwork.central.common.dao;

/**
 * Search criteria for identifier related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface IdentifierCriteria {

	/**
	 * Get an array of identifiers.
	 * 
	 * @return array of identifiers (may be {@code null})
	 */
	String[] getIdentifiers();

	/**
	 * Get the first identifier.
	 * 
	 * <p>
	 * This returns the first available identifier from the
	 * {@link #getIdentifiers()} array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first identifier, or {@code null} if not available
	 */
	default String getIdentifier() {
		String[] a = getIdentifiers();
		return (a != null && a.length > 0 ? a[0] : null);
	}

	/**
	 * Test if this filter has any identifier criteria.
	 * 
	 * @return {@code true} if the identifier is non-null
	 */
	default boolean hasIdentifierCriteria() {
		return getIdentifier() != null;
	}

}
