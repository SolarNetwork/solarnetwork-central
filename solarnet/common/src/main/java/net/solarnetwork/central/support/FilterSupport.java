/* ==================================================================
 * FilterSupport.java - 11/11/2016 12:47:05 PM
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

package net.solarnetwork.central.support;

import java.util.Arrays;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.SolarNodeFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;

/**
 * Supporting base class for {@link Filter} implementations.
 * 
 * @author matt
 * @version 1.4
 * @since 1.32
 */
public class FilterSupport extends BaseFilterSupport
		implements SolarNodeFilter, SolarNodeMetadataFilter {

	private static final long serialVersionUID = -4826724231965486643L;

	private Long[] locationIds;
	private Long[] nodeIds;
	private String[] sourceIds;

	@Override
	public Map<String, ?> getFilter() {
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>) super.getFilter();
		if ( locationIds != null ) {
			filter.put("locationIds", locationIds);
		}
		if ( nodeIds != null ) {
			filter.put("nodeIds", nodeIds);
		}
		if ( sourceIds != null ) {
			filter.put("sourceIds", sourceIds);
		}
		return filter;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FilterSupport{");
		if ( nodeIds != null ) {
			builder.append("nodeIds=");
			builder.append(Arrays.toString(nodeIds));
			builder.append(", ");
		}
		if ( sourceIds != null ) {
			builder.append("sourceIds=");
			builder.append(Arrays.toString(sourceIds));
			builder.append(", ");
		}
		if ( locationIds != null ) {
			builder.append("locationIds=");
			builder.append(Arrays.toString(locationIds));
			builder.append(", ");
		}
		if ( getUserIds() != null ) {
			builder.append("userIds=");
			builder.append(Arrays.toString(getUserIds()));
			builder.append(", ");
		}
		if ( getTags() != null ) {
			builder.append("tags=");
			builder.append(Arrays.toString(getTags()));
			builder.append(", ");
		}
		if ( getMetadataFilter() != null ) {
			builder.append("metadataFilter=");
			builder.append(getMetadataFilter());
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Set a single node ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single node ID at a
	 * time. The node ID is still stored on the {@code nodeIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code nodeIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	@JsonSetter
	public void setNodeId(Long nodeId) {
		this.nodeIds = new Long[] { nodeId };
	}

	@JsonIgnore
	@Override
	public Long getNodeId() {
		return (this.nodeIds == null || this.nodeIds.length < 1 ? null : this.nodeIds[0]);
	}

	@Override
	public Long[] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set a list of node IDs to filter on.
	 * 
	 * @param nodeIds
	 *        The nodeIds IDs to filter on.
	 */
	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Set a single source ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single source ID at
	 * a time. The source ID is still stored on the {@code sourceIds} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code sourceIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 */
	@JsonSetter
	public void setSourceId(String sourceId) {
		this.sourceIds = (sourceId == null ? null : new String[] { sourceId });
	}

	/**
	 * Get the first source ID.
	 * 
	 * <p>
	 * This returns the first available source ID from the {@code sourceIds}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first source ID, or {@literal null}
	 */
	@JsonIgnore
	public String getSourceId() {
		return (this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0]);
	}

	/**
	 * Get all source IDs to filter on.
	 * 
	 * @return The source IDs, or {@literal null}.
	 */
	public String[] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set a list of source IDs to filter on.
	 * 
	 * @param sourceIds
	 *        The source IDs to filter on.
	 */
	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Set a single location ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single location ID
	 * at a time. The location ID is still stored on the {@code locationIds}
	 * array, just as the first value. Calling this method replaces any existing
	 * {@code locationIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param locationId
	 *        the ID of the location
	 */
	@JsonSetter
	public void setLocationId(Long locationId) {
		this.locationIds = (locationId == null ? null : new Long[] { locationId });
	}

	/**
	 * Get the first location ID.
	 * 
	 * This returns the first available location ID from the {@code locationIds}
	 * array, or {@literal null} if not available.
	 * 
	 * @return the first location ID, or {@literal null}
	 */
	@JsonIgnore
	public Long getLocationId() {
		return (locationIds != null && locationIds.length > 0 ? locationIds[0] : null);
	}

	/**
	 * Get all location IDs to filter on.
	 * 
	 * @return The location IDs, or {@literal null}.
	 */
	public Long[] getLocationIds() {
		return locationIds;
	}

	/**
	 * Set a list of location IDs to filter on.
	 * 
	 * @param locationIds
	 *        The location IDs to filter on.
	 */
	public void setLocationIds(Long[] locationIds) {
		this.locationIds = locationIds;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(locationIds);
		result = prime * result + Arrays.hashCode(nodeIds);
		result = prime * result + Arrays.hashCode(sourceIds);
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof FilterSupport) ) {
			return false;
		}
		FilterSupport other = (FilterSupport) obj;
		return Arrays.equals(locationIds, other.locationIds) && Arrays.equals(nodeIds, other.nodeIds)
				&& Arrays.equals(sourceIds, other.sourceIds);
	}

}
