/* ==================================================================
 * SolarFluxPublishingConfig.java - 10/11/2021 9:22:14 PM
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.agg.StaleSolarFluxProcessor;
import net.solarnetwork.central.datum.flux.SolarFluxDatumPublisher;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.scheduler.ManagedJob;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnectionFactory;

/**
 * Configuration for SolarFlux publishing.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("mqtt")
public class SolarFluxPublishingConfig {

	/** A qualifier for SolarFlux. */
	public static final String SOLARFLUX = "solarflux";

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private MqttConnectionFactory mqttConnectionFactory;

	@Autowired
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Bean
	@Qualifier(SOLARFLUX)
	public ObjectMapper solarFluxObjectMapper() {
		return JsonUtils.createObjectMapper(new CBORFactory(), JsonUtils.JAVA_TIMESTAMP_MODULE)
				.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
	}

	@ConfigurationProperties(prefix = "app.datum.solarflux-publish")
	@Bean(initMethod = "serviceDidStartup", destroyMethod = "serviceDidShutdown")
	@Qualifier(SOLARFLUX)
	public SolarFluxDatumPublisher solarFluxDatumPublisher() {
		SolarFluxDatumPublisher processor = new SolarFluxDatumPublisher(mqttConnectionFactory,
				nodeOwnershipDao, solarFluxObjectMapper());
		return processor;
	}

	@ConfigurationProperties(prefix = "app.job.datum.agg.flux")
	@Bean
	public ManagedJob staleSolarFluxProcessor() {
		StaleSolarFluxProcessor processor = new StaleSolarFluxProcessor(jdbcOperations, datumDao,
				solarFluxDatumPublisher());
		processor.setId("StaleSolarFluxProcessor");
		processor.setParallelTaskExecutor(taskExecutor);
		return processor;
	}

}