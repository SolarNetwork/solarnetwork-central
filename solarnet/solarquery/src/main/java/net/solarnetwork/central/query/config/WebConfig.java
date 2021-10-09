/* ==================================================================
 * WebConfig.java - 9/10/2021 3:20:51 PM
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

package net.solarnetwork.central.query.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import net.solarnetwork.central.web.PingController;

/**
 * Web layer configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	/** A qualifier for the source ID path matcher. */
	public static final String SOURCE_ID_PATH_MATCHER = "source-id";

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("GET", "POST")
				// setting allowCredentials to false to Spring returns Access-Control-Allow-Origin: *
				.allowCredentials(false);
	}

	@Bean
	@Qualifier(SOURCE_ID_PATH_MATCHER)
	public PathMatcher sourceIdPathMatcher() {
		AntPathMatcher matcher = new AntPathMatcher();
		matcher.setCachePatterns(true);
		matcher.setCaseSensitive(false);
		return matcher;
	}

	@Bean
	public PingController pingController() {
		return new PingController();
	}

}
