/* ==================================================================
 * OscpV20MqttConfig.java - 7/10/2022 2:59:40 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.fp.config;

import static net.solarnetwork.central.oscp.fp.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.http.ExternalSystemClient;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructionHandler;

/**
 * Configuration for OSCP instruction handling.
 * 
 * @author matt
 * @version 1.0
 */
@Profile(OscpV20MqttConfig.MQTT_OSCP_V20)
@Configuration(proxyBeanMethods = false)
public class OscpV20MqttConfig {

	/** Profile expression for MQTT with OSCP v20. */
	public static final String MQTT_OSCP_V20 = "mqtt & " + SolarNetOscpConfiguration.OSCP_V20;

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private ExternalSystemClient client;

	/**
	 * A node instruction queue hook to process OSCP instructions (from a
	 * Capacity Optimizer) and forward them to a Capacity Provider
	 * 
	 * @return the hook
	 */
	@ConfigurationProperties(prefix = "app.oscp.v20.mqtt.instr-handler")
	@Qualifier(SOLARQUEUE)
	@Bean
	public OscpMqttInstructionHandler oscpMqttInstructionHandler_v20() {
		OscpMqttInstructionHandler handler = new OscpMqttInstructionHandler(taskExecutor, objectMapper,
				nodeInstructionDao, capacityGroupDao, capacityOptimizerDao, capacityProviderDao, client);
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		return handler;
	}
}
