/* ==================================================================
 * VirtualThreadTaskConfig.java - 20/10/2021 4:56:03 PM
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

package net.solarnetwork.central.common.mixin.config;

import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Task management configuration using virtual threads.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class VirtualThreadTaskConfig implements SchedulingConfigurer {

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskScheduler());
	}

	/**
	 * General task scheduler.
	 * 
	 * @return the scheduler
	 */
	@Bean
	public TaskScheduler taskScheduler() {
		return new SimpleAsyncTaskSchedulerBuilder().virtualThreads(true)
				.threadNamePrefix("SolarNet-Sched-").build();
	}

	/**
	 * General task executor.
	 * 
	 * @return the executor
	 */
	@Primary
	@Bean
	public AsyncTaskExecutor taskExecutor() {
		return new SimpleAsyncTaskExecutorBuilder().virtualThreads(true).threadNamePrefix("SolarNet-")
				.build();
	}
}
