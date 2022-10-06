/* ==================================================================
 * OscpJsonConfig.java - 6/10/2022 6:14:47 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.config;

import static net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration.OSCP_V20;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.networknt.schema.JsonSchemaFactory;
import net.solarnetwork.central.oscp.util.OscpUtils;

/**
 * JSON configuration for OSCP.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OscpJsonConfig {

	/**
	 * JSON schema validator for OSCP 2.0 messages.
	 * 
	 * @return the validator
	 */
	@Qualifier(OSCP_V20)
	@Bean
	public JsonSchemaFactory oscpJsonSchemaValidationFactory() {
		return OscpUtils.oscpSchemaFactory_v20();
	}

}
