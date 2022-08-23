/* ==================================================================
 * WebSecurityConfig.java - 11/08/2022 2:43:07 pm
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

package net.solarnetwork.central.oscp.fp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import net.solarnetwork.central.oscp.dao.AuthTokenAuthorizationDao;
import net.solarnetwork.central.oscp.security.OscpTokenAuthenticationProvider;
import net.solarnetwork.central.oscp.security.Role;
import net.solarnetwork.central.oscp.web.OscpTokenAuthorizationHeaderAuthenticationFilter;

/**
 * Web security configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		// we can simply return 403 because of expected credentials supplied with each request
		return new Http403ForbiddenEntryPoint();
	}

	/**
	 * API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(2)
	public static class ApiWebSecurityConfig {

		@Autowired
		private AuthTokenAuthorizationDao authTokenAuthorizationDao;

		@Bean
		public OscpTokenAuthenticationProvider tokenAuthenticationProvider() {
			return new OscpTokenAuthenticationProvider(authTokenAuthorizationDao);
		}

		@Bean
		public AuthenticationManager authenticationManager() {
			return new ProviderManager(tokenAuthenticationProvider());
		}

		@Bean
		public OscpTokenAuthorizationHeaderAuthenticationFilter tokenAuthenticationFilter() {
			OscpTokenAuthorizationHeaderAuthenticationFilter filter = new OscpTokenAuthorizationHeaderAuthenticationFilter();
			filter.setAuthenticationManager(authenticationManager());
			return filter;
		}

		@Order(2)
		@Bean
		public SecurityFilterChain filterChainApi(HttpSecurity http,
				AuthenticationEntryPoint unauthorizedEntryPoint) throws Exception {
			// @formatter:off
			http
				// limit this configuration to specific paths
				.requestMatchers()
					.antMatchers("/oscp/**")
					.and()

				// CSRF not needed for stateless calls
				.csrf().disable()
				  
				// make sure CORS honored
				.cors().and()
		      
				// can simply return 403 on auth failures
				.exceptionHandling((exc) -> exc.authenticationEntryPoint(unauthorizedEntryPoint))
		      
				// no sessions
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
		      
				// token auth filter
				.addFilterBefore(tokenAuthenticationFilter(),
						UsernamePasswordAuthenticationFilter.class)
		      
				.authorizeRequests()
					.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
					.antMatchers("/oscp/fp/**").hasAnyAuthority(
						Role.ROLE_CAPACITYOPTIMIZER.toString(),
						Role.ROLE_CAPACITYPROVIDER.toString())
					.anyRequest().denyAll()
		    ;   
		    // @formatter:on
			return http.build();
		}
	}

	/**
	 * Last set of security rules, for public resources else deny all others.
	 */
	@Configuration
	@Order(3)
	public static class PublicWebSecurityConfig {

		@Order(3)
		@Bean
		public SecurityFilterChain filterChainPublic(HttpSecurity http) throws Exception {
			// @formatter:off
		    http
		      // CSRF not needed for stateless calls
		      .csrf().disable()
		      
		      // make sure CORS honored
		      .cors().and()
		      
		      // no sessions
		      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
		      
		      .authorizeRequests()
		      	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		        .antMatchers(HttpMethod.GET, 
		        		"/", 
		        		"/error",
		        		"/*.html",
		        		"/ping").permitAll()
		        .anyRequest().denyAll();
		    // @formatter:on
			return http.build();
		}

	}

}
