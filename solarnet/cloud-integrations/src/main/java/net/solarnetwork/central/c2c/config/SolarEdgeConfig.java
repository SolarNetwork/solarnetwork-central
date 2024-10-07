/* ==================================================================
 * SolarEdgeConfig.java - 7/10/2024 7:12:26 am
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
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;

/**
 * Configuration for the SolarEdge cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class SolarEdgeConfig {

	/** A qualifier for SolarEdge configuraiton. */
	public static final String SOLAREDGE = "solaredge";

	@Autowired
	private UserEventAppenderBiz userEventAppender;

	@Autowired
	private CloudIntegrationConfigurationDao integrationConfigurationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamConfigurationDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyConfigurationDao;

	@Autowired
	private RestOperations restOps;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Bean
	@Qualifier(SOLAREDGE)
	public CloudDatumStreamService solarEdgeCloudDatumStreamService() {
		var service = new SolarEdgeCloudDatumStreamService(userEventAppender, expressionService,
				integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamPropertyConfigurationDao, restOps);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public CloudIntegrationService solarEdgeCloudIntegrationService(
			@Qualifier(SOLAREDGE) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new SolarEdgeCloudIntegrationService(datumStreamServices, userEventAppender,
				restOps);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
