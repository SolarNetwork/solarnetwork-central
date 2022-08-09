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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.mqtt.MqttDataCollector;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttStats;

/**
 * MQTT instruction publishing configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile("mqtt")
public class SolarQueueDataCollectorConfig {

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private DataCollectorBiz dataCollectorBiz;

	@Bean
	@Qualifier(SOLARQUEUE)
	@ConfigurationProperties(prefix = "app.solarqueue.data-collector")
	public MqttDataCollector mqttDataCollector(@Qualifier(SOLARQUEUE) MqttStats mqttStats) {
		ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper(new CBORFactory());
		MqttDataCollector collector = new MqttDataCollector(objectMapper, dataCollectorBiz,
				nodeInstructionDao, mqttStats);
		return collector;
	}

}
