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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * DTO for cloud control configuration.
 *
 * @author matt
 * @version 1.0
 */
public class CloudControlConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<CloudControlConfiguration, UserLongCompositePK>
		implements CloudIntegrationsConfigurationInput<CloudControlConfiguration, UserLongCompositePK> {

	@NotNull
	private Long integrationId;

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String controlId;

	@NotNull
	@NotBlank
	@Size(max = 4096)
	private String controlReference;

	/**
	 * Constructor.
	 */
	public CloudControlConfigurationInput() {
		super();
	}

	@Override
	public CloudControlConfiguration toEntity(UserLongCompositePK id, Instant date) {
		CloudControlConfiguration conf = new CloudControlConfiguration(requireNonNullArgument(id, "id"),
				date);
		populateConfiguration(conf);
		return conf;
	}

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
	public Long getIntegrationId() {
		return integrationId;
	}

	/**
	 * Set the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public void setIntegrationId(Long integrationId) {
		this.integrationId = integrationId;
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
	 *        the node to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control reference.
	 *
	 * @return the control reference
	 */
	public String getControlReference() {
		return controlReference;
	}

	/**
	 * Set the control reference.
	 *
	 * @param controlReference
	 *        the control reference to set
	 */
	public void setControlReference(String controlReference) {
		this.controlReference = controlReference;
	}

}
