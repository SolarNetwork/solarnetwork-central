/* ==================================================================
 * OscpV20MqttConfig.java - 9/10/2022 11:20:46 am
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration.OSCP_V20;
import static net.solarnetwork.central.reg.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.networknt.schema.JsonSchemaFactory;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.mqtt.OscpMqttCountStat;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructionQueueHook;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttStats;

/**
 * Configuration for OSCP v2.0 MQTT.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OscpV20MqttConfig.MQTT_OSCP_V20)
public class OscpV20MqttConfig {

	/** Profile expression for MQTT with OSCP v20. */
	public static final String MQTT_OSCP_V20 = "mqtt & " + SolarNetOscpConfiguration.OSCP_V20;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * The MQTT statistics to use.
	 * 
	 * @return
	 */
	@Qualifier(OSCP_V20)
	@Bean
	public MqttStats oscpMqttStats() {
		return new MqttStats("SolarOSCP-MQTT", 200, OscpMqttCountStat.values());
	}

	/**
	 * A node instruction queue hook to process OSCP instructions (from a
	 * Capacity Optimizer) and publish them to a MQTT topic, for the Flexibility
	 * Provider to subscribe to and process.
	 * 
	 * @param jsonSchemaFactory
	 *        the JSON schema validator
	 * @return the hook
	 */
	@Order(10)
	@Qualifier(SOLARQUEUE)
	@Bean
	public OscpMqttInstructionQueueHook oscpMqttInstructionQueueHook_v20(
			@Qualifier(OSCP_V20) MqttStats stats,
			@Qualifier(OSCP_V20) JsonSchemaFactory jsonSchemaFactory) {
		ObjectMapper objectMapper = JsonUtils.newObjectMapper(new CBORFactory());
		OscpMqttInstructionQueueHook hook = new OscpMqttInstructionQueueHook(stats, objectMapper,
				userNodeDao, capacityGroupDao, capacityOptimizerDao, capacityProviderDao);
		hook.setJsonSchemaFactory(jsonSchemaFactory);
		hook.setUserEventAppenderBiz(userEventAppenderBiz);
		return hook;
	}

}
