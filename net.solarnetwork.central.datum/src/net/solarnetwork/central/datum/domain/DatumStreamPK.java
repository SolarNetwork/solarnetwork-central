/* ==================================================================
 * DatumStreamPK.java - 22/10/2020 8:55:47 am
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

package net.solarnetwork.central.datum.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Primary key for a datum stream.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class DatumStreamPK extends BasePK implements Serializable, Cloneable, Comparable<DatumStreamPK> {

	private static final long serialVersionUID = -8674108064779256512L;

	private final UUID streamId;
	private final Instant timestamp;

	/**
	 * Constructor.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timestamp
	 *        the time stamp
	 */
	public DatumStreamPK(UUID streamId, Instant timestamp) {
		super();
		this.streamId = streamId;
		this.timestamp = timestamp;
	}

	@Override
	protected DatumStreamPK clone() {
		return (DatumStreamPK) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("s=");
		if ( streamId != null ) {
			buf.append(streamId);
		}
		buf.append(";t=");
		if ( timestamp != null ) {
			buf.append(timestamp.getEpochSecond()).append('.').append(timestamp.getNano());
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( streamId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("streamId=");
			buf.append(streamId);
		}
		if ( timestamp != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("timestamp=");
			buf.append(timestamp);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(streamId, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof DatumStreamPK) ) {
			return false;
		}
		DatumStreamPK other = (DatumStreamPK) obj;
		return Objects.equals(streamId, other.streamId) && Objects.equals(timestamp, other.timestamp);
	}

	/**
	 * Compare two key objects.
	 * 
	 * <p>
	 * This compares stream ID values followed by timestamp values. Both are
	 * ordered in ascending order with {@literal null} values ordered last.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(DatumStreamPK o) {
		if ( this == o ) {
			return 0;
		}
		if ( o == null ) {
			return -1;
		}
		int result = 0;
		if ( streamId != o.streamId ) {
			if ( streamId == null ) {
				return 1;
			} else if ( o.streamId == null ) {
				return -1;
			}
			result = streamId.compareTo(o.streamId);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( timestamp == o.timestamp ) {
			return 0;
		} else if ( timestamp == null ) {
			return 1;
		} else if ( o.timestamp == null ) {
			return -1;
		}
		return timestamp.compareTo(o.timestamp);
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
	 * Get the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

}
