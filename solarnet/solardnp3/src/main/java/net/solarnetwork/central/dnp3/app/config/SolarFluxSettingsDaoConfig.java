/* ==================================================================
 * SolarFluxSettingsDaoConfig.java - 26/06/2024 3:48:19 pm
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

package net.solarnetwork.central.dnp3.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.datum.flux.dao.FluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.dao.StaticFluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettingsInfo;

/**
 * SolarFlux settings DAO configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class SolarFluxSettingsDaoConfig {

	@Bean
	public FluxPublishSettingsDao staticFluxPublishSettingsDao() {
		return new StaticFluxPublishSettingsDao(FluxPublishSettingsInfo.PUBLISH_NOT_RETAINED);
	}
}
