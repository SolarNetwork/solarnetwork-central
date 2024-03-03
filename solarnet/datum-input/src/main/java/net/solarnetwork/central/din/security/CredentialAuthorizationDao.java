/* ==================================================================
 * CredentialAuthorizationDao.java - 23/02/2024 1:36:28 pm
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

import java.util.UUID;

/**
 * API for endpoint credential authorization.
 *
 * @author matt
 * @version 1.0
 */
public interface CredentialAuthorizationDao {

	/**
	 * Look up the credentials for an endpoint with the given username.
	 *
	 * @param endpointId
	 *        the endpoint ID
	 * @param username
	 *        the username
	 * @return the matching credentials
	 */
	EndpointUserDetails credentialsForEndpoint(UUID endpointId, String username);

}
