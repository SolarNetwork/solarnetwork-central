/* ==================================================================
 * AppConfig.java - 9/10/2021 3:45:11 PM
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.common.config.SolarNetCommonConfiguration;
import net.solarnetwork.central.common.dao.config.SolarNetCommonDaoConfiguration;
import net.solarnetwork.central.datum.config.SolarNetDatumConfiguration;
import net.solarnetwork.central.support.BasicAppConfigurationBiz;
import net.solarnetwork.central.support.SimpleAppConfiguration;

/**
 * Application configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
//@formatter:off
@Import({
		SolarNetCommonConfiguration.class,
		SolarNetCommonDaoConfiguration.class,
		SolarNetDatumConfiguration.class,
})
//@formatter:on
public class AppConfig {

	@Bean
	@ConfigurationProperties(prefix = "app.config.service-urls")
	public Map<String, String> serviceUrls() {
		return new LinkedHashMap<>();
	}

	@Bean
	public AppConfigurationBiz appConfigurationBiz() {
		return new BasicAppConfigurationBiz(new SimpleAppConfiguration(serviceUrls()));
	}

}
