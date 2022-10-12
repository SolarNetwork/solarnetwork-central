/* ==================================================================
 * JobSchedulerConfig.java - 8/11/2021 6:53:03 AM
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

package net.solarnetwork.oscp.sim.cp.config;

import static net.solarnetwork.central.scheduler.SchedulerUtils.triggerForExpression;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import net.solarnetwork.central.oscp.jobs.OscpJobs;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.scheduler.SimpleSchedulerManager;

/**
 * Scheduler configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OscpJobs.JOBS_PROFILE)
public class JobSchedulerConfig {

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private List<ManagedJob> managedJobs;

	@ConfigurationProperties(prefix = "app.scheduler-manager")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	public SimpleSchedulerManager schedulerManager() {
		SimpleSchedulerManager manager = new SimpleSchedulerManager(taskScheduler);
		for ( ManagedJob job : managedJobs ) {
			Trigger trigger = triggerForExpression(job.getSchedule(), TimeUnit.MILLISECONDS, false);
			manager.scheduleJob(job.getGroupId(), job.getId(), job, trigger);
		}
		return manager;
	}

}
