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

import net.solarnetwork.central.domain.Location;

/**
 * Filter API for {@link GeneralLocationDatumMetadata}.
 * 
 * @author matt
 * @version 1.1
 */
public interface GeneralLocationDatumMetadataFilter extends GeneralDatumMetadataFilter {

	/**
	 * Get a location filter to restrict the results to. This provides a way to
	 * query for {@link GeneralLocationDatumMetadata} indirectly that match a
	 * {@link Location} criteria, for example all metadata that match the region
	 * <em>Wellington</em>.
	 * 
	 * @return the location filter
	 */
	public Location getLocation();

	/**
	 * Get the first location ID. This returns the first available location ID
	 * from the {@link #getLocationIds()} array, or {@literal null} if not
	 * available.
	 * 
	 * @return the location ID, or {@literal null} if not available
	 */
	public Long getLocationId();

	/**
	 * Get an array of location IDs.
	 * 
	 * @return array of location IDs (may be {@literal null})
	 */
	public Long[] getLocationIds();

}
