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

	private final char key;

	private BasicClaimableJobState(char key) {
		this.key = key;
	}

	@Override
	public char getKey() {
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
			if ( type.key == key ) {
				return type;
			}
		}
		return BasicClaimableJobState.Unknown;
	}

}
