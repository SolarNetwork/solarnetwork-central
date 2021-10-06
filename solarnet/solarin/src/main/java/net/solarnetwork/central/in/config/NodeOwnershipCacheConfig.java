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

package net.solarnetwork.central.in.config;

import static net.solarnetwork.central.common.dao.config.SolarNodeOwnershipDaoConfig.NODE_OWNERSHIP_CACHE;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import java.time.Duration;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.domain.SolarNodeOwnership;

/**
 * Configuration for the node ownership cache.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class NodeOwnershipCacheConfig {

	@Autowired
	private CacheManager cacheManager;

	public static class NodeOwnershipCacheSettings {

		private long ttl = 60;
		private int heapMaxEntries = 10000;
		private int diskMaxSizeMb = 100;
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.node-ownership-cache")
	public NodeOwnershipCacheSettings nodeOwnershipCacheSettings() {
		return new NodeOwnershipCacheSettings();
	}

	/**
	 * Get the datum cache.
	 * 
	 * @return the actor cache
	 */
	@Bean
	@Qualifier(NODE_OWNERSHIP_CACHE)
	public Cache<Long, SolarNodeOwnership> nodeOwnershipCache() {
		NodeOwnershipCacheSettings settings = nodeOwnershipCacheSettings();
		// @formatter:off
		CacheConfiguration<Long, SolarNodeOwnership> conf = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Long.class, SolarNodeOwnership.class,
						heap(settings.heapMaxEntries)
						.disk(settings.diskMaxSizeMb, MemoryUnit.MB, true))
				.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(settings.ttl)))
				.build();
		// @formatter:on
		return cacheManager.createCache(NODE_OWNERSHIP_CACHE,
				Eh107Configuration.fromEhcacheCacheConfiguration(conf));
	}

}
