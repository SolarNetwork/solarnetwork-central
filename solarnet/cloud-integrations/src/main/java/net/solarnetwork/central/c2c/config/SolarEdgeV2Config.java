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
import java.time.Clock;
import java.time.ZoneId;
import java.util.Collection;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.retry.RetryOperations;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.solaredge.SolarEdgeV2CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Configuration for the SolarEdge cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class SolarEdgeV2Config implements SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for SolarEdge configuration. */
	public static final String SOLAREDGE_V2 = "solaredge2";

	/** A qualifier for SolarEdge site time zone configuration. */
	public static final String SOLAREDGE_V2_SITE_TZ = "solaredge2-site-tz";

	/** A qualifier for SolarEdge site inventory configuration. */
	public static final String SOLAREDGE_V2_SITE_INVENTORY = "solaredge2-site-inventory";

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

	@Value("${app.c2c.allow-http-local-hosts:false}")
	private boolean allowHttpLocalHosts;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_POLL)
	private RetryOperations pollRetryOperations;

	@Bean
	@Qualifier(SOLAREDGE_V2_SITE_TZ)
	@ConfigurationProperties(prefix = "app.c2c.cache.solaredge-site-tz")
	public CacheSettings solarEdgeV2SiteTimeZoneCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SOLAREDGE_V2_SITE_TZ)
	public Cache<Long, ZoneId> solarEdgeV2SiteTimeZoneCache(
			@Qualifier(SOLAREDGE_V2_SITE_TZ) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, ZoneId.class,
				SOLAREDGE_V2_SITE_TZ + "-cache");
	}

	@Bean
	@Qualifier(SOLAREDGE_V2_SITE_INVENTORY)
	@ConfigurationProperties(prefix = "app.c2c.cache.solaredge-site-inventory")
	public CacheSettings solarEdgeV2SiteInventoryCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SOLAREDGE_V2_SITE_INVENTORY)
	public Cache<Long, CloudDataValue[]> solarEdgeV2SiteInventoryCache(
			@Qualifier(SOLAREDGE_V2_SITE_INVENTORY) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, CloudDataValue[].class,
				SOLAREDGE_V2_SITE_INVENTORY + "-cache");
	}

	@Bean
	@Qualifier(SOLAREDGE_V2)
	public CloudDatumStreamService solarEdgeV2CloudDatumStreamService(
			@Qualifier(SOLAREDGE_V2_SITE_TZ) Cache<Long, ZoneId> solarEdgeSiteTimeZoneCache,
			@Qualifier(SOLAREDGE_V2_SITE_INVENTORY) Cache<Long, CloudDataValue[]> solarEdgeSiteInventoryCache) {
		var service = new SolarEdgeV2CloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeV2CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setRetryOps(pollRetryOperations);
		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);
		service.setDatumStreamMetadataCache(datumStreamMetadataCache);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);
		service.setSiteTimeZoneCache(solarEdgeSiteTimeZoneCache);
		service.setSiteInventoryCache(solarEdgeSiteInventoryCache);
		service.setHttpCache(httpCache);
		service.setAllowLocalHosts(allowHttpLocalHosts);

		return service;
	}

	@Bean
	@Qualifier(SOLAREDGE_V2)
	public CloudIntegrationService solarEdgeV2CloudIntegrationService(
			@Qualifier(SOLAREDGE_V2) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new SolarEdgeV2CloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps, Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeV2CloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

}
