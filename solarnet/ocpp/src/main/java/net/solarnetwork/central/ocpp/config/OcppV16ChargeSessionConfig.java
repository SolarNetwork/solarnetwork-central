/* ==================================================================
 * OcppV16ChargeSessionConfig.java - 12/11/2021 4:10:01 PM
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
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16_CHARGE_SESSION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.cs.ChargeSessionManager;
import net.solarnetwork.ocpp.v16.jakarta.cs.MeterValuesProcessor;
import net.solarnetwork.ocpp.v16.jakarta.cs.StartTransactionProcessor;
import net.solarnetwork.ocpp.v16.jakarta.cs.StopTransactionProcessor;
import ocpp.v16.jakarta.cs.MeterValuesRequest;
import ocpp.v16.jakarta.cs.MeterValuesResponse;
import ocpp.v16.jakarta.cs.StartTransactionRequest;
import ocpp.v16.jakarta.cs.StartTransactionResponse;
import ocpp.v16.jakarta.cs.StopTransactionRequest;
import ocpp.v16.jakarta.cs.StopTransactionResponse;

/**
 * OCPP v1.6 charge session configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V16_CHARGE_SESSION)
public class OcppV16ChargeSessionConfig {

	@Autowired
	private ChargeSessionManager ocppChargeSessionManager;

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<MeterValuesRequest, MeterValuesResponse> ocppMeterValuesProcessor_v16() {
		return new MeterValuesProcessor(ocppChargeSessionManager);
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<StartTransactionRequest, StartTransactionResponse> ocppStartTransactionProcessor_v16() {
		return new StartTransactionProcessor(ocppChargeSessionManager);
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<StopTransactionRequest, StopTransactionResponse> ocppStopTransactionProcessor_v16() {
		return new StopTransactionProcessor(ocppChargeSessionManager);
	}

}
