/* ==================================================================
 * OcppV201ChargeSessionConfig.java - 17/02/2024 6:16:02 pm
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
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201_CHARGE_SESSION;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.cs.ChargeSessionManager;
import net.solarnetwork.ocpp.v201.service.MeterValuesProcessor;
import net.solarnetwork.ocpp.v201.service.TransactionEventProcessor;
import ocpp.v201.MeterValuesRequest;
import ocpp.v201.MeterValuesResponse;
import ocpp.v201.TransactionEventRequest;
import ocpp.v201.TransactionEventResponse;

/**
 * OCPP v2.0.1 charge session configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V201_CHARGE_SESSION)
public class OcppV201ChargeSessionConfig {

	@Bean
	@OcppCentralServiceQualifier(OCPP_V201)
	public ActionMessageProcessor<MeterValuesRequest, MeterValuesResponse> ocppMeterValuesProcessor_v201(
			ChargeSessionManager ocppChargeSessionManager) {
		return new MeterValuesProcessor(ocppChargeSessionManager);
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V201)
	public ActionMessageProcessor<TransactionEventRequest, TransactionEventResponse> ocppTransactionEventProcesor_v201(
			ChargeSessionManager ocppChargeSessionManager) {
		return new TransactionEventProcessor(ocppChargeSessionManager);
	}

}
