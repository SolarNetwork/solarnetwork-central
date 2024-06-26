/* ==================================================================
 * DatumExportJobsConfig.java - 9/11/2021 9:08:46 AM
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
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.jobs.DatumImportJobInfoCleanerJob;
import net.solarnetwork.central.datum.imp.jobs.DatumImportProcessorJob;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * Datum import jobs configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class DatumImportJobsConfig {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private DatumImportJobBiz datumImportJobBiz;

	@ConfigurationProperties(prefix = "app.job.datum.import.processor")
	@Bean
	public ManagedJob datumImportProcessorJob() {
		DatumImportProcessorJob job = new DatumImportProcessorJob(datumImportJobBiz);
		job.setId("DatumImportProcessor");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

	@ConfigurationProperties(prefix = "app.job.datum.import.cleaner")
	@Bean
	public ManagedJob datumImportJobInfoCleanerJob() {
		DatumImportJobInfoCleanerJob job = new DatumImportJobInfoCleanerJob(datumImportJobBiz);
		job.setId("DatumImportJobInfoCleaner");
		job.setParallelTaskExecutor(taskExecutor);
		return job;
	}

}
