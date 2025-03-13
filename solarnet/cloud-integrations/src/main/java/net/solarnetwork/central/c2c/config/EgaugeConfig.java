/* ==================================================================
 * EgaugeConfig.java - 25/10/2024 2:37:56 pm
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
import java.util.random.RandomGenerator;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.EgaugeCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.EgaugeCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.http.CachableRequestEntity;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Configuration for the eGauge cloud integration services.
 *
 * @author matt
 * @version 1.3
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class EgaugeConfig implements SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for eGauge configuration. */
	public static final String EGAUGE = "egauge";

	/** A qualifier for eGauge device register configuration. */
	public static final String EGAUGE_DEVICE_REGISTERS = "egague-device-registers";

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
	@Qualifier(CLOUD_INTEGRATIONS)
	private BytesEncryptor bytesEncryptor;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private RandomGenerator rng;

	@Autowired
	private CacheManager cacheManager;

	@Autowired(required = false)
	private UserServiceAuditor userServiceAuditor;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired(required = false)
	private QueryAuditor queryAuditor;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_DATUM_STREAM_METADATA)
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_HTTP)
	private Cache<CachableRequestEntity, Result<?>> httpCache;

	@Bean
	@Qualifier(EGAUGE_DEVICE_REGISTERS)
	@ConfigurationProperties(prefix = "app.c2c.cache.egague-device-registers")
	public CacheSettings eGaugeDeviceRegistersCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(EGAUGE_DEVICE_REGISTERS)
	public Cache<String, CloudDataValue[]> eGaugeDeviceRegistersCache(
			@Qualifier(EGAUGE_DEVICE_REGISTERS) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, CloudDataValue[].class,
				EGAUGE_DEVICE_REGISTERS + "-cache");
	}

	@Bean
	@Qualifier(EGAUGE)
	public CloudDatumStreamService egaugeCloudDatumStreamService(
			@Qualifier(EGAUGE_DEVICE_REGISTERS) Cache<String, CloudDataValue[]> deviceRegistersCache) {
		var accessTokenDao = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations,
				new ClientRegistrationRepository() {

					@Override
					public ClientRegistration findByRegistrationId(String registrationId) {
						// we're not using this API here
						throw new UnsupportedOperationException();
					}
				});

		var service = new EgaugeCloudDatumStreamService(userEventAppender, encryptor, expressionService,
				integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC(), rng, accessTokenDao);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(EgaugeCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);
		service.setDatumStreamMetadataCache(datumStreamMetadataCache);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);
		service.setDeviceRegistersCache(deviceRegistersCache);
		service.setHttpCache(httpCache);

		return service;
	}

	@Bean
	@Qualifier(EGAUGE)
	public CloudIntegrationService egaugeCloudIntegrationService(
			@Qualifier(EGAUGE) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new EgaugeCloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(EgaugeCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

}
