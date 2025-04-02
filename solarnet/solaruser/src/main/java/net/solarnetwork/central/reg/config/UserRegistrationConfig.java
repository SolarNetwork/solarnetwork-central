/* ==================================================================
 * UserConfig.java - 21/10/2021 10:00:06 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.user.config.RegistrationBizConfig.USER_REGISTRATION;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.central.user.biz.dao.UserValidator;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.domain.UserAuthToken;

/**
 * Configuration for user registration.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
public class UserRegistrationConfig implements SolarNetUserConfiguration {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(USER_REGISTRATION)
	public Validator userValidator() {
		return new UserValidator();
	}

	@Bean
	@Qualifier(USER_AUTH_TOKEN)
	@ConfigurationProperties(prefix = "app.user.cache.user-auth-token")
	public CacheSettings userAuthTokenCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(USER_AUTH_TOKEN)
	public Cache<UserStringCompositePK, UserAuthToken> userAuthTokenCache(
			@Qualifier(USER_SECRET) CacheSettings settings) {
		return settings.createCache(cacheManager, UserStringCompositePK.class, UserAuthToken.class,
				USER_AUTH_TOKEN + "-cache");
	}

}
