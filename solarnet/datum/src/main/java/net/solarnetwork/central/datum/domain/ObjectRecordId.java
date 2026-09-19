/* ==================================================================
 * ObjectRecordId.java - 9/03/2026 7:23:43 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
import org.jspecify.annotations.Nullable;

/**
 * ID for fully optional object ID, source ID, and timestamp.
 *
 * @author matt
 * @version 1.0
 */
public class ObjectRecordId implements Serializable, Cloneable, Comparable<ObjectRecordId> {

	private static final long serialVersionUID = 2983823062065837435L;

	private final @Nullable Long objectId;
	private final @Nullable String sourceId;
	private final @Nullable Instant timestamp;

	/**
	 * Constructor.
	 *
	 * @param objectId
	 *        the object ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ObjectRecordId(@Nullable Long objectId, @Nullable String sourceId,
			@Nullable Instant timestamp) {
		super();
		this.objectId = objectId;
		this.sourceId = sourceId;
		this.timestamp = timestamp;
	}

	@Override
	public ObjectRecordId clone() {
		try {
			return (ObjectRecordId) super.clone();
		} catch ( CloneNotSupportedException e ) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ObjectRecordId{objectId=");
		builder.append(objectId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append(", timestamp=");
		builder.append(timestamp);
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(objectId, sourceId, timestamp);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ObjectRecordId other) ) {
			return false;
		}
		return Objects.equals(objectId, other.objectId) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(timestamp, other.timestamp);
	}

	@Override
	public int compareTo(ObjectRecordId o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.objectId == null ) {
			return 1;
		} else if ( objectId == null ) {
			return -1;
		}
		int comparison = objectId.compareTo(o.objectId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.sourceId == null ) {
			return 1;
		} else if ( sourceId == null ) {
			return -1;
		}
		comparison = sourceId.compareToIgnoreCase(o.sourceId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.timestamp == null ) {
			return 1;
		} else if ( timestamp == null ) {
			return -1;
		}
		return timestamp.compareTo(o.timestamp);
	}

	/**
	 * Get the object ID.
	 *
	 * @return the object ID
	 */
	public final @Nullable Long getObjectId() {
		return objectId;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Get the timestamp.
	 *
	 * @return the timestamp
	 */
	public final @Nullable Instant getTimestamp() {
		return timestamp;
	}

}
