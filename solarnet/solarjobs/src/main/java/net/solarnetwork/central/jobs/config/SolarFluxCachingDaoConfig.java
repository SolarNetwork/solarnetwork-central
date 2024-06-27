/* ==================================================================
 * SolarFluxCachingDaoConfig.java - 26/06/2024 2:49:02 pm
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

package net.solarnetwork.central.jobs.config;

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import net.solarnetwork.central.datum.flux.dao.CachingFluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.dao.FluxPublishSettingsDao;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.support.CacheSettings;

/**
 * SolarFlux caching DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile("mqtt & !no-solarflux")
public class SolarFluxCachingDaoConfig implements SolarJobsAppConfiguration {

	/**
	 * A qualifier to use for the SolarFlux datum publish settings
	 * {@link Cache}.
	 */
	public static final String SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE = "flux-datum-publish-settings";

	@Autowired
	private CacheManager cacheManager;

	/**
	 * Settings for the SolarFlux datum publish user settings cache.
	 *
	 * @return the cache settings
	 */
	@Bean
	@Qualifier(SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE)
	@ConfigurationProperties(prefix = "app.solarflux.datum-publish.user-settings-cache")
	public CacheSettings solarFluxDatumPublishUserSettingsCacheSettings() {
		return new CacheSettings();
	}

	/**
	 * Get the node ownership cache.
	 *
	 * @return the node ownership cache
	 */
	@Bean
	@Qualifier(SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE)
	public Cache<UserLongStringCompositePK, FluxPublishSettings> solarFluxDatumPublishUserSettingsCache(
			@Qualifier(SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE) CacheSettings settings) {
		return settings.createCache(cacheManager, UserLongStringCompositePK.class,
				FluxPublishSettings.class, SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE);
	}

	/**
	 * A caching user SolarFlux publish settings DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	@Primary
	public FluxPublishSettingsDao cachingDatumEndpointConfigurationDao(
			FluxPublishSettingsDao dao,
			@Qualifier(SOLARFLUX_DATUM_PUBLISH_SETTINGS_CACHE) Cache<UserLongStringCompositePK, FluxPublishSettings> cache) {
		return new CachingFluxPublishSettingsDao(dao, cache);
	}

}
