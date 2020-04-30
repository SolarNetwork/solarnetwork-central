/* ==================================================================
 * CombiningType.java - 25/05/2018 11:57:22 AM
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

package net.solarnetwork.central.datum.domain;

/**
 * An action to perform when combining data elements.
 * 
 * @author matt
 * @version 1.1
 * @since 1.25
 */
public enum CombiningType {

	/** Add values. */
	Sum("s"),

	/** Average of values. */
	Average("a"),

	/**
	 * Subtract values.
	 * 
	 * @since 1.1
	 */
	Difference("d"),

	/**
	 * Multiply values.
	 * 
	 * @since 1.1
	 */
	Multiply("m"),

	/**
	 * Divide values.
	 * 
	 * @since 1.1
	 */
	Divide("v");

	private final String key;

	private CombiningType(String key) {
		this.key = key;
	}

	/**
	 * Get a key value.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get an enum instance for a key value.
	 * 
	 * @param key
	 *        the key value; if {@literal null} or empty then {@link #Sum} will
	 *        be returned
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static CombiningType forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			return Sum;
		}
		try {
			// try name() value first for convenience
			return CombiningType.valueOf(key);
		} catch ( IllegalArgumentException e ) {
			for ( CombiningType type : CombiningType.values() ) {
				if ( type.key.equals(key) ) {
					return type;
				}
			}
		}
		throw new IllegalArgumentException("Invalid CombiningType value");
	}
}
