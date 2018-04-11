/* ==================================================================
 * OutputCompressionType.java - 5/03/2018 8:28:19 PM
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

package net.solarnetwork.central.datum.domain.export;

/**
 * Enumeration of output compression types.
 * 
 * @author matt
 * @version 1.0
 */
public enum OutputCompressionType {

	/** No compression. */
	None('n', ""),

	/** Gzip compression. */
	GZIP('z', "gz");

	private final char key;
	private final String ext;

	private OutputCompressionType(char key, String ext) {
		this.key = key;
		this.ext = ext;
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
	 * Get the filename extension.
	 * 
	 * @return the extension, which may be an empty string but never
	 *         {@literal null}
	 */
	public String getFilenameExtension() {
		return ext;
	}

	/**
	 * Get an enum for a key value.
	 * 
	 * @param key
	 *        the key of the enum to get
	 * @return the enum with the given key
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static OutputCompressionType forKey(char key) {
		for ( OutputCompressionType type : OutputCompressionType.values() ) {
			if ( type.key == key ) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported key: " + key);
	}

}
