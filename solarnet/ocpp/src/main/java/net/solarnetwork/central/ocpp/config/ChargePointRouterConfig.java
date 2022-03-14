/* ==================================================================
 * ChargePointRouterConfig.java - 12/11/2021 2:49:22 PM
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

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointBrokerTracker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.service.StaticOptionalServiceCollection;

/**
 * OCPP ChargePoint router configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class ChargePointRouterConfig {

	@Autowired
	@Lazy
	private List<ChargePointBroker> ocppChargePointBrokers;

	@Bean
	public ChargePointRouter ocppChargePointRouter() {
		return new ChargePointBrokerTracker(
				new StaticOptionalServiceCollection<>(ocppChargePointBrokers));
	}

}
