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

package net.solarnetwork.central.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.domain.BaseId;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A general aggregate datum identifier.
 * 
 * @author matt
 * @version 1.1
 */
public class AggregateDatumId extends BaseId
		implements Cloneable, Serializable, Comparable<AggregateDatumId> {

	private static final long serialVersionUID = 182649624035421355L;

	private final ObjectDatumKind kind;
	private final Long objectId;
	private final String sourceId;
	private final Instant timestamp;
	private final Aggregation aggregation;

	/**
	 * Create a new node aggregate datum ID.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @return the new instance
	 */
	public static AggregateDatumId nodeId(Long nodeId, String sourceId, Instant timestamp,
			Aggregation aggregation) {
		return new AggregateDatumId(ObjectDatumKind.Node, nodeId, sourceId, timestamp, aggregation);
	}

	/**
	 * Create a new location datum stream ID.
	 * 
	 * @param locationId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @param aggregation
	 *        the aggregation
	 * @return the new instance
	 */
	public static AggregateDatumId locationId(Long locationId, String sourceId, Instant timestamp,
			Aggregation aggregation) {
		return new AggregateDatumId(ObjectDatumKind.Location, locationId, sourceId, timestamp,
				aggregation);
	}

	/**
	 * Constructor.
	 * 
	 * @param kind
	 *        the kind
	 * @param objectId
	 *        the stream object ID
	 * @param sourceId
	 *        ID the stream source ID
	 * @param timestamp
	 *        the time stamp
	 * @param aggregation
	 *        the aggregation
	 */
	public AggregateDatumId(ObjectDatumKind kind, Long objectId, String sourceId, Instant timestamp,
			Aggregation aggregation) {
		super();
		this.kind = kind;
		this.objectId = objectId;
		this.sourceId = sourceId;
		this.timestamp = timestamp;
		this.aggregation = aggregation;
	}

	@Override
	public AggregateDatumId clone() {
		return (AggregateDatumId) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("k=");
		if ( kind != null ) {
			buf.append(kind.getKey());
		}
		buf.append(";o=");
		if ( objectId != null ) {
			buf.append(objectId);
		}
		buf.append(";s=");
		if ( sourceId != null ) {
			buf.append(sourceId);
		}
		buf.append(";t=");
		if ( timestamp != null ) {
			buf.append(timestamp.getEpochSecond()).append('.').append(timestamp.getNano());
		}
		buf.append(";a=");
		if ( aggregation != null ) {
			buf.append(aggregation);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( kind != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("kind=");
			buf.append(kind);
		}
		if ( objectId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("objectId=");
			buf.append(objectId);
		}
		if ( sourceId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("sourceId=");
			buf.append(sourceId);
		}
		if ( timestamp != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("timestamp=");
			buf.append(timestamp);
		}
		if ( aggregation != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("aggregation=");
			buf.append(aggregation);
		}
	}

	@Override
	public int compareTo(AggregateDatumId o) {
		if ( this == o ) {
			return 0;
		}
		if ( o == null ) {
			return -1;
		}
		int result = 0;
		if ( kind != o.kind ) {
			if ( kind == null ) {
				return 1;
			} else if ( o.kind == null ) {
				return -1;
			}
			result = kind.compareTo(o.kind);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( objectId != o.objectId ) {
			if ( objectId == null ) {
				return 1;
			} else if ( o.objectId == null ) {
				return -1;
			}
			result = objectId.compareTo(o.objectId);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( sourceId != o.sourceId ) {
			if ( sourceId == null ) {
				return 1;
			} else if ( o.sourceId == null ) {
				return -1;
			}
			result = sourceId.compareTo(o.sourceId);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( timestamp != o.timestamp ) {
			if ( timestamp == null ) {
				return 1;
			} else if ( o.timestamp == null ) {
				return -1;
			}
			result = timestamp.compareTo(o.timestamp);
			if ( result != 0 ) {
				return result;
			}
		}
		if ( aggregation == o.aggregation ) {
			return 0;
		} else if ( aggregation == null ) {
			return 1;
		} else if ( o.aggregation == null ) {
			return -1;
		}
		return aggregation.compareTo(o.aggregation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(kind, objectId, sourceId, timestamp, aggregation);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof AggregateDatumId) ) {
			return false;
		}
		AggregateDatumId other = (AggregateDatumId) obj;
		return Objects.equals(kind, other.kind) && Objects.equals(objectId, other.objectId)
				&& Objects.equals(sourceId, other.sourceId) && Objects.equals(timestamp, other.timestamp)
				&& Objects.equals(aggregation, other.aggregation);
	}

	/**
	 * Test if this object ID is fully specified.
	 * 
	 * @param expectedKind
	 *        the kind to match
	 * @return {@literal true} if {@code expectedKind} is the same as this
	 *         object's {@code kind} and {@code objectId}, {@code sourceId},
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
