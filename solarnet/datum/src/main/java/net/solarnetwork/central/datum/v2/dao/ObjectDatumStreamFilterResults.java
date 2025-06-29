/* ==================================================================
 * ObjectDatumStreamFilterResults.java - 1/12/2020 12:00:54 pm
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

import java.util.UUID;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;

/**
 * A collection of filtered results with associated stream metadata.
 *
 * <p>
 * This result API is used when querying for datum streams. The results can
 * include datum from different streams; the {@link #metadataForStreamId(UUID)}
 * method provides a way to access the metadata for the streams included in the
 * results.
 * </p>
 *
 * @author matt
 * @version 2.0
 * @since 2.8
 */
public interface ObjectDatumStreamFilterResults<M extends Identity<K>, K extends Comparable<K>>
		extends FilterResults<M, K>, ObjectDatumStreamMetadataProvider {

	@Override
	default ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId) {
		for ( UUID streamId : metadataStreamIds() ) {
			ObjectDatumStreamMetadata meta = metadataForStreamId(streamId);
			if ( meta != null && meta.getObjectId().equals(objectId)
					&& meta.getSourceId().equals(sourceId) ) {
				return meta;
			}
		}
		return null;
	}

}
