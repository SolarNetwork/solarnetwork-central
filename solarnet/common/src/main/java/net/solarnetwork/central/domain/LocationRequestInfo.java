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
import org.jspecify.annotations.Nullable;
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

	private @Nullable Long locationId;
	private @Nullable Location location;
	private @Nullable String sourceId;
	private @Nullable Set<String> features;

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
	public final @Nullable Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the location ID.
	 * 
	 * @param locationId
	 *        the locationId to set
	 */
	public final void setLocationId(@Nullable Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location
	 */
	public final @Nullable Location getLocation() {
		return location;
	}

	/**
	 * Set the location.
	 * 
	 * @param location
	 *        the location to set
	 */
	public final void setLocation(@Nullable Location location) {
		this.location = location;
	}

	/**
	 * Get the source ID.
	 * 
	 * @return the sourceId
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID.
	 * 
	 * @param sourceId
	 *        the sourceId to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the features.
	 * 
	 * @return the features
	 */
	public final @Nullable Set<String> getFeatures() {
		return features;
	}

	/**
	 * Set the features.
	 * 
	 * @param features
	 *        the features to set
	 */
	public final void setFeatures(@Nullable Set<String> features) {
		this.features = features;
	}

}
