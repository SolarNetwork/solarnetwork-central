/* ==================================================================
 * NodeOwnershipCacheConfig.java - 7/10/2021 11:25:40 AM
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.common.dao.config.SolarNodeOwnershipDaoConfig.NODE_OWNERSHIP_CACHE;
import static net.solarnetwork.central.common.dao.config.SolarNodeOwnershipDaoConfig.STREAM_METADATA_ID_CACHE_NAME;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Configuration for the node ownership cache.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class NodeOwnershipCacheConfig {

	@Autowired
	private CacheManager cacheManager;

	/**
	 * The node ownership cache settings.
	 *
	 * @return the settings
	 */
	@Qualifier(NODE_OWNERSHIP_CACHE)
	@Bean
	@ConfigurationProperties(prefix = "app.node-ownership-cache")
	public CacheSettings nodeOwnershipCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the node ownership cache.
	 *
	 * @return the node ownership cache
	 */
	@Bean
	@Qualifier(NODE_OWNERSHIP_CACHE)
	public Cache<Long, SolarNodeOwnership> nodeOwnershipCache(
			@Qualifier(NODE_OWNERSHIP_CACHE) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, SolarNodeOwnership.class,
				NODE_OWNERSHIP_CACHE);
	}

	@Bean
	@Qualifier(STREAM_METADATA_ID_CACHE_NAME)
	@ConfigurationProperties(prefix = "app.datum.stream-metadata-id-cache")
	public CacheSettings streamMetadataIdCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the metadata ID cache.
	 *
	 * @return the metadata ID cache
	 */
	@Bean
	@Qualifier(STREAM_METADATA_ID_CACHE_NAME)
	public Cache<UUID, ObjectDatumStreamMetadataId> streamMetadataIdCache(
			@Qualifier(STREAM_METADATA_ID_CACHE_NAME) CacheSettings settings) {
		return settings.createCache(cacheManager, UUID.class, ObjectDatumStreamMetadataId.class,
				STREAM_METADATA_ID_CACHE_NAME);
	}

}
