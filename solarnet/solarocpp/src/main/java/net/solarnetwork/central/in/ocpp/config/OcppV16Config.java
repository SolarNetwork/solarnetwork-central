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
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.solarnetwork.central.ocpp.config.OcppCentralServiceQualifier;
import net.solarnetwork.central.ocpp.config.OcppChargePointQualifier;
import net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import ocpp.json.ActionPayloadDecoder;
import ocpp.v16.cp.json.ChargePointActionPayloadDecoder;
import ocpp.v16.cs.json.CentralServiceActionPayloadDecoder;

/**
 * Configuration for OCPP v1.6.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableWebSocket
@Profile(OCPP_V16)
@ComponentScan(basePackageClasses = SolarNetOcppConfiguration.class)
public class OcppV16Config {

	@Bean
	@Qualifier(OCPP_V16)
	public ObjectMapper ocppObjectMapper_v16() {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(Arrays.asList(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
		factory.setModules(Arrays.asList(new JaxbAnnotationModule()));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	@OcppCentralServiceQualifier(OCPP_V16)
	public ActionPayloadDecoder centralServiceActionPayloadDecoder_v16() {
		return new CentralServiceActionPayloadDecoder(ocppObjectMapper_v16());
	}

	@Bean
	@OcppChargePointQualifier(OCPP_V16)
	public ActionPayloadDecoder chargePointActionPayloadDecoder_v16() {
		return new ChargePointActionPayloadDecoder(ocppObjectMapper_v16());
	}

}
