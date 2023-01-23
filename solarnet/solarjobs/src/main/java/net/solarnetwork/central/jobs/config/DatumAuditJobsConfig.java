/* ==================================================================
 * DatumAggJobsConfig.java - 8/11/2021 7:30:10 AM
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
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.job.JdbcCallJob;
import net.solarnetwork.central.datum.agg.StaleAuditDataProcessor;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Datum aggregate jobs configuration.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
public class DatumAuditJobsConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@ConfigurationProperties(prefix = "app.job.datum.audit.raw")
	@Bean
	public ManagedJob staleAuditDatumProcessorRaw() {
		StaleAuditDataProcessor processor = new StaleAuditDataProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditNodeDatumProcessorRaw");
		processor.setTierProcessType(Aggregation.None.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.audit.hourly")
	@Bean
	public ManagedJob staleAuditDatumProcessorHourly() {
		StaleAuditDataProcessor processor = new StaleAuditDataProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditNodeDatumProcessorHourly");
		processor.setTierProcessType(Aggregation.Hour.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.audit.daily")
	@Bean
	public ManagedJob staleAuditDatumProcessorDaily() {
		StaleAuditDataProcessor processor = new StaleAuditDataProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditNodeDatumProcessorDaily");
		processor.setTierProcessType(Aggregation.Day.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.audit.monthly")
	@Bean
	public ManagedJob staleAuditDatumProcessorMonthly() {
		StaleAuditDataProcessor processor = new StaleAuditDataProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setId("StaleAuditNodeDatumProcessorMonthly");
		processor.setTierProcessType(Aggregation.Month.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.audit.missing")
	@Bean
	public ManagedJob auditDatumDailyMissingPopulator() {
		JdbcCallJob job = new JdbcCallJob(jdbcOperations);
		job.setId("AuditDatumDailyMissingPopulator");
		job.setJdbcCall("{? = call solardatm.populate_audit_datm_daily_missing()}");
		return job;
	}

}
