/* ==================================================================
 * OcppJobsConfig.java - 26/08/2023 4:51:01 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.central.ocpp.dao.CentralChargeSessionDao;
import net.solarnetwork.central.ocpp.jobs.ChargeSessionCleanerJob;
import net.solarnetwork.central.ocpp.jobs.OcppJobs;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * OCPP jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OcppJobs.JOBS_PROFILE)
@ComponentScan(basePackageClasses = SolarNetOcppConfiguration.class)
public class OcppJobsConfig {

	@Autowired
	private CentralChargeSessionDao chargeSessionDao;

	@ConfigurationProperties(prefix = "app.job.ocpp.charge-session-cleaner")
	@Bean
	public ManagedJob ocppChargeSessionCleanerJob() {
		// delete on whole hours only for orderliness
		final Clock clock = Clock.tick(Clock.systemUTC(), Duration.ofHours(1));
		return new ChargeSessionCleanerJob(clock, chargeSessionDao);
	}

}
