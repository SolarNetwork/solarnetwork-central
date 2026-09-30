/* ==================================================================
 * MqttInConfig.java - 11/11/2021 4:10:24 PM
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

import static net.solarnetwork.central.in.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.util.StatTracker;
import tools.jackson.dataformat.cbor.CBORMapper;

/**
 * MQTT instruction publishing configuration.
 * 
 * @author matt
 * @version 2.1
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt")
public class SolarQueueDataCollectorConfig {

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private DataCollectorBiz dataCollectorBiz;

	@Value("${app.solarqueue.data-collector.thread-count:20}")
	private int dataCollectorThreadCount = 20;

	@Value("${app.solarqueue.data-collector.shutdown-wait:30s}")
	private Duration dataCollectorShutdownWait = Duration.ofSeconds(30);

	@Autowired
	@Qualifier(JsonConfig.CBOR_MAPPER)
	private CBORMapper cborMapper;

	/**
	 * Executor for handling inbound MQTT datum messages.
	 *
	 * <p>
	 * Deliberately bounded, with no work queue, so that messages beyond its
	 * capacity are rejected rather than accumulating in memory: a rejected
	 * message is not acknowledged, and the broker redelivers it. The default
	 * size matches the usual broker in-flight limit, which is what caps the
	 * number of unacknowledged messages that can be outstanding at once.
	 * </p>
	 *
	 * @return the executor
	 * @since 2.1
	 */
	@Qualifier(SOLARQUEUE)
	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskExecutor mqttDataCollectorExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("SolarIn-MQTT-");
		executor.setCorePoolSize(dataCollectorThreadCount);
		executor.setMaxPoolSize(dataCollectorThreadCount);
		executor.setQueueCapacity(0); // hand off directly, or reject
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		// let in-flight messages finish, so their acknowledgements are sent
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds((int) dataCollectorShutdownWait.toSeconds());
		return executor;
	}

	@Bean
	@Qualifier(SOLARQUEUE)
	@ConfigurationProperties(prefix = "app.solarqueue.data-collector")
	public MqttDataCollector mqttDataCollector(@Qualifier(SOLARQUEUE) StatTracker mqttStats,
			@Qualifier(SOLARQUEUE) ThreadPoolTaskExecutor mqttDataCollectorExecutor) {
		MqttDataCollector collector = new MqttDataCollector(cborMapper, dataCollectorBiz,
				nodeInstructionDao, mqttStats);
		collector.setExecutor(mqttDataCollectorExecutor);
		return collector;
	}

}
