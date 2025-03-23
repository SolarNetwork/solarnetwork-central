/* ==================================================================
 * UserSecretsConfig.java - 23/03/2025 11:08:03 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import java.security.KeyPair;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.domain.UserSecretEntity;

/**
 * User secrets general configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_SECRETS)
@Configuration(proxyBeanMethods = false)
public class UserSecretsConfig implements SolarNetUserConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(USER_KEYPAIR)
	@ConfigurationProperties(prefix = "app.user.secret.cache.keypair-cache")
	public CacheSettings userKeyPairCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_KEYPAIR)
	public Cache<UserStringCompositePK, KeyPair> userKeyPairCache(
			@Qualifier(USER_KEYPAIR) CacheSettings settings) {
		return settings.createCache(cacheManager, UserStringCompositePK.class, KeyPair.class,
				USER_KEYPAIR + "-cache");
	}

	@Bean
	@Qualifier(USER_SECRET)
	@ConfigurationProperties(prefix = "app.user.secret.cache.secret-cache")
	public CacheSettings userSecretCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_SECRET)
	public Cache<UserStringStringCompositePK, UserSecretEntity> userSecretCache(
			@Qualifier(USER_SECRET) CacheSettings settings) {
		return settings.createCache(cacheManager, UserStringStringCompositePK.class,
				UserSecretEntity.class, USER_SECRET + "-cache");
	}

}
