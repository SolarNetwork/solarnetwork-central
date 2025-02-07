/* ==================================================================
 * ClientAccessTokenEntity.java - 25/10/2024 9:09:49 am
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

package net.solarnetwork.central.security;

import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserStringStringCompositePK;

/**
 * Entity implementation of {@link ClientAccessToken}.
 *
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "modified", "enabled", "accessTokenValue", "refreshTokenValue" })
@JsonPropertyOrder({ "userId", "registrationId", "principalName", "created", "accessTokenType",
		"accessToken", "accessTokenIssuedAt", "accessTokenExpiresAt", "accessTokenScopes",
		"refreshToken", "refreshTokenIssuedAt" })
public class ClientAccessTokenEntity
		extends BaseUserModifiableEntity<ClientAccessTokenEntity, UserStringStringCompositePK>
		implements ClientAccessToken {

	@Serial
	private static final long serialVersionUID = 4075961449877401018L;

	private String accessTokenType;
	private byte[] accessToken;
	private Instant accessTokenIssuedAt;
	private Instant accessTokenExpiresAt;
	private Set<String> accessTokenScopes;
	private byte[] refreshToken;
	private Instant refreshTokenIssuedAt;

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
	public ClientAccessTokenEntity(UserStringStringCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param registrationId
	 *        the registration ID
	 * @param principalName
	 *        name the principal name
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ClientAccessTokenEntity(Long userId, String registrationId, String principalName,
			Instant created) {
		this(new UserStringStringCompositePK(userId, registrationId, principalName), created);
	}

	@Override
	public ClientAccessTokenEntity copyWithId(UserStringStringCompositePK id) {
		var copy = new ClientAccessTokenEntity(id, getCreated());
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ClientAccessTokenEntity entity) {
		super.copyTo(entity);
		entity.setAccessTokenType(accessTokenType);
		entity.setAccessToken(accessToken);
		entity.setAccessTokenIssuedAt(accessTokenIssuedAt);
		entity.setAccessTokenExpiresAt(accessTokenExpiresAt);
		entity.setAccessTokenScopes(accessTokenScopes);
		entity.setRefreshToken(refreshToken);
		entity.setRefreshTokenIssuedAt(refreshTokenIssuedAt);
	}

	@Override
	public boolean isSameAs(ClientAccessTokenEntity other) {
		boolean result = super.isSameAs(other);
		if ( !result ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(this.accessTokenType, other.accessTokenType)
				&& Objects.equals(this.accessTokenIssuedAt, other.accessTokenIssuedAt)
				&& Objects.equals(this.accessTokenExpiresAt, other.accessTokenExpiresAt)
				&& Objects.equals(this.accessTokenScopes, other.accessTokenScopes)
				&& Objects.equals(this.refreshTokenIssuedAt, other.refreshTokenIssuedAt)
				&& Arrays.equals(this.accessToken, other.accessToken)
				&& Arrays.equals(this.refreshToken, other.refreshToken)
				;
		// @formatter:on
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ClientAccessToken{");
		builder.append("userId=");
		builder.append(getUserId());
		builder.append(", registrationId=");
		builder.append(getRegistrationId());
		builder.append(", principalName=");
		builder.append(getPrincipalName());
		if ( accessTokenType != null ) {
			builder.append(", accessTokenType=");
			builder.append(accessTokenType);
			builder.append(", ");
		}
		if ( accessTokenIssuedAt != null ) {
			builder.append(", accessTokenIssuedAt=");
			builder.append(accessTokenIssuedAt);
			builder.append(", ");
		}
		if ( accessTokenExpiresAt != null ) {
			builder.append(", accessTokenExpiresAt=");
			builder.append(accessTokenExpiresAt);
			builder.append(", ");
		}
		if ( accessTokenScopes != null ) {
			builder.append(", accessTokenScopes=");
			builder.append(accessTokenScopes);
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public final String getRegistrationId() {
		UserStringStringCompositePK id = getId();
		return (id != null ? id.getGroupId() : null);
	}

	@Override
	public final String getPrincipalName() {
		UserStringStringCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	@Override
	public final String getAccessTokenType() {
		return accessTokenType;
	}

	/**
	 * Set the access token type.
	 *
	 * @param accessTokenType
	 *        the type to set
	 */
	public final void setAccessTokenType(String accessTokenType) {
		this.accessTokenType = accessTokenType;
	}

	@Override
	public final byte[] getAccessToken() {
		return accessToken;
	}

	/**
	 * Set the access token.
	 *
	 * @param accessToken
	 *        the access token to set
	 */
	public final void setAccessToken(byte[] accessToken) {
		this.accessToken = accessToken;
	}

	@Override
	public final Instant getAccessTokenIssuedAt() {
		return accessTokenIssuedAt;
	}

	/**
	 * Set the access token issue date.
	 *
	 * @param accessTokenIssuedAt
	 *        the date to set
	 */
	public final void setAccessTokenIssuedAt(Instant accessTokenIssuedAt) {
		this.accessTokenIssuedAt = accessTokenIssuedAt;
	}

	@Override
	public final Instant getAccessTokenExpiresAt() {
		return accessTokenExpiresAt;
	}

	/**
	 * Set the access token expire date.
	 *
	 * @param accessTokenExpiresAt
	 *        the date to set
	 */
	public final void setAccessTokenExpiresAt(Instant accessTokenExpiresAt) {
		this.accessTokenExpiresAt = accessTokenExpiresAt;
	}

	@Override
	public final Set<String> getAccessTokenScopes() {
		return accessTokenScopes;
	}

	/**
	 * Set the access token scopes.
	 *
	 * @param accessTokenScopes
	 *        the scopes to set, or {@literal null}
	 */
	public final void setAccessTokenScopes(Set<String> accessTokenScopes) {
		this.accessTokenScopes = accessTokenScopes;
	}

	@Override
	public final byte[] getRefreshToken() {
		return refreshToken;
	}

	/**
	 * Set the refresh token.
	 *
	 * @param refreshToken
	 *        the refresh token to set, or {@literal null}
	 */
	public final void setRefreshToken(byte[] refreshToken) {
		this.refreshToken = refreshToken;
	}

	@Override
	public final Instant getRefreshTokenIssuedAt() {
		return refreshTokenIssuedAt;
	}

	/**
	 * Set the refresh token issue date.
	 *
	 * @param refreshTokenIssuedAt
	 *        the date to set, or {@literal null}
	 */
	public final void setRefreshTokenIssuedAt(Instant refreshTokenIssuedAt) {
		this.refreshTokenIssuedAt = refreshTokenIssuedAt;
	}

}
