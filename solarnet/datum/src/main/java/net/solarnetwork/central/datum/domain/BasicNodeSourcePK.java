/* ==================================================================
 * BasicNodeSourcePK.java - 11/04/2019 9:42:03 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.central.domain.BasePK;

/**
 * Basic primary key composed of a node ID and source ID.
 *
 * @author matt
 * @version 1.0
 * @since 1.39
 */
@SuppressWarnings("MissingImplementsComparable")
public class BasicNodeSourcePK extends BasePK implements Serializable, Cloneable {

	@Serial
	private static final long serialVersionUID = -6312919371283470806L;

	private Long nodeId;
	private String sourceId;

	/**
	 * Default constructor.
	 */
	public BasicNodeSourcePK() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 */
	public BasicNodeSourcePK(Long nodeId, String sourceId) {
		super();
		this.nodeId = nodeId;
		this.sourceId = sourceId;
	}

	@Override
	public BasicNodeSourcePK clone() {
		return (BasicNodeSourcePK) super.clone();
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("n=");
		if ( nodeId != null ) {
			buf.append(nodeId);
		}
		buf.append(";s=");
		if ( sourceId != null ) {
			buf.append(sourceId);
		}
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		if ( nodeId != null ) {
			if ( !buf.isEmpty() ) {
				buf.append(", ");
			}
			buf.append("nodeId=");
			buf.append(nodeId);
		}
		if ( sourceId != null ) {
			if ( !buf.isEmpty() ) {
				buf.append(", ");
			}
			buf.append("sourceId=");
			buf.append(sourceId);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, sourceId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( (obj == null) || !(obj instanceof BasicNodeSourcePK other) ) {
			return false;
		}
		return Objects.equals(nodeId, other.nodeId) && Objects.equals(sourceId, other.sourceId);
	}

	/**
	 * Compare two {@code BasicNodeSourcePK} objects.
	 *
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 *
	 * <ol>
	 * <li>nodeId</li>
	 * <li>sourceId</li>
	 * </ol>
	 *
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 *
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or l to, or greater than the specified
	 *         object.
	 */
	public int compareTo(BasicNodeSourcePK o) {
		if ( o == null ) {
			return 1;
		}
		Long oNodeId = o.getNodeId();
		Long nodeId = getNodeId();
		if ( oNodeId == null ) {
			return 1;
		} else if ( nodeId == null ) {
			return -1;
		}
		int comparison = nodeId.compareTo(oNodeId);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.sourceId == null ) {
			return 1;
		} else if ( sourceId == null ) {
			return -1;
		}
		return sourceId.compareToIgnoreCase(o.sourceId);
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
