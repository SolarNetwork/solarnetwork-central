/* ==================================================================
 * SimpleCapabilityInformation.java - Jun 8, 2011 2:45:13 PM
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

package net.solarnetwork.central.dras.support;

import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.CapabilityInformation;

/**
 * Implementation of {@link CapabilityInformation}.
 * 
 * <p>The fact that this class extends {@link Capability} is an implementation
 * detail, and should not be relied on by code using the {@link CapabilityInformation}
 * API.</p>
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleCapabilityInformation extends Capability 
implements CapabilityInformation {

	private static final long serialVersionUID = 7174005509071346253L;

	private String name;
	private Location location;
	
	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Increment the generation capacity.
	 * @param amount the amount to add
	 */
	public void addGenerationCapacityWatts(Long amount) {
		setGenerationCapacityWatts(getGenerationCapacityWatts() + amount);
	}
	
	/**
	 * Increment the storage capacity.
	 * @param amount the amount to add
	 */
	public void addStorageCapacityWattHours(Long amount) {
		setStorageCapacityWattHours(getStorageCapacityWattHours() + amount);
	}
	
	public void setLocation(Location location) {
		this.location = location;
	}

	public void setName(String name) {
		this.name = name;
	}

}
