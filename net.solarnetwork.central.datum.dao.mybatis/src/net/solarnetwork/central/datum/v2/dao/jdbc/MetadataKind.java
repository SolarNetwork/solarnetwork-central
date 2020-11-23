/* ==================================================================
 * MetadataKind.java - 6/11/2020 3:38:49 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

/**
 * The type of metadata to parse.
 * 
 * <p>
 * This type is used in situations where the actual metadata kind might be
 * determined at runtime.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public enum MetadataKind {

	/** Node metadata. */
	Node('n'),

	/** Location metadata. */
	Location('l'),

	/** Dynamically determined metadata. */
	Dynamic('*');

	private final char key;

	private MetadataKind(char key) {
		this.key = key;
	}

	/**
	 * Get the key value.
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
	public static MetadataKind forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			throw new IllegalArgumentException("Key must not be null.");
		}
		if ( key.length() == 1 ) {
			switch (key.charAt(0)) {
				case 'n':
					return Node;

				case 'l':
					return Location;

				case '*':
					return Dynamic;

				default:
					throw new IllegalArgumentException("Invalid MetadataKind value [" + key + "]");
			}
		}
		// try name() value for convenience
		return MetadataKind.valueOf(key);
	}

}
