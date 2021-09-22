/* ==================================================================
 * Location.java - Aug 8, 2010 7:55:08 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import java.math.BigDecimal;

/**
 * Information describing the physical location of an object.
 * 
 * @author matt
 * @version 1.2
 */
public interface Location extends Filter {

	/**
	 * A generalized name, can be used for "virtual" locations.
	 * 
	 * @return the location name
	 */
	String getName();

	/**
	 * Get the ISO 3166-1 alpha-2 character country code.
	 * 
	 * @return 2-character country code
	 */
	String getCountry();

	/**
	 * A country-specific regional identifier.
	 * 
	 * @return region identifier
	 */
	String getRegion();

	/**
	 * Get a country-specific state or province identifier.
	 * 
	 * @return state or province identifier
	 */
	String getStateOrProvince();

	/**
	 * Get a country-specific postal code.
	 * 
	 * @return postal code
	 */
	String getPostalCode();

	/**
	 * Get the locality (city, town, etc.).
	 * 
	 * @return locality
	 */
	String getLocality();

	/**
	 * Get the street address.
	 * 
	 * @return street
	 */
	String getStreet();

	/**
	 * Get the decimal latitude.
	 * 
	 * @return latitude
	 */
	BigDecimal getLatitude();

	/**
	 * Get the decimal longitude.
	 * 
	 * @return longitude
	 */
	BigDecimal getLongitude();

	/**
	 * Get the elevation, in meters.
	 * 
	 * @return the elevation
	 * @since 1.2
	 */
	BigDecimal getElevation();

	/**
	 * Get a time zone ID, e.g. {@code Pacific/Auckland}.
	 * 
	 * @return a time zone ID
	 */
	String getTimeZoneId();

}
