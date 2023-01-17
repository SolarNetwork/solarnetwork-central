/* ==================================================================
 * SimpleInstructionFilter.java - Sep 30, 2011 9:31:17 AM
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

package net.solarnetwork.central.instructor.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Simple implementation of {@link InstructionFilter}.
 * 
 * @author matt
 * @version 2.1
 */
public class SimpleInstructionFilter implements InstructionFilter {

	private Long[] nodeIds;
	private Long[] instructionIds;
	private List<InstructionState> states;
	private Instant startDate;
	private Instant endDate;

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>(2);
		if ( nodeIds != null && nodeIds.length > 0 ) {
			f.put("nodeId", nodeIds[0]); // backwards compatibility
			f.put("nodeIds", nodeIds);
		}
		if ( instructionIds != null && instructionIds.length > 0 ) {
			f.put("instructionIds", instructionIds);
		}
		if ( states != null && states.isEmpty() == false ) {
			f.put("state", states.iterator().next().toString());
			if ( states.size() > 1 ) {
				f.put("states", states.toArray(new InstructionState[states.size()]));
			}
		}
		if ( startDate != null ) {
			f.put("startDate", startDate);
		}
		if ( endDate != null ) {
			f.put("endDate", endDate);
		}
		return f;
	}

	/**
	 * Set a single node ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single node ID at a
	 * time. The node ID is still stored on the {@code nodeIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code nodeIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node
	 */
	public void setNodeId(Long nodeId) {
		this.nodeIds = new Long[] { nodeId };
	}

	/**
	 * Get the first node ID.
	 * 
	 * <p>
	 * This returns the first available node ID from the {@code nodeIds} array,
	 * or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first node ID
	 */
	@Override
	public Long getNodeId() {
		return this.nodeIds == null || this.nodeIds.length < 1 ? null : this.nodeIds[0];
	}

	@Override
	public Long[] getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	@Override
	public InstructionState getState() {
		return (states != null && states.isEmpty() == false ? states.iterator().next() : null);
	}

	public void setState(InstructionState state) {
		if ( state == null ) {
			states = null;
		} else {
			states = Arrays.asList(state);
		}
	}

	@Override
	public List<InstructionState> getStates() {
		return states;
	}

	/**
	 * Set the {@code states} property via a Set. This is useful when using an
	 * {@link EnumSet}.
	 * 
	 * @param stateSet
	 *        the Set to convert to a List of {@link InstructionState} values
	 *        for the {@code states} property
	 */
	public void setStateSet(Set<InstructionState> stateSet) {
		if ( stateSet == null ) {
			this.states = null;
		} else {
			this.states = new ArrayList<InstructionState>(stateSet);
		}
	}

	public void setStates(List<InstructionState> states) {
		if ( states == null ) {
			this.states = null;
		} else {
			// filter out duplicates
			Set<InstructionState> set = EnumSet.copyOf(states);
			this.states = new ArrayList<InstructionState>(set);
		}
	}

	/**
	 * Get a set of instruction IDs.
	 * 
	 * @since 1.1
	 */
	@Override
	public Long[] getInstructionIds() {
		return instructionIds;
	}

	/**
	 * Set an instruction IDs list.
	 * 
	 * @param instructionIds
	 *        the IDs to set
	 * @since 1.1
	 */
	public void setInstructionIds(Long[] instructionIds) {
		this.instructionIds = instructionIds;
	}

	@Override
	public Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date (inclusive).
	 * 
	 * @param startDate
	 *        the start date
	 * @since 2.1
	 */
	public void setStartDate(Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date (exclusive).
	 * 
	 * @param endDate
	 *        the end date
	 * @since 2.1
	 */
	public void setEndDate(Instant endDate) {
		this.endDate = endDate;
	}

}
