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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.support.EventDetailsProvider;

/**
 * Endpoint authentication details.
 *
 * @author matt
 * @version 1.1
 */
public class EndpointAuthenticationDetails extends WebAuthenticationDetails
		implements EventDetailsProvider {

	@Serial
	private static final long serialVersionUID = 1532466056959330725L;

	private final @Nullable Long userId;
	private final @Nullable UUID endpointId;

	/**
	 * Constructor.
	 *
	 * @param request
	 *        the request
	 * @param userId
	 *        the user ID
	 * @param endpointId
	 *        the endpoint ID
	 * @throws IllegalArgumentException
	 *         if {@code request} is {@code null}
	 */
	public EndpointAuthenticationDetails(HttpServletRequest request, @Nullable Long userId,
			@Nullable UUID endpointId) {
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
	 * @throws IllegalArgumentException
	 *         if {@code remoteAddress} is {@code null}
	 */
	public EndpointAuthenticationDetails(String remoteAddress, @Nullable String sessionId,
			@Nullable Long userId, @Nullable UUID endpointId) {
		super(requireNonNullArgument(remoteAddress, "remoteAddress"), sessionId);
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
	public final @Nullable Long getUserId() {
		return userId;
	}

	/**
	 * Get the endpoint ID.
	 *
	 * @return the endpoint ID
	 */
	public final @Nullable UUID getEndpointId() {
		return endpointId;
	}

}
