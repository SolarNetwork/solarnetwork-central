/* ==================================================================
 * SecurityUtils.java - 23/02/2024 2:12:49 pm
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

import static net.solarnetwork.central.security.SecurityUtils.getCurrentAuthentication;
import org.springframework.security.core.Authentication;
import net.solarnetwork.central.security.BasicSecurityException;
import net.solarnetwork.central.security.SecurityUser;

/**
 * Security constants for datum input.
 *
 * @author matt
 * @version 1.0
 */
public final class SecurityUtils {

	private SecurityUtils() {
		// not available
	}

	/** The datum input role name. */
	public static final String ROLE_DIN = "ROLE_DIN";

	/**
	 * Get the current {@link SecurityEndpointCredential}.
	 *
	 * @return the current security actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available
	 */
	public static SecurityEndpointCredential getCurrentEndpointCredential() throws BasicSecurityException {
		return getEndpointCredential(getCurrentAuthentication());
	}

	/**
	 * Get a {@link SecurityUser} for a given authentication.
	 *
	 * @param auth
	 *        the authentication
	 * @return the endpoint actor, never {@literal null}
	 * @throws BasicSecurityException
	 *         if the actor is not available or is not an endpoint credential
	 */
	public static SecurityEndpointCredential getEndpointCredential(Authentication auth)
			throws BasicSecurityException {
		if ( auth != null && auth.getPrincipal() instanceof SecurityEndpointCredential c ) {
			return c;
		} else if ( auth != null && auth.getDetails() instanceof SecurityEndpointCredential c ) {
			return c;
		}
		throw new BasicSecurityException("EndpointCredential not available");
	}

}
