/* ==================================================================
 * SolarNodeMetadataBiz.java - 11/11/2016 11:12:50 AM
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

package net.solarnetwork.central.biz;

import java.util.List;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadataFilter;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for manipulating node metadata.
 * 
 * @author matt
 * @version 2.0
 * @since 1.32
 */
public interface SolarNodeMetadataBiz {

	/**
	 * Add metadata to a specific node. If metadata already exists for the given
	 * node, the values will be merged such that tags are added and info values
	 * are added or updated.
	 * 
	 * @param nodeId
	 *        the node ID to add to
	 * @param meta
	 *        the metadata to add
	 */
	void addSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta);

	/**
	 * Store metadata to a specific node, replacing any existing metadata with
	 * the provided metadata.
	 * 
	 * @param nodeId
	 *        the node ID to add to
	 * @param meta
	 *        the metadata to store
	 */
	void storeSolarNodeMetadata(Long nodeId, GeneralDatumMetadata meta);

	/**
	 * Remove all metadata to a specific node.
	 * 
	 * @param nodeId
	 *        the node ID to remove from
	 */
	void removeSolarNodeMetadata(Long nodeId);

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
	 */
	FilterResults<SolarNodeMetadataFilterMatch> findSolarNodeMetadata(SolarNodeMetadataFilter criteria,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);

}
