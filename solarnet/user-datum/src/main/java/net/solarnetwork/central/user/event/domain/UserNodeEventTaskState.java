/* ==================================================================
 * UserNodeEventTaskState.java - 4/06/2020 11:19:02 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.domain;

/**
 * User node event task state enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum UserNodeEventTaskState {

	/**
	 * The state is not known.
	 */
	Unknown('u'),

	/**
	 * The task has been queued, but not started yet.
	 */
	Queued('q'),

	/**
	 * The task as been "claimed" for execution but has not started execution
	 * yet.
	 */
	Claimed('p'),

	/**
	 * The task is being executed currently.
	 */
	Executing('e'),

	/**
	 * The task completed.
	 */
	Completed('c');

	private final char key;

	private UserNodeEventTaskState(char key) {
		this.key = key;
	}

	/**
	 * Get the key value.
	 * 
	 * @return the key value
	 */
	public char getKey() {
		return key;
	}

	/**
	 * Get an enum for a key value.
	 * 
	 * @param key
	 *        the key of the enum to get
	 * @return the enum with the given key, or
	 *         {@link UserNodeEventTaskState#Unknown} if not recognized
	 */
	public static UserNodeEventTaskState forKey(char key) {
		for ( UserNodeEventTaskState type : UserNodeEventTaskState.values() ) {
			if ( type.key == key ) {
				return type;
			}
		}
		return UserNodeEventTaskState.Unknown;
	}

}
