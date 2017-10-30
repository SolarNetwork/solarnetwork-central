/* ==================================================================
 * VirtualMachineBiz.java - 30/10/2017 6:58:17 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.cloud.biz;

import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;

/**
 * API for controlling cloud virtual machine instances.
 * 
 * @author matt
 * @version 1.0
 */
public interface VirtualMachineBiz {

	/**
	 * Get the state for a set of virtual machines.
	 * 
	 * @param machineIds
	 *        the IDs of the machines to get the state for
	 * @return a mapping of machine IDs to associated states; never
	 *         {@literal null}
	 */
	Map<String, VirtualMachineState> stateForVirtualMachines(Set<String> machineIds);

	/**
	 * Change the state of a set of virtual machines.
	 * 
	 * @param machineIds
	 *        the IDs of the machines to change the state of
	 * @param state
	 *        the desired state
	 */
	void changeVirtualMachinesState(Set<String> machineIds, VirtualMachineState state);

}
