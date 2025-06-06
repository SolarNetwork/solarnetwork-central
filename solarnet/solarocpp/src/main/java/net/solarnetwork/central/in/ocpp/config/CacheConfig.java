/* ==================================================================
 * CacheConfig.java - 5/10/2021 6:43:38 AM
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

package net.solarnetwork.central.in.ocpp.config;

import java.nio.file.Path;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import org.ehcache.core.config.DefaultConfiguration;
import org.ehcache.impl.config.persistence.DefaultPersistenceConfiguration;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

	@Value("${app.cache.persistence.path}")
	private Path persistencePath;

	@Bean
	public CacheManager jCacheManager() {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		if ( cachingProvider instanceof EhcacheCachingProvider eh ) {
			DefaultConfiguration configuration = new DefaultConfiguration(
					cachingProvider.getDefaultClassLoader(),
					new DefaultPersistenceConfiguration(persistencePath.toFile()));
			return eh.getCacheManager(cachingProvider.getDefaultURI(), configuration);
		} else {
			return cachingProvider.getCacheManager();
		}
	}

}
