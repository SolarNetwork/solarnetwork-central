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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.domain.InstructionStatus;

/**
 * Instruction for a specific node.
 * 
 * @author matt
 * @version 2.1
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
	public NodeInstruction(String topic, Instant instructionDate, Long nodeId) {
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

	public InstructionStatus toStatus() {
		return new NodeInstructionStatus();
	}

	private final class NodeInstructionStatus implements net.solarnetwork.domain.InstructionStatus {

		@Override
		public Long getInstructionId() {
			return getId();
		}

		@Override
		public Instant getStatusDate() {
			return NodeInstruction.this.getStatusDate();
		}

		@Override
		public Map<String, ?> getResultParameters() {
			return NodeInstruction.this.getResultParameters();
		}

		@Override
		public InstructionStatus.InstructionState getInstructionState() {
			return switch (getState()) {
				case Completed -> InstructionStatus.InstructionState.Completed;
				case Declined -> InstructionStatus.InstructionState.Declined;
				case Executing -> InstructionStatus.InstructionState.Executing;
				case Queued -> InstructionStatus.InstructionState.Queued;
				case Queuing -> InstructionStatus.InstructionState.Queuing;
				case Received -> InstructionStatus.InstructionState.Received;
				case Unknown -> InstructionStatus.InstructionState.Unknown;
			};
		}

		@Override
		public InstructionStatus newCopyWithState(InstructionStatus.InstructionState newState,
				Map<String, ?> resultParameters) {
			var copy = new NodeInstruction(NodeInstruction.this);
			copy.setState(switch (newState) {
				case Completed -> net.solarnetwork.central.instructor.domain.InstructionState.Completed;
				case Declined -> net.solarnetwork.central.instructor.domain.InstructionState.Declined;
				case Executing -> net.solarnetwork.central.instructor.domain.InstructionState.Executing;
				case Queued -> net.solarnetwork.central.instructor.domain.InstructionState.Queued;
				case Queuing -> net.solarnetwork.central.instructor.domain.InstructionState.Queuing;
				case Received -> net.solarnetwork.central.instructor.domain.InstructionState.Received;
				case Unknown -> net.solarnetwork.central.instructor.domain.InstructionState.Unknown;
			});
			copy.setStatusDate(Instant.now());
			if ( resultParameters != null ) {
				if ( copy.getResultParameters() != null ) {
					copy.getResultParameters().putAll(resultParameters);
				} else {
					Map<String, Object> p = new HashMap<>(resultParameters.size());
					p.putAll(resultParameters);
					copy.setResultParameters(p);
				}
			}
			return copy.toStatus();
		}
	}

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 * 
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

}
