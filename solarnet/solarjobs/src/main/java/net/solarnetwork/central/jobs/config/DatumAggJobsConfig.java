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

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.agg.StaleDatumStreamProcessor;
import net.solarnetwork.central.datum.biz.DatumAppEventAcceptor;
import net.solarnetwork.central.datum.biz.DatumAppEventProducer;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.scheduler.ManagedJob;

/**
 * Datum aggregate jobs configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class DatumAggJobsConfig {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private List<DatumAppEventProducer> datumAppEventProducers;

	@Autowired
	private List<DatumAppEventAcceptor> datumAppEventAcceptors;

	@ConfigurationProperties(prefix = "app.job.datum.agg.hourly")
	@Bean
	public ManagedJob staleDatumProcessorHourly() {
		StaleDatumStreamProcessor processor = new StaleDatumStreamProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setDatumAppEventAcceptors(datumAppEventAcceptors);
		processor.setId("StaleDatumStreamProcessorHourly");
		processor.setAggregateProcessType(Aggregation.Hour.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.agg.daily")
	@Bean
	public ManagedJob staleDatumProcessorDaily() {
		StaleDatumStreamProcessor processor = new StaleDatumStreamProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setDatumAppEventAcceptors(datumAppEventAcceptors);
		processor.setId("StaleDatumStreamProcessorDaily");
		processor.setAggregateProcessType(Aggregation.Day.getKey());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.agg.monthly")
	@Bean
	public ManagedJob staleDatumProcessorMonthly() {
		StaleDatumStreamProcessor processor = new StaleDatumStreamProcessor(jdbcOperations);
		processor.setParallelTaskExecutor(taskExecutor);
		processor.setDatumAppEventAcceptors(datumAppEventAcceptors);
		processor.setId("StaleDatumStreamProcessorMonthly");
		processor.setAggregateProcessType(Aggregation.Hour.getKey());
		return processor;
	}

}
