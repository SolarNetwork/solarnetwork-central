/* ==================================================================
 * DatumRollupType.java - 12/07/2018 8:00:47 PM
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
 * A rollup type for datum queries.
 * 
 * @author matt
 * @version 1.0
 * @since 1.27
 */
public enum DatumRollupType {

	/** No rollup. */
	None("0"),

	/** Rollup everything into a single result. */
	All("a"),

	/** Rollup the time component of the results. */
	Time("t"),

	/** Rollup the node component of the results. */
	Node("n"),

	/** Rollup the source component of the results. */
	Source("s");

	private final String key;

	private DatumRollupType(String key) {
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
	 * <p>
	 * Note this method will also call {@link #valueOf(String)} first to support
	 * parsing the full enum name.
	 * </p>
	 * 
	 * @param key
	 *        the key value; if {@literal null} or empty then {@link #None} will
	 *        be returned
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static DatumRollupType forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			return None;
		}
		try {
			// try name() value first for convenience
			return DatumRollupType.valueOf(key);
		} catch ( IllegalArgumentException e ) {
			for ( DatumRollupType type : DatumRollupType.values() ) {
				if ( type.key.equals(key) ) {
					return type;
				}
			}
		}
		throw new IllegalArgumentException("Invalid DatumRollupType value");
	}
}
