/* ==================================================================
 * CommonFilter.java - 22/04/2018 9:08:34 AM
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

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import net.solarnetwork.central.domain.Filter;

/**
 * API for common filter properties.
 * 
 * @author matt
 * @version 1.1
 * @since 1.22
 */
public interface CommonFilter extends Filter {

	/**
	 * Flag to indicate that only the most recently available data should be
	 * returned.
	 * 
	 * @return the most recent only
	 */
	boolean isMostRecent();

	/**
	 * Get a start date.
	 * 
	 * @return the start date
	 */
	DateTime getStartDate();

	/**
	 * Get an end date.
	 * 
	 * @return the end date
	 */
	DateTime getEndDate();

	/**
	 * Get a start date in local time.
	 * 
	 * <p>
	 * This is meant to be used as an alternative to {@link #getStartDate()} and
	 * does not represent a local version of that instance.
	 * </p>
	 * 
	 * @return the local start date
	 * @since 1.1
	 */
	LocalDateTime getLocalStartDate();

	/**
	 * Get an end date in local time.
	 * 
	 * <p>
	 * This is meant to be used as an alternative to {@link #getEndDate()} and
	 * does not represent a local version of that instance.
	 * </p>
	 * 
	 * @return the local end date
	 * @since 1.1
	 */
	LocalDateTime getLocalEndDate();

	/**
	 * Get the first source ID. This returns the first available source ID from
	 * the {@link #getSourceIds()} array, or <em>null</em> if not available.
	 * 
	 * @return the first source ID, or <em>null</em> if not available
	 */
	String getSourceId();

	/**
	 * Get an array of source IDs.
	 * 
	 * @return array of source IDs (may be <em>null</em>)
	 */
	String[] getSourceIds();

	/**
	 * Get a bean object path to a specific data value key to extract and return
	 * from the results, instead of all data. For example a path like
	 * {@code i.watts} might return a watt value.
	 * 
	 * @return bean object path to extract
	 */
	String getDataPath();

	/**
	 * Get the {@link #getDataPath()} value split into bean path elements. For
	 * example a path like {@code i.watts} would return an array like
	 * {@code ["i", "watts"]}.
	 * 
	 * @return the data path elements, or <em>null</em>
	 */
	String[] getDataPathElements();

	/**
	 * Hint that a total result count is not necessary.
	 * 
	 * <p>
	 * Setting this to {@literal true} can improve the performance of most
	 * queries, when the overall total count of results is not needed. When set,
	 * features like
	 * {@link net.solarnetwork.central.domain.FilterResults#getTotalResults()}
	 * will not be available in the results.
	 * </p>
	 * 
	 * @return {@literal true} to optimize query to omit a total result count
	 */
	boolean isWithoutTotalResultsCount();

}
