/* ==================================================================
 * ProxyConnectionSettings.java - 1/08/2023 11:03:56 am
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
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Proxy connection settings.
 * 
 * <p>
 * Note if an implementation of {@link ProxyConnectionSettings} implements
 * {@link ServiceLifecycleObserver} those methods will be called before the
 * destination connection is opened and then after the destination connection
 * closes.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ProxyConnectionSettings extends ProxyConfiguration {

	/**
	 * Get the original connection request.
	 * 
	 * @return the connection request
	 */
	ProxyConnectionRequest connectionRequest();

	/**
	 * Get a client key store for trust validation.
	 * 
	 * @return the client trust store
	 */
	KeyStore clientTrustStore();

}
