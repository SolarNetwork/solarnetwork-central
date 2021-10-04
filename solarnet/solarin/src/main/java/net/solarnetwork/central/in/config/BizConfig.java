/* ==================================================================
 * BizConfig.java - 4/10/2021 4:50:12 PM
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

import static org.ehcache.config.builders.ExpiryPolicyBuilder.noExpiration;
import static org.ehcache.config.builders.ResourcePoolsBuilder.heap;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.cache.Cache;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.in.biz.dao.AsyncDaoDatumCollector;
import net.solarnetwork.central.in.biz.dao.CollectorStats;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.in.biz.dao.SimpleNetworkIdentityBiz;
import net.solarnetwork.central.support.BufferingDelegatingCache;

/**
 * Business service configuration for the SolarIn application.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class BizConfig {

	/**
	 * A cache name to use for datum objects.
	 */
	public static final String DATUM_CACHE_NAME = "datum-buffer";

	@Autowired
	private javax.cache.CacheManager cacheManager;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private DatumStreamMetadataDao metaDao;

	@Autowired
	private SolarLocationDao solarLocationDao;

	@Autowired
	private SolarNodeDao solarNodeDao;

	@Autowired
	private SolarNodeMetadataDao solarNodeMetadataDao;

	@Autowired
	private DatumMetadataBiz datumMetadataBiz;

	@Autowired
	private PlatformTransactionManager txManager;

	@Bean
	public SolarNodeMetadataBiz solarNodeMetadataBiz() {
		DaoSolarNodeMetadataBiz biz = new DaoSolarNodeMetadataBiz(solarNodeMetadataDao);
		return biz;
	}

	@Bean
	public TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(txManager);
	}

	/** Settings for the datum cache. */
	public static class DatumCacheSettings {

		private int tempMaxEntries = 100;
		private long heapMaxEntries = 200;
		private long diskMaxSizeMb = 2000;
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.datum-buffer")
	public DatumCacheSettings datumCacheSettings() {
		return new DatumCacheSettings();
	}

	/**
	 * Get the datum cache.
	 * 
	 * @return the actor cache
	 */
	@Bean
	@Qualifier(DATUM_CACHE_NAME)
	public Cache<Serializable, Serializable> datumCache() {
		return cacheManager.createCache(DATUM_CACHE_NAME, datumCacheConfiguration(datumCacheSettings()));
	}

	private javax.cache.configuration.Configuration<Serializable, Serializable> datumCacheConfiguration(
			DatumCacheSettings settings) {
		// @formatter:off
		CacheConfiguration<Serializable, Serializable> conf = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Serializable.class, Serializable.class,
						heap(settings.heapMaxEntries)
						.disk(settings.diskMaxSizeMb, MemoryUnit.MB, true))
				.withExpiry(noExpiration())
				.build();
		// @formatter:on
		return Eh107Configuration.fromEhcacheCacheConfiguration(conf);
	}

	@Bean
	public Cache<Serializable, Serializable> bufferingDatumCache() {
		DatumCacheSettings settings = datumCacheSettings();
		return new BufferingDelegatingCache<>(datumCache(), settings.tempMaxEntries);
	}

	/** Settings for the AsyncDaoDatumCollector. */
	public static class AsyncDatumCollectorSettings {

		private int threads = 2;
		private int shutdownWaitSecs = 30;
		private int queueSize = 200;
		private int statFrequency = 200;
		private int datumCacheRemovalAlertThreshold = 500;
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.async-collector")
	public AsyncDatumCollectorSettings asyncDatumCollectorSettings() {
		return new AsyncDatumCollectorSettings();
	}

	@Bean
	public AsyncDaoDatumCollector asyncDaoDatumCollector() {
		AsyncDatumCollectorSettings settings = asyncDatumCollectorSettings();
		CollectorStats stats = new CollectorStats("AsyncDaoDatum", settings.statFrequency);
		AsyncDaoDatumCollector collector = new AsyncDaoDatumCollector(bufferingDatumCache(), datumDao,
				transactionTemplate(), stats);
		collector.setConcurrency(settings.threads);
		collector.setShutdownWaitSecs(settings.shutdownWaitSecs);
		collector.setQueueSize(settings.queueSize);
		collector.setDatumCacheRemovalAlertThreshold(settings.datumCacheRemovalAlertThreshold);
		return collector;
	}

	@Bean
	public DataCollectorBiz dataCollectorBiz() {
		DaoDataCollectorBiz biz = new DaoDataCollectorBiz();
		biz.setDatumDao(datumDao);
		biz.setMetaDao(metaDao);
		biz.setSolarLocationDao(solarLocationDao);
		biz.setSolarNodeDao(solarNodeDao);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setSolarNodeMetadataBiz(solarNodeMetadataBiz());
		biz.setTransactionTemplate(transactionTemplate());
		biz.setDatumCache(bufferingDatumCache());
		return biz;
	}

	/** Settings for the NetworkIdentityBiz. */
	public static class NetworkIdentitySettings {

		private String networkIdentityKey = "replace:identity:here";
		private String termsOfService = "replace:tos:here";
		private String host = "localhost";
		private int port = 8080;
		private boolean forceTls = false;
		private Map<String, String> serviceUrls = defaultNetworkServiceUrls();

		private static Map<String, String> defaultNetworkServiceUrls() {
			Map<String, String> map = new LinkedHashMap<>(4);
			map.put("solaruser", "http://localhost/solaruser");
			map.put("solarquery", "http://localhost/solarquery");
			map.put("solarin-mqtt", "mqtts://localhost:8883");
			return map;
		}
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.network-identity")
	public NetworkIdentitySettings networkIdentitySettings() {
		return new NetworkIdentitySettings();
	}

	@Bean
	public NetworkIdentityBiz networkIdentityBiz() {
		NetworkIdentitySettings settings = networkIdentitySettings();
		SimpleNetworkIdentityBiz biz = new SimpleNetworkIdentityBiz();
		biz.setNetworkIdentityKey(settings.networkIdentityKey);
		biz.setTermsOfService(settings.termsOfService);
		biz.setHost(settings.host);
		biz.setPort(settings.port);
		biz.setForceTLS(settings.forceTls);
		biz.setNetworkServiceURLs(settings.serviceUrls);
		return biz;
	}

}
