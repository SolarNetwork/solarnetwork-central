/* ==================================================================
 * JdbcDatumEntityDaoConfig.java - 4/10/2021 9:07:25 PM
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

package net.solarnetwork.central.datum.dao.config;

import java.util.UUID;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.support.CacheSettings;

/**
 * JDBC datum entity DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class JdbcDatumEntityDaoConfig {

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

	@Bean
	@ConfigurationProperties(prefix = "app.datum.stream-metadata-cache")
	public CacheSettings streamMetadataCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the datum cache.
	 * 
	 * @return the actor cache
	 */
	@Bean
	@Qualifier(STREAM_METADATA_CACHE_NAME)
	public Cache<UUID, ObjectDatumStreamMetadata> streamMetadataCache() {
		CacheSettings settings = streamMetadataCacheSettings();
		return settings.createCache(cacheManager, UUID.class, ObjectDatumStreamMetadata.class,
				STREAM_METADATA_CACHE_NAME);
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
