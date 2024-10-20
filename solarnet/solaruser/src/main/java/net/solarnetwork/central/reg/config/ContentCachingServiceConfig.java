/* ==================================================================
 * ContentCachingServiceConfig.java - 9/10/2021 2:09:33 PM
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

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.web.support.CachedContent;
import net.solarnetwork.central.web.support.JCacheContentCachingService;

/**
 * Content caching service configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("app.query-cache.enabled")
public class ContentCachingServiceConfig {

	/** The cache for query results. */
	public static final String QUERY_CACHE = "query-cache";

	/** The bean name for the query cache service. */
	public static final String QUERY_CACHING_SERVICE = "queryCachingService";

	@Autowired
	private CacheManager cacheManager;

	public static class QueryCacheSettings extends CacheSettings {

		private int compressMinimumLength = 512;
		private int statLogAccessCount = 500;

		public int getCompressMinimumLength() {
			return compressMinimumLength;
		}

		public void setCompressMinimumLength(int compressMinimumLength) {
			this.compressMinimumLength = compressMinimumLength;
		}

		public int getStatLogAccessCount() {
			return statLogAccessCount;
		}

		public void setStatLogAccessCount(int statLogAccessCount) {
			this.statLogAccessCount = statLogAccessCount;
		}

	}

	@Bean
	@Qualifier(QUERY_CACHE)
	@ConfigurationProperties(prefix = "app.query-cache.cache")
	public QueryCacheSettings queryCacheSettings() {
		return new QueryCacheSettings();
	}

	/**
	 * Get the datum cache.
	 *
	 * @return the actor cache
	 */
	@Bean
	@Qualifier(QUERY_CACHE)
	public Cache<String, CachedContent> queryCache(@Qualifier(QUERY_CACHE) QueryCacheSettings settings) {
		return settings.createCache(cacheManager, String.class, CachedContent.class, QUERY_CACHE);
	}

	@Bean(QUERY_CACHING_SERVICE)
	@Qualifier(QUERY_CACHE)
	public JCacheContentCachingService queryCachingService(
			@Qualifier(QUERY_CACHE) QueryCacheSettings settings,
			@Qualifier(QUERY_CACHE) Cache<String, CachedContent> queryCache) {
		JCacheContentCachingService service = new JCacheContentCachingService(queryCache);
		service.setCompressMinimumLength(settings.compressMinimumLength);
		service.setStatLogAccessCount(settings.statLogAccessCount);
		return service;
	}

}
