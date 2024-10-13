/* ==================================================================
 * OAuthClientCacheConfig.java - 14/10/2024 6:14:00 am
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.OAUTH_CLIENT_REGISTRATION;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import net.solarnetwork.central.support.CacheSettings;

/**
 * OAuth client configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OAuthClientCacheConfig {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(OAUTH_CLIENT_REGISTRATION)
	@ConfigurationProperties(prefix = "app.oauth.client.registration-cache")
	public CacheSettings oauthClientRegistrationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(OAUTH_CLIENT_REGISTRATION)
	public Cache<String, ClientRegistration> oauthClientRegistrationCache(
			@Qualifier(OAUTH_CLIENT_REGISTRATION) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, ClientRegistration.class,
				OAUTH_CLIENT_REGISTRATION);
	}

}
