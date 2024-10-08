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

import java.util.Map;
import java.util.UUID;
import net.solarnetwork.central.datum.domain.ObjectSourcePK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * DAO API for datum stream metadata.
 *
 * @author matt
 * @version 2.3
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
	 * <p>
	 * The {@link ObjectStreamCriteria#getObjectKind()} determines the type of
	 * metadata returned. If not specified, {@link ObjectDatumKind#Node} will be
	 * assumed.
	 * </p>
	 *
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 */
	Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectStreamCriteria filter);

	/**
	 * Find all available object datum stream metadata IDs for a given search
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
	 * @param filter
	 *        the search filter
	 * @return the matching results, never {@literal null}
	 * @since 2.0
	 */
	Iterable<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(ObjectStreamCriteria filter);

	/**
	 * Get a mapping of stream IDs to {@link ObjectDatumStreamMetadataId}
	 * instances.
	 *
	 * @param streamIds
	 *        the stream IDs to look up
	 * @return the mapping, never {@literal null}
	 * @since 2.2
	 */
	Map<UUID, ObjectDatumStreamMetadataId> getDatumStreamMetadataIds(UUID... streamIds);

	/**
	 * Replace the JSON metadata associated with an object datum stream.
	 *
	 * @param id
	 *        the ID of the stream to update
	 * @param json
	 *        the new JSON, or {@literal null} to remove
	 */
	void replaceJsonMeta(ObjectSourcePK id, String json);

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
	 * @return the updated stream metadata ID, or {@literal null} if the
	 *         metadata was not updated
	 * @throws IllegalArgumentException
	 *         if either {@code kind} or {@code streamId} is {@literal null} or
	 *         both {@code objectId} and {@code sourceId} are {@literal null}
	 * @since 2.1
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
	 * @since 2.3
	 */
	ObjectDatumStreamMetadata updateAttributes(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties);

}
