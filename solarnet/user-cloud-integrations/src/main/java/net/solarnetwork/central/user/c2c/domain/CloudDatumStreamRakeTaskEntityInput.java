/* ==================================================================
 * CloudDatumStreamRakeTaskEntityInput.java - 22/09/2025 9:44:28 am
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

package net.solarnetwork.central.user.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.Period;
import java.util.Map;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.validation.PositiveTemporalAmount;
import net.solarnetwork.central.domain.validation.StartStopClaimableJobState;

/**
 * DTO for cloud datum stream rake task entity.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamRakeTaskEntityInput {

	@NotNull
	private Long datumStreamId;

	@NotNull
	@StartStopClaimableJobState
	private BasicClaimableJobState state;

	@NotNull
	private Instant executeAt;

	@NotNull
	@PositiveTemporalAmount
	private Period offset;

	@Size(max = 4096)
	private String message;

	private Map<String, Object> serviceProperties;

	private Set<BasicClaimableJobState> requiredStates;

	/**
	 * Constructor.
	 */
	public CloudDatumStreamRakeTaskEntityInput() {
		super();
	}

	public CloudDatumStreamRakeTaskEntity toEntity(UserLongCompositePK id) {
		CloudDatumStreamRakeTaskEntity conf = new CloudDatumStreamRakeTaskEntity(
				requireNonNullArgument(id, "id"));
		populateConfiguration(conf);
		return conf;
	}

	private void populateConfiguration(CloudDatumStreamRakeTaskEntity conf) {
		conf.setDatumStreamId(datumStreamId);
		conf.setState(state);
		conf.setExecuteAt(executeAt);
		conf.setOffset(offset);
		conf.setMessage(message);
		conf.setServiceProps(serviceProperties);
	}

	/**
	 * Get the datum stream ID.
	 *
	 * @return the datum stream ID
	 */
	public Long getDatumStreamId() {
		return datumStreamId;
	}

	/**
	 * Set the datum stream ID.
	 *
	 * @param datumStreamId
	 *        the datum stream ID to set
	 */
	public void setDatumStreamId(Long datumStreamId) {
		this.datumStreamId = datumStreamId;
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
	 * Get the offset at which the data should be queried.
	 *
	 * @return the offset
	 */
	public Period getOffset() {
		return offset;
	}

	/**
	 * Set the offset at which the data should be queried.
	 *
	 * @param offset
	 *        the offset to set
	 */
	public void setOffset(Period offset) {
		this.offset = offset;
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
	 * Get the status message.
	 *
	 * @return the message
	 */
	public final String getMessage() {
		return message;
	}

	/**
	 * Set the status message.
	 *
	 * @param message
	 *        the message to set
	 */
	public final void setMessage(String message) {
		this.message = message;
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
