/* ==================================================================
 * OpenWeatherMapConfig.java - 31/10/2024 3:15:09 pm
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
import java.time.Clock;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.BaseOpenWeatherMapCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapDayCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapForecastCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.OpenWeatherMapWeatherCloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;

/**
 * Configuration for the OpenWeatherMap cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class OpenWeatherMapConfig {

	/** A qualifier for OpenWeatherMap configuration. */
	public static final String OPEN_WEATHER_MAP = "owm";

	@Autowired
	private UserEventAppenderBiz userEventAppender;

	@Autowired
	private CloudIntegrationConfigurationDao integrationConfigurationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamConfigurationDao;

	@Autowired
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingConfigurationDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyConfigurationDao;

	@Autowired
	private RestOperations restOps;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private TextEncryptor encryptor;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Autowired(required = false)
	private UserServiceAuditor userServiceAuditor;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired(required = false)
	private QueryAuditor queryAuditor;

	@Bean
	@Qualifier(OPEN_WEATHER_MAP)
	public CloudDatumStreamService openWeatherMapWeatherCloudDatumStreamService() {
		var service = new OpenWeatherMapWeatherCloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(OpenWeatherMapWeatherCloudDatumStreamService.class.getName(),
				BaseOpenWeatherMapCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);

		return service;
	}

	@Bean
	@Qualifier(OPEN_WEATHER_MAP)
	public CloudDatumStreamService openWeatherMapForecastCloudDatumStreamService() {
		var service = new OpenWeatherMapForecastCloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(OpenWeatherMapForecastCloudDatumStreamService.class.getName(),
				BaseOpenWeatherMapCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

	@Bean
	@Qualifier(OPEN_WEATHER_MAP)
	public CloudDatumStreamService openWeatherMapDayCloudDatumStreamService() {
		var service = new OpenWeatherMapDayCloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(OpenWeatherMapDayCloudDatumStreamService.class.getName(),
				BaseOpenWeatherMapCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

	@Bean
	@Qualifier(OPEN_WEATHER_MAP)
	public CloudIntegrationService openWeatherMapCloudIntegrationService(
			@Qualifier(OPEN_WEATHER_MAP) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new OpenWeatherMapCloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(OpenWeatherMapCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

}
