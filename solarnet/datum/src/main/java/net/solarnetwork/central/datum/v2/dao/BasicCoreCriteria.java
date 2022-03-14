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

import java.util.Arrays;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.SimplePagination;

/**
 * Basic implementation of some core criteria APIs.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicCoreCriteria extends SimplePagination implements PaginationCriteria, LocationCriteria,
		NodeCriteria, SourceCriteria, UserCriteria, SecurityTokenCriteria, SearchFilterCriteria {

	private Long[] locationIds;
	private Long[] nodeIds;
	private String[] sourceIds;
	private Long[] userIds;
	private String[] tokenIds;
	private SimpleLocation location;
	private String searchFilter;

	@Override
	public BasicCoreCriteria clone() {
		BasicCoreCriteria result = (BasicCoreCriteria) super.clone();
		if ( location != null ) {
			result.location = location.clone();
		}
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(locationIds);
		result = prime * result + Arrays.hashCode(nodeIds);
		result = prime * result + Arrays.hashCode(sourceIds);
		result = prime * result + Arrays.hashCode(tokenIds);
		result = prime * result + Arrays.hashCode(userIds);
		result = prime * result + Objects.hashCode(location);
		result = prime * result + Objects.hashCode(searchFilter);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicCoreCriteria) ) {
			return false;
		}
		BasicCoreCriteria other = (BasicCoreCriteria) obj;
		return Arrays.equals(locationIds, other.locationIds) && Arrays.equals(nodeIds, other.nodeIds)
				&& Arrays.equals(sourceIds, other.sourceIds) && Arrays.equals(tokenIds, other.tokenIds)
				&& Arrays.equals(userIds, other.userIds) && Objects.equals(location, other.location)
				&& Objects.equals(searchFilter, other.searchFilter);
	}

	/**
	 * Copy the properties of another criteria into this instance.
	 * 
	 * <p>
	 * This method will test for conformance to all the various criteria
	 * interfaces implemented by this class, and copy those properties as well.
	 * </p>
	 * 
	 * @param criteria
	 *        the criteria to copy
	 */
	public void copyFrom(PaginationCriteria criteria) {
		setMax(criteria.getMax());
		setOffset(criteria.getOffset());
		setSorts(criteria.getSorts());
		if ( criteria instanceof BasicCoreCriteria ) {
			BasicCoreCriteria bcc = (BasicCoreCriteria) criteria;
			setLocationIds(bcc.getLocationIds());
			setLocation(bcc.getLocation());
			setNodeIds(bcc.getNodeIds());
			setSourceIds(bcc.getSourceIds());
			setUserIds(bcc.getUserIds());
			setTokenIds(bcc.getTokenIds());
			setSearchFilter(bcc.getSearchFilter());
		} else {
			if ( criteria instanceof LocationCriteria ) {
				LocationCriteria lc = (LocationCriteria) criteria;
				setLocationIds(lc.getLocationIds());
				setLocation(SimpleLocation.locationValue(lc.getLocation()));
			}
			if ( criteria instanceof NodeCriteria ) {
				setNodeIds(((NodeCriteria) criteria).getNodeIds());
			}
			if ( criteria instanceof SourceCriteria ) {
				setSourceIds(((SourceCriteria) criteria).getSourceIds());
			}
			if ( criteria instanceof UserCriteria ) {
				setUserIds(((UserCriteria) criteria).getUserIds());
			}
			if ( criteria instanceof SecurityTokenCriteria ) {
				setTokenIds(((SecurityTokenCriteria) criteria).getTokenIds());
			}
			if ( criteria instanceof SearchFilterCriteria ) {
				setSearchFilter(((SearchFilterCriteria) criteria).getSearchFilter());
			}
		}
	}

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

	/**
	 * Set a single user ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single user ID at a
	 * time. The user ID is still stored on the {@code userIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code userIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param userId
	 *        the ID of the user
	 */
	@JsonSetter
	public void setUserId(Long userId) {
		this.userIds = (userId == null ? null : new Long[] { userId });
	}

	@Override
	@JsonIgnore
	public Long getUserId() {
		return (this.userIds == null || this.userIds.length < 1 ? null : this.userIds[0]);
	}

	@Override
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
	 * Set a single token ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single token ID at a
	 * time. The token ID is still stored on the {@code tokenIds} array, as the
	 * first value. Calling this method replaces any existing {@code tokenIds}
	 * value with a new array containing just the ID passed into this method.
	 * </p>
	 * 
	 * @param tokenId
	 *        the token ID
	 */
	@JsonSetter
	public void setTokenId(String tokenId) {
		setTokenIds(tokenId == null ? null : new String[] { tokenId });
	}

	@Override
	@JsonIgnore
	public String getTokenId() {
		return (this.tokenIds == null || this.tokenIds.length < 1 ? null : this.tokenIds[0]);
	}

	@Override
	public String[] getTokenIds() {
		return tokenIds;
	}

	/**
	 * Set the token IDs.
	 * 
	 * @param tokenIds
	 *        the tokenIds to set
	 */
	public void setTokenIds(String[] tokenIds) {
		this.tokenIds = tokenIds;
	}

	@Override
	public SimpleLocation getLocation() {
		return location;
	}

	/**
	 * Set the location geographic criteria.
	 * 
	 * @param location
	 *        the location to set
	 */
	public void setLocation(SimpleLocation location) {
		this.location = location;
	}

	@Override
	public String getSearchFilter() {
		return searchFilter;
	}

	/**
	 * Set the search filter.
	 * 
	 * @param searchFilter
	 *        the filter to set
	 */
	public void setSearchFilter(String searchFilter) {
		this.searchFilter = searchFilter;
	}

}
