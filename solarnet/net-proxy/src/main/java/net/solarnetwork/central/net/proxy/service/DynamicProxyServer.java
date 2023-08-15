/* ==================================================================
 * DynamicProxyServer.java - 1/08/2023 10:44:44 am
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

/**
 * API for a dynamic proxy server.
 * 
 * @author matt
 * @version 1.0
 */
public interface DynamicProxyServer {

	/**
	 * Register a configuration provider.
	 * 
	 * @param provider
	 *        the provider to register
	 */
	void registerConfigurationProvider(ProxyConfigurationProvider provider);

	/**
	 * Unregister a previously registered configuration provider.
	 * 
	 * @param provider
	 *        the provider to unregister
	 * @return {@literal true} if the provider was unregistered
	 */
	boolean unregisterConfigurationProvider(ProxyConfigurationProvider provider);

}
