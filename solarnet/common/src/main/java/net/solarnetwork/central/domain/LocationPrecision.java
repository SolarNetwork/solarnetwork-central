/* ==================================================================
 * LocationPrecision.java - 9/10/2016 7:44:53 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
 * Enumeration of locatioin precision levels.
 * 
 * @author matt
 * @version 1.0
 */
public enum LocationPrecision {

	LatLong(1),

	Block(5),

	Street(10),

	PostalCode(20),

	Locality(30),

	StateOrProvince(40),

	Region(50),

	TimeZone(60),

	Country(70);

	private final Integer precision;

	private LocationPrecision(int precision) {
		this.precision = precision;
	}

	/**
	 * Compare the precision of this to another.
	 * 
	 * @param other
	 *        the other
	 * @return -1 if this precision less than other precision, 0 if precisions
	 *         are equal, or 1 if this precision is greater than other precision
	 */
	public int comparePrecision(LocationPrecision other) {
		return this.precision.compareTo(other.precision);
	}

	/**
	 * Get a relative precision value for this enum. The smaller the value, the
	 * more precise a location of this level represents.
	 * 
	 * @return The precision.
	 */
	public Integer getPrecision() {
		return precision;
	}

}
