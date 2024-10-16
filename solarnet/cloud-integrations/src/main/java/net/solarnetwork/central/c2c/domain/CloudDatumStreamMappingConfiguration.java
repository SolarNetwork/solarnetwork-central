/* ==================================================================
 * CloudDatumStreamMappingConfiguration.java - 16/10/2024 6:55:31 am
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.JsonUtils;

/**
 * Cloud datum stream mapping configuration.
 *
 * <p>
 * The purpose of this entity is to provide a named mapping of datum stream
 * properties that can be used across multiple datum streams.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @see CloudDatumStreamPropertyConfiguration
 */
@JsonIgnoreProperties({ "id", "enabled", "fullyConfigured" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "name", "integrationId",
		"serviceProperties" })
public class CloudDatumStreamMappingConfiguration extends
		BaseUserModifiableEntity<CloudDatumStreamMappingConfiguration, UserLongCompositePK> implements
		CloudIntegrationsConfigurationEntity<CloudDatumStreamMappingConfiguration, UserLongCompositePK> {

	private static final long serialVersionUID = -5099340175851992871L;

	/** The cloud integration ID. */
	private Long integrationId;

	/** The name. */
	private String name;

	/** The service properties as JSON. */
	private String servicePropsJson;

	/** The service properties. */
	private volatile transient Map<String, Object> serviceProps;

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
	public CloudDatumStreamMappingConfiguration(UserLongCompositePK id, Instant created) {
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
	public CloudDatumStreamMappingConfiguration(Long userId, Long configId, Instant created) {
		this(new UserLongCompositePK(userId, configId), created);
	}

	@Override
	public CloudDatumStreamMappingConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CloudDatumStreamMappingConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CloudDatumStreamMappingConfiguration entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setIntegrationId(integrationId);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(CloudDatumStreamMappingConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.getName())
				&& Objects.equals(this.integrationId, other.integrationId)
				// compare decoded JSON, as JSON key order not assumed
				&& Objects.equals(getServiceProperties(), other.getServiceProperties())
				;
		// @formatter:on
	}

	@Override
	public boolean isFullyConfigured() {
		return integrationId != null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloudDatumStream{");
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
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if ( integrationId != null ) {
			builder.append("integrationId=");
			builder.append(integrationId);
			builder.append(", ");
		}
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
	 * Get the configuration name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the configuration name.
	 *
	 * @param name
	 *        the name to use
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @return the integration ID
	 */
	public final Long getIntegrationId() {
		return integrationId;
	}

	/**
	 * Set the associated {@link CloudIntegrationConfiguration}
	 * {@code configId}.
	 *
	 * @param integrationId
	 *        the integration ID to set
	 */
	public final void setIntegrationId(Long integrationId) {
		this.integrationId = integrationId;
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@literal null} if no service
	 *         properties available
	 */
	@JsonIgnore
	public String getServicePropsJson() {
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
	public void setServicePropsJson(String json) {
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
	public Map<String, Object> getServiceProps() {
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
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
	}

	/**
	 * Add a collection of service properties.
	 *
	 * @param props
	 *        the properties to add
	 */
	public void putServiceProps(Map<String, Object> props) {
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
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
