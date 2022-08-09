/* ==================================================================
 * SolarFluxPublishingConfig.java - 10/11/2021 9:22:14 PM
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.reg.config.SolarFluxMqttConnectionConfig.SOLARFLUX;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.support.UserEventSerializer;
import net.solarnetwork.codec.JsonUtils;

/**
 * Configuration for SolarFlux publishing.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
@Profile("mqtt")
public class SolarFluxPublishingConfig {

	/**
	 * A module for handling SolarFlux objects.
	 * 
	 * @since 1.1
	 */
	public static final com.fasterxml.jackson.databind.Module SOLARFLUX_MODULE;
	static {
		SimpleModule m = new SimpleModule("SolarFlux");
		m.addSerializer(UserEvent.class, UserEventSerializer.INSTANCE);
		SOLARFLUX_MODULE = m;
	}

	@Bean
	@Qualifier(SOLARFLUX)
	public ObjectMapper solarFluxObjectMapper() {
		ObjectMapper mapper = JsonUtils.newDatumObjectMapper(new CBORFactory());
		mapper.registerModule(SOLARFLUX_MODULE);
		return mapper;
	}

}
