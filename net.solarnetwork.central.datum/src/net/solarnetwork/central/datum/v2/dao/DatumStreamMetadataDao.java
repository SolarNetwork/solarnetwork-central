/* ==================================================================
 * DatumStreamMetadataDao.java - 17/11/2020 10:02:05 am
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

import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * DAO API for datum stream metadata.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumStreamMetadataDao {

	/**
	 * Get the metadata associated with a datum stream.
	 * 
	 * @param filter
	 *        the search filter
	 * @return the metadata, or {@literal null} if not available
	 */
	ObjectDatumStreamMetadata findStreamMetadata(StreamMetadataCriteria filter);

	/**
	 * Find all available object datum stream metadata for a given search
	 * filter.
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 */
	Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectMetadataCriteria filter);

	/**
	 * Find all available node datum stream metadata for a given search filter.
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 */
	Iterable<NodeDatumStreamMetadata> findNodeDatumStreamMetadata(NodeMetadataCriteria filter);

	/**
	 * Find all available location datum stream metadata for a given search
	 * filter.
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 */
	Iterable<LocationDatumStreamMetadata> findLocationDatumStreamMetadata(
			LocationMetadataCriteria filter);

}
