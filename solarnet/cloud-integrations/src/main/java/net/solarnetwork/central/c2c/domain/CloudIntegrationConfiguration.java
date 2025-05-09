/* ==================================================================
 * CloudIntegrationConfiguration.java - 26/09/2024 1:00:09 pm
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

import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.dao.UserRelatedStdIdentifiableConfigurationEntity;
import net.solarnetwork.central.domain.UserIdentifiableSystem;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * A cloud integration configuration entity.
 *
 * <p>
 * The purpose of this entity is to provide external cloud integration details,
 * such as credentials and any other connection-related information.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "fullyConfigured" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "enabled", "name", "serviceIdentifier",
		"serviceProperties" })
public final class CloudIntegrationConfiguration
		extends BaseIdentifiableUserModifiableEntity<CloudIntegrationConfiguration, UserLongCompositePK>
		implements
		CloudIntegrationsConfigurationEntity<CloudIntegrationConfiguration, UserLongCompositePK>,
		UserRelatedStdIdentifiableConfigurationEntity<CloudIntegrationConfiguration, UserLongCompositePK>,
		UserIdentifiableSystem {

	/**
	 * A system identifier component included in {@link #systemIdentifier()}.
	 */
	public static final String CLOUD_INTEGRATION_SYSTEM_IDENTIFIER = "c2c-i9n";

	@Serial
	private static final long serialVersionUID = 9018138639840148323L;

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
	public CloudIntegrationConfiguration(UserLongCompositePK id, Instant created) {
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
	public CloudIntegrationConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public CloudIntegrationConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CloudIntegrationConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public boolean isFullyConfigured() {
		return true; // can't really tell with this one
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudIntegration{");
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
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
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
	 * Get a unique identifier based on this configuration.
	 *
	 * <p>
	 * The identifier follows this syntax:
	 * </p>
	 *
	 * <pre>{@code
	 * USER_ID:c2c-int:CONFIG_ID
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

}
