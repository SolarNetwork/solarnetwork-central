/* ==================================================================
 * OcppV201ServiceConfig.java - 18/02/2024 2:00:27 pm
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

package net.solarnetwork.central.ocpp.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_INSTRUCTION;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.common.config.VersionedQualifier;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.service.ConnectorStatusDatumPublisher;
import net.solarnetwork.central.ocpp.v201.service.OcppController;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.AuthorizationService;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.service.cs.ChargePointManager;
import net.solarnetwork.ocpp.v201.service.AuthorizeProcessor;
import net.solarnetwork.ocpp.v201.service.BootNotificationProcessor;
import net.solarnetwork.ocpp.v201.service.StatusNotificationProcessor;
import ocpp.v201.AuthorizeRequest;
import ocpp.v201.AuthorizeResponse;
import ocpp.v201.BootNotificationRequest;
import ocpp.v201.BootNotificationResponse;
import ocpp.v201.StatusNotificationRequest;
import ocpp.v201.StatusNotificationResponse;

/**
 * OCPP v2.0.1 controller configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V201)
public class OcppV201ServiceConfig {

	@Autowired
	private Executor executor;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private UserEventAppenderBiz userEventAppenderBiz;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private CentralChargePointConnectorDao ocppCentralChargePointConnectorDao;

	@Autowired
	private ChargePointRouter ocppChargePointRouter;

	@Autowired
	private ConnectorStatusDatumPublisher ocppConnectorStatusDatumPublisher;

	@Autowired
	@Qualifier(OCPP_V201)
	private ObjectMapper objectMapper;

	@Autowired
	@Qualifier(OCPP_V201)
	private ActionPayloadDecoder ocppChargePointActionPayloadDecoder;

	@Autowired(required = false)
	@Qualifier("solarflux")
	private DatumProcessor fluxPublisher;

	@Autowired(required = false)
	@VersionedQualifier(value = OCPP_INSTRUCTION, version = OCPP_V201)
	private ActionMessageProcessor<JsonNode, Void> ocppInstructionHandler;

	@Bean
	@Qualifier(OCPP_V201)
	public OcppController ocppController_v201() {
		OcppController controller = new OcppController(executor, ocppChargePointRouter, userNodeDao,
				nodeInstructionDao, ocppCentralChargePointDao, ocppCentralChargePointConnectorDao,
				objectMapper);
		controller.setTransactionTemplate(transactionTemplate);
		controller.setChargePointActionPayloadDecoder(ocppChargePointActionPayloadDecoder);
		controller.setDatumPublisher(ocppConnectorStatusDatumPublisher);
		controller.setUserEventAppenderBiz(userEventAppenderBiz);
		controller.setInstructionHandler(ocppInstructionHandler);
		return controller;
	}

	@Bean
	@Qualifier(OCPP_V201)
	public ActionMessageProcessor<AuthorizeRequest, AuthorizeResponse> ocppAuthorizeProcessor_v201(
			AuthorizationService authorizationService) {
		return new AuthorizeProcessor(authorizationService);
	}

	@ConfigurationProperties(prefix = "app.ocpp.v201.cs.boot-notification")
	@Bean
	@Qualifier(OCPP_V201)
	public ActionMessageProcessor<BootNotificationRequest, BootNotificationResponse> ocppBootNotificationProcessor_v201(
			@Qualifier(OCPP_V201) ChargePointManager chargePointManager) {
		return new BootNotificationProcessor(Clock.system(ZoneOffset.UTC), chargePointManager);
	}

	@Bean
	@Qualifier(OCPP_V201)
	public ActionMessageProcessor<StatusNotificationRequest, StatusNotificationResponse> ocppStatusNotification_v201(
			@Qualifier(OCPP_V201) ChargePointManager chargePointManager) {
		return new StatusNotificationProcessor(chargePointManager);
	}

}
