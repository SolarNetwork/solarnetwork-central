/* ==================================================================
 * TomcatConfig.java - 13/11/2021 4:08:04 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.config;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.SSLValve;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Tomcat when running dual HTTP/HTTPS.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class TomcatConfig {

	@ConditionalOnProperty("server.http.port")
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> connectorCustomizerWithSsl(
			@Value("${server.http.port}") int httpPort) {
		Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
		connector.setPort(httpPort);
		return (tomcat) -> {
			tomcat.addAdditionalTomcatConnectors(connector);
			tomcat.addEngineValves(new SSLValve());
		};
	}

	@ConditionalOnProperty(name = "server.forward-headers-strategy", havingValue = "NATIVE")
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> connectorCustomizerProxySsl() {
		return (tomcat) -> {
			tomcat.addEngineValves(new SSLValve());
		};
	}

}
