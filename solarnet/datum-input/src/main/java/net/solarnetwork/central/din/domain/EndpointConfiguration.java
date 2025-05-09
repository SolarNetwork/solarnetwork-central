/* ==================================================================
 * EndpointConfiguration.java - 19/02/2024 5:03:49 pm
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

package net.solarnetwork.central.din.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * Datum input endpoint configuration.
 *
 * @author matt
 * @version 1.2
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "endpointId", "created", "modified", "enabled", "name", "nodeId",
		"sourceId", "transformId", "publishToSolarFlux", "previousInputTracking", "includeResponseBody",
		"requestContentType" })
public class EndpointConfiguration extends BaseUserModifiableEntity<EndpointConfiguration, UserUuidPK>
		implements DatumInputConfigurationEntity<EndpointConfiguration, UserUuidPK> {

	@Serial
	private static final long serialVersionUID = -3845517573525287732L;

	private String name;
	private Long nodeId;
	private String sourceId;
	private Long transformId;
	private boolean publishToSolarFlux = true;
	private boolean previousInputTracking;
	private boolean includeResponseBody;
	private String requestContentType;

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
	 * @param userId
	 *        the user ID
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
		entity.setNodeId(nodeId);
		entity.setSourceId(sourceId);
		entity.setTransformId(transformId);
		entity.setPublishToSolarFlux(publishToSolarFlux);
		entity.setPreviousInputTracking(previousInputTracking);
		entity.setIncludeResponseBody(includeResponseBody);
		entity.setRequestContentType(requestContentType);
	}

	@Override
	public boolean isSameAs(EndpointConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.name)
				&& Objects.equals(this.nodeId, other.nodeId)
				&& Objects.equals(this.sourceId, other.sourceId)
				&& Objects.equals(this.transformId, other.transformId)
				&& publishToSolarFlux == other.publishToSolarFlux
				&& previousInputTracking == other.previousInputTracking
				&& includeResponseBody == other.includeResponseBody
				&& Objects.equals(requestContentType, other.requestContentType)
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
		if ( nodeId != null ) {
			builder.append("nodeId=");
			builder.append(nodeId);
			builder.append(", ");
		}
		if ( sourceId != null ) {
			builder.append("sourceId=");
			builder.append(sourceId);
			builder.append(", ");
		}
		if ( transformId != null ) {
			builder.append("transformId=");
			builder.append(transformId);
			builder.append(", ");
		}
		builder.append("enabled=");
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
	 * Get the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param sourceId
	 *        the source ID to set; a blank value will be normalized to
	 *        {@literal null}
	 */
	public void setSourceId(String sourceId) {
		if ( sourceId != null && sourceId.isBlank() ) {
			sourceId = null;
		}
		this.sourceId = sourceId;
	}

	/**
	 * Get the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @return the ID
	 */
	public Long getTransformId() {
		return transformId;
	}

	/**
	 * Set the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @param transformId
	 *        the ID to set
	 */
	public void setTransformId(Long transformId) {
		this.transformId = transformId;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 *
	 * @return {@literal true} if data from this endpoint should be published to
	 *         SolarFlux; defaults to {@literal true}
	 */
	public boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this endpoint should be published to
	 *        SolarFlux
	 */
	public void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

	/**
	 * Get the flag to track previous input values.
	 *
	 * @return {@literal true} to track previous input values; defaults to
	 *         {@literal false}
	 * @since 1.1
	 */
	public boolean isPreviousInputTracking() {
		return previousInputTracking;
	}

	/**
	 * Set the flag to track previous input values.
	 *
	 * @param previousInputTracking
	 *        {@literal true} to track previous input values
	 * @since 1.1
	 */
	public void setPreviousInputTracking(boolean previousInputTracking) {
		this.previousInputTracking = previousInputTracking;
	}

	/**
	 * Get the "include response body" flag.
	 *
	 * @return {@literal true} to include the response content
	 * @since 1.2
	 */
	public boolean isIncludeResponseBody() {
		return includeResponseBody;
	}

	/**
	 * Set the "include response body" flag.
	 *
	 * @param includeResponseBody
	 *        {@literal true} to include the response content
	 * @since 1.2
	 */
	public void setIncludeResponseBody(boolean includeResponseBody) {
		this.includeResponseBody = includeResponseBody;
	}

	/**
	 * Get an implicit request content type.
	 *
	 * @return the request content type to assume
	 * @since 1.2
	 */
	public String getRequestContentType() {
		return requestContentType;
	}

	/**
	 * Set an implicit request content type.
	 *
	 * @param requestContentType
	 *        the request content to assume, or {@literal null} to; a blank
	 *        value will be normalized to {@literal null}
	 * @since 1.2
	 */
	public void setRequestContentType(String requestContentType) {
		if ( requestContentType != null && requestContentType.isBlank() ) {
			requestContentType = null;
		}
		this.requestContentType = requestContentType;
	}

}
