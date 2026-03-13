/* ==================================================================
 * ClientAccessToken.java - 25/10/2024 8:59:10 am
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * API for an opaque client access token.
 *
 * @author matt
 * @version 1.0
 */
public interface ClientAccessToken {

	/**
	 * Get the registration ID.
	 *
	 * @return the registration ID
	 */
	String getRegistrationId();

	/**
	 * Get the principal name.
	 *
	 * @return the principal name
	 */
	String getPrincipalName();

	/**
	 * Get the access token type.
	 *
	 * @return the type
	 */
	String getAccessTokenType();

	/**
	 * Get the access token.
	 *
	 * @return the access token
	 */
	byte[] getAccessToken();

	/**
	 * Get the access token as a UTF-8 string value.
	 *
	 * @return the access token string value
	 * @throws IllegalStateException
	 *         if {@link #getAccessToken()} is {@code null}
	 */
	default String getAccessTokenValue() throws IllegalStateException {
		return new String(nonnull(getAccessToken(), "accessToken"), UTF_8);
	}

	/**
	 * Get the access token issue date.
	 *
	 * @return the date
	 */
	Instant getAccessTokenIssuedAt();

	/**
	 * Get the access token expire date.
	 *
	 * @return the date
	 */
	Instant getAccessTokenExpiresAt();

	/**
	 * Check if an access token has expired.
	 *
	 * @param clock
	 *        the clock to use
	 * @return {@literal true} if the access token expiration date is non-null
	 *         and before the clock's current time
	 */
	default boolean accessTokenExpired(InstantSource clock) {
		Instant date = getAccessTokenExpiresAt();
		return (date != null && date.isBefore(clock.instant()));
	}

	/**
	 * Get the access token scopes.
	 *
	 * @return the scopes, or {@code null}
	 */
	@Nullable
	Set<String> getAccessTokenScopes();

	/**
	 * Get the refresh token.
	 *
	 * @return the refresh token, or {@code null}
	 */
	byte @Nullable [] getRefreshToken();

	/**
	 * Get the refresh token as a UTF-8 string value.
	 *
	 * @return the refresh token string value, or {@code null}
	 */
	default @Nullable String getRefreshTokenValue() {
		byte[] val = getRefreshToken();
		return (val != null ? new String(val, UTF_8) : null);
	}

	/**
	 * Get the refresh token issue date.
	 *
	 * @return the issue date, or {@code null}
	 */
	@Nullable
	Instant getRefreshTokenIssuedAt();

}
