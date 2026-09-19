/* ==================================================================
 * UserEventApiConfig.java - 20/03/2026 12:35:11 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import net.solarnetwork.central.biz.dao.DaoUserEventBiz;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.common.dao.UserEventDao;
import net.solarnetwork.central.reg.support.UserEventFilterValidator;

/**
 * API-level user event configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class UserEventApiConfig {

	@Bean
	public DaoUserEventBiz userEventBiz(UserEventDao userEventDao) {
		return new DaoUserEventBiz(userEventDao);
	}

	@Bean
	@Qualifier(SolarNetCommonConfiguration.USER_EVENTS)
	public Validator userEventFilterValidator() {
		return new UserEventFilterValidator();
	}

}
