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
import java.util.Arrays;
import java.util.UUID;
import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Basic implementation of {@link ObjectDatumStreamMetadata}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicObjectDatumStreamMetadata extends BasicDatumStreamMetadata
		implements ObjectDatumStreamMetadata {

	private static final long serialVersionUID = -4093896601567626604L;

	private final ObjectDatumKind kind;
	private final Long objectId;
	private final String sourceId;
	private final BasicLocation location;
	private final String metaJson;

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
	public static BasicObjectDatumStreamMetadata emptyMeta(UUID streamId, String timeZoneId,
			ObjectDatumKind kind, Long objectId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(streamId, timeZoneId, kind, objectId, sourceId, null,
				null, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code objectId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
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
	 *         if {@code streamId} or {@code objectId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, String timeZoneId, ObjectDatumKind kind,
			Long objectId, String sourceId, String[] instantaneousProperties,
			String[] accumulatingProperties, String[] statusProperties) {
		this(streamId, timeZoneId, kind, objectId, sourceId, null, instantaneousProperties,
				accumulatingProperties, statusProperties, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code objectId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
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
	 *         if {@code streamId} or {@code objectId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, String timeZoneId, ObjectDatumKind kind,
			Long objectId, String sourceId, String[] instantaneousProperties,
			String[] accumulatingProperties, String[] statusProperties, String metaJson) {
		this(streamId, timeZoneId, kind, objectId, sourceId, null, instantaneousProperties,
				accumulatingProperties, statusProperties, metaJson);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code objectId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
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
	 *         if {@code streamId} or {@code objectId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, String timeZoneId, ObjectDatumKind kind,
			Long objectId, String sourceId, Location location, String[] instantaneousProperties,
			String[] accumulatingProperties, String[] statusProperties, String metaJson) {
		super(streamId, timeZoneId, instantaneousProperties, accumulatingProperties, statusProperties);
		this.kind = requireNonNullArgument(kind, "kind");
		this.objectId = requireNonNullArgument(objectId, "objectId");
		this.sourceId = requireNonNullArgument(sourceId, "sourceId");
		this.location = BasicLocation.locationValue(location);
		this.metaJson = metaJson;
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
	public Long getObjectId() {
		return objectId;
	}

	@Override
	public String getSourceId() {
		return sourceId;
	}

	@Override
	public String getMetaJson() {
		return metaJson;
	}

	@Override
	public ObjectDatumKind getKind() {
		return kind;
	}

	@Override
	public BasicLocation getLocation() {
		return location;
	}

}
