/* ==================================================================
 * CloudDatumStreamPollTaskEntity.java - 9/10/2024 6:52:22 pm
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

package net.solarnetwork.central.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Cloud datum stream poll task runtime information.
 *
 * @author matt
 * @version 1.1
 */
@JsonIgnoreProperties({ "id", "enabled" })
@JsonPropertyOrder({ "userId", "datumStreamId", "state", "executeAt", "startAt", "message",
		"serviceProperties" })
public final class CloudDatumStreamPollTaskEntity
		extends BaseUserModifiableEntity<CloudDatumStreamPollTaskEntity, UserLongCompositePK> {

	@Serial
	private static final long serialVersionUID = -8913216603960129724L;

	/** The job state. */
	private BasicClaimableJobState state;

	/** The next time the job should execute. */
	private Instant executeAt;

	/** The start time data should be queried from. */
	private Instant startAt;

	/** A status message. */
	private @Nullable String message;

	/** The service properties as JSON. */
	private @Nullable String servicePropsJson;

	/** The service properties. */
	private volatile transient @Nullable Map<String, Object> serviceProps;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param state
	 *        the state
	 * @param executeAt
	 *        the execute date
	 * @param startAt
	 *        the start date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudDatumStreamPollTaskEntity(UserLongCompositePK id, BasicClaimableJobState state,
			Instant executeAt, Instant startAt) {
		super(id);
		this.state = requireNonNullArgument(state, "state");
		this.executeAt = requireNonNullArgument(executeAt, "executeAt");
		this.startAt = requireNonNullArgument(startAt, "startAt");
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param dataSourceId
	 *        the data source ID
	 * @param state
	 *        the state
	 * @param executeAt
	 *        the execute date
	 * @param startAt
	 *        the start date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CloudDatumStreamPollTaskEntity(Long userId, Long dataSourceId, BasicClaimableJobState state,
			Instant executeAt, Instant startAt) {
		this(new UserLongCompositePK(userId, dataSourceId), state, executeAt, startAt);
	}

	@Override
	public CloudDatumStreamPollTaskEntity copyWithId(UserLongCompositePK id) {
		var copy = new CloudDatumStreamPollTaskEntity(id, state, executeAt, startAt);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamPollTaskEntity entity) {
		super.copyTo(entity);
		entity.setState(state);
		entity.setExecuteAt(executeAt);
		entity.setStartAt(startAt);
		entity.setMessage(message);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(@Nullable CloudDatumStreamPollTaskEntity other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.state, o.state)
				&& Objects.equals(this.executeAt, o.executeAt)
				&& Objects.equals(this.startAt, o.startAt)
				&& Objects.equals(this.message, o.message)
				&& Objects.equals(getServiceProperties(), o.getServiceProperties())
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(64);
		builder.append("CloudDatumStreamPollTaskEntity{");
		builder.append("userId=");
		builder.append(getUserId());
		builder.append(", datumStreamId=");
		builder.append(getDatumStreamId());
		if ( state != null ) {
			builder.append(", state=");
			builder.append(state);
		}
		if ( executeAt != null ) {
			builder.append(", executeAt=");
			builder.append(executeAt);
		}
		if ( startAt != null ) {
			builder.append(", startAt=");
			builder.append(startAt);
		}
		if ( message != null ) {
			builder.append(", message=");
			builder.append(message);
		}
		if ( serviceProps != null ) {
			builder.append(", serviceProps=");
			builder.append(serviceProps);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the cloud datum stream ID.
	 *
	 * @return the cloud datum stream ID
	 */
	public final Long getDatumStreamId() {
		return pk().getEntityId();
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setState(BasicClaimableJobState state) {
		this.state = requireNonNullArgument(state, "state");
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setStartAt(Instant startAt) {
		this.startAt = requireNonNullArgument(startAt, "startAt");
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
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setExecuteAt(Instant executeAt) {
		this.executeAt = requireNonNullArgument(executeAt, "executeAt");
		;
	}

	/**
	 * Get the status message.
	 *
	 * @return the message
	 */
	public final @Nullable String getMessage() {
		return message;
	}

	/**
	 * Set the status message.
	 *
	 * @param message
	 *        the message to set
	 */
	public final void setMessage(@Nullable String message) {
		this.message = message;
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@code null} if no service properties
	 *         available
	 */
	@JsonIgnore
	public final @Nullable String getServicePropsJson() {
		return servicePropsJson;
	}

	/**
	 * Set the service properties object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created service properties and
	 * replace it with the values parsed from the JSON. All floating point
	 * values will be converted to {@link BigDecimal} instances.
	 * </p>
	 *
	 * @param json
	 *        the JSON to parse as service properties
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public final void setServicePropsJson(@Nullable String json) {
		servicePropsJson = json;
		serviceProps = null;
	}

	/**
	 * Get the service properties.
	 *
	 * <p>
	 * This will decode the {@link #getServicePropsJson()} value into a map
	 * instance.
	 * </p>
	 *
	 * @return the service properties
	 */
	@JsonIgnore
	public final @Nullable Map<String, Object> getServiceProps() {
		if ( serviceProps == null && servicePropsJson != null ) {
			serviceProps = JsonUtils.getStringMap(servicePropsJson);
		}
		return serviceProps;
	}

	/**
	 * Set the service properties to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setServicePropsJson(String)} as well.
	 * </p>
	 *
	 * @param serviceProps
	 *        the service properties to set
	 */
	@JsonSetter("serviceProperties")
	public final void setServiceProps(@Nullable Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
	}

	/**
	 * Add a collection of service properties.
	 *
	 * @param props
	 *        the properties to add
	 */
	public final void putServiceProps(@Nullable Map<String, Object> props) {
		Map<String, Object> serviceProps = getServiceProps();
		if ( serviceProps == null ) {
			serviceProps = props;
		} else {
			serviceProps.putAll(props);
		}
		setServiceProps(serviceProps);
	}

	/**
	 * Get the service properties.
	 *
	 * @return the service properties
	 */
	public final @Nullable Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
