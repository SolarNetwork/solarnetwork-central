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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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
	private @Nullable Long nodeId;
	private @Nullable String sourceId;
	private @Nullable Long transformId;
	private boolean publishToSolarFlux = true;
	private boolean previousInputTracking;
	private boolean includeResponseBody;
	private @Nullable String requestContentType;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public EndpointConfiguration(UserUuidPK id, Instant created, String name) {
		super(id, created);
		this.name = requireNonNullArgument(name, "name");
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
	 * @param name
	 *        the name
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public EndpointConfiguration(Long userId, UUID entityId, Instant created, String name) {
		this(new UserUuidPK(userId, entityId), created, name);
		this.name = requireNonNullArgument(name, "name");
	}

	@Override
	public EndpointConfiguration copyWithId(UserUuidPK id) {
		var copy = new EndpointConfiguration(id, created(), name);
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
	public boolean isSameAs(@Nullable EndpointConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.name, o.name)
				&& Objects.equals(this.nodeId, o.nodeId)
				&& Objects.equals(this.sourceId, o.sourceId)
				&& Objects.equals(this.transformId, o.transformId)
				&& publishToSolarFlux == o.publishToSolarFlux
				&& previousInputTracking == o.previousInputTracking
				&& includeResponseBody == o.includeResponseBody
				&& Objects.equals(requestContentType, o.requestContentType)
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
	public final UUID getEndpointId() {
		return pk().getUuid();
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

	/**
	 * Get the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the node ID
	 */
	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the source ID
	 */
	public final @Nullable String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param sourceId
	 *        the source ID to set; a blank value will be normalized to
	 *        {@code null}
	 */
	public final void setSourceId(@Nullable String sourceId) {
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
	public final @Nullable Long getTransformId() {
		return transformId;
	}

	/**
	 * Set the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @param transformId
	 *        the ID to set
	 */
	public final void setTransformId(@Nullable Long transformId) {
		this.transformId = transformId;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 *
	 * @return {@literal true} if data from this endpoint should be published to
	 *         SolarFlux; defaults to {@literal true}
	 */
	public final boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this endpoint should be published to
	 *        SolarFlux
	 */
	public final void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

	/**
	 * Get the flag to track previous input values.
	 *
	 * @return {@literal true} to track previous input values; defaults to
	 *         {@literal false}
	 * @since 1.1
	 */
	public final boolean isPreviousInputTracking() {
		return previousInputTracking;
	}

	/**
	 * Set the flag to track previous input values.
	 *
	 * @param previousInputTracking
	 *        {@literal true} to track previous input values
	 * @since 1.1
	 */
	public final void setPreviousInputTracking(boolean previousInputTracking) {
		this.previousInputTracking = previousInputTracking;
	}

	/**
	 * Get the "include response body" flag.
	 *
	 * @return {@literal true} to include the response content
	 * @since 1.2
	 */
	public final boolean isIncludeResponseBody() {
		return includeResponseBody;
	}

	/**
	 * Set the "include response body" flag.
	 *
	 * @param includeResponseBody
	 *        {@literal true} to include the response content
	 * @since 1.2
	 */
	public final void setIncludeResponseBody(boolean includeResponseBody) {
		this.includeResponseBody = includeResponseBody;
	}

	/**
	 * Get an implicit request content type.
	 *
	 * @return the request content type to assume
	 * @since 1.2
	 */
	public final @Nullable String getRequestContentType() {
		return requestContentType;
	}

	/**
	 * Set an implicit request content type.
	 *
	 * @param requestContentType
	 *        the request content to assume, or {@code null} to; a blank value
	 *        will be normalized to {@code null}
	 * @since 1.2
	 */
	public final void setRequestContentType(@Nullable String requestContentType) {
		if ( requestContentType != null && requestContentType.isBlank() ) {
			requestContentType = null;
		}
		this.requestContentType = requestContentType;
	}

}
