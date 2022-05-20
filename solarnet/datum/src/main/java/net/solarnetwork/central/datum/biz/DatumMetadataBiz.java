/* ==================================================================
 * DatumMetadataBiz.java - Oct 3, 2014 3:58:16 PM
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

package net.solarnetwork.central.datum.biz;

import java.util.List;
import java.util.Set;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.central.domain.LocationRequestStatus;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * API for manipulating general datum metadata.
 * 
 * @author matt
 * @version 2.1
 */
public interface DatumMetadataBiz {

	/**
	 * Add metadata to a specific node and source. If metadata already exists
	 * for the given node and source, the values will be merged such that tags
	 * are added and info values are added or updated.
	 * 
	 * @param nodeId
	 *        the node ID to add to
	 * @param sourceId
	 *        the source ID to add to
	 * @param meta
	 *        the metadata to add
	 */
	void addGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta);

	/**
	 * Store metadata to a specific node and source, replacing any existing
	 * metadata with the provided metadata.
	 * 
	 * @param nodeId
	 *        the node ID to add to
	 * @param sourceId
	 *        the source ID to add to
	 * @param meta
	 *        the metadata to store
	 */
	void storeGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta);

	/**
	 * Remove all metadata to a specific node and source.
	 * 
	 * @param nodeId
	 *        the node ID to remove from
	 * @param sourceId
	 *        the source ID to remove from
	 */
	void removeGeneralNodeDatumMetadata(Long nodeId, String sourceId);

	/**
	 * Search for datum metadata.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 */
	FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

	/**
	 * Add metadata to a specific location and source. If metadata already
	 * exists for the given location and source, the values will be merged such
	 * that tags are added and info values are added or updated.
	 * 
	 * @param locationId
	 *        the location ID to add to
	 * @param sourceId
	 *        the source ID to add to
	 * @param meta
	 *        the metadata to add
	 * @since 1.1
	 */
	void addGeneralLocationDatumMetadata(Long locationId, String sourceId, GeneralDatumMetadata meta);

	/**
	 * Store metadata to a specific location and source, replacing any existing
	 * metadata with the provided metadata.
	 * 
	 * @param locationId
	 *        the location ID to add to
	 * @param sourceId
	 *        the source ID to add to
	 * @param meta
	 *        the metadata to store
	 * @since 1.1
	 */
	void storeGeneralLocationDatumMetadata(Long locationId, String sourceId, GeneralDatumMetadata meta);

	/**
	 * Remove all metadata to a specific location and source.
	 * 
	 * @param locationId
	 *        the location ID to remove from
	 * @param sourceId
	 *        the source ID to remove from
	 * @since 1.1
	 */
	void removeGeneralLocationDatumMetadata(Long locationId, String sourceId);

	/**
	 * Search for datum metadata.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never <em>null</em>
	 * @since 1.1
	 */
	FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

	/**
	 * Find available datum source IDs that match a datum metadata filter.
	 * 
	 * The metadata filter must be expressed in LDAP search filter style, using
	 * JSON pointer style paths for keys, for example {@code (/m/foo=bar)},
	 * {@code (t=foo)}, or {@code (&(&#47;**&#47;foo=bar)(t=special))}.
	 * 
	 * @param nodeIds
	 *        the node IDs to search for
	 * @param metadataFilter
	 *        A metadata search filter, in LDAP search filter syntax.
	 * @return the distinct node ID and source IDs combinations that match the
	 *         given filter (never <em>null</em>)
	 * @since 1.2
	 */
	Set<NodeSourcePK> getGeneralNodeDatumMetadataFilteredSources(Long[] nodeIds, String metadataFilter);

	/**
	 * Find available location source IDs that match a location metadata filter.
	 * 
	 * The metadata filter must be expressed in LDAP search filter style, using
	 * JSON pointer style paths for keys, for example {@code (/m/foo=bar)},
	 * {@code (t=foo)}, or {@code (&(&#47;**&#47;foo=bar)(t=special))}.
	 * 
	 * @param locationIds
	 *        the node IDs to search for
	 * @param metadataFilter
	 *        A metadata search filter, in LDAP search filter syntax.
	 * @return the distinct node ID and source IDs combinations that match the
	 *         given filter (never <em>null</em>)
	 * @since 1.2
	 */
	Set<LocationSourcePK> getGeneralLocationDatumMetadataFilteredSources(Long[] locationIds,
			String metadataFilter);

	/**
	 * Find all available object datum stream metadata for a given search
	 * filter.
	 * 
	 * <p>
	 * The {@link ObjectStreamCriteria#getObjectKind()} determines the type of
	 * metadata returned. If not specified, {@link ObjectDatumKind#Node} will be
	 * assumed.
	 * </p>
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 * @since 1.3
	 */
	Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectStreamCriteria filter);

	/**
	 * Find all available object datum stream metadata for a given search
	 * filter.
	 * 
	 * <p>
	 * The {@link ObjectStreamCriteria#getObjectKind()} determines the type of
	 * metadata returned. If not specified, {@link ObjectDatumKind#Node} will be
	 * assumed.
	 * </p>
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 * @since 2.0
	 */
	Set<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(ObjectStreamCriteria filter);

	/**
	 * Get a set of location requests for a given user and filter.
	 * 
	 * @param userId
	 *        the user ID to get requests for
	 * @param filter
	 *        the search criteria
	 * @param sortDescriptors
	 *        optional sort descriptors
	 * @param offset
	 *        optional starting offset
	 * @param max
	 *        optional max number of results
	 * @return the results, never {@literal null}
	 * @since 2.1
	 */
	net.solarnetwork.dao.FilterResults<LocationRequest, Long> findLocationRequests(Long userId,
			LocationRequestCriteria filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max);

	/**
	 * Get a specific location request.
	 * 
	 * @param userId
	 *        the user ID to get the request for
	 * @param id
	 *        the request ID
	 * @return the matching entity, or {@literal null} if not found
	 * @since 2.1
	 */
	LocationRequest getLocationRequest(Long userId, Long id);

	/**
	 * Submit a location request.
	 * 
	 * @param userId
	 *        the user ID
	 * @param info
	 *        the request info
	 * @return the request entity
	 */
	LocationRequest submitLocationRequest(Long userId, LocationRequestInfo info);

	/**
	 * Delete a specific location request.
	 * 
	 * <p>
	 * Only requests in the {@link LocationRequestStatus#Submitted} state can be
	 * removed.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID of the request to delete
	 * @param id
	 *        the ID of the request to delete
	 * @since 2.1
	 */
	void removeLocationRequest(Long userId, Long id);

	/**
	 * Update an existing location request.
	 * 
	 * <p>
	 * Only requests in the {@link LocationRequestStatus#Submitted} state can be
	 * updated.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID of the request to save
	 * @param id
	 *        the ID of the request to update
	 * @param request
	 *        the request details to save; the {@code id} property must be
	 *        specified
	 * @return the saved request details
	 * @since 2.1
	 */
	LocationRequest updateLocationRequest(Long userId, Long id, LocationRequestInfo info);

}
