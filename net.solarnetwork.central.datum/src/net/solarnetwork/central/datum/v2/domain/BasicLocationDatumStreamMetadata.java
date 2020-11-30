/* ==================================================================
 * BasicNodeDatumStreamMetadata.java - 26/10/2020 9:22:48 pm
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

import java.util.Arrays;
import java.util.UUID;

/**
 * Basic implementation of {@link LocationDatumStreamMetadata}
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicLocationDatumStreamMetadata extends BasicObjectDatumStreamMetadata
		implements LocationDatumStreamMetadata {

	private static final long serialVersionUID = 1972606246792871187L;

	/**
	 * Create a new metadata instance with no property names.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @return the new instance
	 */
	public static BasicLocationDatumStreamMetadata emptyMeta(UUID streamId, String timeZoneId,
			Long locationId, String sourceId) {
		return new BasicLocationDatumStreamMetadata(streamId, timeZoneId, locationId, sourceId, null,
				null, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code locationId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param instantaneousProperties
	 *        the instantaneous property names
	 * @param accumulatingProperties
	 *        the accumulating property names
	 * @param statusProperties
	 *        the status property names
	 * @throws IllegalArgumentException
	 *         if {@code streamId} or {@code nodeId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicLocationDatumStreamMetadata(UUID streamId, String timeZoneId, Long locationId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties) {
		this(streamId, timeZoneId, locationId, sourceId, instantaneousProperties, accumulatingProperties,
				statusProperties, null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code locationId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param locationId
	 *        the location ID
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
	 *         if {@code streamId} or {@code nodeId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicLocationDatumStreamMetadata(UUID streamId, String timeZoneId, Long locationId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties, String metaJson) {
		super(streamId, timeZoneId, ObjectDatumKind.Location, locationId, sourceId,
				instantaneousProperties, accumulatingProperties, statusProperties, metaJson);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicLocationDatumStreamMetadata{");
		if ( getStreamId() != null ) {
			builder.append("streamId=");
			builder.append(getStreamId());
			builder.append(", ");
		}
		if ( getLocationId() != null ) {
			builder.append("locationId=");
			builder.append(getLocationId());
			builder.append(", ");
		}
		if ( getSourceId() != null ) {
			builder.append("sourceId=");
			builder.append(getSourceId());
			builder.append(", ");
		}
		if ( getPropertyNames() != null ) {
			builder.append("propertyNames=");
			builder.append(Arrays.toString(getPropertyNames()));
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public Long getLocationId() {
		return getObjectId();
	}

}
