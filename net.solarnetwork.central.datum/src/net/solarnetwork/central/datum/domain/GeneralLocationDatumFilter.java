/* ==================================================================
 * GeneralLocationDatumFilter.java - Oct 17, 2014 12:44:06 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.domain.Filter;
import org.joda.time.DateTime;

/**
 * Filter API for {@link GeneralLocationDatum}.
 * 
 * @author matt
 * @version 1.0
 */
public interface GeneralLocationDatumFilter extends Filter {

	/**
	 * Flag to indicate that only the most recently available data should be
	 * returned.
	 * 
	 * @return the most recent only
	 */
	public boolean isMostRecent();

	/**
	 * Get a start date.
	 * 
	 * @return the start date
	 */
	public DateTime getStartDate();

	/**
	 * Get an end date.
	 * 
	 * @return the end date
	 */
	public DateTime getEndDate();

	/**
	 * Get the first location ID. This returns the first available location ID
	 * from the {@link #getLocationIds()} array, or <em>null</em> if not
	 * available.
	 * 
	 * @return the location ID, or <em>null</em> if not available
	 */
	public Long getLocationId();

	/**
	 * Get an array of location IDs.
	 * 
	 * @return array of location IDs (may be <em>null</em>)
	 */
	public Long[] getLocationIds();

	/**
	 * Get the first source ID. This returns the first available source ID from
	 * the {@link #getSourceIds()} array, or <em>null</em> if not available.
	 * 
	 * @return the first source ID, or <em>null</em> if not available
	 */
	public String getSourceId();

	/**
	 * Get an array of source IDs.
	 * 
	 * @return array of source IDs (may be <em>null</em>)
	 */
	public String[] getSourceIds();

	/**
	 * Get a bean object path to a specific data value key to extract and return
	 * from the results, instead of all data. For example a path like
	 * {@code i.temp} might return a temperature value.
	 * 
	 * @return bean object path to extract
	 * @since 1.1
	 */
	public String getDataPath();

	/**
	 * Get the {@link #getDataPath()} value split into bean path elements. For
	 * example a path like {@code i.temp} would return an array like
	 * {@code ["i", "temp"]}.
	 * 
	 * @return the data path elements, or <em>null</em>
	 * @since 1.1
	 */
	public String[] getDataPathElements();

}
