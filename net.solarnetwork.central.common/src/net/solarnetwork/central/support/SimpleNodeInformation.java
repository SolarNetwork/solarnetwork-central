/* ==================================================================
 * SimpleNodeInformation.java - May 1, 2011 3:05:47 PM
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

package net.solarnetwork.central.support;

import net.solarnetwork.central.domain.BaseIdentity;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.NodeInformation;
import net.solarnetwork.central.domain.SolarNodeCapability;

/**
 * Simple implementation of {@link NodeInformation}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleNodeInformation extends BaseIdentity implements NodeInformation {

	private static final long serialVersionUID = -7130984585644772072L;

	private Location location;
	private SolarNodeCapability capability;
	
	/**
	 * Default constructor.
	 */
	public SimpleNodeInformation() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param group the group to copy values from.
	 * @param location the location
	 */
	public SimpleNodeInformation(SolarNodeCapability capability, Location location) {
		setId(capability.getNodeId());
		this.capability = capability;
		this.location = location;
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * @return the generationCapacityWatts
	 */
	public Long getGenerationCapacityWatts() {
		return capability.getGenerationCapacityWatts();
	}

	/**
	 * @return the storageCapacityWattHours
	 */
	public Long getStorageCapacityWattHours() {
		return capability.getStorageCapacityWattHours();
	}

}
