/* ==================================================================
 * VirtualMachine.java - 30/10/2017 7:46:28 PM
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

package net.solarnetwork.central.cloud.domain;

import net.solarnetwork.dao.Entity;

/**
 * A virtual machine.
 * 
 * @author matt
 * @version 1.0
 */
public interface VirtualMachine extends Entity<String> {

	/**
	 * Get a name for this machine.
	 * 
	 * @return a name
	 */
	String getDisplayName();

	/**
	 * Get the state of this machine.
	 * 
	 * <p>
	 * This value might be a cached, last known value. Use
	 * {@code VirtualMachineBiz} to read the current state.
	 * </p>
	 * 
	 * @return the state
	 */
	VirtualMachineState getState();

}
