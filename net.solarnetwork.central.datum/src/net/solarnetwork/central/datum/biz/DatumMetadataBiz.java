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
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * API for manipulating general datum metadata.
 * 
 * @author matt
 * @version 1.1
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

}
