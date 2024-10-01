/* ==================================================================
 * LocusEnergyConfig.java - 30/09/2024 12:03:52 pm
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

package net.solarnetwork.central.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudDatumStreamService;

/**
 * Configuration for the {@link LocusEnergyCloudDatumStreamService}.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class LocusEnergyConfig {

	@Bean
	public LocusEnergyCloudDatumStreamService locusEnergyCloudDatumStreamService() {
		var service = new LocusEnergyCloudDatumStreamService();

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(LocusEnergyCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public LocusEnergyCloudIntegrationService locusEnergyCloudIntegrationService(
			LocusEnergyCloudDatumStreamService datumStreamService) {
		var service = new LocusEnergyCloudIntegrationService(datumStreamService);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(LocusEnergyCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
