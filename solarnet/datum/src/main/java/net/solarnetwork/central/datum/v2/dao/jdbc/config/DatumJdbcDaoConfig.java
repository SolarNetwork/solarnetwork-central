/* ==================================================================
 * DatumJdbcDaoConfig.java - 4/10/2021 9:07:25 PM
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

package net.solarnetwork.central.datum.v2.dao.jdbc.config;

import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import java.time.Duration;
import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.sql.DataSource;
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
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Datum JDBC DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumJdbcDaoConfig {

	/**
	 * A cache name to use for stream metadata objects.
	 */
	public static final String STREAM_METADATA_CACHE_NAME = "metadata-for-stream";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PlatformTransactionManager txManager;

	@Autowired
	@Qualifier("central")
	private JdbcOperations jdbcOperations;

	@Autowired
	private CacheManager cacheManager;

	public static class StreamMetadataCacheSettings {

		private long ttl = 300;
		private int heapMaxEntries = 10000;
		private int diskMaxSizeMb = 100;
	}

	@Bean
	@ConfigurationProperties(prefix = "app.datum.stream-metadata-cache")
	public StreamMetadataCacheSettings streamMetadataCacheSettings() {
		return new StreamMetadataCacheSettings();
	}

	/**
	 * Get the datum cache.
	 * 
	 * @return the actor cache
	 */
	@Bean
	@Qualifier(STREAM_METADATA_CACHE_NAME)
	public Cache<UUID, ObjectDatumStreamMetadata> streamMetadataCache() {
		return cacheManager.createCache(STREAM_METADATA_CACHE_NAME,
				streamMetadataCacheConfiguration(streamMetadataCacheSettings()));
	}

	private javax.cache.configuration.Configuration<UUID, ObjectDatumStreamMetadata> streamMetadataCacheConfiguration(
			StreamMetadataCacheSettings settings) {
		// @formatter:off
		CacheConfiguration<UUID, ObjectDatumStreamMetadata> conf = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(UUID.class, ObjectDatumStreamMetadata.class,
						heap(settings.heapMaxEntries)
						.disk(settings.diskMaxSizeMb, MemoryUnit.MB, true))
				.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(settings.ttl)))
				.build();
		// @formatter:on
		return Eh107Configuration.fromEhcacheCacheConfiguration(conf);
	}

	@Bean
	public JdbcDatumEntityDao datumEntityDao() {
		JdbcDatumEntityDao dao = new JdbcDatumEntityDao(jdbcOperations);
		dao.setStreamMetadataCache(streamMetadataCache());
		dao.setBulkLoadDataSource(dataSource);
		dao.setBulkLoadTransactionManager(txManager);
		return dao;
	}

}
