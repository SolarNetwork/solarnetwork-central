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

import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.BaseUserModifiableEntity;
import net.solarnetwork.central.domain.UserStringStringCompositePK;

/**
 * Entity implementation of {@link ClientAccessToken}.
 *
 * @author matt
 * @version 1.1
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

	private final String accessTokenType;
	private final byte[] accessToken;
	private final Instant accessTokenIssuedAt;
	private final Instant accessTokenExpiresAt;
	private @Nullable Set<String> accessTokenScopes;
	private byte @Nullable [] refreshToken;
	private @Nullable Instant refreshTokenIssuedAt;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param accessTokenType
	 *        the access token type
	 * @param accessToken
	 *        the access token
	 * @param accessTokenIssuedAt
	 *        the access token issue date
	 * @param accessTokenExpiresAt
	 *        the access token expiration date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ClientAccessTokenEntity(UserStringStringCompositePK id, Instant created,
			String accessTokenType, byte[] accessToken, Instant accessTokenIssuedAt,
			Instant accessTokenExpiresAt) {
		super(id, created);
		this.accessTokenType = requireNonNullArgument(accessTokenType, "accessTokenType");
		this.accessToken = requireNonNullArgument(accessToken, "accessToken");
		this.accessTokenIssuedAt = requireNonNullArgument(accessTokenIssuedAt, "accessTokenIssuedAt");
		this.accessTokenExpiresAt = requireNonNullArgument(accessTokenExpiresAt, "accessTokenExpiresAt");
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
	 * @param accessTokenType
	 *        the access token type
	 * @param accessToken
	 *        the access token
	 * @param accessTokenIssuedAt
	 *        the access token issue date
	 * @param accessTokenExpiresAt
	 *        the access token expiration date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ClientAccessTokenEntity(Long userId, String registrationId, String principalName,
			Instant created, String accessTokenType, byte[] accessToken, Instant accessTokenIssuedAt,
			Instant accessTokenExpiresAt) {
		this(new UserStringStringCompositePK(userId, registrationId, principalName), created,
				accessTokenType, accessToken, accessTokenIssuedAt, accessTokenExpiresAt);
	}

	@Override
	public ClientAccessTokenEntity copyWithId(@Nullable UserStringStringCompositePK id) {
		var copy = new ClientAccessTokenEntity(requireNonNullArgument(id, "id"), created(),
				accessTokenType, accessToken, accessTokenIssuedAt, accessTokenExpiresAt);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(ClientAccessTokenEntity entity) {
		super.copyTo(entity);
		entity.setAccessTokenScopes(accessTokenScopes);
		entity.setRefreshToken(refreshToken);
		entity.setRefreshTokenIssuedAt(refreshTokenIssuedAt);
	}

	@Override
	public boolean isSameAs(@Nullable ClientAccessTokenEntity other) {
		if ( !super.isSameAs(other) ) {
			return false;
		}
		final var o = nonnull(other, "other");
		// @formatter:off
		return Objects.equals(this.accessTokenType, o.accessTokenType)
				&& Objects.equals(this.accessTokenIssuedAt, o.accessTokenIssuedAt)
				&& Objects.equals(this.accessTokenExpiresAt, o.accessTokenExpiresAt)
				&& Objects.equals(this.accessTokenScopes, o.accessTokenScopes)
				&& Objects.equals(this.refreshTokenIssuedAt, o.refreshTokenIssuedAt)
				&& Arrays.equals(this.accessToken, o.accessToken)
				&& Arrays.equals(this.refreshToken, o.refreshToken)
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
		return nonnull(getId(), "id").getGroupId();
	}

	@Override
	public final String getPrincipalName() {
		return nonnull(getId(), "id").getEntityId();
	}

	@Override
	public final String getAccessTokenType() {
		return accessTokenType;
	}

	@Override
	public final byte[] getAccessToken() {
		return accessToken;
	}

	@Override
	public final Instant getAccessTokenIssuedAt() {
		return accessTokenIssuedAt;
	}

	@Override
	public final Instant getAccessTokenExpiresAt() {
		return accessTokenExpiresAt;
	}

	@Override
	public final @Nullable Set<String> getAccessTokenScopes() {
		return accessTokenScopes;
	}

	/**
	 * Set the access token scopes.
	 *
	 * @param accessTokenScopes
	 *        the scopes to set, or {@code null}
	 */
	public final void setAccessTokenScopes(@Nullable Set<String> accessTokenScopes) {
		this.accessTokenScopes = accessTokenScopes;
	}

	@Override
	public final byte @Nullable [] getRefreshToken() {
		return refreshToken;
	}

	/**
	 * Set the refresh token.
	 *
	 * @param refreshToken
	 *        the refresh token to set, or {@code null}
	 */
	public final void setRefreshToken(byte @Nullable [] refreshToken) {
		this.refreshToken = refreshToken;
	}

	@Override
	public final @Nullable Instant getRefreshTokenIssuedAt() {
		return refreshTokenIssuedAt;
	}

	/**
	 * Set the refresh token issue date.
	 *
	 * @param refreshTokenIssuedAt
	 *        the date to set, or {@code null}
	 */
	public final void setRefreshTokenIssuedAt(@Nullable Instant refreshTokenIssuedAt) {
		this.refreshTokenIssuedAt = refreshTokenIssuedAt;
	}

}
