/* ==================================================================
 * CachingUserMetadataDaoConfig.java - 28/11/2025 10:52:09 am
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.CACHING;
import java.util.concurrent.Executor;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.dao.CachingUserMetadataDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;

/**
 * Caching {@link UserMetadataDao} configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class CachingUserMetadataDaoConfig implements SolarNetUserConfiguration {

	@Autowired
	private Executor executor;

	/**
	 * A caching user metadata configuration DAO.
	 *
	 * @return the DAO
	 */
	@Qualifier(CACHING)
	@Bean
	public UserMetadataDao cachingUserMetadataDao(UserMetadataDao dao,
			@Qualifier(USER_METADATA) Cache<Long, UserMetadataEntity> entityCache,
			@Qualifier(USER_METADATA_PATH) Cache<UserStringCompositePK, String> pathCache) {
		return new CachingUserMetadataDao(dao, entityCache, executor, pathCache);
	}

}
