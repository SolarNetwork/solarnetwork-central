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

import java.util.LinkedHashMap;
import java.util.Map;

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
	private InstructionState state;
	
	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>(2);
		if ( nodeId != null ) {
			f.put("nodeId", nodeId);
		}
		if ( state != null ) {
			f.put("state", state.toString());
		}
		return f;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public InstructionState getState() {
		return state;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
	public void setState(InstructionState state) {
		this.state = state;
	}

}
