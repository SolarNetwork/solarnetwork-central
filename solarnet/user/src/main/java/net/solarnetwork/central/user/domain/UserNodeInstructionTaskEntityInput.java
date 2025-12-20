/* ==================================================================
 * UserNodeInstructionTaskEntityInput.java - 12/11/2025 10:35:00 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.validation.StartStopClaimableJobState;

/**
 * DTO for user instruction task entity.
 *
 * @author matt
 * @version 1.0
 */
public class UserNodeInstructionTaskEntityInput {

	private boolean enabled;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String topic;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String schedule;

	@NotNull
	@StartStopClaimableJobState
	private BasicClaimableJobState state;

	@NotNull
	private Instant executeAt;

	private Map<String, Object> serviceProperties;

	private Set<BasicClaimableJobState> requiredStates;

	/**
	 * Constructor.
	 */
	public UserNodeInstructionTaskEntityInput() {
		super();
	}

	/**
	 * Create an entity instance from this input.
	 *
	 * @param id
	 *        the ID to assign to the entity
	 * @return the entity
	 */
	public UserNodeInstructionTaskEntity toEntity(UserLongCompositePK id) {
		UserNodeInstructionTaskEntity conf = new UserNodeInstructionTaskEntity(
				requireNonNullArgument(id, "id"));
		conf.setEnabled(enabled);
		conf.setName(name);
		conf.setNodeId(nodeId);
		conf.setTopic(topic);
		conf.setSchedule(schedule);
		conf.setState(state);
		conf.setExecuteAt(executeAt);
		conf.setServiceProps(serviceProperties);
		return conf;
	}

	/**
	 * Get the enabled state.
	 * 
	 * @return the enabled state
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled state.
	 * 
	 * @param enabled
	 *        the state to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
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

	/**
	 * Get the topic.
	 *
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * Set the topic.
	 *
	 * @param topic
	 *        the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * Get the schedule at which to execute the instruction.
	 *
	 * @return the schedule, as either a cron schedule or a number of seconds
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule at which to execute the instruction.
	 *
	 * @param schedule
	 *        the schedule to set, as either a cron schedule or a number of
	 *        seconds
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the job state.
	 *
	 * @return the state
	 */
	public final BasicClaimableJobState getState() {
		return state;
	}

	/**
	 * Set the job state.
	 *
	 * @param state
	 *        the state to set
	 */
	public final void setState(BasicClaimableJobState state) {
		this.state = state;
	}

	/**
	 * Get the time at which the job should next execute.
	 *
	 * @return the date
	 */
	public final Instant getExecuteAt() {
		return executeAt;
	}

	/**
	 * Set the time at which the job should next execute.
	 *
	 * @param executeAt
	 *        the date to set
	 */
	public final void setExecuteAt(Instant executeAt) {
		this.executeAt = executeAt;
	}

	/**
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public Map<String, Object> getServiceProperties() {
		return serviceProperties;
	}

	/**
	 * Set the service properties to use.
	 *
	 * @param serviceProperties
	 *        the service properties to set
	 */
	public void setServiceProperties(Map<String, Object> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	/**
	 * Get a list of states the job must have in order to perform an update.
	 *
	 * @return the states, or {@literal null}
	 */
	public final Set<BasicClaimableJobState> getRequiredStates() {
		return requiredStates;
	}

	/**
	 * Set a list of states the job must have in order to perform an update.
	 *
	 * @param requiredStates
	 *        the states to set, or {@literal null}
	 */
	public final void setRequiredStates(Set<BasicClaimableJobState> requiredStates) {
		this.requiredStates = requiredStates;
	}

}
