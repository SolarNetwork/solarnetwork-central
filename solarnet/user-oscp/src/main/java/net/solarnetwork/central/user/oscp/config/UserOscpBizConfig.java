/* ==================================================================
 * UserOscpBizConfig.java - 15/08/2022 2:39:55 pm
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

package net.solarnetwork.central.user.oscp.config;

import static net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration.OSCP_V20;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.user.oscp.biz.dao.DaoUserOscpBiz;

/**
 * Configuration for User OSCP services.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(OSCP_V20)
public class UserOscpBizConfig {

	@Autowired
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Autowired
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Autowired
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Autowired
	private AssetConfigurationDao assetDao;

	@Bean
	public DaoUserOscpBiz userOscpBiz() {
		return new DaoUserOscpBiz(capacityProviderDao, capacityOptimizerDao, capacityGroupDao, assetDao);
	}

}
