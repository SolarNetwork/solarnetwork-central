/* ==================================================================
 * SolarInBizConfig.java - 4/10/2021 4:50:12 PM
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

import java.io.Serializable;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.NetworkIdentificationBiz;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.dao.NetworkAssociationDao;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.support.AsyncDatumCollector;
import net.solarnetwork.central.datum.support.AsyncDatumCollectorSettings;
import net.solarnetwork.central.datum.support.DatumCacheSettings;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.in.biz.dao.SimpleNetworkIdentityBiz;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.util.StatTracker;

/**
 * Business service configuration for the SolarIn application.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class SolarInBizConfig {

	/**
	 * A cache name to use for datum objects.
	 */
	public static final String DATUM_CACHE_NAME = "datum-buffer";

	/**
	 * A buffering cache name to use for datum objects.
	 */
	public static final String BUFFERING_DATUM_CACHE_NAME = "buffering-datum-cache";

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private DatumStreamMetadataDao metaDao;

	@Autowired
	private NetworkAssociationDao networkAssociationDao;

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

	@Autowired
	private NetworkIdentificationBiz networkIdentificationBiz;

	@Bean
	public SolarNodeMetadataBiz solarNodeMetadataBiz() {
		DaoSolarNodeMetadataBiz biz = new DaoSolarNodeMetadataBiz(solarNodeMetadataDao);
		return biz;
	}

	@Bean
	public TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(txManager);
	}

	@Bean
	@Qualifier(DATUM_CACHE_NAME)
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
	public Cache<Serializable, Serializable> datumCache(
			@Qualifier(DATUM_CACHE_NAME) DatumCacheSettings settings) {
		return settings.createCache(cacheManager, Serializable.class, Serializable.class,
				DATUM_CACHE_NAME);
	}

	@Qualifier(BUFFERING_DATUM_CACHE_NAME)
	@Bean
	public Cache<Serializable, Serializable> bufferingDatumCache(
			@Qualifier(DATUM_CACHE_NAME) Cache<Serializable, Serializable> datumCache,
			@Qualifier(DATUM_CACHE_NAME) DatumCacheSettings settings) {
		return new BufferingDelegatingCache<>(datumCache, settings.getTempMaxEntries());
	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.async-collector")
	public AsyncDatumCollectorSettings asyncDatumCollectorSettings() {
		return new AsyncDatumCollectorSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public AsyncDatumCollector asyncDaoDatumCollector(AsyncDatumCollectorSettings settings,
			TransactionTemplate transactionTemplate,
			@Qualifier(BUFFERING_DATUM_CACHE_NAME) Cache<Serializable, Serializable> bufferingDatumCache) {
		StatTracker stats = new StatTracker("AsyncDaoDatum",
				"net.solarnetwork.central.datum.support.AsyncDatumCollector",
				LoggerFactory.getLogger(AsyncDatumCollector.class), settings.getStatFrequency());
		AsyncDatumCollector collector = new AsyncDatumCollector(bufferingDatumCache, datumDao,
				transactionTemplate, stats);
		collector.setConcurrency(settings.getThreads());
		collector.setShutdownWaitSecs(settings.getShutdownWaitSecs());
		collector.setQueueSize(settings.getQueueSize());
		collector.setQueueRefillThreshold(settings.getQueueRefillThreshold());
		collector.setDatumCacheRemovalAlertThreshold(settings.getDatumCacheRemovalAlertThreshold());
		return collector;
	}

	@Bean
	public DataCollectorBiz dataCollectorBiz(SolarNodeMetadataBiz solarNodeMetadataBiz,
			TransactionTemplate transactionTemplate,
			@Qualifier(BUFFERING_DATUM_CACHE_NAME) Cache<Serializable, Serializable> bufferingDatumCache) {
		DaoDataCollectorBiz biz = new DaoDataCollectorBiz();
		biz.setDatumDao(datumDao);
		biz.setMetaDao(metaDao);
		biz.setSolarLocationDao(solarLocationDao);
		biz.setSolarNodeDao(solarNodeDao);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setSolarNodeMetadataBiz(solarNodeMetadataBiz);
		biz.setTransactionTemplate(transactionTemplate);
		biz.setDatumCache(bufferingDatumCache);
		return biz;
	}

	@Bean
	public NetworkIdentityBiz networkIdentityBiz() {
		return new SimpleNetworkIdentityBiz(networkIdentificationBiz, networkAssociationDao);
	}

}
