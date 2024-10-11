/* ==================================================================
 * CloudIntegrationsConfig.java - 8/10/2024 9:31:24 am
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.security.PrefixedTextEncryptor;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Cloud integrations general configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(CLOUD_INTEGRATIONS)
@Configuration(proxyBeanMethods = false)
public class CloudIntegrationsConfig implements SolarNetCloudIntegrationsConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(CLOUD_INTEGRATIONS_EXPRESSIONS)
	@ConfigurationProperties(prefix = "app.c2c.cache.expression-cache")
	public CacheSettings cloudIntegrationsExpressionCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(CLOUD_INTEGRATIONS_EXPRESSIONS)
	public Cache<String, Expression> cloudIntegrationsExpressionCache(
			@Qualifier(CLOUD_INTEGRATIONS_EXPRESSIONS) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, Expression.class,
				CLOUD_INTEGRATIONS_EXPRESSIONS + "-cache");
	}

	@Bean
	@Qualifier(CLOUD_INTEGRATIONS)
	public PrefixedTextEncryptor cloudIntegrationsTextEncryptor(
			@Value("${app.c2c.encryptor.password}") String password,
			@Value("${app.c2c.encryptor.salt-hex}") String salt) {
		return PrefixedTextEncryptor.aesTextEncryptor(password, salt);
	}

}
