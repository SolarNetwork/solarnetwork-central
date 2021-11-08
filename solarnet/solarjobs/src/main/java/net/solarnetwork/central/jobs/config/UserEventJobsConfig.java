/* ==================================================================
 * UserEventJobsConfig.java - 8/11/2021 4:28:25 PM
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

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.event.dao.jobs.UserNodeEventTaskCleanerJob;
import net.solarnetwork.central.user.event.dao.jobs.UserNodeEventTaskProcessorJob;

/**
 * User event jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserEventJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	public UserNodeEventTaskDao userNodeEventTaskDao;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private List<UserNodeEventHookService> userNodeEventHookServices;

	@ConfigurationProperties(prefix = "app.job.datum.user-event.cleaner")
	@Bean
	public ManagedJob userNodeEventTaskProcessorJob() {
		UserNodeEventTaskProcessorJob job = new UserNodeEventTaskProcessorJob(transactionTemplate,
				userNodeEventTaskDao, userNodeEventHookServices);
		job.setId("UserNodeEventTaskProcessor");
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.datum.user-event.processor")
	@Bean
	public ManagedJob userNodeEventTaskCleanerJob() {
		UserNodeEventTaskCleanerJob job = new UserNodeEventTaskCleanerJob(userNodeEventTaskDao);
		job.setId("UserNodeEventTaskCleaner");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
