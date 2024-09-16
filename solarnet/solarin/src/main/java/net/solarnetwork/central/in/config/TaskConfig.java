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

package net.solarnetwork.central.in.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import net.solarnetwork.central.scheduler.ThreadPoolTaskExecutorPingTest;
import net.solarnetwork.central.scheduler.ThreadPoolTaskSchedulerPingTest;
import net.solarnetwork.service.PingTest;

/**
 * Task management configuration.
 * 
 * @author matt
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
public class TaskConfig {

	@ConfigurationProperties(prefix = "app.task.scheduler")
	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix("SolarNet-Sched-");
		scheduler.setPoolSize(1);
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	@Primary
	@ConfigurationProperties(prefix = "app.task.executor")
	@Bean(destroyMethod = "shutdown")
	public AsyncTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarNet-");
		executor.setCorePoolSize(2);
		return executor;
	}

	/**
	 * Expose a ping test for the task scheduler.
	 *
	 * @param taskScheduler
	 *        the scheduler
	 * @return the ping test
	 * @since 1.2
	 */
	@Bean
	public PingTest taskSchedulerPingTest(ThreadPoolTaskScheduler taskScheduler) {
		return new ThreadPoolTaskSchedulerPingTest(taskScheduler);
	}

	/**
	 * Expose a ping test for the task executor.
	 *
	 * @param taskExecutor
	 *        the executor
	 * @return the ping test
	 * @since 1.2
	 */
	@Bean
	public PingTest taskExecutorPingTest(ThreadPoolTaskExecutor taskExecutor) {
		return new ThreadPoolTaskExecutorPingTest(taskExecutor);
	}

}
