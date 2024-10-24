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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SolarEdgeV1CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Configuration for the SolarEdge cloud integration services.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class SolarEdgeConfig {

	/** A qualifier for SolarEdge configuration. */
	public static final String SOLAREDGE = "solaredge";

	/** A qualifier for SolarEdge site time zone configuration. */
	public static final String SOLAREDGE_SITE_TZ = "solaredge-site-tz";

	/** A qualifier for SolarEdge site inventory configuration. */
	public static final String SOLAREDGE_SITE_INVENTORY = "solaredge-site-inventory";

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

	@Bean
	@Qualifier(SOLAREDGE_SITE_TZ)
	@ConfigurationProperties(prefix = "app.c2c.cache.solaredge-site-tz")
	public CacheSettings solarEdgeSiteTimeZoneCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SOLAREDGE_SITE_TZ)
	public Cache<Long, ZoneId> solarEdgeSiteTimeZoneCache(
			@Qualifier(SOLAREDGE_SITE_TZ) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, ZoneId.class,
				SOLAREDGE_SITE_TZ + "-cache");
	}

	@Bean
	@Qualifier(SOLAREDGE_SITE_INVENTORY)
	@ConfigurationProperties(prefix = "app.c2c.cache.solaredge-site-inventory")
	public CacheSettings solarEdgeSiteInventoryCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SOLAREDGE_SITE_INVENTORY)
	public Cache<Long, CloudDataValue[]> solarEdgeSiteInventoryCache(
			@Qualifier(SOLAREDGE_SITE_INVENTORY) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, CloudDataValue[].class,
				SOLAREDGE_SITE_INVENTORY + "-cache");
	}

	@Bean
	@Qualifier(SOLAREDGE)
	public CloudDatumStreamService solarEdgeV1CloudDatumStreamService(
			@Qualifier(SOLAREDGE_SITE_TZ) Cache<Long, ZoneId> solarEdgeSiteTimeZoneCache,
			@Qualifier(SOLAREDGE_SITE_INVENTORY) Cache<Long, CloudDataValue[]> solarEdgeSiteInventoryCache) {
		var service = new SolarEdgeV1CloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				Clock.systemUTC());

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeV1CloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setSiteTimeZoneCache(solarEdgeSiteTimeZoneCache);
		service.setSiteInventoryCache(solarEdgeSiteInventoryCache);

		return service;
	}

	@Bean
	@Qualifier(SOLAREDGE)
	public CloudIntegrationService solarEdgeV1CloudIntegrationService(
			@Qualifier(SOLAREDGE) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new SolarEdgeV1CloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SolarEdgeV1CloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
