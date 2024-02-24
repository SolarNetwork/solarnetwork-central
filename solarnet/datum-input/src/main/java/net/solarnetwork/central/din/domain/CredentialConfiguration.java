/* ==================================================================
 * CredentialConfiguration.java - 21/02/2024 6:35:51 am
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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Username/password credential configuration.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "credentialId", "created", "modified", "enabled", "username", "password",
		"expires" })
public class CredentialConfiguration
		extends BaseUserModifiableEntity<CredentialConfiguration, UserLongCompositePK>
		implements DatumInputConfigurationEntity<CredentialConfiguration, UserLongCompositePK> {

	private static final long serialVersionUID = 4246051639093121204L;

	private String username;
	private String password;
	private Instant expires;

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
	public CredentialConfiguration(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param credentialId
	 *        the credential ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public CredentialConfiguration(Long userId, Long credentialId, Instant created) {
		this(new UserLongCompositePK(userId, credentialId), created);
	}

	@Override
	public void eraseCredentials() {
		this.password = null;
	}

	@Override
	public CredentialConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CredentialConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CredentialConfiguration entity) {
		super.copyTo(entity);
		entity.setUsername(username);
		entity.setPassword(password);
		entity.setExpires(expires);
	}

	@Override
	public boolean isSameAs(CredentialConfiguration other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.username, other.username)
				&& Objects.equals(this.password, other.password)
				&& Objects.equals(this.expires, other.expires)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Credential{");
		if ( getUserId() != null ) {
			builder.append("userId=");
			builder.append(getUserId());
			builder.append(", ");
		}
		if ( getCredentialId() != null ) {
			builder.append("credentialId=");
			builder.append(getCredentialId());
			builder.append(", ");
		}
		if ( username != null ) {
			builder.append("username=");
			builder.append(username);
			builder.append(", ");
		}
		if ( expires != null ) {
			builder.append("expires=");
			builder.append(expires);
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the credential ID.
	 *
	 * @return the credential ID
	 */
	public Long getCredentialId() {
		UserLongCompositePK id = getId();
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
	 * Set the username.
	 *
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
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

	/**
	 * Get the expiration date.
	 *
	 * @return the expiration date
	 */
	public Instant getExpires() {
		return expires;
	}

	/**
	 * Set the expiration date.
	 *
	 * @param expires
	 *        the expiration date to set
	 */
	public void setExpires(Instant expires) {
		this.expires = expires;
	}

}
