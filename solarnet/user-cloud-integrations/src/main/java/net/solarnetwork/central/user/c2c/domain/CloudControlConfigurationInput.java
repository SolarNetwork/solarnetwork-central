/* ==================================================================
 * CloudControlConfigurationInput.java - 3/11/2025 8:26:57 am
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
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.domain.NodeIdRelated;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for cloud control configuration.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class CloudControlConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<CloudControlConfiguration, UserLongCompositePK>
		implements CloudIntegrationsConfigurationInput<CloudControlConfiguration, UserLongCompositePK>,
		NodeIdRelated {

	@NotNull
	private @Nullable Long integrationId;

	@NotNull
	private @Nullable Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private @Nullable String controlId;

	@NotNull
	@NotBlank
	@Size(max = 4096)
	private @Nullable String controlReference;

	/**
	 * Constructor.
	 */
	public CloudControlConfigurationInput() {
		super();
	}

	@SuppressWarnings("NullAway")
	@Override
	public CloudControlConfiguration toEntity(UserLongCompositePK id, Instant date) {
		CloudControlConfiguration conf = new CloudControlConfiguration(requireNonNullArgument(id, "id"),
				date, getName(), getServiceIdentifier(), integrationId, nodeId, controlId);
		populateConfiguration(conf);
		return conf;
	}

	@SuppressWarnings("NullAway")
	@Override
	protected void populateConfiguration(CloudControlConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setIntegrationId(integrationId);
		conf.setNodeId(nodeId);
		conf.setControlId(controlId);
		conf.setControlReference(controlReference);
	}

	/**
	 * Get the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @return the integration ID
	 */
	public final @Nullable Long getIntegrationId() {
		return integrationId;
	}

	/**
	 * Set the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public final void setIntegrationId(@Nullable Long integrationId) {
		this.integrationId = integrationId;
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	@Override
	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the node to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public final @Nullable String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public final void setControlId(@Nullable String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control reference.
	 *
	 * @return the control reference
	 */
	public final @Nullable String getControlReference() {
		return controlReference;
	}

	/**
	 * Set the control reference.
	 *
	 * @param controlReference
	 *        the control reference to set
	 */
	public final void setControlReference(@Nullable String controlReference) {
		this.controlReference = controlReference;
	}

}
