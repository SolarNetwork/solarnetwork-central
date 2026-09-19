/* ==================================================================
 * UserNodeInstructionsJobsConfig.java - 28/11/2025 10:27:48 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.central.user.biz.UserNodeInstructionService;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.job.UserNodeInstructionTaskProcessor;
import net.solarnetwork.central.user.job.UserNodeInstructionTaskResetAbandoned;

/**
 * Configuration for the user instruction jobs.
 *
 * @author matt
 * @version 1.0
 */
@Profile(SolarNetUserConfiguration.USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserNodeInstructionsJobsConfig {

	@Autowired
	private UserNodeInstructionService userNodeInstructionService;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@ConfigurationProperties(prefix = "app.job.user-instr.processor")
	@Bean
	public ManagedJob userNodeInstructionTaskProcessor() {
		var job = new UserNodeInstructionTaskProcessor(userNodeInstructionService);
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.user-instr.abandoned")
	@Bean
	public ManagedJob userNodeInstructionTaskResetAbandoned() {
		return new UserNodeInstructionTaskResetAbandoned(userNodeInstructionService);
	}

}
