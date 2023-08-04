/* ==================================================================
 * ProxyConfigurationProvider.java - 1/08/2023 10:47:32 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.net.proxy.service;

import java.security.cert.X509Certificate;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionRequest;
import net.solarnetwork.central.net.proxy.domain.ProxyConnectionSettings;
import net.solarnetwork.central.security.AuthorizationException;

/**
 * API for a service that can resolve a
 * 
 * @author matt
 * @version 1.0
 */
public interface ProxyConfigurationProvider {

	/**
	 * Authorize a connection request.
	 * 
	 * @param request
	 *        the request information
	 * @return {@literal true} if the request is authorized, or anything else to
	 *         allow passing to other providers for authorization
	 * @throws AuthorizationException
	 *         if the request is not authorized and should not be passed to any
	 *         other providers for handling and the client connection is denied
	 *         and closed
	 */
	Boolean authorize(ProxyConnectionRequest request) throws AuthorizationException;

	/**
	 * Get connection settings for a connection request.
	 * 
	 * @param request
	 *        the request information
	 * @return the settings, or {@literal null} if none found but the request
	 *         can be passed to another provider for possible handling
	 * @throws AuthorizationException
	 *         if the request is not authorized and should not be passed to any
	 *         other providers for handling and the client connection is denied
	 *         and closed
	 */
	ProxyConnectionSettings settingsForRequest(ProxyConnectionRequest request)
			throws AuthorizationException;

	/**
	 * Get a list of accepted identity issuers.
	 * 
	 * @return the accepted issuers
	 */
	Iterable<X509Certificate> acceptedIdentityIssuers();

}
