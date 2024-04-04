/* ==================================================================
 * DatumInputCacheConfig.java - 24/02/2024 4:09:06 pm
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

package net.solarnetwork.central.din.app.config;

import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Datum input cache configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class InstructionInputCacheConfig implements InstructionInputConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(INSTR_ENDPOINT_CONF)
	@ConfigurationProperties(prefix = "app.inin.cache.endpoint-conf-cache")
	public CacheSettings instructionEndpointConfigurationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(INSTR_ENDPOINT_CONF)
	public Cache<UserUuidPK, EndpointConfiguration> instructionEndpointConfigurationCache(
			@Qualifier(INSTR_ENDPOINT_CONF) CacheSettings settings) {
		return settings.createCache(cacheManager, UserUuidPK.class, EndpointConfiguration.class,
				INSTR_ENDPOINT_CONF + "-cache");
	}

	@Bean
	@Qualifier(REQ_TRANSFORM_CONF)
	@ConfigurationProperties(prefix = "app.inin.cache.req-transform-conf-cache")
	public CacheSettings instructionRequestTransformConfigurationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(REQ_TRANSFORM_CONF)
	public Cache<UserLongCompositePK, RequestTransformConfiguration> instructionRequestTransformConfigurationCache(
			@Qualifier(REQ_TRANSFORM_CONF) CacheSettings settings) {
		return settings.createCache(cacheManager, UserLongCompositePK.class,
				RequestTransformConfiguration.class, REQ_TRANSFORM_CONF + "-cache");
	}

	@Bean
	@Qualifier(RES_TRANSFORM_CONF)
	@ConfigurationProperties(prefix = "app.inin.cache.res-transform-conf-cache")
	public CacheSettings instructionResponseTransformConfigurationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(RES_TRANSFORM_CONF)
	public Cache<UserLongCompositePK, ResponseTransformConfiguration> instructionResponseTransformConfigurationCache(
			@Qualifier(RES_TRANSFORM_CONF) CacheSettings settings) {
		return settings.createCache(cacheManager, UserLongCompositePK.class,
				ResponseTransformConfiguration.class, RES_TRANSFORM_CONF + "-cache");
	}

	@Bean
	@Qualifier(USER_METADATA)
	public Cache<Long, UserMetadataEntity> instructionUserMetadataCache(
			@Qualifier(USER_METADATA) CacheSettings settings) {
		return settings.createCache(cacheManager, Long.class, UserMetadataEntity.class,
				USER_METADATA + "-cache");
	}

	@Bean
	@Qualifier(USER_METADATA_PATH)
	public Cache<UserStringCompositePK, String> instructionUserMetadataPathCache(
			@Qualifier(USER_METADATA_PATH) CacheSettings settings) {
		return settings.createCache(cacheManager, UserStringCompositePK.class, String.class,
				USER_METADATA_PATH + "-cache");
	}

}
