/* ==================================================================
 * UserCloudIntegrationsBizConfig.java - 30/09/2024 11:22:23 am
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

package net.solarnetwork.central.user.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.user.c2c.biz.impl.DaoUserCloudIntegrationsBiz;

/**
 * Configuration for user cloud integrations services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsBizConfig {

	@Autowired
	private Collection<CloudIntegrationService> integrationServices;

	@Bean
	public DaoUserCloudIntegrationsBiz userCloudIntegrationsBiz() {
		DaoUserCloudIntegrationsBiz biz = new DaoUserCloudIntegrationsBiz(integrationServices);
		return biz;
	}

}
