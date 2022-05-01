/* ==================================================================
 * ObjectDatumStreamId.java - 13/10/2021 11:29:19 AM
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

package net.solarnetwork.central.datum.v2.domain;

import java.util.Objects;
import java.util.UUID;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.ObjectUtils;

/**
 * Extension of
 * {@link net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId} with a
 * stream ID included.
 * 
 * @author matt
 * @version 1.1
 */
public class ObjectDatumStreamMetadataId
		extends net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId {

	private static final long serialVersionUID = 5974028952580494428L;

	private final UUID streamId;

	/**
	 * Create a metadata ID instance from a metadata instance.
	 * 
	 * @param meta
	 *        the metadata to create the ID instance for
	 * @return the ID, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code meta} is {@literal null}
	 * @since 1.1
	 */
	public static ObjectDatumStreamMetadataId idForMetadata(ObjectDatumStreamMetadata meta) {
		ObjectUtils.requireNonNullArgument(meta, "meta");
		return new ObjectDatumStreamMetadataId(meta.getStreamId(), meta.getKind(), meta.getObjectId(),
				meta.getSourceId());
	}

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 */
	public ObjectDatumStreamMetadataId(UUID streamId, ObjectDatumKind kind, Long objectId,
			String sourceId) {
		super(kind, objectId, sourceId);
		this.streamId = streamId;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("ObjectDatumStreamMetadataId{");
		buf.append("streamId=").append(streamId);
		buf.append(", kind=").append(getKind());
		buf.append(", objectId=").append(getObjectId());
		buf.append(", sourceId=").append(getSourceId());
		buf.append("}");
		return buf.toString();
	}

	@Override
	public ObjectDatumStreamMetadataId clone() {
		return (ObjectDatumStreamMetadataId) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(streamId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof ObjectDatumStreamMetadataId) ) {
			return false;
		}
		ObjectDatumStreamMetadataId other = (ObjectDatumStreamMetadataId) obj;
		return Objects.equals(streamId, other.streamId);
	}

	/**
	 * Get the stream ID.
	 * 
	 * @return the stream ID
	 */
	public UUID getStreamId() {
		return streamId;
	}

}
