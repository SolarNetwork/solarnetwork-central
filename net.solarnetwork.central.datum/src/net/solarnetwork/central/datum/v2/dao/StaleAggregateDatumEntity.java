/* ==================================================================
 * StaleAggregateDatumEntity.java - 3/11/2020 12:58:01 pm
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Entity for a "stale" aggregation record, which is used to mark specific
 * aggregation time periods as needing to be (re)computed.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class StaleAggregateDatumEntity {

	private final UUID streamId;
	private final Instant timestamp;
	private final Aggregation kind;
	private final Instant created;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the timestamp
	 * @param kind
	 *        the aggregation kind
	 * @param created
	 *        the creation date
	 */
	public StaleAggregateDatumEntity(UUID streamId, Instant timestamp, Aggregation kind,
			Instant created) {
		super();
		this.kind = kind;
		this.timestamp = timestamp;
		this.streamId = streamId;
		this.created = created;
	}

	@Override
	public int hashCode() {
		return Objects.hash(kind, streamId, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof StaleAggregateDatumEntity) ) {
			return false;
		}
		StaleAggregateDatumEntity other = (StaleAggregateDatumEntity) obj;
		return kind == other.kind && Objects.equals(streamId, other.streamId)
				&& Objects.equals(timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StaleAggregateDatumEntity{");
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
		if ( timestamp != null ) {
			builder.append("timestamp=");
			builder.append(timestamp);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the aggregation kind.
	 * 
	 * @return the kind
	 */
	public Aggregation getKind() {
		return kind;
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
	 * Get the stream ID.
	 * 
	 * @return the streamId
	 */
	public UUID getStreamId() {
		return streamId;
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the created
	 */
	public Instant getCreated() {
		return created;
	}

}
