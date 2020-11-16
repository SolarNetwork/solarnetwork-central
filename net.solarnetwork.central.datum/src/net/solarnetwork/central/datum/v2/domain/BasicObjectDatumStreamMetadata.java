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

import java.util.Arrays;
import java.util.UUID;

/**
 * Basic implementation of {@link ObjectDatumStreamMetadata}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicObjectDatumStreamMetadata extends BasicDatumStreamMetadata
		implements ObjectDatumStreamMetadata {

	private final Long objectId;
	private final String sourceId;

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
	 * @param objectId
	 *        the node ID
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
	public BasicObjectDatumStreamMetadata(UUID streamId, String timeZoneId, Long objectId,
			String sourceId, String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties) {
		super(streamId, timeZoneId, instantaneousProperties, accumulatingProperties, statusProperties);
		if ( objectId == null ) {
			throw new IllegalArgumentException("The objectId argument must not be null.");
		}
		this.objectId = objectId;
		if ( sourceId == null ) {
			throw new IllegalArgumentException("The sourceId argument must not be null.");
		}
		this.sourceId = sourceId;
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code objectId}, and
	 * {@code sourceId} are allowed to be {@literal null}. The other arguments
	 * are {@code Object} to work around MyBatis mapping issues. If any array is
	 * empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param objectId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param instantaneousProperties
	 *        the instantaneous property names; must be a {@code String[]}
	 * @param accumulatingProperties
	 *        the accumulating property names; must be a {@code String[]}
	 * @param statusProperties
	 *        the status property names; must be a {@code String[]}
	 */
	public BasicObjectDatumStreamMetadata(UUID streamId, String timeZoneId, Long objectId,
			String sourceId, Object instantaneousProperties, Object accumulatingProperties,
			Object statusProperties) {
		this(streamId, timeZoneId, objectId, sourceId, (String[]) instantaneousProperties,
				(String[]) accumulatingProperties, (String[]) statusProperties);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicNodeDatumStreamMetadata{");
		if ( getStreamId() != null ) {
			builder.append("streamId=");
			builder.append(getStreamId());
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
		if ( getPropertyNames() != null ) {
			builder.append("propertyNames=");
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

}
