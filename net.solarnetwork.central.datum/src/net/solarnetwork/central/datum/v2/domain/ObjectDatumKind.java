/* ==================================================================
 * ObjectDatumKind.java - 22/11/2020 9:51:48 pm
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

package net.solarnetwork.central.datum.v2.domain;

/**
 * A datum object kind enumeration.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public enum ObjectDatumKind {

	/** Node datum, with node ID and source ID. */
	Node('n'),

	/** Location datum, with location ID and source ID. */
	Location('l');

	private final char key;

	private ObjectDatumKind(char key) {
		this.key = key;
	}

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	public char getKey() {
		return key;
	}

	/**
	 * Get an enum instance for a key value.
	 * 
	 * @param key
	 *        the key value
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static ObjectDatumKind forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			throw new IllegalArgumentException("Key must not be null.");
		}
		if ( key.length() == 1 ) {
			switch (key.charAt(0)) {
				case 'n':
					return Node;

				case 'l':
					return Location;

				default:
					throw new IllegalArgumentException("Invalid ObjectDatumKind value [" + key + "]");
			}
		}
		// try name() value for convenience
		return ObjectDatumKind.valueOf(key);
	}
}
