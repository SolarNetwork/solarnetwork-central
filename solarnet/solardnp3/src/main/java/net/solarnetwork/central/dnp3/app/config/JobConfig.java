/* ==================================================================
 * JobConfig.java - 22/08/2022 3:33:29 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.dnp3.app.jobs.ServerKeystoreReloadJob;
import net.solarnetwork.central.dnp3.app.jobs.SolarDnp3Jobs;
import net.solarnetwork.central.net.proxy.config.DynamicProxyServerSettings;
import net.solarnetwork.central.net.proxy.service.impl.NettyDynamicProxyServer;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * Configuration for jobs.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(SolarDnp3Jobs.JOBS_PROFILE)
public class JobConfig {

	/**
	 * A job to reload the DNP3 TLS server certificate (to pick up renewals).
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.dnp3.server-reload-tls")
	@Bean
	public ManagedJob dnp3ServerReloadTlsJob(DynamicProxyServerSettings settings,
			NettyDynamicProxyServer server) {
		return new ServerKeystoreReloadJob(settings.tls(), server);
	}

}
