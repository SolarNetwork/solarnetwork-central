/* ==================================================================
 * ApplicationMetadataConfig.java - 21/02/2022 10:23:30 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import net.solarnetwork.central.ApplicationMetadata;

/**
 * Application metadata configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationMetadataConfig {

	private final Logger log = LoggerFactory.getLogger(ApplicationMetadataConfig.class);

	@Value("${app.meta.name:}")
	private String appName;

	@Value("${app.meta.version:}")
	private String appVersion;

	@Value("${app.meta.instance-id:}")
	private String appInstanceId;

	@Bean
	public ApplicationMetadata applicationMetadata() {
		ApplicationMetadata meta = new ApplicationMetadata(appName, appVersion, appInstanceId);
		log.info("App meta: {}", meta);
		return meta;
	}

}
