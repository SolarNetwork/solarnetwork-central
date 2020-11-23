/* ==================================================================
 * ObjectDatumStreamPK.java - 21/11/2020 9:40:24 pm
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.datum.domain.BasePK;

/**
 * Primary key for a datum based on object/source/timestamp values.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public class ObjectDatumStreamPK extends BasePK
		implements Serializable, Cloneable, Comparable<ObjectDatumStreamPK> {

	private static final long serialVersionUID = -5549743564570744068L;

	private final Long objectId;
	private final String sourceId;
	private final Instant timestamp;

	/**
	 * Create a new node datum stream PK.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @return the key
	 */
	public static NodeDatumStreamPK nodeId(Long nodeId, String sourceId, Instant timestamp) {
		return new NodeDatumStreamPK(nodeId, sourceId, timestamp);
	}

	/**
	 * Create a new location datum stream PK.
	 * 
	 * @param locationId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param timestamp
	 *        the timestamp
	 * @return the key
	 */
	public static LocationDatumStreamPK locationId(Long locationId, String sourceId, Instant timestamp) {
		return new LocationDatumStreamPK(locationId, sourceId, timestamp);
	}

	/**
	 * Extension of {@link ObjectDatumStreamPK} for node data streams.
	 */
	public static class NodeDatumStreamPK extends ObjectDatumStreamPK {

		private static final long serialVersionUID = 1842938618226898862L;

		/**
		 * Constructor.
		 * 
		 * @param nodeId
		 *        the node ID
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 */
		public NodeDatumStreamPK(Long nodeId, String sourceId, Instant timestamp) {
			super(nodeId, sourceId, timestamp);
		}

		@Override
		public NodeDatumStreamPK clone() {
			return (NodeDatumStreamPK) super.clone();
		}

		/**
		 * Alias for {@link #getObjectId()}.
		 * 
		 * @return the node ID
		 */
		public Long getNodeId() {
			return getObjectId();
		}

	}

	/**
	 * Extension of {@link ObjectDatumStreamPK} for location data streams.
	 */
	public static class LocationDatumStreamPK extends ObjectDatumStreamPK {

		private static final long serialVersionUID = 3614436622439831647L;

		/**
		 * Constructor.
		 * 
		 * @param locationId
		 *        the location ID
		 * @param sourceId
		 *        the source ID
		 * @param timestamp
		 *        the timestamp
		 */
		public LocationDatumStreamPK(Long locationId, String sourceId, Instant timestamp) {
			super(locationId, sourceId, timestamp);
		}

		@Override
		public LocationDatumStreamPK clone() {
			return (LocationDatumStreamPK) super.clone();
		}

		/**
		 * Alias for {@link #getObjectId()}.
		 * 
		 * @return the location ID
		 */
		public Long getLocationId() {
			return getObjectId();
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param objectId
	 *        the stream object ID
	 * @param source
	 *        ID the stream source ID
	 * @param timestamp
	 *        the time stamp
	 */
	public ObjectDatumStreamPK(Long objectId, String sourceId, Instant timestamp) {
		super();
		this.objectId = objectId;
		this.sourceId = sourceId;
		this.timestamp = timestamp;
	}

	@Override
	protected ObjectDatumStreamPK clone() {
		return (ObjectDatumStreamPK) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("o=");
		if ( objectId != null ) {
			buf.append(objectId);
		}
		buf.append("s=");
		if ( sourceId != null ) {
			buf.append(sourceId);
		}
		buf.append(";t=");
		if ( timestamp != null ) {
			buf.append(timestamp.getEpochSecond()).append('.').append(timestamp.getNano());
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
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
	}

	@Override
	public int compareTo(ObjectDatumStreamPK o) {
		if ( this == o ) {
			return 0;
		}
		if ( o == null ) {
			return -1;
		}
		int result = 0;
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
		if ( timestamp == o.timestamp ) {
			return 0;
		} else if ( timestamp == null ) {
			return 1;
		} else if ( o.timestamp == null ) {
			return -1;
		}
		return timestamp.compareTo(o.timestamp);
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
		if ( !(obj instanceof ObjectDatumStreamPK) ) {
			return false;
		}
		ObjectDatumStreamPK other = (ObjectDatumStreamPK) obj;
		return Objects.equals(objectId, other.objectId) && Objects.equals(sourceId, other.sourceId)
				&& Objects.equals(timestamp, other.timestamp);
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

}
