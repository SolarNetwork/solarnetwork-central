/* ==================================================================
 * DatumImportState.java - 6/11/2018 4:33:38 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.imp.domain;

/**
 * The state of a datum import task.
 * 
 * <p>
 * At a basic level, an import task starts in the {@code Queued} state, then
 * will transition to {@code Executing} and then finally {@code Completed}. The
 * {@code Claimed} state may be used before the {@code Executing} state to
 * assist with execution locking support.
 * </p>
 * <p>
 * The {@code Staged} state can be used to test or preview a task, by uploading
 * the data and associated configuration but not actually importing it. A user
 * must initiate a change from {@code Stated} to {@code Queued} for task
 * processing to proceed.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public enum DatumImportState {

	/**
	 * The state is not known.
	 */
	Unknown('u'),

	/**
	 * The import task has been staged (for testing/previewing), but is not yet
	 * queued.
	 */
	Staged('s'),

	/**
	 * The import task has been queued, but not started yet.
	 */
	Queued('q'),

	/**
	 * The import task as been "claimed" for execution but has not started
	 * execution yet.
	 */
	Claimed('p'),

	/**
	 * The import task is being executed currently.
	 */
	Executing('e'),

	/**
	 * The import task has completed.
	 */
	Completed('c');

	private final char key;

	private DatumImportState(char key) {
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
	 * @return the enum with the given key, or {@link DatumImportState#Unknown}
	 *         if not recognized
	 */
	public static DatumImportState forKey(char key) {
		for ( DatumImportState type : DatumImportState.values() ) {
			if ( type.key == key ) {
				return type;
			}
		}
		return DatumImportState.Unknown;
	}

}
