/* ==================================================================
 * DatumInputCacheConfig.java - 24/02/2024 4:09:06 pm
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

package net.solarnetwork.central.din.app.config;

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Datum input cache configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputCacheConfig implements DatumInputConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(ENDPOINT_CONF)
	@ConfigurationProperties(prefix = "app.din.cache.endpoint-conf-cache")
	public CacheSettings endpointConfigurationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(ENDPOINT_CONF)
	public Cache<UserUuidPK, EndpointConfiguration> endpointConfigurationCache(
			@Qualifier(ENDPOINT_CONF) CacheSettings settings) {
		return settings.createCache(cacheManager, UserUuidPK.class, EndpointConfiguration.class,
				ENDPOINT_CONF + "-cache");
	}

	@Bean
	@Qualifier(TRANSFORM_CONF)
	@ConfigurationProperties(prefix = "app.din.cache.transform-conf-cache")
	public CacheSettings transformConfigurationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(TRANSFORM_CONF)
	public Cache<UserLongCompositePK, TransformConfiguration> transformConfigurationCache(
			@Qualifier(TRANSFORM_CONF) CacheSettings settings) {
		return settings.createCache(cacheManager, UserLongCompositePK.class,
				TransformConfiguration.class, TRANSFORM_CONF + "-cache");
	}

}
