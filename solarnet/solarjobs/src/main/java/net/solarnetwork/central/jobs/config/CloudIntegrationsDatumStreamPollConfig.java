/* ==================================================================
 * CloudIntegrationsConfig.java - 8/10/2024 9:31:24 am
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
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.DaoCloudDatumStreamPollService;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;

/**
 * Cloud integrations datum stream poll configuration.
 *
 * @author matt
 * @version 1.1
 */
@Profile(CLOUD_INTEGRATIONS)
@Configuration(proxyBeanMethods = false)
public class CloudIntegrationsDatumStreamPollConfig implements SolarNetCloudIntegrationsConfiguration {

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Autowired
	private CloudDatumStreamPollTaskDao taskDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Autowired
	private CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;

	@Autowired
	private DatumWriteOnlyDao datumWriteOnlyDao;

	@Autowired(required = false)
	@Qualifier("solarflux")
	private DatumProcessor fluxPublisher;

	@ConfigurationProperties(prefix = "app.c2c.ds-poll.executor")
	@Qualifier(CLOUD_INTEGRATIONS_POLL)
	@Bean
	public ThreadPoolTaskExecutor cloudDatumStreamPollExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-C2C-Ds-Poll-");
		return executor;
	}

	@ConfigurationProperties(prefix = "app.c2c.ds-poll.service")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public CloudDatumStreamPollService cloudDatumStreamPollService(
			@Qualifier(CLOUD_INTEGRATIONS_POLL) ThreadPoolTaskExecutor taskExecutor,
			Collection<CloudDatumStreamService> datumStreamServices) {
		var dsMap = datumStreamServices.stream()
				.collect(Collectors.toMap(CloudDatumStreamService::getId, Function.identity()));
		var service = new DaoCloudDatumStreamPollService(Clock.systemUTC(), userEventAppenderBiz,
				nodeOwnershipDao, taskDao, datumStreamDao, datumStreamSettingsDao, datumWriteOnlyDao,
				taskExecutor.getThreadPoolExecutor(), dsMap::get);
		service.setFluxPublisher(fluxPublisher);
		return service;
	}

}
