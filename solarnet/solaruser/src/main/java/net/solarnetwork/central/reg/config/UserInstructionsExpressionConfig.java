/* ==================================================================
 * CloudIntegrationsExpressionConfig.java - 8/10/2024 8:55:48 am
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

package net.solarnetwork.central.reg.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.CACHING;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS_EXPRESSIONS;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserMetadataReadOnlyDao;
import net.solarnetwork.central.user.dao.UserSecretAccessDao;
import net.solarnetwork.central.user.support.BasicInstructionsExpressionService;
import net.solarnetwork.common.expr.spel.SpelExpressionService;

/**
 * User instructions expression configuration.
 *
 * @author matt
 * @version 1.0
 */
@Profile(USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserInstructionsExpressionConfig {

	@Bean
	public BasicInstructionsExpressionService spelInstructionsExpressionService(
	// @formatter:off
			SolarNodeOwnershipDao nodeOwnershipDao,

			@Qualifier(CACHING)
			@Autowired
			SolarNodeMetadataReadOnlyDao nodeMetadataDao,

			@Qualifier(CACHING)
			@Autowired
			UserMetadataReadOnlyDao userMetadataDao,

			@Qualifier(USER_INSTRUCTIONS_EXPRESSIONS)
			@Autowired(required = false)
			Cache<String, Expression> expressionCache,

			@Autowired(required = false)
			UserSecretAccessDao userSecretAccessDao
			// @formatter:on
	) {
		var service = new BasicInstructionsExpressionService(new SpelExpressionService());
		service.setNodeMetadataDao(nodeMetadataDao);
		service.setExpressionCache(expressionCache);
		service.setUserMetadataDao(userMetadataDao);
		service.setUserSecretAccessDao(userSecretAccessDao);
		return service;
	}

}
