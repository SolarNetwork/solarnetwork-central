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

import java.util.List;
import java.util.UUID;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * API for manipulating datum stream metadata.
 *
 * @author matt
 * @version 1.1
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
	 */
	ObjectDatumStreamMetadataId updateIdAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId);

	/**
	 * Update the object and/or source IDs associated with a stream.
	 *
	 * <p>
	 * Note that if any of the {@code *Properties} arguments are provided, their
	 * length must be at least as long as the current values or else no change
	 * will be saved.
	 * </p>
	 *
	 * @param kind
	 *        the stream kind
	 * @param streamId
	 *        the ID of the stream metadata to update
	 * @param objectId
	 *        the object ID to set, or {@literal null} to keep unchanged
	 * @param sourceId
	 *        the source ID to set, or {@literal null} to keep unchanged
	 * @param instantaneousProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param accumulatingProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param statusProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @return the updated stream metadata, or {@literal null} if the metadata
	 *         was not updated
	 * @throws IllegalArgumentException
	 *         if either {@code kind} or {@code streamId} is {@literal null} or
	 *         all other arguments are {@literal null}
	 * @since 1.1
	 */
	ObjectDatumStreamMetadata updateAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties);

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
	 * @param actor
	 *        the actor to find metadata for
	 * @param criteria
	 *        the search criteria
	 * @return the matching results, never {@literal null}
	 */
	List<ObjectDatumStreamMetadata> findDatumStreamMetadata(SecurityActor actor,
			ObjectStreamCriteria criteria);

	/**
	 * FInd all available object datum stream metadata IDs for a given search
	 * filter.
	 *
	 * <p>
	 * The {@link ObjectStreamCriteria#getObjectKind()} determines the type of
	 * metadata returned. If not specified, {@link ObjectDatumKind#Node} will be
	 * assumed.
	 * </p>
	 *
	 * <p>
	 * This can be more efficient than
	 * {@link #findDatumStreamMetadata(ObjectStreamCriteria)} when all you need
	 * are the IDs.
	 * </p>
	 *
	 * @param actor
	 *        the actor to find metadata for
	 * @param criteria
	 *        the search criteria
	 * @return the matching results, never {@literal null}
	 */
	List<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(SecurityActor actor,
			ObjectStreamCriteria criteria);

}
