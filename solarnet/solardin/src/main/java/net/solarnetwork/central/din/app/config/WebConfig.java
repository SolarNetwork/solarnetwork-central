/* ==================================================================
 * WebConfig.java - 10/10/2021 12:56:05 PM
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

package net.solarnetwork.central.din.app.config;

import static java.lang.String.format;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import net.solarnetwork.central.web.PingController;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.central.web.support.WebServiceErrorAttributes;
import net.solarnetwork.central.web.support.WebServiceGlobalControllerSupport;
import net.solarnetwork.service.PingTest;

/**
 * Web layer configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ WebServiceErrorAttributes.class, WebServiceControllerSupport.class,
		WebServiceGlobalControllerSupport.class })
public class WebConfig implements WebMvcConfigurer {

	@Controller
	@RequestMapping("/ping")
	static class SolarDinPingController extends PingController {

		public SolarDinPingController(List<PingTest> tests) {
			super(tests);
		}

	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// enable client caching of static resources
		// @formatter:off
		for ( String dir : new String[] {"css", "img"} ) {
			registry.addResourceHandler(format("/%s/", dir))
					.addResourceLocations("classpath:/static/")
					.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
		}
		// @formatter:on
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// @formatter:off
		registry.addMapping("/**")
			.allowCredentials(true)
			.allowedOriginPatterns(CorsConfiguration.ALL)
			.maxAge(TimeUnit.HOURS.toSeconds(24))
			.allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("Authorization", "Content-MD5", "Content-Type", "Digest", "X-SN-Date")
			;
		// @formatter:on
	}

}
