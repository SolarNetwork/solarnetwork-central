/* ==================================================================
 * OcppV16ControllerConfig.java - 12/11/2021 1:55:49 PM
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

package net.solarnetwork.central.ocpp.config;

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.dao.ChargePointSettingsDao;
import net.solarnetwork.central.ocpp.v16.controller.ConnectorStatusDatumPublisher;
import net.solarnetwork.central.ocpp.v16.controller.OcppController;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.ocpp.dao.ChargeSessionDao;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.v16.cs.AuthorizeProcessor;
import net.solarnetwork.ocpp.v16.cs.BootNotificationProcessor;
import net.solarnetwork.ocpp.v16.cs.StatusNotificationProcessor;
import ocpp.json.ActionPayloadDecoder;
import ocpp.v16.cs.AuthorizeRequest;
import ocpp.v16.cs.AuthorizeResponse;
import ocpp.v16.cs.BootNotificationRequest;
import ocpp.v16.cs.BootNotificationResponse;
import ocpp.v16.cs.StatusNotificationRequest;
import ocpp.v16.cs.StatusNotificationResponse;

/**
 * OCPP v1.6 controller configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OCPP_V16)
public class OcppV16ControllerConfig {

	@Autowired
	private Executor executor;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private UserNodeDao userNodeDao;

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired
	private CentralAuthorizationDao ocppCentralAuthorizationDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Autowired
	private CentralChargePointConnectorDao ocppCentralChargePointConnectorDao;

	@Autowired
	private ChargePointSettingsDao ocppChargePointSettingsDao;

	@Autowired
	private ChargeSessionDao ocppChargeSessionDao;

	@Autowired
	private ChargePointRouter ocppChargePointRouter;

	@Autowired
	@OcppChargePointQualifier(OCPP_V16)
	private ActionPayloadDecoder ocppChargePointActionPayloadDecoder;

	@Autowired(required = false)
	@Qualifier("solarflux")
	private DatumProcessor fluxPublisher;

	@Bean
	@Qualifier(OCPP_V16)
	public OcppController ocppController_v16() {
		OcppController controller = new OcppController(executor, ocppChargePointRouter, userNodeDao,
				nodeInstructionDao, ocppCentralAuthorizationDao, ocppCentralChargePointDao,
				ocppCentralChargePointConnectorDao);
		controller.setTransactionTemplate(transactionTemplate);
		controller.setChargePointActionPayloadDecoder(ocppChargePointActionPayloadDecoder);

		ConnectorStatusDatumPublisher publisher = new ConnectorStatusDatumPublisher(
				ocppChargePointSettingsDao, ocppCentralChargePointConnectorDao, ocppChargeSessionDao,
				datumDao);
		publisher.setFluxPublisher(fluxPublisher);
		controller.setDatumPublisher(publisher);

		return controller;
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<AuthorizeRequest, AuthorizeResponse> ocppAuthorizeProcessor_v16() {
		return new AuthorizeProcessor(ocppController_v16());
	}

	@ConfigurationProperties(prefix = "app.ocpp.v16.cs.boot-notification")
	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<BootNotificationRequest, BootNotificationResponse> ocppBootNotificationProcessor_v16() {
		return new BootNotificationProcessor(ocppController_v16());
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<StatusNotificationRequest, StatusNotificationResponse> ocppStatusNotification_v16() {
		return new StatusNotificationProcessor(ocppController_v16());
	}

}
