/* ==================================================================
 * SimpleProxyConnectionSettings.java - 3/08/2023 9:34:53 am
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

package net.solarnetwork.central.net.proxy.domain;

import java.security.KeyStore;

/**
 * Simple implementation of {@link ProxyConnectionSettings}.
 * 
 * @param connectionRequest
 *        the original connection request
 * @param clientTrustStore
 *        the client trust store
 * @param destinationHost
 *        the destination host
 * @param destinationPort
 *        the destination port
 * @author matt
 * @version 1.0
 */
public record SimpleProxyConnectionSettings(ProxyConnectionRequest connectionRequest,
		KeyStore clientTrustStore, String destinationHost, int destinationPort)
		implements ProxyConnectionSettings {

}
