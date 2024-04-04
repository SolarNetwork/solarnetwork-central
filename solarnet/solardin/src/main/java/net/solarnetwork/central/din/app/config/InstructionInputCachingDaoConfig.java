/* ==================================================================
 * DatumInputCachingDaoConfig.java - 24/02/2024 4:16:57 pm
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

import java.util.concurrent.Executor;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import net.solarnetwork.central.dao.CachingUserMetadataDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.dao.CachingEndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.CachingTransformConfigurationDao;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * DAO configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class InstructionInputCachingDaoConfig implements InstructionInputConfiguration {

	@Autowired
	private Executor executor;

	/**
	 * A caching instruction input endpoint configuration DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	@Primary
	public EndpointConfigurationDao cachingInstructionEndpointConfigurationDao(
			EndpointConfigurationDao dao,
			@Qualifier(INSTR_ENDPOINT_CONF) Cache<UserUuidPK, EndpointConfiguration> cache) {
		return new CachingEndpointConfigurationDao(dao, cache, executor);
	}

	/**
	 * A caching instruction input transform configuration DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	@Primary
	public TransformConfigurationDao<RequestTransformConfiguration> cachingInstructionRequestTransformConfigurationDao(
			TransformConfigurationDao<RequestTransformConfiguration> dao,
			@Qualifier(REQ_TRANSFORM_CONF) Cache<UserLongCompositePK, RequestTransformConfiguration> cache) {
		return new CachingTransformConfigurationDao<RequestTransformConfiguration>(dao, cache, executor);
	}

	/**
	 * A caching instruction input transform configuration DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	@Primary
	public TransformConfigurationDao<ResponseTransformConfiguration> cachingInstructionResponseTransformConfigurationDao(
			TransformConfigurationDao<ResponseTransformConfiguration> dao,
			@Qualifier(RES_TRANSFORM_CONF) Cache<UserLongCompositePK, ResponseTransformConfiguration> cache) {
		return new CachingTransformConfigurationDao<ResponseTransformConfiguration>(dao, cache,
				executor);
	}

	/**
	 * A caching instruction input transform configuration DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	@Primary
	public UserMetadataDao cachingInstructionUserMetadataDao(UserMetadataDao dao,
			@Qualifier(USER_METADATA) Cache<Long, UserMetadataEntity> entityCache,
			@Qualifier(USER_METADATA_PATH) Cache<UserStringCompositePK, String> pathCache) {
		return new CachingUserMetadataDao(dao, entityCache, executor, pathCache);
	}

}
