/* ==================================================================
 * UserServiceAuditJobsConfig.java - 29/05/2024 4:54:43 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.job.StaleAuditUserServiceProcessor;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Configuration for user service audit jobs.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class UserServiceAuditJobsConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@ConfigurationProperties(prefix = "app.job.user.audit.daily")
	@Bean
	public ManagedJob staleAuditUserServiceProcessorDaily() {
		StaleAuditUserServiceProcessor processor = new StaleAuditUserServiceProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditUserServiceProcessorDaily");
		processor.setTierProcessType(Aggregation.Day.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.user.audit.monthly")
	@Bean
	public ManagedJob staleAuditUserServiceProcessorMonthly() {
		StaleAuditUserServiceProcessor processor = new StaleAuditUserServiceProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditUserServiceProcessorMonthly");
		processor.setTierProcessType(Aggregation.Month.getKey());
		return processor;
	}

}
