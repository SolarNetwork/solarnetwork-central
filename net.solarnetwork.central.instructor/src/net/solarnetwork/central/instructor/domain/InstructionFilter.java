/* ==================================================================
 * InstructionFilter.java - Sep 29, 2011 8:04:22 PM
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

package net.solarnetwork.central.instructor.domain;

import java.util.List;
import net.solarnetwork.central.domain.Filter;

/**
 * Filter for Instruction entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface InstructionFilter extends Filter {

	/**
	 * Filter based on a node ID.
	 * 
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Filter based on state.
	 * 
	 * @return the state
	 */
	InstructionState getState();

	/**
	 * Filter based on a set of states.
	 * 
	 * @return the states, treated as a logical <em>or</em> so an instruction
	 *         matches if its state is contained in this set
	 */
	List<InstructionState> getStates();
}
