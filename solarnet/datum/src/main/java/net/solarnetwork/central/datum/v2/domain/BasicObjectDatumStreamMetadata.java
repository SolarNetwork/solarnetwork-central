/* ==================================================================
 * BasicObjectDatumStreamMetadata.java - 5/11/2020 4:03:50 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.ObjectDatumIdRelated;
import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Basic implementation of {@link ObjectDatumStreamMetadata}.
 *
 * @author matt
 * @version 1.2
 * @since 2.8
 */
public class BasicObjectDatumStreamMetadata extends BasicDatumStreamMetadata
		implements ObjectDatumStreamMetadata, ObjectDatumIdRelated {

	@Serial
	private static final long serialVersionUID = -4093896601567626604L;

	private final ObjectDatumKind kind;
	private final Long objectId;
	private final String sourceId;
	private final @Nullable BasicLocation location;
	private final @Nullable String metaJson;

	/**
	 * Create a new metadata instance with no property names.
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @return the new instance
	 */
	public static BasicObjectDatumStreamMetadata emptyMeta(UUID streamId, @Nullable String timeZoneId,
			ObjectDatumKind kind, Long objectId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(streamId, timeZoneId, kind, objectId, sourceId, null,
				null, null, null);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * All arguments except {@code streamId}, {@code kind}, {@code objectId},
	 * and {@code sourceId} are allowed to be {@code null}. If any array is
	 * empty, it will be treated as if it were {@code null}.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code kind} or {@code objectId} or
	 *         {@code sourceId} is {@code null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, @Nullable String timeZoneId,
			ObjectDatumKind kind, Long objectId, String sourceId,
			String @Nullable [] instantaneousProperties, String @Nullable [] accumulatingProperties,
			String @Nullable [] statusProperties) {
		this(streamId, timeZoneId, kind, objectId, sourceId, null, instantaneousProperties,
				accumulatingProperties, statusProperties, null);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * All arguments except {@code streamId}, {@code kind}, {@code objectId},
	 * and {@code sourceId} are allowed to be {@code null}. If any array is
	 * empty, it will be treated as if it were {@code null}.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @param metaJson
	 *        the JSON metadata
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code kind} or {@code objectId} or
	 *         {@code sourceId} is {@code null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, @Nullable String timeZoneId,
			ObjectDatumKind kind, Long objectId, String sourceId,
			String @Nullable [] instantaneousProperties, String @Nullable [] accumulatingProperties,
			String @Nullable [] statusProperties, @Nullable String metaJson) {
		this(streamId, timeZoneId, kind, objectId, sourceId, null, instantaneousProperties,
				accumulatingProperties, statusProperties, metaJson);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * All arguments except {@code streamId}, {@code kind}, {@code objectId},
	 * and {@code sourceId} are allowed to be {@code null}. If any array is
	 * empty, it will be treated as if it were {@code null}.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param kind
	 *        the object kind
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param location
	 *        the location
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @param metaJson
	 *        the JSON metadata
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code kind} or {@code objectId} or
	 *         {@code sourceId} is {@code null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, @Nullable String timeZoneId,
			ObjectDatumKind kind, Long objectId, String sourceId, @Nullable Location location,
			String @Nullable [] instantaneousProperties, String @Nullable [] accumulatingProperties,
			String @Nullable [] statusProperties, @Nullable String metaJson) {
		super(streamId, timeZoneId, instantaneousProperties, accumulatingProperties, statusProperties);
		this.kind = requireNonNullArgument(kind, "kind");
		this.objectId = requireNonNullArgument(objectId, "objectId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
		this.location = BasicLocation.locationValue(location);
		this.metaJson = metaJson;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(kind);
		return result;
	}

	/**
	 * Compare for equality.
	 *
	 * <p>
	 * Only the {@code kind} and {@code streamId} are considered.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicObjectDatumStreamMetadata other) ) {
			return false;
		}
		return kind == other.kind;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicObjectDatumStreamMetadata{");
		builder.append("streamId=");
		builder.append(getStreamId());
		builder.append(", kind=");
		builder.append(kind);
		builder.append(", objectId=");
		builder.append(objectId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		if ( getPropertyNames() != null ) {
			builder.append(", propertyNames=");
			builder.append(Arrays.toString(getPropertyNames()));
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public final ObjectDatumKind getKind() {
		return kind;
	}

	@Override
	public final Long getObjectId() {
		return objectId;
	}

	@Override
	public final String getSourceId() {
		return sourceId;
	}

	@Override
	public final @Nullable String getMetaJson() {
		return metaJson;
	}

	@Override
	public final @Nullable BasicLocation getLocation() {
		return location;
	}

}
