/* ==================================================================
 * GeneratedUserAuthToken.java - 16/09/2026 11:05:33 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A newly generated {@link UserAuthToken}, along with the token secret.
 *
 * <p>
 * A token secret is only available to a client when the token is created, so
 * this class exists to return it then: {@link UserAuthToken} itself never
 * serializes the secret.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class GeneratedUserAuthToken {

	private final UserAuthToken token;

	/**
	 * Constructor.
	 *
	 * @param token
	 *        the generated token
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public GeneratedUserAuthToken(UserAuthToken token) {
		super();
		this.token = requireNonNullArgument(token, "token");
	}

	/**
	 * Get the token.
	 *
	 * @return the token, never {@code null}
	 */
	@JsonUnwrapped
	public final UserAuthToken getToken() {
		return token;
	}

	/**
	 * Get the token secret.
	 *
	 * @return the secret, or {@code null}
	 */
	public final @Nullable String getAuthSecret() {
		return token.getAuthSecret();
	}

}
