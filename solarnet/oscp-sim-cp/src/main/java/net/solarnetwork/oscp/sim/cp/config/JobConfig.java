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

package net.solarnetwork.oscp.sim.cp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.oscp.jobs.OscpJobs;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.oscp.sim.cp.dao.CapacityProviderDao;
import net.solarnetwork.oscp.sim.cp.jobs.HeartbeatJob;
import net.solarnetwork.oscp.sim.cp.jobs.OfflineAlertJob;

/**
 * Configuration for jobs.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OscpJobs.JOBS_PROFILE)
public class JobConfig {

	@Autowired
	private CapacityProviderDao capacityProviderDao;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private RestOperations restOps;

	/**
	 * The OSCP Heartbeat job for external systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.heartbeat")
	@Bean
	public ManagedJob heartbeatJob() {
		HeartbeatJob job = new HeartbeatJob(capacityProviderDao, taskExecutor, restOps);
		return job;
	}

	/**
	 * The OSCP Offline Alert job for external systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.offline-alert")
	@Bean
	public ManagedJob offlienAlertJob() {
		OfflineAlertJob job = new OfflineAlertJob(capacityProviderDao);
		return job;
	}

}
