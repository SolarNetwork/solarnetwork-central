/* ==================================================================
 * WebConfig.java - 10/08/2022 5:15:52 pm
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

package net.solarnetwork.oscp.sim.cp.config;

import static net.solarnetwork.central.oscp.web.OscpWebUtils.RESPONSE_SENT;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;
import net.solarnetwork.central.oscp.web.ThreadLocalCompletableHandlerInterceptor;
import net.solarnetwork.central.web.PingController;
import net.solarnetwork.central.web.support.WebServiceErrorAttributes;
import net.solarnetwork.service.PingTest;

/**
 * Web layer configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import({ WebServiceErrorAttributes.class })
public class WebConfig implements WebMvcConfigurer {

	@Autowired(required = false)
	private List<PingTest> pingTests;

	@Bean
	public PingController pingController() {
		PingController controller = new PingController();
		controller.setTests(pingTests);
		return controller;
	}

	@RequestMapping("/ping")
	static class SolarOscpSimCpPingController extends PingController {
		// nothing new
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// @formatter:off
		registry.addMapping("/**")
			.allowCredentials(true)
			.allowedOriginPatterns(CorsConfiguration.ALL)
			.maxAge(TimeUnit.HOURS.toSeconds(24))
			.allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("Authorization", "Content-MD5", "Content-Type", "Digest")
			;
		// @formatter:on
	}

	@Bean
	public MappedInterceptor responseSentInterceptor() {
		return new MappedInterceptor(new String[] { "/**" },
				new ThreadLocalCompletableHandlerInterceptor<Void>(RESPONSE_SENT, null));
	}

}
