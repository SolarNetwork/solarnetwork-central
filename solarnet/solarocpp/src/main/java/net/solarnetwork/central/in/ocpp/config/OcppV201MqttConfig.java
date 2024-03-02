/* ==================================================================
 * OcppV201MqttConfig.java - 18/02/2024 7:18:11 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
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
import net.solarnetwork.ocpp.v201.domain.Action;

/**
 * OCPP v2.0.1 MQTT configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OcppV201MqttConfig.MQTT_OCPP_V201)
public class OcppV201MqttConfig {

	/** Profile expression for MQTT with OCPP v201. */
	public static final String MQTT_OCPP_V201 = "mqtt & " + SolarNetOcppConfiguration.OCPP_V201;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private ChargePointRouter ocppChargePointRouter;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	@Qualifier(OCPP_V201)
	private ObjectMapper objectMapper;

	@ConfigurationProperties(prefix = "app.ocpp.v201.mqtt.instr-handler")
	@Bean
	@Qualifier(SOLARQUEUE)
	@VersionedQualifier(value = SOLARQUEUE, version = OCPP_V201)
	public MqttInstructionHandler<Action> instructionHandler_v201() {
		MqttInstructionHandler<Action> handler = new MqttInstructionHandler<>(Action.class,
				nodeInstructionDao, ocppCentralChargePointDao, objectMapper, ocppChargePointRouter);
		handler.setDisplayName("OCPP 2.0.1 Instructions");
		handler.setUserEventAppenderBiz(userEventAppenderBiz);
		handler.setMqttTopic("instr/OCPP_v201");
		return handler;
	}

	@Bean
	@VersionedQualifier(value = OCPP_INSTRUCTION, version = OCPP_V201)
	public ActionMessageProcessor<JsonNode, Void> instructionHandlerMessageProcessor_v201() {
		return instructionHandler_v201();
	}

}
