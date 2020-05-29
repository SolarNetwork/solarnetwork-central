/* ==================================================================
 * DatumAppEventKey.java - 29/05/2020 5:19:14 pm
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

package net.solarnetwork.central.datum.dao;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Primary key for datum event entities.
 * 
 * @author matt
 * @version 1.0
 * @since 2.6
 */
public class DatumAppEventKey implements Comparable<DatumAppEventKey>, Cloneable, Serializable {

	private static final long serialVersionUID = -8866997135663517216L;

	private final String topic;
	private final Instant created;
	private final Long nodeId;
	private final String sourceId;

	/**
	 * Constructor.
	 * 
	 * @param topic
	 *        the topic
	 * @param created
	 *        the creation date
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public DatumAppEventKey(String topic, Instant created, Long nodeId, String sourceId) {
		super();
		if ( topic == null ) {
			throw new IllegalArgumentException("The topic parameter must not be null.");
		}
		this.topic = topic;
		if ( created == null ) {
			throw new IllegalArgumentException("The created parameter must not be null.");
		}
		this.created = created;
		if ( nodeId == null ) {
			throw new IllegalArgumentException("The nodeId parameter must not be null.");
		}
		this.nodeId = nodeId;
		if ( sourceId == null ) {
			throw new IllegalArgumentException("The sourceId parameter must not be null.");
		}
		this.sourceId = sourceId;
	}

	/**
	 * Create a new key instance.
	 * 
	 * @param topic
	 *        the topic
	 * @param created
	 *        the creation date
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @throws IllegalArgumentException
	 *         if any parameter is {@literal null}
	 */
	public static DatumAppEventKey keyFor(String topic, Instant created, Long nodeId, String sourceId) {
		return new DatumAppEventKey(topic, created, nodeId, sourceId);
	}

	@Override
	public DatumAppEventKey clone() {
		try {
			return (DatumAppEventKey) super.clone();
		} catch ( CloneNotSupportedException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compareTo(DatumAppEventKey o) {
		int result = topic.compareTo(o.topic);
		if ( result == 0 ) {
			result = created.compareTo(o.created);
			if ( result == 0 ) {
				result = nodeId.compareTo(o.nodeId);
				if ( result == 0 ) {
					result = sourceId.compareTo(o.sourceId);
				}
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(created, nodeId, sourceId, topic);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof DatumAppEventKey) ) {
			return false;
		}
		DatumAppEventKey other = (DatumAppEventKey) obj;
		return Objects.equals(created, other.created) && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(sourceId, other.sourceId) && Objects.equals(topic, other.topic);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DatumAppEventKey{topic=");
		builder.append(topic);
		builder.append(", created=");
		builder.append(created);
		builder.append(", nodeId=");
		builder.append(nodeId);
		builder.append(", sourceId=");
		builder.append(sourceId);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the topic.
	 * 
	 * @return the topic the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Get the creation date.
	 * 
	 * @return the creation date
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Get the source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

}
