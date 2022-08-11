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

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

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
	public UserDetailsService userDetailsService() {
		InMemoryUserDetailsManager service = new InMemoryUserDetailsManager();
		return service;
	}

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		// we can simply return 403 because of expected credentials supplied with each request
		return new Http403ForbiddenEntryPoint();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	// @formatter:off
    http
      // CSRF not needed for stateless calls
      .csrf().disable()
      
      // make sure CORS honored
      .cors().and()
      
      // can simply return 403 on auth failures
      .exceptionHandling((exc) -> exc
        .authenticationEntryPoint(unauthorizedEntryPoint()))
      
      // no sessions
      .sessionManagement((sm) -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      
      .authorizeRequests((authz) -> authz
        .antMatchers(OPTIONS, "/**").permitAll()
          .antMatchers(GET, "/", "/error", "/*.html", "/ping").permitAll()
          
          .antMatchers("/oscp/**").permitAll()
          
          .anyRequest().authenticated()
      )
      .httpBasic()
    ;
    // @formatter:on
		return http.build();
	}

}
