/* ==================================================================
 * WebSecurityConfig.java - 9/10/2021 3:10:26 PM
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

package net.solarnetwork.central.in.ocpp.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.1
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Autowired
	private HandlerExceptionResolver handlerExceptionResolver;

	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HandlerExceptionResolverRequestRejectedHandler(handlerExceptionResolver);
	}

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(false);
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "HEAD"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "X-SN-Date"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// @formatter:off
	    http
	      // limit this configuration to specific paths
	      .securityMatchers()
	        .requestMatchers("/solarocpp/**")
	        .and()

	        // CSRF not needed for stateless calls
	      .csrf().disable()
	      
	      // make sure CORS honored
	      .cors().and()
	      
	      // can simply return 403 on auth failures
	      .exceptionHandling()
	      	//.authenticationEntryPoint(unauthorizedEntryPoint())
	      	//.accessDeniedPage("/error/403")
	      	.and()
	      
	      // no sessions
	      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
	      
	      .authorizeHttpRequests()
	      	.requestMatchers(HttpMethod.OPTIONS, "/solarocpp/**").permitAll()
	        .requestMatchers(HttpMethod.GET, 
	        		"/solarocpp/",
	        		"/solarocpp/error",
	        		"/solarocpp/*.html",
	        		"/solarocpp/css/**",
	        		"/solarocpp/img/**",
	        		"/solarocpp/ping").permitAll()
	        
	        .anyRequest().denyAll()
	    ;
	    // @formatter:on
		return http.build();
	}

	@Bean
	public StrictHttpFirewall httpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();

		// this following is disabled to allow for the SSL_CLIENT_CERT header value
		// which is a full PEM encoded certificate with newline characters
		firewall.setAllowedHeaderValues((header) -> true);

		return firewall;
	}

}
