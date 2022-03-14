/* ==================================================================
 * BasicNodeSourceDatePK.java - 11/04/2019 9:47:22 am
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Basic primary key composed of a node ID and source ID and date.
 * 
 * @author matt
 * @version 2.0
 * @since 1.39
 */
public class BasicNodeSourceDatePK extends BasicNodeSourcePK implements Serializable, Cloneable {

	private static final long serialVersionUID = 4113556533271163661L;

	private Instant created;

	/**
	 * Default constructor.
	 */
	public BasicNodeSourceDatePK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param created
	 *        the creation date
	 */
	public BasicNodeSourceDatePK(Long nodeId, String sourceId, Instant created) {
		super();
		setNodeId(nodeId);
		setSourceId(sourceId);
		setCreated(created);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(created);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicNodeSourceDatePK) ) {
			return false;
		}
		BasicNodeSourceDatePK other = (BasicNodeSourceDatePK) obj;
		if ( created == null ) {
			if ( other.created != null ) {
				return false;
			}
		} else if ( !created.equals(other.created) ) {
			return false;
		}
		return true;
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		super.populateIdValue(buf);
		buf.append(";c=");
		if ( created != null ) {
			buf.append(created);
		}
	}

	/**
	 * Compare two {@code GeneralNodeDautumPK} objects.
	 * 
	 * <p>
	 * Keys are ordered based on:
	 * </p>
	 * 
	 * <ol>
	 * <li>nodeId</li>
	 * <li>sourceId</li>
	 * <li>created</li>
	 * </ol>
	 * 
	 * {@literal null} values will be sorted before non-{@literal null} values.
	 * 
	 * @param o
	 *        the object to compare to
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	public int compareTo(BasicNodeSourceDatePK o) {
		int comparison = super.compareTo(o);
		if ( comparison != 0 ) {
			return comparison;
		}
		if ( o.created == null ) {
			return 1;
		} else if ( created == null ) {
			return -1;
		}
		return created.compareTo(o.created);
	}

	/**
	 * Populate a string builder with a friendly string value.
	 * 
	 * <p>
	 * This method is called from {@link #toString()}. Extending classes can add
	 * more data as necessary. The buffer will be initially empty when invoked.
	 * </p>
	 * 
	 * @param buf
	 *        the buffer to populate
	 */
	@Override
	protected void populateStringValue(StringBuilder buf) {
		super.populateStringValue(buf);
		if ( created != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("created=");
			buf.append(created);
		}
	}

	/**
	 * Get the creation time stamp.
	 * 
	 * @return the creation time
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Set the creation time stamp.
	 * 
	 * @param created
	 *        the time
	 */
	public void setCreated(Instant created) {
		this.created = created;
	}

}
