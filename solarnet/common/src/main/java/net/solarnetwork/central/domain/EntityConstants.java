/* ==================================================================
 * EntityConstants.java - 28/03/2026 6:48:22 am
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

package net.solarnetwork.central.domain;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Constants for entities.
 * 
 * @author matt
 * @version 1.0
 */
public final class EntityConstants {

	private EntityConstants() {
		throw new IllegalStateException();
	}

	/**
	 * A special "not a value" instance to be used for generated entity ID
	 * values yet to be generated.
	 */
	public static final Integer UNASSIGNED_INTEGER_ID = Integer.MIN_VALUE;

	/**
	 * A special "not a value" instance to be used for generated entity ID
	 * values yet to be generated.
	 */
	public static final Long UNASSIGNED_LONG_ID = Long.MIN_VALUE;

	/**
	 * A special "not a value" instance to be used for generated entity ID
	 * values yet to be generated.
	 */
	public static final String UNASSIGNED_STRING_ID = "";

	/**
	 * A special "not a value" instance to be used for generated UUID values yet
	 * to be generated.
	 */
	public static final UUID UNASSIGNED_UUID_ID = UUID
			.fromString("00000000-0000-7000-b000-000000000000");

	/**
	 * Test if an Integer is assigned a value.
	 * 
	 * @param id
	 *        the value to test
	 * @return {@code true} if {@code id} is not {@code null} and not equal to
	 *         {@link #UNASSIGNED_LONG_ID}
	 */
	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	public static boolean isAssigned(@Nullable Integer id) {
		return !(id == null || id == UNASSIGNED_INTEGER_ID);
	}

	/**
	 * Test if a Long is assigned a value.
	 * 
	 * @param id
	 *        the value to test
	 * @return {@code true} if {@code id} is not {@code null} and not equal to
	 *         {@link #UNASSIGNED_LONG_ID}
	 */
	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	public static boolean isAssigned(@Nullable Long id) {
		return !(id == null || id == UNASSIGNED_LONG_ID);
	}

	/**
	 * Test if a String is assigned a value.
	 * 
	 * @param id
	 *        the value to test
	 * @return {@code true} if {@code id} is not {@code null} and not empty
	 */
	@SuppressWarnings({ "BoxedPrimitiveEquality", "ReferenceEquality" })
	public static boolean isAssigned(@Nullable String id) {
		return !(id == null || id.isEmpty());
	}

	/**
	 * Test if a UUID is assigned a value.
	 * 
	 * @param id
	 *        the value to test
	 * @return {@code true} if {@code id} is not {@code null} and not equal to
	 *         {@link #UNASSIGNED_UUID_ID}
	 */
	public static boolean isAssigned(@Nullable UUID id) {
		return !(id == null || id.equals(UNASSIGNED_UUID_ID));
	}

}
