/* ==================================================================
 * UserIdRelated.java - 25/02/2024 8:14:45 am
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

/**
 * API for objects related to a user entity by way of a user ID.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserIdRelated {

	/**
	 * A special "not a value" instance to be used for generated user ID values
	 * yet to be generated.
	 * 
	 * @since 1.1
	 */
	Long UNASSIGNED_USER_ID = Long.MIN_VALUE;

	/**
	 * Get the user ID this entity is related to.
	 * 
	 * @return the user ID
	 * @throws IllegalStateException
	 *         if the user ID is not available
	 */
	Long getUserId() throws IllegalStateException;

	/**
	 * Test if the user ID is assigned.
	 * 
	 * @return {@literal true} if the user ID value is assigned,
	 *         {@literal false} if it is considered "not a value"
	 * @since 1.1
	 */
	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	default boolean userIdIsAssigned() {
		final Long userId = getUserId();
		return userId != null && userId != UNASSIGNED_USER_ID;
	}

}
