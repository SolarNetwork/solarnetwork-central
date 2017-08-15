/* ==================================================================
 * GeneralReportableIntervalCommand.java - Aug 28, 2014 6:06:46 AM
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

package net.solarnetwork.central.query.web.domain;

import org.joda.time.DateTime;

/**
 * Command for general reportable interval queries.
 * 
 * @author matt
 * @version 1.3
 */
public class GeneralReportableIntervalCommand {

	private Long locationId;
	private Long[] nodeIds;
	private String sourceId;
	private DateTime startDate;
	private DateTime endDate;
	private String metadataFilter;

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
	public void setNodeId(Long nodeId) {
		this.nodeIds = new Long[] { nodeId };
	}

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This returns the first available node ID from the {@code nodeIds} array,
	 * or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first node ID
	 */
	public Long getNodeId() {
		return this.nodeIds == null || this.nodeIds.length < 1 ? null : this.nodeIds[0];
	}

	/**
	 * Get the node IDs.
	 * 
	 * @return The node IDs.
	 * @since 1.2
	 */
	public Long[] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 * 
	 * @param nodeIds
	 *        The node IDs to set.
	 * @since 1.2
	 */
	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Set the start date.
	 * 
	 * @param start
	 *        the start date
	 * @deprecated use {@link #setStartDate(DateTime)}
	 */
	@Deprecated
	public void setStart(DateTime start) {
		setStartDate(start);
	}

	/**
	 * Set the end date.
	 * 
	 * @param end
	 *        the end date
	 * @deprecated use {@link #setEndDate(DateTime)}
	 */
	@Deprecated
	public void setEnd(DateTime end) {
		setEndDate(end);
	}

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 * @since 1.3
	 */
	public DateTime getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 * 
	 * @param startDate
	 *        the start date to set
	 * @since 1.3
	 */
	public void setStartDate(DateTime startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the end date.
	 * 
	 * @return the end date
	 * @since 1.3
	 */
	public DateTime getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 * 
	 * @param endDate
	 *        the end date to set
	 * @since 1.3
	 */
	public void setEndDate(DateTime endDate) {
		this.endDate = endDate;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	/**
	 * Get the LDAP style metadata filter.
	 * 
	 * @return The configured filter.
	 * @since 1.2
	 */
	public String getMetadataFilter() {
		return metadataFilter;
	}

	/**
	 * Set the LDAP style metadata filter.
	 * 
	 * @param metadataFilter
	 *        The filter to set.
	 * @since 1.2
	 */
	public void setMetadataFilter(String metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

}
