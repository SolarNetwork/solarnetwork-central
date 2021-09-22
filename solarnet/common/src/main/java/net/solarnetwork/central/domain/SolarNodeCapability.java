/* ==================================================================
 * SolarNodeCapability.java - Jun 2, 2011 8:28:33 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;

/**
 * A set of capabilities for a node.
 * 
 * @author matt
 * @version $Revision$
 */
public class SolarNodeCapability extends SolarCapability implements Cloneable, Serializable {

	private static final long serialVersionUID = -1896754053131443476L;

	private Long nodeId;
	
	/**
	 * Default constructor.
	 */
	public SolarNodeCapability() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param nodeId the node ID
	 * @param generationCapacityWatts the generation capacity
	 * @param storageCapacityWattHours the energy storage capacity
	 */
	public SolarNodeCapability(Long nodeId, Long generationCapacityWatts,
			Long storageCapacityWattHours) {
		setNodeId(nodeId);
		setGenerationCapacityWatts(generationCapacityWatts);
		setStorageCapacityWattHours(storageCapacityWattHours);
	}
	
	public Long getNodeId() {
		return nodeId;
	}
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
