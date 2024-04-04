/* ==================================================================
 * EndpointConfiguration.java - 28/03/2024 11:33:36 am
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

package net.solarnetwork.central.inin.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Datum input endpoint configuration.
 *
 * @author matt
 * @version 1.1
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "endpointId", "created", "modified", "enabled", "name", "nodeIds",
		"requestTransformId", "responseTransformId", "maxExecutionSeconds", "userMetadataPath" })
public class EndpointConfiguration extends BaseUserModifiableEntity<EndpointConfiguration, UserUuidPK>
		implements InstructionInputConfigurationEntity<EndpointConfiguration, UserUuidPK> {

	private static final long serialVersionUID = -7843134190113157004L;

	/** The {@code maxExecutionSeconds} property default value. */
	public static final int DEFAULT_MAX_EXECUTION_SECONDS = 10;

	private String name;
	private Set<Long> nodeIds;
	private Long requestTransformId;
	private Long responseTransformId;
	private int maxExecutionSeconds = DEFAULT_MAX_EXECUTION_SECONDS;
	private String userMetadataPath;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointConfiguration(UserUuidPK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointConfiguration(Long userId, UUID entityId, Instant created) {
		this(new UserUuidPK(userId, entityId), created);
	}

	@Override
	public EndpointConfiguration copyWithId(UserUuidPK id) {
		var copy = new EndpointConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(EndpointConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setNodeIds(nodeIds);
		entity.setRequestTransformId(requestTransformId);
		entity.setResponseTransformId(responseTransformId);
		entity.setMaxExecutionSeconds(maxExecutionSeconds);
		entity.setUserMetadataPath(userMetadataPath);
	}

	@Override
	public boolean isSameAs(EndpointConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.nodeIds, other.nodeIds)
				&& Objects.equals(this.requestTransformId, other.requestTransformId)
				&& Objects.equals(this.responseTransformId, other.responseTransformId)
				&& this.maxExecutionSeconds == other.maxExecutionSeconds
				&& Objects.equals(this.userMetadataPath, other.userMetadataPath)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Endpoint{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getEndpointId() != null ) {
			builder.append("endpointId=");
			builder.append(getEndpointId());
			builder.append(", ");
		}
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if ( nodeIds != null ) {
			builder.append("nodeIds=");
			builder.append(nodeIds);
			builder.append(", ");
		}
		if ( requestTransformId != null ) {
			builder.append("requestTransformId=");
			builder.append(requestTransformId);
			builder.append(", ");
		}
		if ( responseTransformId != null ) {
			builder.append("responseTransformId=");
			builder.append(responseTransformId);
			builder.append(", ");
		}
		builder.append("maxExecutionSeconds=");
		builder.append(maxExecutionSeconds);
		if ( userMetadataPath != null ) {
			builder.append(", userMetadataPath=");
			builder.append(userMetadataPath);
		}
		builder.append(", enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	public UUID getEndpointId() {
		UserUuidPK id = getId();
		return (id != null ? id.getUuid() : null);
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
	 * Get the node ID set.
	 *
	 * @return the node IDs
	 */
	public Set<Long> getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node ID set.
	 *
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public void setNodeIds(Set<Long> nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the ID of the associated request {@link TransformConfiguration}
	 * entity.
	 *
	 * @return the ID
	 */
	public Long getRequestTransformId() {
		return requestTransformId;
	}

	/**
	 * Set the ID of the associated request {@link TransformConfiguration}
	 * entity.
	 *
	 * @param transformId
	 *        the ID to set
	 */
	public void setRequestTransformId(Long transformId) {
		this.requestTransformId = transformId;
	}

	/**
	 * Get the ID of the associated response {@link TransformConfiguration}
	 * entity.
	 *
	 * @return the ID
	 */
	public Long getResponseTransformId() {
		return responseTransformId;
	}

	/**
	 * Set the ID of the associated response {@link TransformConfiguration}
	 * entity.
	 *
	 * @param transformId
	 *        the ID to set
	 */
	public void setResponseTransformId(Long transformId) {
		this.responseTransformId = transformId;
	}

	/**
	 * Get the maximum execution seconds.
	 *
	 * @return the seconds; defaults to {@link #DEFAULT_MAX_EXECUTION_SECONDS}
	 */
	public int getMaxExecutionSeconds() {
		return maxExecutionSeconds;
	}

	/**
	 * Set the maximum execution seconds.
	 *
	 * @param maxExecutionSeconds
	 *        the seconds to set; anything less than 1 will be saved as 1
	 */
	public void setMaxExecutionSeconds(int maxExecutionSeconds) {
		this.maxExecutionSeconds = (maxExecutionSeconds > 0 ? maxExecutionSeconds : 1);
	}

	/**
	 * Get the user metadata path to provide to the transforms.
	 *
	 * @return the userMetadataPath the user metadata path to extract
	 * @since 1.1
	 */
	public String getUserMetadataPath() {
		return userMetadataPath;
	}

	/**
	 * Set the user metadata path to provide to the transforms.
	 *
	 * @param userMetadataPath
	 *        the user metadata path to set
	 * @see net.solarnetwork.domain.datum.DatumMetadataOperations#metadataAtPath(String)
	 * @since 1.1
	 */
	public void setUserMetadataPath(String userMetadataPath) {
		this.userMetadataPath = userMetadataPath;
	}

}
