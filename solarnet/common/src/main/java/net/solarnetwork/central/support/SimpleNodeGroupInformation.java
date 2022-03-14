/* ==================================================================
 * SimpleNodeGroupInformation.java - Apr 30, 2011 1:18:09 PM
 * 
 * Copyright 2007 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import net.solarnetwork.central.domain.BaseIdentity;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.NodeGroupInformation;
import net.solarnetwork.central.domain.SolarCapability;
import net.solarnetwork.central.domain.SolarNodeGroupCapability;

/**
 * Simple implementation of {@link NodeGroupInformation}.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleNodeGroupInformation extends BaseIdentity implements NodeGroupInformation {

	private static final long serialVersionUID = -1983417976743765775L;

	private String name;
	private Location location;
	private SolarCapability capability;

	/**
	 * Default constructor.
	 */
	public SimpleNodeGroupInformation() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the name
	 * @param capability
	 *        the capability
	 * @param location
	 *        the location
	 */
	public SimpleNodeGroupInformation(String name, SolarNodeGroupCapability capability,
			Location location) {
		setId(capability.getGroupId());
		this.name = name;
		this.capability = capability;
		this.location = location;
	}

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
	 * 
	 * @param amount
	 *        the amount to add
	 */
	public void addGenerationCapacityWatts(Long amount) {
		capability.setGenerationCapacityWatts(capability.getGenerationCapacityWatts() + amount);
	}

	/**
	 * Increment the storage capacity.
	 * 
	 * @param amount
	 *        the amount to add
	 */
	public void addStorageCapacityWattHours(Long amount) {
		capability.setStorageCapacityWattHours(capability.getStorageCapacityWattHours() + amount);
	}

	/**
	 * @return the generationCapacityWatts
	 */
	@Override
	public Long getGenerationCapacityWatts() {
		return capability.getGenerationCapacityWatts();
	}

	/**
	 * @return the storageCapacityWattHours
	 */
	@Override
	public Long getStorageCapacityWattHours() {
		return capability.getStorageCapacityWattHours();
	}

}
