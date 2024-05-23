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

import static net.solarnetwork.central.din.app.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import java.io.Serializable;
import java.util.Collection;
import javax.cache.Cache;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.support.AsyncDatumCollector;
import net.solarnetwork.central.datum.support.AsyncDatumCollectorSettings;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.biz.impl.DaoDatumInputEndpointBiz;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.InputDataEntityDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.util.StatTracker;

/**
 * Core service configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class DatumInputServiceConfig implements DatumInputConfiguration {

	@Autowired
	private PlatformTransactionManager txManager;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Qualifier(CACHING)
	@Autowired
	private TransformConfigurationDao transformDao;

	@Qualifier(CACHING)
	@Autowired
	private EndpointConfigurationDao endpointDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private InputDataEntityDao inputDataDao;

	@Autowired
	private Collection<TransformService> transformServices;

	@Autowired(required = false)
	@Qualifier(SOLARFLUX)
	private DatumProcessor fluxPublisher;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.async-collector")
	public AsyncDatumCollectorSettings asyncDatumCollectorSettings() {
		return new AsyncDatumCollectorSettings();
	}

	@Qualifier(CACHING)
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public AsyncDatumCollector asyncDaoDatumCollector(AsyncDatumCollectorSettings settings,
			@Qualifier(DATUM_BUFFER) Cache<Serializable, Serializable> buffer) {
		TransactionTemplate tt = new TransactionTemplate(txManager);
		StatTracker stats = new StatTracker("AsyncDaoDatum", null,
				LoggerFactory.getLogger(AsyncDatumCollector.class), settings.getStatFrequency());
		AsyncDatumCollector collector = new AsyncDatumCollector(buffer, datumDao, tt, stats);
		collector.setConcurrency(settings.getThreads());
		collector.setShutdownWaitSecs(settings.getShutdownWaitSecs());
		collector.setQueueSize(settings.getQueueSize());
		collector.setQueueRefillThreshold(settings.getQueueRefillThreshold());
		collector.setDatumCacheRemovalAlertThreshold(settings.getDatumCacheRemovalAlertThreshold());
		return collector;
	}

	@Bean
	public DaoDatumInputEndpointBiz datumInputEndpointBiz(
			@Qualifier(CACHING) DatumWriteOnlyDao datumDao) {
		var biz = new DaoDatumInputEndpointBiz(nodeOwnershipDao, endpointDao, transformDao, datumDao,
				inputDataDao, transformServices);
		biz.setFluxPublisher(fluxPublisher);
		biz.setUserEventAppenderBiz(userEventAppenderBiz);
		return biz;
	}

}
