/* ==================================================================
 * CredentialConfiguration.java - 28/03/2024 11:10:47 am
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

package net.solarnetwork.central.inin.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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
		"expires", "oauth", "expired" })
public class CredentialConfiguration
		extends BaseUserModifiableEntity<CredentialConfiguration, UserLongCompositePK>
		implements InstructionInputConfigurationEntity<CredentialConfiguration, UserLongCompositePK> {

	@Serial
	private static final long serialVersionUID = 599200829065711466L;

	private String username;
	private @Nullable String password;
	private @Nullable Instant expires;
	private boolean oauth;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param username
	 *        the username
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CredentialConfiguration(UserLongCompositePK id, Instant created, String username) {
		super(id, created);
		this.username = requireNonNullArgument(username, "username");
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
	 * @param username
	 *        the username
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public CredentialConfiguration(Long userId, Long credentialId, Instant created, String username) {
		this(new UserLongCompositePK(userId, credentialId), created, username);
	}

	@Override
	public void eraseCredentials() {
		this.password = null;
	}

	@Override
	public CredentialConfiguration copyWithId(UserLongCompositePK id) {
		var copy = new CredentialConfiguration(id, created(), username);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(CredentialConfiguration entity) {
		super.copyTo(entity);
		entity.setUsername(username);
		entity.setPassword(password);
		entity.setOauth(oauth);
		entity.setExpires(expires);
	}

	@Override
	public boolean isSameAs(@Nullable CredentialConfiguration other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.username, o.username)
				&& Objects.equals(this.password, o.password)
				&& this.oauth == o.oauth
				&& Objects.equals(this.expires, o.expires)
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
		builder.append("oauth=");
		builder.append(oauth);
		builder.append(", enabled=");
		builder.append(isEnabled());
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the credential ID.
	 *
	 * @return the credential ID
	 */
	public final Long getCredentialId() {
		return pk().getEntityId();
	}

	/**
	 * Get the username.
	 *
	 * @return the username
	 */
	public final String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 *
	 * @param username
	 *        the username to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setUsername(String username) {
		this.username = requireNonNullArgument(username, "username");
	}

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	public final @Nullable String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 *
	 * @param password
	 *        the password to set
	 */
	public final void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * Get the expiration date.
	 *
	 * @return the expiration date
	 */
	public final @Nullable Instant getExpires() {
		return expires;
	}

	/**
	 * Set the expiration date.
	 *
	 * @param expires
	 *        the expiration date to set
	 */
	public final void setExpires(@Nullable Instant expires) {
		this.expires = expires;
	}

	/**
	 * Test if the entity is expired.
	 *
	 * @return {@literal true} if {@code expires} is set and is before the
	 *         current time
	 */
	public final boolean isExpired() {
		return expires != null && expires.isBefore(Instant.now());
	}

	/**
	 * Get the OAuth mode.
	 *
	 * @return {@literal true} if the {@code username} represents an OAuth
	 *         client credentials issuer URL
	 */
	public final boolean isOauth() {
		return oauth;
	}

	/**
	 * Set the OAuth mode.
	 *
	 * @param oauth
	 *        {@literal true} if the {@code username} represents an OAuth client
	 *        credentials issuer URL
	 */
	public final void setOauth(boolean oauth) {
		this.oauth = oauth;
	}

}
