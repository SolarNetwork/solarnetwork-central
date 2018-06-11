/* ==================================================================
 * NodeInstruction.java - Mar 1, 2011 11:28:59 AM
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

import org.joda.time.DateTime;
import net.solarnetwork.central.domain.EntityMatch;

/**
 * Instruction for a specific node.
 * 
 * @author matt
 * @version 1.1
 */
public class NodeInstruction extends Instruction implements EntityMatch {

	private static final long serialVersionUID = -8910808111207075055L;

	private Long nodeId;

	/**
	 * Default constructor.
	 */
	public NodeInstruction() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param topic
	 *        the topic
	 * @param instructionDate
	 *        the instruction date
	 * @param nodeId
	 *        the node ID
	 */
	public NodeInstruction(String topic, DateTime instructionDate, Long nodeId) {
		super(topic, instructionDate);
		setNodeId(nodeId);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the instance to copy
	 * @since 1.1
	 */
	public NodeInstruction(NodeInstruction other) {
		super(other);
		setNodeId(other.getNodeId());

	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
