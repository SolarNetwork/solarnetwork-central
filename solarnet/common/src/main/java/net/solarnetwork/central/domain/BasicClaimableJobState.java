/* ==================================================================
 * BasicClaimableJobState.java - 9/10/2024 8:47:37 pm
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Basic implementation of {@link ClaimableJobState} as an enumeration.
 *
 * @author matt
 * @version 1.0
 */
public enum BasicClaimableJobState implements ClaimableJobState {

	/**
	 * The state is not known.
	 */
	Unknown(UNKNOWN_KEY),

	/**
	 * The task has been queued, but not started yet.
	 */
	Queued(QUEUED_KEY),

	/**
	 * The task as been "claimed" for execution but has not started execution
	 * yet.
	 */
	Claimed(CLAIMED_KEY),

	/**
	 * The task is being executed currently.
	 */
	Executing(EXECUTING_KEY),

	/**
	 * The task has completed.
	 */
	Completed(COMPLETED_KEY);

	private final String key;

	private BasicClaimableJobState(char key) {
		this.key = String.valueOf(key);
	}

	@Override
	public char getKey() {
		return key.charAt(0);
	}

	/**
	 * Get a key value for this enum.
	 *
	 * @return the key as a string
	 */
	@Override
	@JsonValue
	public String keyValue() {
		return key;
	}

	/**
	 * Get an enum for a key value.
	 *
	 * @param key
	 *        the key of the enum to get
	 * @return the enum with the given key, or {@link #Unknown} if not
	 *         recognized
	 */
	public static BasicClaimableJobState forKey(char key) {
		for ( BasicClaimableJobState type : BasicClaimableJobState.values() ) {
			if ( type.getKey() == key ) {
				return type;
			}
		}
		return BasicClaimableJobState.Unknown;
	}

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitive
	 * @return the enum, or {@literal null} if value is {@literal null} or empty
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 * @since 1.1
	 */
	@JsonCreator
	public static BasicClaimableJobState fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		final char key = value.length() == 1 ? Character.toLowerCase(value.charAt(0)) : 0;
		for ( BasicClaimableJobState e : BasicClaimableJobState.values() ) {
			if ( key == e.getKey() || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown BasicClaimableJobState value [" + value + "]");
	}

}
