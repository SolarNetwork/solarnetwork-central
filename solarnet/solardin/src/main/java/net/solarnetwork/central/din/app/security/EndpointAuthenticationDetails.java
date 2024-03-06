/* ==================================================================
 * EndpointAuthenticationDetails.java - 23/02/2024 11:34:44 am
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

package net.solarnetwork.central.din.app.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.support.EventDetailsProvider;

/**
 * Endpoint authentication details.
 *
 * @author matt
 * @version 1.1
 */
public class EndpointAuthenticationDetails extends WebAuthenticationDetails
		implements UserIdRelated, EventDetailsProvider {

	private static final long serialVersionUID = 1532466056959330725L;

	private final Long userId;
	private final UUID endpointId;

	/**
	 * Constructor.
	 *
	 * @param request
	 *        the request
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 */
	public EndpointAuthenticationDetails(HttpServletRequest request, Long userId, UUID endpointId) {
		super(request);
		this.userId = userId;
		this.endpointId = endpointId;
	}

	/**
	 * Constructor.
	 *
	 * @param remoteAddress
	 *        the remote address
	 * @param sessionId
	 *        the session ID
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 */
	public EndpointAuthenticationDetails(String remoteAddress, String sessionId, Long userId,
			UUID endpointId) {
		super(remoteAddress, sessionId);
		this.userId = userId;
		this.endpointId = endpointId;
	}

	@Override
	public Map<String, ?> eventDetails() {
		return Map.of("endpointId", endpointId);
	}

	/**
	 * Get the user ID.
	 *
	 * @return the user ID
	 * @since 1.1
	 */
	@Override
	public Long getUserId() {
		return userId;
	}

	/**
	 * Get the endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	public UUID getEndpointId() {
		return endpointId;
	}

}
