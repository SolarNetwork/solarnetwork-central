/* ==================================================================
 * SigenergyConfig.java - 6/12/2025 2:45:09 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import java.util.List;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.sigen.SigenergyRestOperationsHelper;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.service.StaticOptionalService;
import tools.jackson.databind.ObjectMapper;

/**
 * Configuration for the Sigenergy cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class SigenergyConfig implements SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for Sigenergy configuration. */
	public static final String SIGENERGY = "sigen";

	@Autowired
	private UserEventAppenderBiz userEventAppender;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private RestTemplate restOps;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private TextEncryptor encryptor;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private BytesEncryptor bytesEncryptor;

	@Autowired(required = false)
	private UserServiceAuditor userServiceAuditor;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_INTEGRATION_LOCKS)
	private Cache<UserLongCompositePK, Lock> integrationLocksCache;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_HTTP)
	private Cache<CachableRequestEntity, Result<?>> httpCache;

	@Value("${app.c2c.allow-http-local-hosts:false}")
	private boolean allowHttpLocalHosts;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Autowired
	private CloudIntegrationConfigurationDao integrationConfigurationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamConfigurationDao;

	@Autowired
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingConfigurationDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyConfigurationDao;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired(required = false)
	private QueryAuditor queryAuditor;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_DATUM_STREAM_METADATA)
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache;

	@Autowired
	private CacheManager cacheManager;

	/** A qualifier for Sigenergy system device configuration. */
	public static final String SIGENERGY_SYSTEM_DEVICES = "sigen-sysdev";

	@Bean
	@Qualifier(SIGENERGY_SYSTEM_DEVICES)
	@ConfigurationProperties(prefix = "app.c2c.cache.sigenergy-system-devices")
	public CacheSettings sigenergySystemDevicesCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SIGENERGY_SYSTEM_DEVICES)
	public Cache<String, CloudDataValue[]> sigenergySystemDevicesCache(
			@Qualifier(SIGENERGY_SYSTEM_DEVICES) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, CloudDataValue[].class,
				SIGENERGY_SYSTEM_DEVICES + "-cache");
	}

	@Bean
	@Qualifier(SIGENERGY)
	public SigenergyRestOperationsHelper sigenergyRestOpsHelper() {
		var accessTokenDao = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations,
				new ClientRegistrationRepository() {

					@Override
					public ClientRegistration findByRegistrationId(String registrationId) {
						// we're not using this API here
						throw new UnsupportedOperationException();
					}
				});
		return new SigenergyRestOperationsHelper(
				LoggerFactory.getLogger(SigenergyCloudIntegrationService.class), userEventAppender,
				restOps, CloudIntegrationsUserEvents.INTEGRATION_HTTP_ERROR_TAGS, encryptor,
				_ -> SigenergyCloudIntegrationService.SECURE_SETTINGS, Clock.systemUTC(), objectMapper,
				accessTokenDao, new StaticOptionalService<>(integrationLocksCache));
	}

	@Bean
	@Qualifier(SIGENERGY)
	public CloudIntegrationService sigenergyCloudIntegrationService(
			@Qualifier(SIGENERGY) RestOperationsHelper restOpsHelper,
			@Qualifier(SIGENERGY) Collection<CloudDatumStreamService> datumStreamServices
	/*- TODO
	, @Qualifier(SIGENERGY) Collection<CloudControlService> controlServices
	*/ ) {
		var service = new SigenergyCloudIntegrationService(datumStreamServices, List.of(),
				userEventAppender, encryptor, restOpsHelper, objectMapper);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SigenergyCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

	@Bean
	@Qualifier(SIGENERGY)
	public CloudDatumStreamService sigenergyCloudDatumStreamService(
			@Qualifier(SIGENERGY) final RestOperationsHelper restOpsHelper,
			@Qualifier(SIGENERGY_SYSTEM_DEVICES) final Cache<String, CloudDataValue[]> systemDeviceCache) {
		var service = new SigenergyCloudDatumStreamService(Clock.systemUTC(), userEventAppender,
				encryptor, expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOpsHelper,
				objectMapper);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SigenergyCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);
		service.setDatumStreamMetadataCache(datumStreamMetadataCache);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);
		service.setHttpCache(httpCache);
		service.setAllowLocalHosts(allowHttpLocalHosts);
		service.setSystemDeviceCache(systemDeviceCache);

		return service;
	}

}
