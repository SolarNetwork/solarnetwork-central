/* ==================================================================
 * CachingSolarNodeMetadataDaoConfig.java - 13/11/2024 8:34:31 am
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

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.CACHING;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.common.dao.CachingSolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Caching {@link SolarNodeMetadataDao} configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class CachingSolarNodeMetadataDaoConfig {

	/** A qualifier for node metadata cache. */
	public static final String NODE_METADATA_CACHE = "metadata-for-node";

	@Autowired
	private Executor executor;

	@Autowired
	private CacheManager cacheManager;

	/**
	 * The node metadata cache settings.
	 *
	 * @return the settings
	 */
	@Qualifier(NODE_METADATA_CACHE)
	@Bean
	@ConfigurationProperties(prefix = "app.node-metadata-cache")
	public CacheSettings nodeMetadataCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the node metadata cache.
	 *
	 * @return the node metadata cache
	 */
	@Bean
	@Qualifier(NODE_METADATA_CACHE)
	public Cache<Long, SolarNodeMetadata> nodeMetadataCache(
			@Qualifier(NODE_METADATA_CACHE) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, SolarNodeMetadata.class,
				NODE_METADATA_CACHE);
	}

	/**
	 * The caching {@link SolarNodeMetadataDao}.
	 *
	 * @param cache
	 *        the cache
	 * @param delegate
	 *        the delegate DAO
	 * @return the caching DAO
	 */
	@Bean
	@Qualifier(CACHING)
	public SolarNodeMetadataDao cachingNodeMetadataDao(
			@Qualifier(NODE_METADATA_CACHE) Cache<Long, SolarNodeMetadata> cache,
			SolarNodeMetadataDao delegate) {
		return new CachingSolarNodeMetadataDao(delegate, cache, executor);
	}

}
