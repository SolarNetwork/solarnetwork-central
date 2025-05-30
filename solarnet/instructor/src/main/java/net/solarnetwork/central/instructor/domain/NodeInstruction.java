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

import java.io.Serial;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.EntityMatch;
import net.solarnetwork.central.instructor.support.NodeInstructionSerializer;
import net.solarnetwork.domain.InstructionStatus;

/**
 * Instruction for a specific node.
 *
 * @author matt
 * @version 3.0
 */
@JsonSerialize(using = NodeInstructionSerializer.class)
public class NodeInstruction extends BaseEntity implements EntityMatch {

	@Serial
	private static final long serialVersionUID = 4904518821205446583L;

	private Long nodeId;
	private Instruction instruction;

	/**
	 * Default constructor.
	 */
	public NodeInstruction() {
		super();
		setInstruction(new Instruction());
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
		this(topic, instructionDate, nodeId, null);
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
	 * @since 2.4
	 */
	public NodeInstruction(String topic, Instant instructionDate, Long nodeId, Instant expirationDate) {
		super();
		setNodeId(nodeId);
		Instruction instr = new Instruction(topic, instructionDate);
		instr.setExpirationDate(expirationDate);
		setInstruction(instr);
	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *        the instance to copy
	 * @since 1.1
	 */
	public NodeInstruction(NodeInstruction other) {
		super();
		setInstruction(new Instruction());
		other.copyTo(this);
	}

	@Override
	public NodeInstruction clone() {
		return (NodeInstruction) super.clone();
	}

	/**
	 * Create a copy with a specific node ID.
	 *
	 * @param nodeId
	 *        the node ID to assign to the copy
	 * @return the new copy
	 * @since 2.3
	 */
	public NodeInstruction copyWithNodeId(Long nodeId) {
		NodeInstruction copy = new NodeInstruction(this);
		copyTo(copy);
		copy.setNodeId(nodeId);
		return copy;
	}

	/**
	 * Copy the properties of this class to another instance.
	 * 
	 * <p>
	 * Note the properties of the super class are <b>not</b> copied.
	 * </p>
	 * 
	 * @param other
	 *        the instance to copy to
	 * @since 2.4
	 */
	public void copyTo(NodeInstruction other) {
		other.setNodeId(nodeId);
		other.setInstruction(new Instruction(this.instruction));
	}

	/**
	 * Get the status.
	 * 
	 * @return the status
	 */
	public InstructionStatus toStatus() {
		return new NodeInstructionStatus();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Instruction{id=");
		builder.append(getId());
		builder.append(", nodeId=");
		builder.append(nodeId);
		if ( instruction.getExpirationDate() != null ) {
			builder.append(", expirationDate=");
			builder.append(instruction.getExpirationDate());
		}
		builder.append(", topic=");
		builder.append(instruction.getTopic());
		builder.append(", state=");
		builder.append(instruction.getState());
		builder.append(", parameters=");
		builder.append(instruction.getParameters());
		builder.append("}");
		return builder.toString();
	}

	private final class NodeInstructionStatus implements net.solarnetwork.domain.InstructionStatus {

		@Override
		public Long getInstructionId() {
			return getId();
		}

		@Override
		public Instant getStatusDate() {
			return instruction.getStatusDate();
		}

		@Override
		public Map<String, ?> getResultParameters() {
			return instruction.getResultParameters();
		}

		@Override
		public InstructionStatus.InstructionState getInstructionState() {
			return instruction.getState();
		}

		@Override
		public InstructionStatus newCopyWithState(InstructionStatus.InstructionState newState,
				Map<String, ?> resultParameters) {
			var copy = new NodeInstruction(NodeInstruction.this);
			copy.instruction.setState(newState);
			copy.instruction.setStatusDate(Instant.now());
			if ( resultParameters != null ) {
				if ( copy.instruction.getResultParameters() != null ) {
					copy.instruction.getResultParameters().putAll(resultParameters);
				} else {
					Map<String, Object> p = new HashMap<>(resultParameters.size());
					p.putAll(resultParameters);
					copy.instruction.setResultParameters(p);
				}
			}
			return copy.toStatus();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NodeInstructionStatus{");
			if ( getInstructionId() != null ) {
				builder.append("id=");
				builder.append(getInstructionId());
				builder.append(", ");
			}
			if ( getResultParameters() != null ) {
				builder.append("resultParameters=");
				builder.append(getResultParameters());
			}
			builder.append("}");
			return builder.toString();
		}

	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public final Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public final void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the expiration date.
	 * 
	 * @return the expiration date
	 */
	public Instant getExpirationDate() {
		return instruction.getExpirationDate();
	}

	/**
	 * Set the expiration date.
	 * 
	 * <p>
	 * This date represents the point in time that a "pending" instruction can
	 * be automatically transitioned to the {@code Declined} state, adding an
	 * appropriate {@code message} result property.
	 * </p>
	 * 
	 * @param expirationDate
	 *        the expiration date to set
	 */
	public void setExpirationDate(Instant expirationDate) {
		instruction.setExpirationDate(expirationDate);
	}

	/**
	 * Get the instruction.
	 * 
	 * @return the instruction
	 */
	public Instruction getInstruction() {
		return instruction;
	}

	/**
	 * Set the instruction.
	 * 
	 * @param instruction
	 *        the instruction to set
	 */
	public void setInstruction(Instruction instruction) {
		this.instruction = instruction;
	}

}
