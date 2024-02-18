/* ==================================================================
 * OcppV16Config.java - 12/11/2021 11:23:20 AM
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.ocpp.config.OcppCentralServiceQualifier;
import net.solarnetwork.central.ocpp.config.OcppChargePointQualifier;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.v16.jakarta.cp.json.ChargePointActionPayloadDecoder;
import net.solarnetwork.ocpp.v16.jakarta.cs.json.CentralServiceActionPayloadDecoder;
import net.solarnetwork.ocpp.v16.jakarta.json.BaseActionPayloadDecoder;

/**
 * Configuration for OCPP v1.6.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V16)
public class OcppV16Config {

	@Bean
	@Qualifier(OCPP_V16)
	public ObjectMapper ocppObjectMapper_v16() {
		return BaseActionPayloadDecoder.defaultObjectMapper();
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionPayloadDecoder centralServiceActionPayloadDecoder_v16(
			@Qualifier(OCPP_V16) ObjectMapper objectMapper) {
		return new CentralServiceActionPayloadDecoder(objectMapper);
	}

	@Bean
	@OcppChargePointQualifier(OCPP_V16)
	public ActionPayloadDecoder chargePointActionPayloadDecoder_v16(
			@Qualifier(OCPP_V16) ObjectMapper objectMapper) {
		return new ChargePointActionPayloadDecoder(objectMapper);
	}

}
