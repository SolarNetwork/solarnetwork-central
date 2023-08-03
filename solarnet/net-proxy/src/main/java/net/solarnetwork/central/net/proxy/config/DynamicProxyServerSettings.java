/* ==================================================================
 * NettyDynamicProxyServerSettings.java - 4/08/2023 6:20:35 am
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

package net.solarnetwork.central.net.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration settings for the dynamic proxy server.
 * 
 * @param bindAddress
 *        the address to bind the server to, for example 127.0.0.1 or 0.0.0.0
 * @param port
 *        the socket port to listen on
 * @param wireLoggingEnabled
 *        {@literal true} to enable wire-level logging
 * @param tls
 *        the TLS server settings
 * @author matt
 * @version 1.0
 */
@ConfigurationProperties(prefix = "app.proxy.server")
public record DynamicProxyServerSettings(String bindAddress, Integer port, Boolean wireLoggingEnabled,
		TlsServerSettings tls) {

	/** The default port value. */
	public static final int DEFAULT_PORT = 8802;

	/**
	 * Get the bind port.
	 * 
	 * @return the bind port, defaulting to {@link #DEFAULT_PORT} if not defined
	 */
	public int bindPort() {
		return port() != null ? port().intValue() : DEFAULT_PORT;
	}

	/**
	 * Get the wire-logging enabled flag.
	 * 
	 * @return the wire-logging enabled flag, defaulting to {@literal false} if
	 *         not defined
	 */
	public boolean isWireLoggingEnabled() {
		return wireLoggingEnabled() != null ? wireLoggingEnabled().booleanValue() : false;
	}

	/**
	 * Test if TLS settings are available.
	 * 
	 * @return {@literal true} if TLS settings are available
	 */
	public boolean hasTlsSettings() {
		TlsServerSettings tls = tls();
		return tls != null && tls.certificatePath() != null && tls.certificateKey() != null;
	}

}
