/* ==================================================================
 * BaseExpireConfigurationEntity.java - 9/07/2018 10:07:47 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.datum.expire.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.UserRelatedIdentifiableConfigurationEntity;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * Base class for expire configuration entities.
 *
 * @author matt
 * @version 2.2
 */
public class BaseExpireConfigurationEntity extends BaseEntity
		implements UserRelatedIdentifiableConfigurationEntity<Long>, Serializable {

	@Serial
	private static final long serialVersionUID = 3903309200783206216L;

	private Long userId;
	private String name;
	private String serviceIdentifier;
	private @Nullable String servicePropsJson;

	private @Nullable Map<String, Object> serviceProps;

	/**
	 * Constructor.
	 * @param id
	 *        the primary key
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the configuration name
	 * @param serviceIdentifier
	 *        the service identifier
	 * 
	 * @since 2.2
	 */
	public BaseExpireConfigurationEntity(Long id, Long userId, Instant created, String name,
			String serviceIdentifier) {
		super();
		setId(id);
		setCreated(created);
		this.userId = requireNonNullArgument(userId, "userId");
		this.name = requireNonNullArgument(name, "name");
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	@Override
	public final Long getUserId() {
		return userId;
	}

	public final void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

	@Override
	public final String getServiceIdentifier() {
		return serviceIdentifier;
	}

	public final void setServiceIdentifier(String serviceIdentifier) {
		this.serviceIdentifier = requireNonNullArgument(serviceIdentifier, "serviceIdentifier");
	}

	/**
	 * Get the service properties object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@code null} if no service properties
	 *         available
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable String getServicePropsJson() {
		if ( servicePropsJson == null ) {
			servicePropsJson = JsonUtils.getJSONString(serviceProps, null);
		}
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
	public final void setServicePropsJson(@Nullable String json) {
		servicePropsJson = json;
		serviceProps = null;
	}

	@JsonIgnore
	public final @Nullable Map<String, Object> getServiceProps() {
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
	public final void setServiceProps(@Nullable Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
		servicePropsJson = null;
	}

	@Override
	public final @Nullable Map<String, ?> getServiceProperties() {
		return getServiceProps();
	}

}
