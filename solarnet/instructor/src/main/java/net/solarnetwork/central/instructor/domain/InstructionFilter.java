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
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Filter for Instruction entities.
 *
 * @author matt
 * @version 1.5
 */
public interface InstructionFilter extends Filter, DateRangeCriteria {

	/**
	 * Filter based on a node ID.
	 *
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Get an array of node IDs.
	 *
	 * @return array of node IDs (may be {@literal null})
	 * @since 1.2
	 */
	Long[] getNodeIds();

	/**
	 * Test if a node ID is present.
	 * 
	 * @return {@code true} if at least one node ID is present
	 * @since 1.5
	 */
	default boolean hasNodeIdCriteria() {
		return getNodeId() != null;
	}

	/**
	 * Get an array of instruction IDs.
	 *
	 * @return array of instruction IDs (may be {@literal null})
	 * @since 1.2
	 */
	Long[] getInstructionIds();

	/**
	 * Test if an instruction ID is present.
	 * 
	 * @return {@code true} if at least one instruction ID is available
	 * @since 1.5
	 */
	default boolean hasInstructionIdCriteria() {
		Long[] ids = getInstructionIds();
		return (ids != null && ids.length > 0);
	}

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

	/**
	 * Test if an instruction state is present.
	 * 
	 * @return {@code true} if at least one instruction state is available
	 * @since 1.5
	 */
	default boolean hasInstructionStateCriteria() {
		return getState() != null;
	}

}
