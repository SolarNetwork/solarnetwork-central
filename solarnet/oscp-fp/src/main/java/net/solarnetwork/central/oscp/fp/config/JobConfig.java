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

package net.solarnetwork.central.oscp.fp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.MeasurementDao;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.jobs.CapacityGroupMeasurementJob;
import net.solarnetwork.central.oscp.jobs.HeartbeatJob;
import net.solarnetwork.central.oscp.jobs.OscpJobs;
import net.solarnetwork.central.scheduler.ManagedJob;

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
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Autowired
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Autowired
	private AssetConfigurationDao assetDao;

	@Autowired
	private MeasurementDao measurementDao;

	@Autowired
	private ExternalSystemClient externalSystemClient;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private TransactionTemplate txTemplate;

	/**
	 * The OSCP Heartbeat job for capacity provider systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.heartbeat.cp")
	@Bean
	public ManagedJob capacityProviderHeartbeatJob() {
		var job = new HeartbeatJob(OscpRole.CapacityProvider, capacityProviderDao, externalSystemClient)
				.withTxTemplate(txTemplate);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	/**
	 * The OSCP Heartbeat job for capacity optimizer systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.heartbeat.co")
	@Bean
	public ManagedJob capacityOptimizerHeartbeatJob() {
		var job = new HeartbeatJob(OscpRole.CapacityOptimizer, capacityOptimizerDao,
				externalSystemClient).withTxTemplate(txTemplate);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	/**
	 * The OSCP Heartbeat job for capacity provider systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.measurement.cp")
	@Bean
	public ManagedJob capacityProviderMeasurementJob() {
		var job = new CapacityGroupMeasurementJob(OscpRole.CapacityProvider, capacityProviderDao,
				capacityGroupDao, assetDao, measurementDao, externalSystemClient)
						.withTxTemplate(txTemplate);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	/**
	 * The OSCP Heartbeat job for capacity optimizer systems.
	 * 
	 * @return the job
	 */
	@ConfigurationProperties(prefix = "app.job.oscp.measurement.co")
	@Bean
	public ManagedJob capacityOptimizerMeasurementJob() {
		var job = new CapacityGroupMeasurementJob(OscpRole.CapacityOptimizer, capacityOptimizerDao,
				capacityGroupDao, assetDao, measurementDao, externalSystemClient)
						.withTxTemplate(txTemplate);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
