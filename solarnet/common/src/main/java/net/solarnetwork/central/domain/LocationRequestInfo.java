/* ==================================================================
 * LocationRequestInfo.java - 20/05/2022 9:24:15 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.util.Set;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.Location;

/**
 * Location request information.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
@JsonPropertyOrder({ "locationId", "sourceId", "features", "location" })
public class LocationRequestInfo implements Cloneable {

	private Long locationId;
	private Location location;
	private String sourceId;
	private Set<String> features;

	@Override
	public LocationRequestInfo clone() {
		try {
			return (LocationRequestInfo) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should not be here
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the location ID.
	 * 
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the location ID.
	 * 
	 * @param locationId
	 *        the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Set the location.
	 * 
	 * @param location
	 *        the location to set
	 */
	public void setLocation(Location location) {
		this.location = location;
	}

	/**
	 * Get the source ID.
	 * 
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the features.
	 * 
	 * @return the features
	 */
	public Set<String> getFeatures() {
		return features;
	}

	/**
	 * Set the features.
	 * 
	 * @param features
	 *        the features to set
	 */
	public void setFeatures(Set<String> features) {
		this.features = features;
	}

}
