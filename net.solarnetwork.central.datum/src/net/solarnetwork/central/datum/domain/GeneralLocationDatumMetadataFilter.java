/* ==================================================================
 * GeneralLocationDatumMetadataFilter.java - Oct 3, 2014 10:21:42 AM
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

/**
 * Filter API for {@link GeneralLocationDatumMetadata}.
 * 
 * @author matt
 * @version 1.0
 */
public interface GeneralLocationDatumMetadataFilter extends Filter {

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
	 * Get the first tag. This returns the first available tag from the
	 * {@link #getTags()} array, or <em>null</em> if not available.
	 * 
	 * @return the first tag, or <em>null</em> if not available
	 */
	public String getTag();

	/**
	 * Get an array of tags.
	 * 
	 * @return array of tags (may be <em>null</em>)
	 */
	public String[] getTags();

}
