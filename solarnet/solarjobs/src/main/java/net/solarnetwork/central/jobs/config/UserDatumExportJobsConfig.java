/* ==================================================================
 * UserDatumExportJobsConfig.java - 11/11/2021 8:32:05 AM
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
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.jobs.DefaultUserExportJobsService;
import net.solarnetwork.central.user.export.jobs.UserExportJobsService;
import net.solarnetwork.central.user.export.jobs.UserExportTaskCleanerJob;
import net.solarnetwork.central.user.export.jobs.UserExportTaskPopulatorJob;

/**
 * User datum export jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class UserDatumExportJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private UserDatumExportConfigurationDao userDatumExportConfigurationDao;

	@Autowired
	private UserDatumExportTaskInfoDao userDatumExportTaskInfoDao;

	@Autowired
	private UserExportTaskBiz userExportTaskBiz;

	@Bean
	public UserExportJobsService userExportJobsService() {
		return new DefaultUserExportJobsService(userDatumExportConfigurationDao, userExportTaskBiz);
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-export.hourly")
	@Bean
	public ManagedJob hourlyTaskPopulatorJob() {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(ScheduleType.Hourly,
				userExportJobsService());
		job.setId("UserExportTaskPopulatorHourly");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-export.daily")
	@Bean
	public ManagedJob dailyTaskPopulatorJob() {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(ScheduleType.Daily,
				userExportJobsService());
		job.setId("UserExportTaskPopulatorDaily");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-export.weekly")
	@Bean
	public ManagedJob weeklyTaskPopulatorJob() {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(ScheduleType.Weekly,
				userExportJobsService());
		job.setId("UserExportTaskPopulatorWeekly");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-export.monthly")
	@Bean
	public ManagedJob monthlyTaskPopulatorJob() {
		UserExportTaskPopulatorJob job = new UserExportTaskPopulatorJob(ScheduleType.Monthly,
				userExportJobsService());
		job.setId("UserExportTaskPopulatorMonthly");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-datum-export.cleaner")
	@Bean
	public ManagedJob userExportTaskCleanerJob() {
		UserExportTaskCleanerJob job = new UserExportTaskCleanerJob(userDatumExportTaskInfoDao);
		job.setId("UserExportTaskCleaner");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
