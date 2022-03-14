/* ==================================================================
 * VirtualMachineState.java - 30/10/2017 7:00:14 PM
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

/**
 * Enum of possible states for a virtual machine to be in.
 * 
 * @author matt
 * @version 1.0
 */
public enum VirtualMachineState {

	/** The machine is starting. */
	Starting,

	/** The machine is running. */
	Running,

	/** The machine is not running, but could be started again. */
	Stopped,

	/**
	 * The machine is shutting down, either to end in the {@code Stopped} or
	 * {@code Terminated} states.
	 */
	Stopping,

	/** The machine is terminated, and cannot be started again. */
	Terminated,

	/** The machine is in an unknown state. */
	Unknown;

}
