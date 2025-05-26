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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A general object datum identifier.
 *
 * @author matt
 * @version 1.2
 */
@JsonPropertyOrder({ "kind", "streamId", "objectId", "sourceId", "timestamp", "aggregation" })
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "kind",
		defaultImpl = ObjectDatumId.NodeDatumId.class)
@JsonSubTypes({ @Type(names = { "l", "Location" }, value = ObjectDatumId.LocationDatumId.class),
		@Type(names = { "n", "Node" }, value = ObjectDatumId.NodeDatumId.class),
		@Type(value = ObjectDatumId.NodeDatumId.class) })
public sealed class ObjectDatumId implements Cloneable, Serializable
		permits ObjectDatumId.LocationDatumId, ObjectDatumId.NodeDatumId {

	@Serial
	private static final long serialVersionUID = 7571299682812609193L;

	private final ObjectDatumKind kind;
	private final UUID streamId;
	private final Long objectId;
	private final String sourceId;
	private final Instant timestamp;
	private final Aggregation aggregation;

	/**
	 * Create a new node datum stream key.
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
	 * Create a new location datum stream key.
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
	 * Create a new datum stream key.
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
	 * @return the instance, will be either a {@link LocationDatumId} or
	 *         {@link NodeDatumId} instance, depending on {@code kind}
	 * @since 1.2
	 */
	public static ObjectDatumId datumId(ObjectDatumKind kind, UUID streamId, Long objectId,
			String sourceId, Instant timestamp, Aggregation aggregation) {
		return switch (kind) {
			case Location -> locationId(streamId, objectId, sourceId, timestamp, aggregation);
			case Node -> nodeId(streamId, objectId, sourceId, timestamp, aggregation);
		};
	}

	/**
	 * Extension of {@link ObjectDatumId} for node data streams.
	 */
	public static final class NodeDatumId extends ObjectDatumId {

		@Serial
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
		@JsonCreator
		public NodeDatumId(@JsonProperty(value = "streamId", required = false) UUID streamId,
				@JsonProperty(value = "objectId", required = false) Long nodeId,
				@JsonProperty(value = "sourceId", required = false) String sourceId,
				@JsonProperty("timestamp") Instant timestamp,
				@JsonProperty(value = "aggregation", required = false) Aggregation aggregation) {
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
		@JsonIgnore
		public Long getNodeId() {
			return getObjectId();
		}

	}

	/**
	 * Extension of {@link ObjectDatumId} for location data streams.
	 */
	public static final class LocationDatumId extends ObjectDatumId {

		@Serial
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
		@JsonCreator
		public LocationDatumId(@JsonProperty(value = "streamId", required = false) UUID streamId,
				@JsonProperty(value = "objectId", required = false) Long locationId,
				@JsonProperty(value = "sourceId", required = false) String sourceId,
				@JsonProperty("timestamp") Instant timestamp,
				@JsonProperty(value = "aggregation", required = false) Aggregation aggregation) {
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
		@JsonIgnore
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
		return Objects.hash(aggregation, kind, streamId, objectId, sourceId, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ObjectDatumId other) ) {
			return false;
		}
		return aggregation == other.aggregation && kind == other.kind
				&& Objects.equals(streamId, other.streamId) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(timestamp, other.timestamp);
	}

	/**
	 * Test if this object ID is fully specified.
	 *
	 * @param expectedKind
	 *        the kind to match
	 * @return {@literal true} if {@code expectedKind} is the same as this
	 *         object's {@code kind} and {@code objectId}, {@code sourceId}, and
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
		return isValidObjectId(expectedKind) && aggregation != null;
	}

	/**
	 * Test if this ID is fully specified by having a {@code kind} and
	 * {@code timestamp} and then either a {@code streamId} or the combination
	 * of {@code objectId} and {@code sourceId}.
	 *
	 * @return {@code true} if the ID is fully specified
	 */
	@JsonIgnore
	public boolean isFullySpecified() {
		return (kind != null && timestamp != null && (streamId != null || isValidObjectId(kind)));
	}

	/**
	 * Compare two IDs for equivalence.
	 *
	 * <p>
	 * Ths is similar to {@link #equals(Object)} however if both this instance
	 * and {@code other} have a {@code streamId} value then the {@code streamId}
	 * values of each are compared, ignoring the {@code objectId} and
	 * {@code sourceId} properties. Conversely, if neither this instance or
	 * {@code other} has a {@code streamId} then the {@code streamId} properties
	 * are ignored and the {@code objectId} and {@code sourceId} properties are
	 * compared.
	 * </p>
	 *
	 * @param other
	 *        the other ID to compare to this instance
	 * @return {@code true} if {@code other} is an equivalent ID to this
	 *         instance
	 * @since 1.2
	 */
	@SuppressWarnings("ReferenceEquality")
	public boolean isEquivalent(ObjectDatumId other) {
		if ( this == other ) {
			return true;
		}
		return aggregation == other.aggregation && kind == other.kind
				&& (streamId != null && other.streamId != null ? Objects.equals(streamId, other.streamId)
						: Objects.equals(objectId, other.objectId)
								&& Objects.equals(sourceId, other.sourceId))
				&& Objects.equals(timestamp, other.timestamp);
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
