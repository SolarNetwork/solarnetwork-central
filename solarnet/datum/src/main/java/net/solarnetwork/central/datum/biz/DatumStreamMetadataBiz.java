/* ==================================================================
 * DatumStreamMetadataBiz.java - 21/11/2021 5:46:53 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.UUID;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * API for manipulating datum stream metadata.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface DatumStreamMetadataBiz {

	/**
	 * Update the object and/or source IDs associated with a stream.
	 * 
	 * @param kind
	 *        the stream kind
	 * @param streamId
	 *        the ID of the stream metadata to update
	 * @param objectId
	 *        the object ID to set, or {@literal null} to keep unchanged
	 * @param sourceId
	 *        the source ID to set, or {@literal null} to keep unchanged
	 * @return the updated stream metadata ID
	 * @throws IllegalArgumentException
	 *         if either {@code kind} or {@code streamId} is {@literal null} or
	 *         both {@code objectId} and {@code sourceId} are {@literal null}
	 * @since 2.1
	 */
	ObjectDatumStreamMetadataId updateIdAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId);

}
