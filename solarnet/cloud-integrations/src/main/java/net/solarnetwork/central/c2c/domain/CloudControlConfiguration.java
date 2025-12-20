/* ==================================================================
 * CloudControlConfiguration.java - 3/11/2025 6:54:21 am
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

package net.solarnetwork.central.c2c.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.dao.UserRelatedStdIdentifiableConfigurationEntity;
import net.solarnetwork.central.domain.UserIdentifiableSystem;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * A cloud datum stream configuration entity.
 *
 * <p>
 * The purpose of this entity is to provide external cloud control
 * configuration.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "fullyConfigured" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name", "integrationId",
		"nodeId", "controlId", "controlReference", "serviceIdentifier", "serviceProperties" })
public class CloudControlConfiguration
		extends BaseIdentifiableUserModifiableEntity<CloudControlConfiguration, UserLongCompositePK>
		implements CloudIntegrationsConfigurationEntity<CloudControlConfiguration, UserLongCompositePK>,
		UserRelatedStdIdentifiableConfigurationEntity<CloudControlConfiguration, UserLongCompositePK>,
		UserIdentifiableSystem {

	/**
	 * A system identifier component included in {@link #systemIdentifier()}.
	 */
	public static final String CLOUD_INTEGRATION_SYSTEM_IDENTIFIER = "c2c-ctrl";

	@Serial
	private static final long serialVersionUID = 1899493393926823115L;

	private Long integrationId;
	private Long nodeId;
	private String controlId;
	private String controlReference;

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
	public CloudControlConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param configId
	 *        the configuration ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CloudControlConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public CloudControlConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CloudControlConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudControlConfiguration entity) {
		super.copyTo(entity);
		entity.setIntegrationId(integrationId);
		entity.setNodeId(nodeId);
		entity.setControlId(controlId);
		entity.setControlReference(controlReference);
	}

	@Override
	public boolean isSameAs(CloudControlConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.integrationId, other.integrationId)
				&& Objects.equals(this.nodeId, other.nodeId)
				&& Objects.equals(this.controlId, other.controlId)
				&& Objects.equals(this.controlReference, other.controlReference)
				;
		// @formatter:on
	}

	@Override
	public boolean isFullyConfigured() {
		return integrationId != null && nodeId != null && controlId != null && !controlId.isEmpty()
				&& controlReference != null && !controlReference.isEmpty();
	}

	/**
	 * Create a datum ID with a given timestamp.
	 *
	 * <p>
	 * The object ID, and control ID values of this configuration will be used.
	 * The kind will be {@code Node}.
	 * </p>
	 *
	 * @param ts
	 *        the desired timestamp of the ID
	 * @return the ID
	 */
	public DatumId datumId(Instant ts) {
		return new DatumId(ObjectDatumKind.Node, nodeId, controlId, ts);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudControl{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getConfigId() != null ) {
			builder.append("configId=");
			builder.append(getConfigId());
			builder.append(", ");
		}
		if ( getName() != null ) {
			builder.append("name=");
			builder.append(getName());
			builder.append(", ");
		}
		if ( integrationId != null ) {
			builder.append("integrationId=");
			builder.append(integrationId);
			builder.append(", ");
		}
		if ( nodeId != null ) {
			builder.append("nodeId=");
			builder.append(nodeId);
			builder.append(", ");
		}
		if ( controlId != null ) {
			builder.append("controlId=");
			builder.append(controlId);
			builder.append(", ");
		}
		if ( controlReference != null ) {
			builder.append("controlReference=");
			builder.append(controlReference);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get a unique identifier based on this configuration.
	 *
	 * <p>
	 * The identifier follows this syntax:
	 * </p>
	 *
	 * <pre>{@code
	 * USER_ID:c2c-ctrl:CONFIG_ID
	 * }</pre>
	 *
	 * <p>
	 * Where {@code USER_ID} is {@link #getUserId()} and {@code CONFIG_ID} is
	 * {@link #getConfigId()}.
	 * </p>
	 *
	 * @return the system identifier
	 * @see #CLOUD_INTEGRATION_SYSTEM_IDENTIFIER
	 */
	@Override
	public String systemIdentifier() {
		return systemIdentifierForComponents(CLOUD_INTEGRATION_SYSTEM_IDENTIFIER, getConfigId());
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	public Long getConfigId() {
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
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
