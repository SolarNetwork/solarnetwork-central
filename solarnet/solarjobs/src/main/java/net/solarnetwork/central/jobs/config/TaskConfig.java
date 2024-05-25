/* ==================================================================
 * TaskConfig.java - 20/10/2021 4:56:03 PM
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

import java.util.concurrent.ExecutorService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Task management configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class TaskConfig {

	@ConfigurationProperties(prefix = "app.task.scheduler")
	@Bean(destroyMethod = "shutdown")
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("SolarNet-Sched-");
		scheduler.setPoolSize(1);
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Primary
	@ConfigurationProperties(prefix = "app.task.executor")
	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-");
		executor.setCorePoolSize(2);
		return executor;
	}

	/**
	 * Expose the task executor as an ExecutorService.
	 *
	 * @return the services
	 */
	@Bean
	public ExecutorService taskExecutorService(ThreadPoolTaskExecutor taskExecutor) {
		return taskExecutor.getThreadPoolExecutor();
	}

}
