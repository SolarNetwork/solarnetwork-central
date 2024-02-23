/* ==================================================================
 * EndpointUserDetails.java - 23/02/2024 2:07:19 pm
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

package net.solarnetwork.central.din.security;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.util.ObjectUtils;

/**
 * User details for an authenticated endpoint credential.
 *
 * @author matt
 * @version 1.0
 */
public class AuthenticatedEndpointCredentials extends User implements EndpointUserDetails {

	private static final long serialVersionUID = -6640758003140500968L;

	private final Long userId;
	private final UUID endpointId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @param enabled
	 *        the enabled state
	 * @param expired
	 *        the expiration state
	 */
	public AuthenticatedEndpointCredentials(Long userId, UUID endpointId, String username,
			String password, boolean enabled, boolean expired) {
		super(username, password, enabled, true, !expired, true,
				createAuthorityList(SecurityConstants.ROLE_DIN));
		this.userId = ObjectUtils.requireNonNullArgument(userId, "userId");
		this.endpointId = ObjectUtils.requireNonNullArgument(endpointId, "endpointId");
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public UUID getEndpointId() {
		return endpointId;
	}

	@Override
	public boolean isAuthenticatedWithToken() {
		return false;
	}

}
