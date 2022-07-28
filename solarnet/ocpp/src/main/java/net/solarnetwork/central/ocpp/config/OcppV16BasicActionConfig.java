/* ==================================================================
 * OcppV16Config.java - 12/11/2021 4:13:54 PM
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.v16.cs.DataTransferProcessor;
import net.solarnetwork.ocpp.v16.cs.FirmwareStatusNotificationProcessor;
import net.solarnetwork.ocpp.v16.cs.HeartbeatProcessor;
import ocpp.v16.cs.DataTransferRequest;
import ocpp.v16.cs.DataTransferResponse;
import ocpp.v16.cs.FirmwareStatusNotificationRequest;
import ocpp.v16.cs.FirmwareStatusNotificationResponse;
import ocpp.v16.cs.HeartbeatRequest;
import ocpp.v16.cs.HeartbeatResponse;

/**
 * OCPP v1.6 general configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Profile(OCPP_V16)
public class OcppV16BasicActionConfig {

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<DataTransferRequest, DataTransferResponse> ocppDataTransferProcessor_v16() {
		return new DataTransferProcessor();
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<FirmwareStatusNotificationRequest, FirmwareStatusNotificationResponse> ocppFirmwareStatusNotification_v16() {
		return new FirmwareStatusNotificationProcessor();
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionMessageProcessor<HeartbeatRequest, HeartbeatResponse> ocppHeartbeat_v16() {
		return new HeartbeatProcessor();
	}

}
