/* ==================================================================
 * IdentifiableCriteria.java - 10/03/2025 10:49:54 am
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
 * Search criteria for {@code Identifiable} related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface IdentifiableCriteria {

	/**
	 * Get the first service identifier.
	 * 
	 * <p>
	 * This returns the first available service identifier from the
	 * {@link #getServiceIdentifiers()} array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first service identifier, or {@code null} if not available
	 */
	default String getServiceIdentifier() {
		String[] idents = getServiceIdentifiers();
		return (idents != null && idents.length > 0 ? idents[0] : null);
	}

	/**
	 * Get an array of service identifiers.
	 * 
	 * @return array of service identifiers (may be {@code null})
	 */
	String[] getServiceIdentifiers();

	/**
	 * Test if this filter has any service identifier criteria.
	 * 
	 * @return {@code true} if the service identifier is non-null
	 */
	default boolean hasServiceIdentifierCriteria() {
		return getServiceIdentifier() != null;
	}

}
