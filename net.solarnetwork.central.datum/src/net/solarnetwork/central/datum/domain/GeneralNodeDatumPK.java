/* ==================================================================
 * GeneralNodeDatumPK.java - Aug 22, 2014 5:51:19 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

/**
 * Primary key for a general node datum.
 * 
 * @author matt
 * @version 1.3
 */
public class GeneralNodeDatumPK implements Serializable, Cloneable, Comparable<GeneralNodeDatumPK> {

	private static final long serialVersionUID = -263871473085176405L;

	private Long nodeId;
	private DateTime created;
	private String sourceId;

	/**
	 * Default constructor.
	 */
	public GeneralNodeDatumPK() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param created
	 *        the creation date
	 * @param sourceId
	 *        the source ID
	 * @since 1.3
	 */
	public GeneralNodeDatumPK(Long nodeId, DateTime created, String sourceId) {
		super();
		this.nodeId = nodeId;
		this.created = created;
		this.sourceId = sourceId;
	}

	/**
	 * Populate a string builder with an ID value.
	 * 
	 * <p>
	 * This method is called from {@link #getId()}. Extending classes can add
	 * more data as necessary.
	 * </p>
	 * 
	 * @param buf
	 *        the buffer to populate
	 * @since 1.3
	 */
	protected void populateIdValue(StringBuilder buf) {
		buf.append("n=");
		if ( nodeId != null ) {
			buf.append(nodeId);
		}
		buf.append(";c=");
		if ( created != null ) {
			buf.append(created);
		}
		buf.append(";s=");
		if ( sourceId != null ) {
			buf.append(sourceId);
		}
	}

	/**
	 * Get a computed string ID value for this primary key.
	 * 
	 * <p>
	 * Note this value is derived from the properties of this class, and not
	 * assigned by the system. This method calls
	 * {@link #populateIdValue(StringBuilder)} and then computes a hex-encoded
	 * SHA1 value from that as the final ID value.
	 * </p>
	 * 
	 * @return computed ID string
	 */
	public final String getId() {
		StringBuilder builder = new StringBuilder();
		populateIdValue(builder);
		return DigestUtils.sha1Hex(builder.toString());
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
	 */
	@Override
	public int compareTo(GeneralNodeDatumPK o) {
		if ( o == null ) {
			return 1;
		}
		if ( o.nodeId == null ) {
			return 1;
		} else if ( nodeId == null ) {
			return -1;
		}
		int comparison = nodeId.compareTo(o.nodeId);
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
	 * @since 1.3
	 */
	protected void populateStringValue(StringBuilder buf) {
		if ( nodeId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("nodeId=");
			buf.append(nodeId);
		}
		if ( created != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("created=");
			buf.append(created);
		}
		if ( sourceId != null ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append("sourceId=");
			buf.append(sourceId);
		}
	}

	/**
	 * Generate a string value.
	 * 
	 * <p>
	 * This method generates a string like <code>Class{data}</code> where
	 * {@code data} is generated via
	 * {@link #populateStringValue(StringBuilder)}.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public final String toString() {
		StringBuilder builder = new StringBuilder();
		populateStringValue(builder);
		builder.insert(0, '{');
		builder.insert(0, getClass().getSimpleName());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		GeneralNodeDatumPK other = (GeneralNodeDatumPK) obj;
		if ( nodeId == null ) {
			if ( other.nodeId != null ) {
				return false;
			}
		} else if ( !nodeId.equals(other.nodeId) ) {
			return false;
		}
		if ( created == null ) {
			if ( other.created != null ) {
				return false;
			}
		} else if ( !created.isEqual(other.created) ) {
			return false;
		}
		if ( sourceId == null ) {
			if ( other.sourceId != null ) {
				return false;
			}
		} else if ( !sourceId.equals(other.sourceId) ) {
			return false;
		}
		return true;
	}

	@Override
	protected Object clone() {
		try {
			return super.clone();
		} catch ( CloneNotSupportedException e ) {
			// shouldn't get here
			throw new RuntimeException(e);
		}
	}

	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
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
