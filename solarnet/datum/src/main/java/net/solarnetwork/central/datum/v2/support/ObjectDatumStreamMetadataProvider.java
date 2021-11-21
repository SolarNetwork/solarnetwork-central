/* ==================================================================
 * ObjectDatumStreamMetadataProvider.java - 5/11/2020 2:29:47 pm
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

package net.solarnetwork.central.datum.v2.support;

import static java.util.stream.Collectors.toList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * API for a service that provides object stream metadata instances for object
 * (node or location) and source ID combinations.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ObjectDatumStreamMetadataProvider {

	/**
	 * Get a collection of all the available stream IDs this result instance has
	 * metadata available for.
	 * 
	 * @return the set of stream IDs that {@link #metadataForStreamId(UUID)} will
	 *         return a value for, or {@literal null} it not known
	 */
	Collection<UUID> metadataStreamIds();

	/**
	 * Get stream metadata for a given stream ID.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @return the stream metadata, or {@literal null} if not available
	 */
	ObjectDatumStreamMetadata metadataForStreamId(UUID streamId);

	/**
	 * Get stream metadata for a given object and source ID combination.
	 * 
	 * @param objectId
	 *        the object ID, e.g. node or location ID
	 * @param sourceId
	 *        the source ID
	 * @return the stream metadata, or {@literal null} if not available
	 */
	ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId);

	/**
	 * Create a simple provider backed by a {@link Map}.
	 * 
	 * @param metadatas
	 *        the collection of metadata
	 * @return the provider, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code metadatas} is {@literal null}
	 */
	static ObjectDatumStreamMetadataProvider staticProvider(
			Iterable<ObjectDatumStreamMetadata> metadatas) {
		if ( metadatas == null ) {
			throw new IllegalArgumentException("The mapping argument must not be null.");
		}
		return new ObjectDatumStreamMetadataProvider() {

			@Override
			public Collection<UUID> metadataStreamIds() {
				return StreamSupport.stream(metadatas.spliterator(), false)
						.map(ObjectDatumStreamMetadata::getStreamId).collect(toList());
			}

			@Override
			public ObjectDatumStreamMetadata metadataForStreamId(UUID streamId) {
				if ( streamId == null ) {
					return null;
				}
				for ( ObjectDatumStreamMetadata meta : metadatas ) {
					if ( meta != null && streamId.equals(meta.getStreamId()) ) {
						return meta;
					}
				}
				return null;
			}

			@Override
			public ObjectDatumStreamMetadata metadataForObjectSource(Long objectId, String sourceId) {
				if ( objectId == null || sourceId == null ) {
					return null;
				}
				for ( ObjectDatumStreamMetadata meta : metadatas ) {
					if ( meta != null && objectId.equals(meta.getObjectId())
							&& sourceId.equals(meta.getSourceId()) ) {
						return meta;
					}
				}
				return null;
			}
		};
	}

}
