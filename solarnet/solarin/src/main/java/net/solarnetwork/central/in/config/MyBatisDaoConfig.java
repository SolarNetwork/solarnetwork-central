/* ==================================================================
 * MyBatisDaoConfig.java - 4/10/2021 4:44:19 PM
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

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis configuration for SolarIn.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class MyBatisDaoConfig {

	private static final Logger log = LoggerFactory.getLogger(MyBatisDaoConfig.class);

	@Bean
	public ConfigurationCustomizer mybatisConfigurationCustomizer() {
		return new ConfigurationCustomizer() {

			@Override
			public void customize(org.apache.ibatis.session.Configuration configuration) {
				// TODO Auto-generated method stub
				log.debug("Got MyBatis config: {}", configuration);
			}
		};
	}

}
