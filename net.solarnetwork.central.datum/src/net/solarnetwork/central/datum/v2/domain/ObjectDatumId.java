/* ==================================================================
 * ObjectDatumId.java - 22/11/2020 9:50:39 pm
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

package net.solarnetwork.central.datum.v2.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.solarnetwork.central.domain.Aggregation;

/**
 * A general object datum identifier.
 * 
 * @author matt
 * @version 1.0
 */
public class ObjectDatumId implements Cloneable, Serializable {

	private static final long serialVersionUID = 7571299682812609193L;

	private final ObjectDatumKind kind;
	private final UUID streamId;
	private final Long objectId;
	private final String sourceId;
	private final Instant timestamp;
	private final Aggregation aggregation;

	/**
	 * Create a new node datum stream PK.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @return the instance
	 */
	public static NodeDatumId nodeId(UUID streamId, Long nodeId, String sourceId, Instant timestamp,
			Aggregation aggregation) {
		return new NodeDatumId(streamId, nodeId, sourceId, timestamp, aggregation);
	}

	/**
	 * Create a new location datum stream PK.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param locationId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @return the instance
	 */
	public static LocationDatumId locationId(UUID streamId, Long locationId, String sourceId,
			Instant timestamp, Aggregation aggregation) {
		return new LocationDatumId(streamId, locationId, sourceId, timestamp, aggregation);
	}

	/**
	 * Extension of {@link ObjectDatumId} for node data streams.
	 */
	public static class NodeDatumId extends ObjectDatumId {

		private static final long serialVersionUID = -851538635627971228L;

		/**
		 * Constructor.
		 * 
		 * @param streamId
		 *        the stream ID
		 * @param nodeId
		 *        the node ID
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 * @param aggregation
		 *        the aggregation
		 */
		public NodeDatumId(UUID streamId, Long nodeId, String sourceId, Instant timestamp,
				Aggregation aggregation) {
			super(ObjectDatumKind.Node, streamId, nodeId, sourceId, timestamp, aggregation);
		}

		@Override
		public NodeDatumId clone() {
			return (NodeDatumId) super.clone();
		}

		/**
		 * Alias for {@link #getObjectId()}.
		 * 
		 * @return the node ID
		 */
		public Long getNodeId() {
			return getObjectId();
		}

	}

	/**
	 * Extension of {@link ObjectDatumId} for location data streams.
	 */
	public static class LocationDatumId extends ObjectDatumId {

		private static final long serialVersionUID = 2579981391355724098L;

		/**
		 * Constructor.
		 * 
		 * @param streamId
		 *        the stream ID
		 * @param locationId
		 *        the location ID
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 * @param aggregation
		 *        the aggregation
		 */
		public LocationDatumId(UUID streamId, Long locationId, String sourceId, Instant timestamp,
				Aggregation aggregation) {
			super(ObjectDatumKind.Location, streamId, locationId, sourceId, timestamp, aggregation);
		}

		@Override
		public LocationDatumId clone() {
			return (LocationDatumId) super.clone();
		}

		/**
		 * Alias for {@link #getObjectId()}.
		 * 
		 * @return the location ID
		 */
		public Long getLocationId() {
			return getObjectId();
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the object kind
	 * @param streamId
	 *        the stream ID
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 */
	public ObjectDatumId(ObjectDatumKind kind, UUID streamId, Long objectId, String sourceId,
			Instant timestamp, Aggregation aggregation) {
		super();
		this.kind = kind;
		this.streamId = streamId;
		this.objectId = objectId;
		this.sourceId = sourceId;
		this.timestamp = timestamp;
		this.aggregation = aggregation;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObjectDatumId{");
		if ( kind != null ) {
			builder.append("kind=");
			builder.append(kind);
			builder.append(", ");
		}
		if ( streamId != null ) {
			builder.append("streamId=");
			builder.append(streamId);
			builder.append(", ");
		}
		if ( objectId != null ) {
			builder.append("objectId=");
			builder.append(objectId);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		if ( timestamp != null ) {
			builder.append("timestamp=");
			builder.append(timestamp);
			builder.append(", ");
		}
		if ( aggregation != null ) {
			builder.append("aggregation=");
			builder.append(aggregation);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public ObjectDatumId clone() {
		try {
			return (ObjectDatumId) super.clone();
		} catch ( CloneNotSupportedException e ) {
			// should not get here
			throw new RuntimeException(e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(aggregation, kind, objectId, sourceId, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ObjectDatumId) ) {
			return false;
		}
		ObjectDatumId other = (ObjectDatumId) obj;
		return aggregation == other.aggregation && kind == other.kind
				&& Objects.equals(objectId, other.objectId) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(timestamp, other.timestamp);
	}

	/**
	 * Test if this object ID is fully specified.
	 * 
	 * @param expectedKind
	 *        the kind to match
	 * @return {@literal true} if {@code expectedKind} is the same as this
	 *         object's {@code kind} and {@code objectId], {@code sourceId}, and
	 *         {@code timestamp} are all non-null and non-empty
	 */
	public boolean isValidObjectId(ObjectDatumKind expectedKind) {
		return (expectedKind == kind && objectId != null && sourceId != null && !sourceId.isEmpty()
				&& timestamp != null);
	}

	/**
	 * Test if this object ID is fully specified as an aggregate.
	 * 
	 * @param expectedKind
	 *        the kind to match
	 * @return {@literal true} if {@link #isValidObjectId(ObjectDatumKind)}
	 *         returns {@literal true} and {@code aggregation} is non-null
	 * @see #isValidObjectId(ObjectDatumKind)
	 */
	public boolean isValidAggregateObjectId(ObjectDatumKind expectedKind) {
		return isValidObjectId(kind) && aggregation != null;
	}

	/**
	 * Get the kind.
	 * 
	 * @return the kind
	 */
	public ObjectDatumKind getKind() {
		return kind;
	}

	/**
	 * Get the stream ID.
	 * 
	 * @return the stream ID
	 */
	public UUID getStreamId() {
		return streamId;
	}

	/**
	 * Get the object ID.
	 * 
	 * @return the object ID
	 */
	public Long getObjectId() {
		return objectId;
	}

	/**
	 * Get the source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Get the aggregation.
	 * 
	 * @return the aggregation
	 */
	public Aggregation getAggregation() {
		return aggregation;
	}

}
