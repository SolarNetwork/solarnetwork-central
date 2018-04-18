/* ==================================================================
 * DatumExportState.java - 29/03/2018 6:03:38 PM
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

package net.solarnetwork.central.datum.export.domain;

/**
 * A status for a datum export job.
 * 
 * <p>
 * An export job starts in the {@code Queued} state, then will transition to
 * {@code Executing} and then finally {@code Completed}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public enum DatumExportState {

	/**
	 * The state is not known.
	 */
	Unknown('u'),

	/**
	 * The export job has been queued, but not started yet.
	 */
	Queued('q'),

	/**
	 * The export job is being executed currently.
	 */
	Executing('e'),

	/**
	 * The export job has completed.
	 */
	Completed('c');

	private final char key;

	private DatumExportState(char key) {
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
	 * @return the enum with the given key, or {@link DatumExportState#Unknown}
	 *         if not recognized
	 */
	public static DatumExportState forKey(char key) {
		for ( DatumExportState type : DatumExportState.values() ) {
			if ( type.key == key ) {
				return type;
			}
		}
		return DatumExportState.Unknown;
	}
}
