/* ==================================================================
 * TcpProxyServerConfig.java - 9/08/2023 4:40:54 pm
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

package net.solarnetwork.central.dnp3.app.config;

import java.security.KeyStore;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.net.proxy.config.DynamicProxyServerSettings;
import net.solarnetwork.central.net.proxy.service.ProxyConfigurationProvider;
import net.solarnetwork.central.net.proxy.service.impl.NettyDynamicProxyServer;
import net.solarnetwork.central.security.CertificateUtils;

/**
 * DNP3 proxy server configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class TcpProxyServerConfig {

	/**
	 * The TCP proxy server.
	 * 
	 * @param settings
	 *        the server settings
	 * @param providers
	 *        the available configuration providers
	 * @return the server instance
	 */
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public NettyDynamicProxyServer tcpProxyServer(DynamicProxyServerSettings settings,
			List<ProxyConfigurationProvider> providers) {
		NettyDynamicProxyServer server = new NettyDynamicProxyServer(settings.bindAddress(),
				settings.bindPort());
		server.setWireLogging(settings.isWireLoggingEnabled());
		if ( settings.hasTlsSettings() ) {
			server.setTlsProtocols(settings.tls().protocols());
			KeyStore keyStore = CertificateUtils.serverKeyStore(settings.tls().certificatePath(),
					settings.tls().certificateKey(), NettyDynamicProxyServer.DEFAULT_KEYSTORE_ALIAS);
			server.setKeyStore(keyStore);
		}
		for ( ProxyConfigurationProvider provider : providers ) {
			server.registerConfigurationProvider(provider);
		}
		return server;
	}

}
