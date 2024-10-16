/* ==================================================================
 * CloudDatumStreamImportConfig.java - 15/10/2024 9:52:53 am
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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.CloudDatumStreamDatumImportInputFormatService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;

/**
 * Configuration for cloud datum stream import.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class CloudDatumStreamImportConfig {

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Bean
	public DatumImportInputFormatService cloudDatumStreamDatumImportInputFormatService(
			Collection<CloudDatumStreamService> datumStreamServices) {
		var dsMap = datumStreamServices.stream()
				.collect(toMap(CloudDatumStreamService::getId, identity()));
		var service = new CloudDatumStreamDatumImportInputFormatService(datumStreamDao, dsMap::get);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(CloudDatumStreamDatumImportInputFormatService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
