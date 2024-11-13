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

package net.solarnetwork.central.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.CACHING;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.expression.Expression;
import net.solarnetwork.central.c2c.biz.impl.BasicCloudIntegrationsExpressionService;
import net.solarnetwork.central.common.dao.SolarNodeMetadataReadOnlyDao;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.tariff.TariffSchedule;

/**
 * Cloud integrations expression configuration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class CloudIntegrationsExpressionConfig implements SolarNetCloudIntegrationsConfiguration {

	@Primary
	@Bean
	public BasicCloudIntegrationsExpressionService spelCloudIntegrationsExpressionService(
	// @formatter:off
			@Qualifier(CACHING)
			@Autowired
			SolarNodeMetadataReadOnlyDao nodeMetadataDao,

			@Qualifier(CLOUD_INTEGRATIONS_EXPRESSIONS)
			@Autowired(required = false)
			Cache<String, Expression> expressionCache,

			@Qualifier(CLOUD_INTEGRATIONS_TARIFF)
			@Autowired(required = false)
			Cache<ObjectDatumStreamMetadataId, TariffSchedule> tariffScheduleCache
			// @formatter:on
	) {
		var service = new BasicCloudIntegrationsExpressionService(new SpelExpressionService());
		service.setMetadataDao(nodeMetadataDao);
		service.setExpressionCache(expressionCache);
		service.setTariffScheduleCache(tariffScheduleCache);
		return service;
	}

}
