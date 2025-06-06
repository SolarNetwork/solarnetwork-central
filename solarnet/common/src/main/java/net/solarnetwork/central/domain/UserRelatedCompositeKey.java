/* ==================================================================
 * UserRelatedCompositeKey.java - 28/09/2024 5:27:20 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.io.Serializable;

/**
 * A standard user-related composite key.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("ComparableType")
public interface UserRelatedCompositeKey<K extends UserIdRelated & CompositeKey & Comparable<K> & Serializable>
		extends UserIdRelated, CompositeKey, Comparable<K>, Serializable {

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	@Override
	default Long getUserId() {
		return (Long) keyComponent(0);
	}

	/**
	 * Test if the user ID is assigned.
	 * 
	 * @return {@literal true} if the user ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 */
	default boolean userIdIsAssigned() {
		return keyComponentIsAssigned(0);
	}

}
