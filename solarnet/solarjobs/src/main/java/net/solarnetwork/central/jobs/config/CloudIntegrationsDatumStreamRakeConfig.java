/* ==================================================================
 * CloudIntegrationsDatumStreamRakeConfig.java - 22/09/2025 11:18:31 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import java.time.Clock;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.DaoCloudDatumStreamRakeService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.scheduler.ThreadPoolTaskExecutorPingTest;
import net.solarnetwork.service.PingTest;

/**
 * Cloud integrations datum stream rake configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(CLOUD_INTEGRATIONS)
@Configuration(proxyBeanMethods = false)
public class CloudIntegrationsDatumStreamRakeConfig implements SolarNetCloudIntegrationsConfiguration {

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private CloudDatumStreamPollTaskDao pollTaskDao;

	@Autowired
	private CloudDatumStreamRakeTaskDao rakeTaskDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Autowired
	private DatumEntityDao datumDao;

	@ConfigurationProperties(prefix = "app.c2c.ds-rake.executor")
	@Qualifier(CLOUD_INTEGRATIONS_RAKE)
	@Bean
	public ThreadPoolTaskExecutor cloudDatumStreamRakeExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-C2C-Ds-Rake-");
		return executor;
	}

	/**
	 * Expose a ping test for the rake task scheduler.
	 *
	 * @param taskExecutor
	 *        the executor
	 * @return the ping test
	 */
	@Bean
	public PingTest cloudDatumStreamRakeExecutorPingTest(
			@Qualifier(CLOUD_INTEGRATIONS_RAKE) ThreadPoolTaskExecutor taskExecutor) {
		return new ThreadPoolTaskExecutorPingTest(taskExecutor);
	}

	@ConfigurationProperties(prefix = "app.c2c.ds-rake.service")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public DaoCloudDatumStreamRakeService cloudDatumStreamRakeService(
			@Qualifier(CLOUD_INTEGRATIONS_RAKE) ThreadPoolTaskExecutor taskExecutor,
			Collection<CloudDatumStreamService> datumStreamServices) {
		var dsMap = datumStreamServices.stream()
				.collect(Collectors.toMap(CloudDatumStreamService::getId, Function.identity()));
		var service = new DaoCloudDatumStreamRakeService(Clock.systemUTC(), userEventAppenderBiz,
				nodeOwnershipDao, rakeTaskDao, pollTaskDao, datumStreamDao, datumDao,
				taskExecutor.getThreadPoolExecutor(), dsMap::get);
		return service;
	}

}
