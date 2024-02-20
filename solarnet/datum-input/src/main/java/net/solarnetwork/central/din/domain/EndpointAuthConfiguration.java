/* ==================================================================
 * EndpointAuthConfiguration.java - 20/02/2024 6:09:43 am
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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserUuidIntegerCompositePK;

/**
 * Endpoint authorization configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "endpointId", "index", "created", "modified", "enabled", "username",
		"password" })
public class EndpointAuthConfiguration
		extends BaseUserModifiableEntity<EndpointAuthConfiguration, UserUuidIntegerCompositePK> {

	private static final long serialVersionUID = 3114002600283221163L;

	private String username;
	private String password;

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
	public EndpointAuthConfiguration(UserUuidIntegerCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param index
	 *        the index
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointAuthConfiguration(Long userId, UUID endpointId, Integer index, Instant created) {
		this(new UserUuidIntegerCompositePK(userId, endpointId, index), created);
	}

	@Override
	public EndpointAuthConfiguration copyWithId(UserUuidIntegerCompositePK id) {
		var copy = new EndpointAuthConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(EndpointAuthConfiguration entity) {
		super.copyTo(entity);
		entity.setUsername(username);
		entity.setPassword(password);
	}

	@Override
	public boolean isSameAs(EndpointAuthConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		return Objects.equals(this.username, other.password)
				&& Objects.equals(this.password, other.password);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ServerAuth{");
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
		if ( getIndex() != null ) {
			builder.append("index=");
			builder.append(getIndex());
			builder.append(", ");
		}
		if ( username != null ) {
			builder.append("username=");
			builder.append(username);
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
	public UUID getEndpointId() {
		UserUuidIntegerCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	/**
	 * Get the index.
	 *
	 * @return the index
	 */
	public Integer getIndex() {
		UserUuidIntegerCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Get the username.
	 *
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username
	 *
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String name) {
		this.username = name;
	}

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 *
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
