/* ==================================================================
 * DatumReadingType.java - 7/08/2018 4:03:49 PM
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
 * An enumeration of different reading types for datum.
 * 
 * <p>
 * A "reading" in this context is like a meter reading, where a query is for the
 * value of a datum at or near specific points in time, or the difference
 * between two points in time.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.28
 */
public enum DatumReadingType {

	/**
	 * Derive a single reading value based from one datum the nearest before a
	 * specific time and one the nearest after.
	 */
	CalculatedAt("at"),

	/**
	 * Calculate the difference between two reading values on two dates, using
	 * the {@code CalcualtedAt} style of deriving the start and end readings.
	 */
	CalculatedAtDifference("atd"),

	/**
	 * Find the difference between two datum that are nearest in time on or
	 * before two dates.
	 */
	NearestDifference("diff"),

	/**
	 * Find the difference between two datum that occurs between two dates,
	 * without any limits on how near to those dates the datum are.
	 * 
	 * @since 1.1
	 */
	Difference("delta");

	private final String key;

	private DatumReadingType(String key) {
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
	 *        the key value; if {@literal null} or empty then
	 *        {@link #NearestDifference} will be returned
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static DatumReadingType forKey(String key) {
		if ( key == null || key.isEmpty() ) {
			return NearestDifference;
		}
		try {
			// try name() value first for convenience
			return DatumReadingType.valueOf(key);
		} catch ( IllegalArgumentException e ) {
			for ( DatumReadingType type : DatumReadingType.values() ) {
				if ( type.key.equals(key) ) {
					return type;
				}
			}
		}
		throw new IllegalArgumentException("Invalid DatumReadingType value");
	}
}
