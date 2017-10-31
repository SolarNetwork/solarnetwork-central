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
import net.solarnetwork.central.cloud.domain.VirtualMachine;
import net.solarnetwork.central.cloud.domain.VirtualMachineState;
import net.solarnetwork.domain.Identifiable;

/**
 * API for controlling cloud virtual machine instances.
 * 
 * @author matt
 * @version 1.0
 */
public interface VirtualMachineBiz extends Identifiable {

	/**
	 * Get a virtual machine by its display name.
	 * 
	 * @param name
	 *        the name of the machine to get
	 * @return the machine, or {@literal null} if not available
	 */
	VirtualMachine virtualMachineForName(String name);

	/**
	 * Get a set of virtual machines by their IDs.
	 * 
	 * @param ids
	 *        the IDs of the machines to get
	 * @return the found machines
	 */
	Iterable<VirtualMachine> virtualMachinesForIds(Set<String> ids);

	/**
	 * Get the state for a set of virtual machines.
	 * 
	 * @param ids
	 *        the IDs of the machines to get the state for
	 * @return a mapping of machine IDs to associated states; never
	 *         {@literal null}
	 */
	Map<String, VirtualMachineState> stateForVirtualMachines(Set<String> ids);

	/**
	 * Change the state of a set of virtual machines.
	 * 
	 * @param ids
	 *        the IDs of the machines to change the state of
	 * @param state
	 *        the desired state
	 */
	void changeVirtualMachinesState(Set<String> ids, VirtualMachineState state);

}
