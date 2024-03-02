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

package net.solarnetwork.central.in.ocpp.config;

import static net.solarnetwork.central.in.ocpp.config.SolarQueueMqttConnectionConfig.SOLARQUEUE;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_INSTRUCTION;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.config.VersionedQualifier;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.mqtt.MqttInstructionHandler;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.v16.jakarta.ChargePointAction;

/**
 * OCPP v1.6 MQTT configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OcppV16MqttConfig.MQTT_OCPP_V16)
public class OcppV16MqttConfig {

	/** Profile expression for MQTT with OCPP v16. */
	public static final String MQTT_OCPP_V16 = "mqtt & " + SolarNetOcppConfiguration.OCPP_V16;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private ChargePointRouter ocppChargePointRouter;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	@Qualifier(OCPP_V16)
	private ObjectMapper objectMapper;

	@ConfigurationProperties(prefix = "app.ocpp.v16.mqtt.instr-handler")
	@Bean
	@Qualifier(SOLARQUEUE)
	@VersionedQualifier(value = SOLARQUEUE, version = OCPP_V16)
	public MqttInstructionHandler<ChargePointAction> instructionHandler_v16() {
		MqttInstructionHandler<ChargePointAction> handler = new MqttInstructionHandler<>(
				ChargePointAction.class, nodeInstructionDao, ocppCentralChargePointDao, objectMapper,
				ocppChargePointRouter);
		handler.setDisplayName("OCPP 1.6 Instructions");
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		handler.setMqttTopic("instr/OCPP_v16");
		return handler;
	}

	@Bean
	@VersionedQualifier(value = OCPP_INSTRUCTION, version = OCPP_V16)
	public ActionMessageProcessor<JsonNode, Void> instructionHandlerMessageProcessor_v16() {
		return instructionHandler_v16();
	}

}
