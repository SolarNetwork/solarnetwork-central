/* ==================================================================
 * LocationMatch.java - Nov 18, 2013 6:54:20 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.EntityMatch;

/**
 * API for Location search results.
 * 
 * @author matt
 * @version 2.0
 */
public interface LocationMatch extends EntityMatch {

	/**
	 * A generalized name, can be used for "virtual" locations.
	 * 
	 * @return the location name
	 */
	@Nullable
	String getName();

	/**
	 * Get the ISO 3166-1 alpha-2 character country code.
	 * 
	 * @return 2-character country code
	 */
	@Nullable
	String getCountry();

	/**
	 * A country-specific regional identifier.
	 * 
	 * @return region identifier
	 */
	@Nullable
	String getRegion();

	/**
	 * Get a country-specific state or province identifier.
	 * 
	 * @return state or province identifier
	 */
	@Nullable
	String getStateOrProvince();

	/**
	 * Get a country-specific postal code.
	 * 
	 * @return postal code
	 */
	@Nullable
	String getPostalCode();

	/**
	 * Get the locality (city, town, etc.).
	 * 
	 * @return locality
	 */
	@Nullable
	String getLocality();

	/**
	 * Get the street address.
	 * 
	 * @return street
	 */
	@Nullable
	String getStreet();

	/**
	 * Get the decimal latitude.
	 * 
	 * @return latitude
	 */
	@Nullable
	BigDecimal getLatitude();

	/**
	 * Get the decimal longitude.
	 * 
	 * @return longitude
	 */
	@Nullable
	BigDecimal getLongitude();

	/**
	 * Get the elevation, in meters.
	 * 
	 * @return the elevation
	 * @since 1.2
	 */
	@Nullable
	BigDecimal getElevation();

}
