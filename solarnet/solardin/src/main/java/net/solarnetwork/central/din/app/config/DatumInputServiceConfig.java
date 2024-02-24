/* ==================================================================
 * DatumInputServiceConfig.java - 24/02/2024 8:26:42 am
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

package net.solarnetwork.central.din.app.config;

import java.io.Serializable;
import java.util.Collection;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.dao.AsyncDaoDatumCollector;
import net.solarnetwork.central.datum.biz.dao.CollectorStats;
import net.solarnetwork.central.datum.support.AsyncDatumCollectorSettings;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.biz.impl.DaoDatumInputEndpointBiz;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;

/**
 * Core service configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputServiceConfig implements DatumInputConfiguration {

	@Autowired
	private PlatformTransactionManager txManager;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private TransformConfigurationDao transformDao;

	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private Collection<TransformService> transformServices;

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.async-collector")
	public AsyncDatumCollectorSettings asyncDatumCollectorSettings() {
		return new AsyncDatumCollectorSettings();
	}

	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public AsyncDaoDatumCollector asyncDaoDatumCollector(AsyncDatumCollectorSettings settings,
			@Qualifier(DATUM_BUFFER) Cache<Serializable, Serializable> buffer) {
		TransactionTemplate tt = new TransactionTemplate(txManager);
		CollectorStats stats = new CollectorStats("AsyncDaoDatum", settings.getStatFrequency());
		AsyncDaoDatumCollector collector = new AsyncDaoDatumCollector(buffer, datumDao, tt, stats);
		collector.setConcurrency(settings.getThreads());
		collector.setShutdownWaitSecs(settings.getShutdownWaitSecs());
		collector.setQueueSize(settings.getQueueSize());
		collector.setDatumCacheRemovalAlertThreshold(settings.getDatumCacheRemovalAlertThreshold());
		return collector;
	}

	@Bean
	public DaoDatumInputEndpointBiz datumInputEndpointBiz() {
		return new DaoDatumInputEndpointBiz(nodeOwnershipDao, endpointDao, transformDao, datumDao,
				transformServices);
	}

}
