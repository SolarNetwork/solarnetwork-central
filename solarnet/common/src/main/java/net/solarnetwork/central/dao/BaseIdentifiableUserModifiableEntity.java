/* ==================================================================
 * BaseIdentifiableUserModifiableEntity.java - 26/09/2024 1:13:54 pm
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

package net.solarnetwork.central.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.codec.JsonUtils;

/**
 * A base user-related entity that is also an identifiable configuration.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseIdentifiableUserModifiableEntity<C extends BaseIdentifiableUserModifiableEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends BaseUserModifiableEntity<C, K>
		implements UserRelatedStdIdentifiableConfigurationEntity<C, K> {

	private static final long serialVersionUID = -7821821709345090306L;

	/** The name. */
	private String name;

	/** The service identifier. */
	private String serviceIdentifier;

	/** The service properties as JSON. */
	private String servicePropsJson;

	/** The service properties. */
	private transient Map<String, Object> serviceProps;

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
	public BaseIdentifiableUserModifiableEntity(K id, Instant created) {
		super(id, created);
	}

	@Override
	public void copyTo(C entity) {
		super.copyTo(entity);
		entity.setName(name);
		entity.setServiceIdentifier(serviceIdentifier);
		entity.setServicePropsJson(servicePropsJson);
	}

	@Override
	public boolean isSameAs(C other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.name, other.getName())
				&& Objects.equals(this.serviceIdentifier, other.getServiceIdentifier())
				&& Objects.equals(this.servicePropsJson, other.getServicePropsJson())
				;
		// @formatter:on
	}

	@Override
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

	@Override
	public String getServiceIdentifier() {
		return serviceIdentifier;
	}

	/**
	 * Set the unique identifier for the service this configuration is
	 * associated with.
	 * 
	 * @param serviceIdentifier
	 *        the identifier of the service to use
	 */
	public void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = serviceIdentifier;
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

	@Override
	public Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
