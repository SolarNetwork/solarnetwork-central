/* ==================================================================
 * OcppV201BasicActionConfig.java - 17/02/2024 6:10:31 pm
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.v201.service.DataTransferProcessor;
import net.solarnetwork.ocpp.v201.service.HeartbeatProcessor;
import ocpp.v201.DataTransferRequest;
import ocpp.v201.DataTransferResponse;
import ocpp.v201.HeartbeatRequest;
import ocpp.v201.HeartbeatResponse;

/**
 * OCPP v2.0.1 general configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V201)
public class OcppV201BasicActionConfig {

	@Bean
	@Qualifier(OCPP_V201)
	@Order(Ordered.LOWEST_PRECEDENCE)
	public ActionMessageProcessor<DataTransferRequest, DataTransferResponse> ocppDataTransferProcessor_v201() {
		return new DataTransferProcessor();
	}

	@Bean
	@Qualifier(OCPP_V201)
	public ActionMessageProcessor<HeartbeatRequest, HeartbeatResponse> ocppHeartbeatProcessor_v201() {
		return new HeartbeatProcessor(Clock.system(ZoneOffset.UTC));
	}

}
