/* ==================================================================
 * OcppServiceConfig.java - 19/02/2024 7:53:47 am
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

import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V201;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.service.OcppAuthorizationService;
import net.solarnetwork.ocpp.service.AuthorizationService;

/**
 * General OCPP service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile({ OCPP_V16, OCPP_V201 })
public class OcppServiceConfig {

	@Autowired
	private CentralAuthorizationDao ocppCentralAuthorizationDao;

	@Autowired
	private CentralChargePointDao ocppCentralChargePointDao;

	@Bean
	public AuthorizationService ocppAuthorizationService() {
		return new OcppAuthorizationService(ocppCentralAuthorizationDao, ocppCentralChargePointDao);
	}

}
