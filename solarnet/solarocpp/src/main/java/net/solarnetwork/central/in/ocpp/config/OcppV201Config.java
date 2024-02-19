/* ==================================================================
 * OcppV201Config.java - 18/02/2024 7:08:24 am
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.ocpp.json.ActionPayloadDecoder;
import net.solarnetwork.ocpp.v201.util.OcppUtils;

/**
 * Configuration for OCPP v2.0.1.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OCPP_V201)
public class OcppV201Config {

	@Bean
	@Qualifier(OCPP_V201)
	public ObjectMapper ocppObjectMapper_v201() {
		return OcppUtils.newObjectMapper();
	}

	@Bean
	@Qualifier(OCPP_V201)
	public ActionPayloadDecoder actionPayloadDecoder_v201(
			@Qualifier(OCPP_V201) ObjectMapper objectMapper) {
		return new net.solarnetwork.ocpp.v201.util.ActionPayloadDecoder(objectMapper,
				OcppUtils.ocppSchemaFactory_v201());
	}

}
