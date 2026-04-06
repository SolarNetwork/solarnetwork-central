/* ==================================================================
 * AuthenticatedUser.java - Feb 2, 2010 3:37:22 PM
 *
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;

/**
 * Extension of Spring Security's {@link User} object to add SolarNetwork
 * attributes.
 *
 * @author matt
 * @version 1.1
 */
public class AuthenticatedUser extends User implements SecurityUser {

	@Serial
	private static final long serialVersionUID = 4517031455367343502L;

	private final Long userId;
	private final @Nullable String name;
	private final boolean authenticatedWithToken;

	/**
	 * Construct from existing {@link User} and details.
	 *
	 * @param user
	 *        the user
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the optional name
	 * @param authenticatedWithToken
	 *        the authenticated with token flag
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code name} is {@code null}
	 */
	public AuthenticatedUser(User user, Long userId, @Nullable String name,
			boolean authenticatedWithToken) {
		super(requireNonNullArgument(user, "user").getUsername(), user.getPassword(), user.isEnabled(),
				user.isAccountNonExpired(), user.isCredentialsNonExpired(), user.isAccountNonLocked(),
				user.getAuthorities());
		this.userId = userId;
		this.name = name;
		this.authenticatedWithToken = authenticatedWithToken;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	public @Nullable String getName() {
		return name;
	}

	@Override
	public @Nullable String getDisplayName() {
		return name;
	}

	@Override
	public String getEmail() {
		return getUsername();
	}

	@Override
	public boolean isAuthenticatedWithToken() {
		return authenticatedWithToken;
	}

}
