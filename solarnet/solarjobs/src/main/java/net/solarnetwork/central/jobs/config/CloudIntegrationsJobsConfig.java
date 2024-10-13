/* ==================================================================
 * CloudIntegrationsJobsConfig.java - 11/10/2024 11:27:31 am
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.c2c.job.CloudDatumStreamPollTaskProcessor;
import net.solarnetwork.central.c2c.job.CloudDatumStreamPollTaskResetAbandoned;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * Cloud integrations jobs configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(CLOUD_INTEGRATIONS)
@Configuration(proxyBeanMethods = false)
public class CloudIntegrationsJobsConfig {

	@Autowired
	private CloudDatumStreamPollService cloudDatumStreamPollService;

	@ConfigurationProperties(prefix = "app.job.c2c.ds-poll")
	@Bean
	public ManagedJob cloudDatumStreamPollTaskProcessor() {
		return new CloudDatumStreamPollTaskProcessor(cloudDatumStreamPollService);
	}

	@ConfigurationProperties(prefix = "app.job.c2c.ds-poll-reset-abandoned")
	@Bean
	public ManagedJob cloudDatumStreamPollTaskResetAbandonedJob() {
		return new CloudDatumStreamPollTaskResetAbandoned(cloudDatumStreamPollService);
	}

}
