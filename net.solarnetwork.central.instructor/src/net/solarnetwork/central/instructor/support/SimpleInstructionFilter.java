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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.instructor.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.instructor.domain.InstructionFilter;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.util.SerializeIgnore;

/**
 * Simple implementation of {@link InstructionFilter}.
 * 
 * @author matt
 * @version $Revision$
 */
public class SimpleInstructionFilter implements InstructionFilter {

	private Long nodeId;
	private List<InstructionState> states;

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>(2);
		if ( nodeId != null ) {
			f.put("nodeId", nodeId);
		}
		if ( states != null && states.isEmpty() == false ) {
			f.put("state", states.iterator().next().toString());
			if ( states.size() > 1 ) {
				f.put("states", states.toArray(new InstructionState[states.size()]));
			}
		}
		return f;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public InstructionState getState() {
		return (states != null && states.isEmpty() == false ? states.iterator().next() : null);
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
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

}
