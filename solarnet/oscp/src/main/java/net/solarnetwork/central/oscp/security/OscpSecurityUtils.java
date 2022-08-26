/* ==================================================================
 * OscpSecurityUtils.java - 17/08/2022 2:43:40 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.security;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import net.solarnetwork.central.oscp.domain.AuthRoleContainer;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityUtils;

/**
 * Security helper methods.
 * 
 * @author matt
 * @version 1.0
 */
public final class OscpSecurityUtils {

	private OscpSecurityUtils() {
		// not supported
	}

	/**
	 * Get the authorization info for the current actor.
	 * 
	 * @return the info
	 * @throws AuthorizationException
	 *         if the info cannot be resolved
	 */
	public static AuthRoleInfo authRoleInfo() {
		return authRoleInfoForAuthentication(SecurityUtils.getCurrentAuthentication());
	}

	/**
	 * Get the authorization info for a given {@link Principal}.
	 * 
	 * @param principal
	 *        the principal
	 * @return the info
	 * @throws AuthorizationException
	 *         if the info cannot be resolved
	 */
	public static AuthRoleInfo authRoleInfoForPrincipal(Principal principal) {
		if ( principal instanceof Authentication auth ) {
			return authRoleInfoForAuthentication(auth);
		}
		throw new AuthorizationException(Reason.ACCESS_DENIED, principal);
	}

	/**
	 * Get the authorization info for a given {@link Principal}.
	 * 
	 * @param auth
	 *        the authentication
	 * @return the info
	 * @throws AuthorizationException
	 *         if the info cannot be resolved
	 */
	public static AuthRoleInfo authRoleInfoForAuthentication(Authentication auth) {
		if ( auth == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		AuthRoleContainer container = null;
		if ( auth instanceof AuthRoleContainer c ) {
			container = c;
		} else if ( auth.getDetails() instanceof AuthRoleContainer c ) {
			container = c;
		}
		if ( container == null ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		AuthRoleInfo info = container.getAuthRole();
		if ( info != null ) {
			return info;
		}
		throw new AuthorizationException(Reason.ACCESS_DENIED, auth.getPrincipal());
	}

}
