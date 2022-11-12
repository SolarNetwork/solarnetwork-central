/* ==================================================================
 * BaseOscpConfigurationEntity.java - 11/08/2022 9:45:01 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * Base OSCP configuration entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "configId", "created", "modified", "name", "enabled", "serviceProps" })
public abstract class BaseOscpConfigurationEntity<C extends BaseOscpConfigurationEntity<C>>
		extends BasicEntity<UserLongCompositePK>
		implements Entity<UserLongCompositePK>, UserRelatedEntity<UserLongCompositePK>,
		CopyingIdentity<UserLongCompositePK, C>, Differentiable<C>, Serializable, Cloneable {

	private static final long serialVersionUID = -4040376195754476954L;

	private Instant modified;
	private String name;
	private boolean enabled;
	private Map<String, Object> serviceProps;

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
	public BaseOscpConfigurationEntity(UserLongCompositePK id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOscpConfigurationEntity(Long userId, Long entityId, Instant created) {
		super(new UserLongCompositePK(userId, entityId), created);
	}

	@SuppressWarnings("unchecked")
	@Override
	public BaseOscpConfigurationEntity<C> clone() {
		return (BaseOscpConfigurationEntity<C>) super.clone();
	}

	@Override
	public void copyTo(C entity) {
		entity.setEnabled(enabled);
		entity.setModified(modified);
		entity.setName(name);
		if ( serviceProps != null ) {
			entity.setServiceProps(new LinkedHashMap<>(serviceProps));
		}
	}

	/**
	 * Test if this entity has the same property values as another.
	 * 
	 * <p>
	 * The {@code id}, {@code created}, and {@code modified} properties are not
	 * compared.
	 * </p>
	 * 
	 * @param other
	 *        the entity to compare to
	 * @return {@literal true} if the properties of this entity are equal to the
	 *         other's
	 */
	public boolean isSameAs(C other) {
		// @formatter:off
		return (this.enabled == other.isEnabled() 
				&& Objects.equals(this.name, other.getName())
				&& Objects.equals(serviceProps, other.getServiceProps()));
		// @formatter:on
	}

	@Override
	public boolean differsFrom(C other) {
		return !isSameAs(other);
	}

	/**
	 * Get OAuth client settings, if available.
	 * 
	 * @return the OAuth client settings, or {@literal null} if not available
	 */
	public OAuthClientSettings oauthClientSettings() {
		final Map<String, Object> props = getServiceProps();
		if ( props == null ) {
			return null;
		}
		Object tokenUrl = props.get(ExternalSystemServiceProperties.OAUTH_TOKEN_URL);
		if ( tokenUrl == null ) {
			return null;
		}
		Object clientId = props.get(ExternalSystemServiceProperties.OAUTH_CLIENT_ID);
		if ( clientId == null ) {
			return null;
		}
		Object clientSecret = props.get(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
		return new OAuthClientSettings(tokenUrl.toString(), clientId.toString(),
				clientSecret != null ? clientSecret.toString() : null);
	}

	/**
	 * Test if OAuth client settings are available.
	 * 
	 * @return {@literal true} if {@link #oauthClientSettings()} would return a
	 *         non-{@literal null} instance
	 */
	public boolean hasOauthClientSettings() {
		final Map<String, Object> props = getServiceProps();
		if ( props == null ) {
			return false;
		}
		return props.containsKey(ExternalSystemServiceProperties.OAUTH_TOKEN_URL)
				&& props.containsKey(ExternalSystemServiceProperties.OAUTH_CLIENT_ID);
	}

	/**
	 * Get a custom URL path, if available.
	 * 
	 * @param name
	 *        the unique name of the custom URL path to get
	 * @return the associated custom URL path, or {@literal null} if not
	 *         available
	 */
	public String customUrlPath(String name) {
		final Map<String, Object> props = getServiceProps();
		if ( props == null ) {
			return null;
		}
		final Object urlsProp = props.get(ExternalSystemServiceProperties.URL_PATHS);
		if ( urlsProp instanceof Map<?, ?> urls ) {
			Object val = urls.get(name);
			if ( val != null ) {
				return val.toString();
			}
		}
		return null;
	}

	/**
	 * Get a custom URL path, using a fallback value if not available.
	 * 
	 * @param name
	 *        the unique name of the custom URL path to get
	 * @param fallback
	 *        the value to use if no custom URL path for {@code name} is
	 *        available
	 * @return the associated custom URL path, or {@code fallback} if not
	 *         available
	 */
	public String customUrlPath(String name, String fallback) {
		String path = customUrlPath(name);
		return (path != null ? path : fallback);
	}

	/**
	 * Get the extra HTTP headers if available.
	 * 
	 * @return the extra HTTP headers, or {@literal null}
	 */
	@SuppressWarnings("unchecked")
	public Map<String, ?> extraHttpHeaders() {
		final Map<String, Object> props = getServiceProps();
		final Object o = (props != null ? props.get(ExternalSystemServiceProperties.EXTRA_HTTP_HEADERS)
				: null);
		if ( o instanceof Map<?, ?> map ) {
			return (Map<String, ?>) map;
		}
		return null;
	}

	@Override
	public Long getUserId() {
		return getId().getUserId();
	}

	@JsonIgnore
	public Long getEntityId() {
		return getId().getEntityId();
	}

	/**
	 * Get the configuration ID (the entity ID).
	 * 
	 * @return the configuration ID
	 */
	public Long getConfigId() {
		return getId().getEntityId();
	}

	/**
	 * Get the last modification date.
	 * 
	 * @return the modified
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * SGet the last modification date.
	 * 
	 * @param modified
	 *        the modified to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get a display name for the configuration.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set a display name for the configuration.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the service properties.
	 * 
	 * @return the serviceProps
	 */
	public Map<String, Object> getServiceProps() {
		return serviceProps;
	}

	/**
	 * Set the service properties.
	 * 
	 * @param serviceProps
	 *        the serviceProps to set
	 */
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
	}

}
