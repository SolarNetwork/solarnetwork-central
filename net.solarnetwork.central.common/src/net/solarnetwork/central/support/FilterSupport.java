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

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;

/**
 * Supporting base class for {@link Filter} implementations.
 * 
 * @author matt
 * @version 1.1
 * @since 1.32
 */
public class FilterSupport implements Filter, Serializable, SolarNodeMetadataFilter {

	private static final long serialVersionUID = 912502801129411927L;

	private Long[] locationIds;
	private Long[] nodeIds;
	private String[] sourceIds;
	private Long[] userIds;
	private String[] tags;

	@Override
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<String, Object>();
		if ( locationIds != null ) {
			filter.put("locationIds", locationIds);
		}
		if ( nodeIds != null ) {
			filter.put("nodeIds", nodeIds);
		}
		if ( sourceIds != null ) {
			filter.put("sourceIds", sourceIds);
		}
		if ( tags != null ) {
			filter.put("tags", tags);
		}
		return filter;
	}

	/**
	 * Set a single node ID.
	 * 
	 * This is a convenience method for requests that use a single node ID at a
	 * time. The node ID is still stored on the {@code nodeIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code nodeIds} value with a new array containing just the ID passed into
	 * this method.
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	public void setNodeId(Long nodeId) {
		this.nodeIds = new Long[] { nodeId };
	}

	/**
	 * Get the first node ID.
	 * 
	 * This returns the first available node ID from the {@code nodeIds} array,
	 * or {@code null} if not available.
	 * 
	 * @return the first node ID, or {@code null}
	 */
	@Override
	public Long getNodeId() {
		return (this.nodeIds == null || this.nodeIds.length < 1 ? null : this.nodeIds[0]);
	}

	/**
	 * Get all node IDs to filter on.
	 * 
	 * @return The node IDs, or {@code null}.
	 */
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
	 * This is a convenience method for requests that use a single source ID at
	 * a time. The source ID is still stored on the {@code sourceIds} array,
	 * just as the first value. Calling this method replaces any existing
	 * {@code sourceIds} value with a new array containing just the ID passed
	 * into this method.
	 * 
	 * @param sourceId
	 *        the source ID
	 */
	public void setSourceId(String sourceId) {
		this.sourceIds = (sourceId == null ? null : new String[] { sourceId });
	}

	/**
	 * Get the first source ID.
	 * 
	 * This returns the first available source ID from the {@code sourceIds}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first source ID, or {@code null}
	 */
	public String getSourceId() {
		return (this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0]);
	}

	/**
	 * Get all source IDs to filter on.
	 * 
	 * @return The source IDs, or {@code null}.
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
	 * Get the first tag.
	 * 
	 * This returns the first available tag from the {@code tags} array, or
	 * {@code null} if not available.
	 * 
	 * @return the first tag, or {@code null}
	 */
	@Override
	public String getTag() {
		return (this.tags == null || this.tags.length < 1 ? null : this.tags[0]);
	}

	/**
	 * Set a single tag.
	 * 
	 * This is a convenience method for requests that use a single tag at a
	 * time. The tag is still stored on the {@code tags} array, just as the
	 * first value. Calling this method replaces any existing {@code tags} value
	 * with a new array containing just the tag passed into this method.
	 * 
	 * @param tag
	 *        the tag
	 */
	public void setTag(String tag) {
		this.tags = (tag == null ? null : new String[] { tag });
	}

	/**
	 * Get all tags to filter on.
	 * 
	 * @return The tags, or {@code null}.
	 */
	@Override
	public String[] getTags() {
		return tags;
	}

	public void setTags(String[] tags) {
		this.tags = tags;
	}

	/**
	 * Set a single location ID.
	 * 
	 * This is a convenience method for requests that use a single location ID
	 * at a time. The location ID is still stored on the {@code locationIds}
	 * array, just as the first value. Calling this method replaces any existing
	 * {@code locationIds} value with a new array containing just the ID passed
	 * into this method.
	 * 
	 * @param locationId
	 *        the ID of the location
	 */
	public void setLocationId(Long locationId) {
		this.locationIds = (locationId == null ? null : new Long[] { locationId });
	}

	/**
	 * Get the first location ID.
	 * 
	 * This returns the first available location ID from the {@code locationIds}
	 * array, or {@code null} if not available.
	 * 
	 * @return the first location ID, or {@code null}
	 */
	public Long getLocationId() {
		return (locationIds != null && locationIds.length > 0 ? locationIds[0] : null);
	}

	/**
	 * Get all location IDs to filter on.
	 * 
	 * @return The location IDs, or {@code null}.
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
	 * Set a single user ID.
	 * 
	 * This is a convenience method for requests that use a single user ID at a
	 * time. The user ID is still stored on the {@code userIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code userIds} value with a new array containing just the ID passed into
	 * this method.
	 * 
	 * @param userId
	 *        the ID of the user
	 */
	public void setUserId(Long userId) {
		this.userIds = (userId == null ? null : new Long[] { userId });
	}

	/**
	 * Get the first user ID.
	 * 
	 * This returns the first available user ID from the {@code userIds} array,
	 * or {@code null} if not available.
	 * 
	 * @return the first user ID, or {@code null}
	 */
	public Long getUserId() {
		return (this.userIds == null || this.userIds.length < 1 ? null : this.userIds[0]);
	}

	/**
	 * Get all user IDs to filter on.
	 * 
	 * @return The user IDs, or {@code null}.
	 */
	public Long[] getUserIds() {
		return userIds;
	}

	/**
	 * Set a list of user IDs to filter on.
	 * 
	 * @param userIds
	 *        The user IDs to filter on.
	 */
	public void setUserIds(Long[] userIds) {
		this.userIds = userIds;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(locationIds);
		result = prime * result + Arrays.hashCode(nodeIds);
		result = prime * result + Arrays.hashCode(sourceIds);
		result = prime * result + Arrays.hashCode(tags);
		result = prime * result + Arrays.hashCode(userIds);
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
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof FilterSupport) ) {
			return false;
		}
		FilterSupport other = (FilterSupport) obj;
		if ( !Arrays.equals(locationIds, other.locationIds) ) {
			return false;
		}
		if ( !Arrays.equals(nodeIds, other.nodeIds) ) {
			return false;
		}
		if ( !Arrays.equals(sourceIds, other.sourceIds) ) {
			return false;
		}
		if ( !Arrays.equals(tags, other.tags) ) {
			return false;
		}
		if ( !Arrays.equals(userIds, other.userIds) ) {
			return false;
		}
		return true;
	}

}
