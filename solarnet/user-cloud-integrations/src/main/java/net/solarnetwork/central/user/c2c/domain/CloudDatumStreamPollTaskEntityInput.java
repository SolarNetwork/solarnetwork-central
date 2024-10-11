/* ==================================================================
 * CloudDatumStreamPollTaskEntityInput.java - 12/10/2024 7:04:16 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import java.util.Map;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.validation.StartStopClaimableJobState;

/**
 * DTO for cloud datum stream poll task entity.
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamPollTaskEntityInput {

	@NotNull
	@StartStopClaimableJobState
	private BasicClaimableJobState state;

	@NotNull
	private Instant executeAt;

	@NotNull
	private Instant startAt;

	@Size(max = 4096)
	private String message;

	private Map<String, Object> serviceProperties;

	/**
	 * Constructor.
	 */
	public CloudDatumStreamPollTaskEntityInput() {
		super();
	}

	public CloudDatumStreamPollTaskEntity toEntity(UserLongCompositePK id) {
		CloudDatumStreamPollTaskEntity conf = new CloudDatumStreamPollTaskEntity(
				requireNonNullArgument(id, "id"));
		populateConfiguration(conf);
		return conf;
	}

	private void populateConfiguration(CloudDatumStreamPollTaskEntity conf) {
		conf.setState(state);
		conf.setExecuteAt(executeAt);
		conf.setStartAt(startAt);
		conf.setMessage(message);
		conf.setServiceProps(serviceProperties);
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
	 * Get the start time at which the data should be queried.
	 *
	 * @return the date
	 */
	public final Instant getStartAt() {
		return startAt;
	}

	/**
	 * Set the start time at which the data should be queried.
	 *
	 * @param startAt
	 *        the date to set
	 */
	public final void setStartAt(Instant startAt) {
		this.startAt = startAt;
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

}
