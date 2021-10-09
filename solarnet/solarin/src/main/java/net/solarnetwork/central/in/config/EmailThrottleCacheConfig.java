/* ==================================================================
 * EmailThrottleCacheConfig.java - 7/10/2021 11:11:44 AM
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

package net.solarnetwork.central.in.config;

import static net.solarnetwork.central.user.config.RegistrationBizConfig.EMAIL_THROTTLE;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.support.CacheSettings;

/**
 * Email throttle cache configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class EmailThrottleCacheConfig {

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@ConfigurationProperties(prefix = "app.solarin.email-throttle-cache")
	public CacheSettings emailThrottleCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(EMAIL_THROTTLE)
	public Cache<String, Boolean> emailThrottleCache() {
		CacheSettings settings = emailThrottleCacheSettings();
		return settings.createCache(cacheManager, String.class, Boolean.class, EMAIL_THROTTLE);
	}

}
