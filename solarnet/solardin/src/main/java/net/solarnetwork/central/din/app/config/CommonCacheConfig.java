/* ==================================================================
 * CommonCacheConfig.java - 6/04/2024 6:23:12 am
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
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Common cache configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class CommonCacheConfig implements SolarDinAppConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(USER_METADATA)
	@ConfigurationProperties(prefix = "app.cache.user-metadata-cache")
	public CacheSettings userMetadataCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_METADATA)
	public Cache<Long, UserMetadataEntity> userMetadataCache(
			@Qualifier(USER_METADATA) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, UserMetadataEntity.class,
				USER_METADATA + "-cache");
	}

	@Bean
	@Qualifier(USER_METADATA_PATH)
	@ConfigurationProperties(prefix = "app.cache.user-metadata-path-cache")
	public CacheSettings userMetadataPathCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_METADATA_PATH)
	public Cache<UserStringCompositePK, String> userMetadataPathCache(
			@Qualifier(USER_METADATA_PATH) CacheSettings settings) {
		return settings.createCache(cacheManager, UserStringCompositePK.class, String.class,
				USER_METADATA_PATH + "-cache");
	}

}
