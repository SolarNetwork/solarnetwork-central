/* ==================================================================
 * ProxyConnectionRequest.java - 1/08/2023 10:55:28 am
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

import java.security.cert.X509Certificate;

/**
 * API for a proxy connection request.
 * 
 * @author matt
 * @version 1.0
 */
public interface ProxyConnectionRequest {

	/**
	 * Get a principal name.
	 * 
	 * @return the principal name
	 */
	String principal();

	/**
	 * Get the principal identity, if available.
	 * 
	 * @return the principal identity
	 */
	X509Certificate[] principalIdentity();

}
