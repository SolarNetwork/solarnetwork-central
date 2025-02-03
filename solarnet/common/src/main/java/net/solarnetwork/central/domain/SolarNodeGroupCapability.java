/* ==================================================================
 * SolarNodeGroupCapability.java - Jun 2, 2011 8:28:33 PM
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
 */

package net.solarnetwork.central.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * A set of capabilities for a node.
 *
 * @author matt
 * @version 1.0
 */
public class SolarNodeGroupCapability extends SolarCapability implements Cloneable, Serializable {

	@Serial
	private static final long serialVersionUID = 5120295683193038735L;

	private Long groupId;

	/**
	 * Default constructor.
	 */
	public SolarNodeGroupCapability() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param groupId
	 *        the node group ID
	 * @param generationCapacityWatts
	 *        the generation capacity
	 * @param storageCapacityWattHours
	 *        the energy storage capacity
	 */
	public SolarNodeGroupCapability(Long groupId, Long generationCapacityWatts,
			Long storageCapacityWattHours) {
		setGroupId(groupId);
		setGenerationCapacityWatts(generationCapacityWatts);
		setStorageCapacityWattHours(storageCapacityWattHours);
	}

	@Override
	public SolarNodeGroupCapability clone() {
		return (SolarNodeGroupCapability) super.clone();
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

}
