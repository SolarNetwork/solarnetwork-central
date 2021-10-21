/* ==================================================================
 * NetworkIdentityConfig.java - 21/10/2021 9:25:21 AM
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

package net.solarnetwork.central.common.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.biz.BasicNetworkIdentificationBiz;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.support.BasicAppConfigurationBiz;
import net.solarnetwork.central.support.SimpleAppConfiguration;

/**
 * Network identity configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class NetworkIdentityConfig {

	/** Settings for the NetworkIdentityBiz. */
	public static class NetworkIdentitySettings {

		private String networkIdentityKey = "replace:identity:here";
		private Resource termsOfService = new ClassPathResource(
				"net/solarnetwork/central/in/config/placeholder-toc.txt");
		private String host = "localhost";
		private int port = 8080;
		private boolean forceTls = false;
		private Map<String, String> serviceUrls = defaultNetworkServiceUrls();

		private static Map<String, String> defaultNetworkServiceUrls() {
			Map<String, String> map = new LinkedHashMap<>(4);
			map.put("solaruser", "http://localhost/solaruser");
			map.put("solarquery", "http://localhost/solarquery");
			map.put("solarin-mqtt", "mqtts://localhost:8883");
			return map;
		}
	}

	@Bean
	@ConfigurationProperties(prefix = "app.network-identity")
	public NetworkIdentitySettings networkIdentitySettings() {
		return new NetworkIdentitySettings();
	}

	@Bean
	public AppConfigurationBiz appConfigurationBiz() {
		NetworkIdentitySettings settings = networkIdentitySettings();
		return new BasicAppConfigurationBiz(new SimpleAppConfiguration(settings.serviceUrls));
	}

	@Bean
	public NetworkIdentificationBiz networkIdentificationBiz() {
		NetworkIdentitySettings settings = networkIdentitySettings();
		return new BasicNetworkIdentificationBiz(settings.networkIdentityKey, settings.termsOfService,
				settings.host, settings.port, settings.forceTls, settings.serviceUrls);
	}

}
