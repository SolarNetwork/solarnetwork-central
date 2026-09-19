/* ==================================================================
 * EndpointConfigurationInput.java - 25/02/2024 7:47:43 am
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

package net.solarnetwork.central.user.din.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * DTO for datum input endpoint configuration.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class EndpointConfigurationInput
		extends BaseDatumInputConfigurationInput<EndpointConfiguration, UserUuidPK> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String name;

	private @Nullable Long nodeId;
	private @Nullable String sourceId;

	@NotNull
	private @Nullable Long transformId;

	private boolean publishToSolarFlux = true;
	private boolean previousInputTracking = false;

	private boolean includeResponseBody = false;

	@Size(max = 96)
	private @Nullable String requestContentType;

	/**
	 * Constructor.
	 */
	public EndpointConfigurationInput() {
		super();
	}

	@SuppressWarnings("NullAway")
	@Override
	public EndpointConfiguration toEntity(UserUuidPK id, Instant date) {
		EndpointConfiguration conf = new EndpointConfiguration(requireNonNullArgument(id, "id"), date,
				name);
		populateConfiguration(conf);
		return conf;
	}

	@SuppressWarnings("NullAway")
	@Override
	protected void populateConfiguration(EndpointConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setName(name);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setTransformId(transformId);
		conf.setPublishToSolarFlux(publishToSolarFlux);
		conf.setPreviousInputTracking(previousInputTracking);
		conf.setIncludeResponseBody(includeResponseBody);
		conf.setRequestContentType(requestContentType);
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		this.name = name;
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
	 *        the source ID to set
	 */
	public final void setSourceId(@Nullable String sourceId) {
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
	 * Get the "omit response body" flag.
	 *
	 * @return {@literal true} to not include any response content
	 * @since 1.2
	 */
	public final boolean isIncludeResponseBody() {
		return includeResponseBody;
	}

	/**
	 * Set the "omit response body" flag.
	 *
	 * @param includeResponseBody
	 *        {@literal true} to not include any response content
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
