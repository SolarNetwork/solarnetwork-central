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
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.in.biz.dao.AsyncDaoDatumCollector;
import net.solarnetwork.central.in.biz.dao.CollectorStats;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.in.biz.dao.SimpleNetworkIdentityBiz;
import net.solarnetwork.central.support.BufferingDelegatingCache;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Business service configuration for the SolarIn application.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SolarInBizConfig {

	/**
	 * A cache name to use for datum objects.
	 */
	public static final String DATUM_CACHE_NAME = "datum-buffer";

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

	/** Settings for the datum cache. */
	public static class DatumCacheSettings extends CacheSettings {

		private int tempMaxEntries = 100;

		public DatumCacheSettings() {
			super();
			setDiskPersistent(true);
		}

		public int getTempMaxEntries() {
			return tempMaxEntries;
		}

		public void setTempMaxEntries(int tempMaxEntries) {
			this.tempMaxEntries = tempMaxEntries;
		}

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
		DatumCacheSettings settings = datumCacheSettings();
		return settings.createCache(cacheManager, Serializable.class, Serializable.class,
				DATUM_CACHE_NAME);
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

		/**
		 * Get the thread count.
		 * 
		 * @return the thread count
		 */
		public int getThreads() {
			return threads;
		}

		/**
		 * Set the thread count.
		 * 
		 * @param threads
		 *        the thread count to set
		 */
		public void setThreads(int threads) {
			this.threads = threads;
		}

		/**
		 * Get the shutdown wait time, in seconds.
		 * 
		 * @return the seconds
		 */
		public int getShutdownWaitSecs() {
			return shutdownWaitSecs;
		}

		/**
		 * Set the shutdown wait time, in seconds.
		 * 
		 * @param shutdownWaitSecs
		 *        the seconds to set
		 */
		public void setShutdownWaitSecs(int shutdownWaitSecs) {
			this.shutdownWaitSecs = shutdownWaitSecs;
		}

		/**
		 * Get the queue size.
		 * 
		 * @return the queueSize
		 */
		public int getQueueSize() {
			return queueSize;
		}

		/**
		 * Set the queue size.
		 * 
		 * @param queueSize
		 *        the queueSize to set
		 */
		public void setQueueSize(int queueSize) {
			this.queueSize = queueSize;
		}

		/**
		 * Get the statistic frequency.
		 * 
		 * @return the frequency
		 */
		public int getStatFrequency() {
			return statFrequency;
		}

		/**
		 * Set the statistic frequency.
		 * 
		 * @param statFrequency
		 *        the frequency to set
		 */
		public void setStatFrequency(int statFrequency) {
			this.statFrequency = statFrequency;
		}

		/**
		 * Get the cache removal alert threshold.
		 * 
		 * @return the threshold
		 */
		public int getDatumCacheRemovalAlertThreshold() {
			return datumCacheRemovalAlertThreshold;
		}

		/**
		 * Set the cache removal alert threshold.
		 * 
		 * @param datumCacheRemovalAlertThreshold
		 *        the threshold to set
		 */
		public void setDatumCacheRemovalAlertThreshold(int datumCacheRemovalAlertThreshold) {
			this.datumCacheRemovalAlertThreshold = datumCacheRemovalAlertThreshold;
		}

	}

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.async-collector")
	public AsyncDatumCollectorSettings asyncDatumCollectorSettings() {
		return new AsyncDatumCollectorSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
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

	@Bean
	public NetworkIdentityBiz networkIdentityBiz() {
		return new SimpleNetworkIdentityBiz(networkIdentificationBiz, networkAssociationDao);
	}

}