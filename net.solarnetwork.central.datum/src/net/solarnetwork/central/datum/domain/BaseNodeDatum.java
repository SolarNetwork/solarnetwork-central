/* ===================================================================
 * BaseNodeDatum.java
 * 
 * Created Dec 1, 2009 4:10:14 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.domain;

import org.joda.time.DateTime;

/**
 * Abstract base class for {@link NodeDatum} implementations.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class BaseNodeDatum extends BaseDatum implements NodeDatum, Cloneable {

	private static final long serialVersionUID = -3071239480960854869L;

	private Long nodeId = null;
	private String sourceId = null;
	private DateTime posted = null;
	
	/**
	 * Default constructor.
	 */
	public BaseNodeDatum() {
		super();
	}
	
	/**
	 * Construct with an ID value.
	 * 
	 * @param id the ID value
	 */
	public BaseNodeDatum(Long id) {
		super();
		setId(id);
	}

	/**
	 * Construct with ID and node ID values.
	 * 
	 * @param id the ID value
	 * @param nodeId the node ID value
	 */
	public BaseNodeDatum(Long id, Long nodeId) {
		this(id);
		setNodeId(nodeId);
	}

	/**
	 * Construct with ID and node ID and source ID values.
	 * 
	 * @param id the ID value
	 * @param nodeId the node ID value
	 * @param sourceId the source ID value
	 */
	public BaseNodeDatum(Long id, Long nodeId, String sourceId) {
		this(id, nodeId);
		setSourceId(sourceId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		result = prime * result + ((getCreated() == null) ? 0 : getCreated().hashCode());
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
		BaseNodeDatum other = (BaseNodeDatum) obj;
		if ( super.equals(obj) ) {
			// we can stop here because IDs are equal
			return true;
		}
		if ( nodeId != null && nodeId.equals(other.nodeId)
				&& sourceId != null && sourceId.equals(other.sourceId)
				&& getCreated() != null && getCreated().equals(other.getCreated()) ) {
			return true;
		}
		return false;
	}

	public String getErrorMessage() {
		// intentionally return null, for backwards compatibility
		return null;
	}
	public void setErrorMessage(String errorMessage) {
		// intentionally left blank, for backwards compatibility
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
	public DateTime getPosted() {
		return posted;
	}
	public void setPosted(DateTime posted) {
		this.posted = posted;
	}

}
