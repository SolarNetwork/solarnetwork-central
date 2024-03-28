/* ==================================================================
 * AuthenticatedEndpointCredentials.java - 28/03/2024 3:16:50 pm
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

package net.solarnetwork.central.inin.security;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;
import net.solarnetwork.central.support.EventDetailsProvider;

/**
 * User details for an authenticated endpoint credential.
 *
 * @author matt
 * @version 1.0
 */
public class AuthenticatedEndpointCredentials extends User
		implements EndpointUserDetails, EventDetailsProvider {

	private static final long serialVersionUID = 1221233194260672700L;

	private final Long userId;
	private final UUID endpointId;
	private final boolean oauth;

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
	 * @param oauth
	 *        the OAuth mode
	 */
	public AuthenticatedEndpointCredentials(Long userId, UUID endpointId, String username,
			String password, boolean enabled, boolean expired, boolean oauth) {
		super(username, password, enabled, true, !expired, true,
				createAuthorityList(SecurityUtils.ROLE_ININ));
		this.userId = requireNonNullArgument(userId, "userId");
		this.endpointId = requireNonNullArgument(endpointId, "endpointId");
		this.oauth = oauth;
	}

	@Override
	public Map<String, ?> eventDetails() {
		return Map.of("endpointId", endpointId);
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

	@Override
	public boolean isOauth() {
		return oauth;
	}

}
