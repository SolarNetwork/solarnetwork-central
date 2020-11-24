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
 * Basic implementation of {@link NodeDatumStreamMetadata}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class BasicNodeDatumStreamMetadata extends BasicObjectDatumStreamMetadata
		implements NodeDatumStreamMetadata {

	private static final long serialVersionUID = 9064956621548468296L;

	/**
	 * Create a new metadata instance with no property names.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @return the new instance
	 */
	public static BasicNodeDatumStreamMetadata emptyMeta(UUID streamId, String timeZoneId, Long nodeId,
			String sourceId) {
		return new BasicNodeDatumStreamMetadata(streamId, timeZoneId, nodeId, sourceId, null, null,
				null);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code nodeId}, and
	 * {@code sourceId} are allowed to be {@literal null}. If any array is
	 * empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param nodeId
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
	 *         if {@code streamId} or {@code nodeId} or {@code sourceId} is
	 *         {@literal null}
	 */
	public BasicNodeDatumStreamMetadata(UUID streamId, String timeZoneId, Long nodeId, String sourceId,
			String[] instantaneousProperties, String[] accumulatingProperties,
			String[] statusProperties) {
		super(streamId, timeZoneId, nodeId, sourceId, instantaneousProperties, accumulatingProperties,
				statusProperties);
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * All arguments except {@code streamId}, {@code nodeId}, and
	 * {@code sourceId} are allowed to be {@literal null}. The other arguments
	 * are {@code Object} to work around MyBatis mapping issues. If any array is
	 * empty, it will be treated as if it were {@literal null}.
	 * </p>
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param nodeId
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
	public BasicNodeDatumStreamMetadata(UUID streamId, String timeZoneId, Long nodeId, String sourceId,
			Object instantaneousProperties, Object accumulatingProperties, Object statusProperties) {
		this(streamId, timeZoneId, nodeId, sourceId, (String[]) instantaneousProperties,
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
		if ( getObjectId() != null ) {
			builder.append("nodeId=");
			builder.append(getObjectId());
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
	public Long getNodeId() {
		return getObjectId();
	}

}
