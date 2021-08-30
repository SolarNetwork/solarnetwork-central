/* ===================================================================
 * DataCollectorBiz.java
 * 
 * Created Aug 31, 2008 3:34:46 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.central.in.biz;

import java.util.List;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * API for collecting data from solar nodes.
 * 
 * <p>
 * Serves as a transactional facade to posting data into central system.
 * </p>
 * 
 * @author matt
 * @version 2.2
 */
public interface DataCollectorBiz {

	/**
	 * Post a collection of {@link GeneralNodeDatum} in a single transaction.
	 * 
	 * @param datums
	 *        the collection of datums
	 */
	void postGeneralNodeDatum(Iterable<GeneralNodeDatum> datums);

	/**
	 * Post a collection of {@link GeneralLocationDatum} in a single
	 * transaction.
	 * 
	 * @param datums
	 *        the collection of datums
	 * @since 1.3
	 */
	void postGeneralLocationDatum(Iterable<GeneralLocationDatum> datums);

	/**
	 * Post a collection of {@link StreamDatum} in a single transaction.
	 * 
	 * @param datums
	 *        the collection of datums
	 * @since 2.1
	 */
	void postStreamDatum(Iterable<StreamDatum> datums);

	/**
	 * Update a node's own location.
	 * 
	 * <p>
	 * The properties allowed on the given location is implementation specific.
	 * Typically this method is expected to support a node updating its own GPS
	 * coordinates.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID to update
	 * @param location
	 *        the location information to update on the node
	 * @since 2.2
	 */
	void updateLocation(Long nodeId, Location location);

	/**
	 * Add metadata to a specific node and source. If metadata already exists
	 * for the given node and source, the values will be merged such that tags
	 * are only added and only new info values will be added.
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
	 * Add metadata to a specific node.
	 * 
	 * <p>
	 * If metadata already exists for the given node and source, the values will
	 * be merged such that tags are only added and only new info values will be
	 * added.
	 * </p>
	 * 
	 * @param nodeId
	 *        the node ID to add to
	 * @param meta
	 *        the metadata to add
	 * @since 1.5
	 */
	void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta);

	/**
	 * Search for node metadata.
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
	 * @since 1.5
	 */
	FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(SolarNodeMetadataFilter criteria,
			final List<SortDescriptor> sortDescriptors, final Integer offset, final Integer max);

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
	 * Search for location datum metadata based on a location criteria. The
	 * location and metadata criteria must both match for results to be
	 * included.
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
	 * @since 1.3
	 */
	FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max);

	/**
	 * Look up location objects based on a location search filter.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @return the matching locations, or an empty list if none found
	 */
	List<LocationMatch> findLocations(Location criteria);

}
