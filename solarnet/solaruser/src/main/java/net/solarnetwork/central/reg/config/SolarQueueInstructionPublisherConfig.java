/* ==================================================================
 * MqttInstructionPublisherConfig.java - 11/11/2021 4:10:24 PM
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

package net.solarnetwork.central.reg.config;

import java.util.concurrent.Executor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.dao.mqtt.MqttNodeInstructionQueueHook;
import net.solarnetwork.codec.CborUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.StatTracker;

/**
 * MQTT instruction publishing configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt")
public class SolarQueueInstructionPublisherConfig {

	@Autowired
	private Executor executor;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Bean
	@ConfigurationProperties(prefix = "app.solarqueue.instr-publish")
	@Qualifier(SolarQueueMqttConnectionConfig.SOLARQUEUE)
	public MqttNodeInstructionQueueHook mqttNodeInstructionQueueHook() {
		ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper(CborUtils.cborFactory());
		return new MqttNodeInstructionQueueHook(objectMapper, executor, nodeInstructionDao,
				new StatTracker("Instruction Publisher", null,
						LoggerFactory.getLogger("net.solarnetwork.central.mqtt.stats.NodeInstructions"),
						500));
	}

}
