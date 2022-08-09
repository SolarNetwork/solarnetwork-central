/* ==================================================================
 * OcppMqttConfig.java - 12/11/2021 1:45:56 PM
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_INSTRUCTION;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import static net.solarnetwork.central.reg.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.mqtt.MqttInstructionHandler;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import ocpp.v16.ChargePointAction;

/**
 * OCPP v1.6 MQTT configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OcppV16MqttConfig.MQTT_OCPP_V16)
public class OcppV16MqttConfig {

	public static final String MQTT_OCPP_V16 = "mqtt & " + SolarNetOcppConfiguration.OCPP_V16;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private ChargePointRouter ocppChargePointRouter;

	@Autowired
	@Qualifier(OCPP_V16)
	private ObjectMapper objectMapper;

	@ConfigurationProperties(prefix = "app.ocpp.v16.mqtt.instr-handler")
	@Bean
	@Qualifier(SOLARQUEUE)
	public MqttInstructionHandler<ChargePointAction> instructionHandler_v16() {
		return new MqttInstructionHandler<>(ChargePointAction.class, nodeInstructionDao,
				ocppCentralChargePointDao, objectMapper, ocppChargePointRouter);
	}

	@Bean
	@Qualifier(OCPP_INSTRUCTION)
	public ActionMessageProcessor<JsonNode, Void> instructionHandlerMessageProcessor_v16() {
		return instructionHandler_v16();
	}

}
