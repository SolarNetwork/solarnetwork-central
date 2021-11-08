/* ==================================================================
 * UserDatumExpireJobsConfig.java - 9/11/2021 8:02:36 AM
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

package net.solarnetwork.central.jobs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteJobBiz;
import net.solarnetwork.central.user.expire.dao.ExpireUserDataConfigurationDao;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.jobs.DatumDeleteJobInfoCleanerJob;
import net.solarnetwork.central.user.expire.jobs.DatumDeleteProcessorJob;
import net.solarnetwork.central.user.expire.jobs.ExpireDatumJob;

/**
 * Datum expire jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserDatumExpireJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private ExpireUserDataConfigurationDao expireUserDataConfigurationDao;

	@Autowired
	private UserDatumDeleteJobBiz userDatumDeleteJobBiz;

	@Autowired
	private UserDatumDeleteJobInfoDao userDatumDeleteJobInfoDao;

	@ConfigurationProperties(prefix = "app.job.user-datum-expire.cleaner")
	@Bean
	public ManagedJob datumDeleteJobInfoCleanerJob() {
		DatumDeleteJobInfoCleanerJob job = new DatumDeleteJobInfoCleanerJob(userDatumDeleteJobBiz);
		job.setId("DatumDeleteJobInfoCleaner");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-expire.delete")
	@Bean
	public ManagedJob datumDeleteProcessorJob() {
		DatumDeleteProcessorJob job = new DatumDeleteProcessorJob(userDatumDeleteJobBiz,
				userDatumDeleteJobInfoDao);
		job.setId("DatumDeleteProcessor");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-expire.expire")
	@Bean
	public ManagedJob expireDatumJob() {
		ExpireDatumJob job = new ExpireDatumJob(expireUserDataConfigurationDao);
		job.setId("ExpireDatum");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
