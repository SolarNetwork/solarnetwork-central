/* ==================================================================
 * BasicCoreCriteria.java - 27/10/2020 7:43:38 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Basic implementation of some core criteria APIs.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCoreCriteria implements LocationCriteria, NodeCriteria, SourceCriteria {

	private Long[] locationIds;
	private Long[] nodeIds;
	private String[] sourceIds;

	/**
	 * Set a single location ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single location ID
	 * at a time. The location ID is still stored on the {@code locationIds}
	 * array, as the first value. Calling this method replaces any existing
	 * {@code locationIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param locationId
	 *        the ID of the location
	 */
	@JsonSetter
	public void setLocationId(Long locationId) {
		setLocationIds(locationId == null ? null : new Long[] { locationId });
	}

	@Override
	@JsonIgnore
	public Long getLocationId() {
		return (locationIds != null && locationIds.length > 0 ? locationIds[0] : null);
	}

	@Override
	public Long[] getLocationIds() {
		return locationIds;
	}

	/**
	 * Set the location IDs.
	 * 
	 * @param locationIds
	 *        the location IDs to set
	 */
	public void setLocationIds(Long[] locationIds) {
		this.locationIds = locationIds;
	}

	/**
	 * Set a single node ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single node ID at a
	 * time. The node ID is still stored on the {@code nodeIds} array, as the
	 * first value. Calling this method replaces any existing {@code nodeIds}
	 * value with a new array containing just the ID passed into this method.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	@JsonSetter
	public void setNodeId(Long nodeId) {
		setNodeIds(nodeId == null ? null : new Long[] { nodeId });
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
	 * Set the node IDs.
	 * 
	 * @param nodeIds
	 *        the nodeIds to set
	 */
	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Set a single source ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single source ID at
	 * a time. The source ID is still stored on the {@code sourceIds} array, as
	 * the first value. Calling this method replaces any existing
	 * {@code sourceIds} value with a new array containing just the ID passed
	 * into this method.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID
	 */
	@JsonSetter
	public void setSourceId(String sourceId) {
		setSourceIds(sourceId == null ? null : new String[] { sourceId });
	}

	@Override
	@JsonIgnore
	public String getSourceId() {
		return (this.sourceIds == null || this.sourceIds.length < 1 ? null : this.sourceIds[0]);
	}

	@Override
	public String[] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 * 
	 * @param sourceIds
	 *        the sourceIds to set
	 */
	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

}
