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

package net.solarnetwork.central.in.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import net.solarnetwork.central.security.NodeUserDetailsService;
import net.solarnetwork.central.security.Role;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private static String[] NODE_AUTHORITIES = new String[] { Role.ROLE_NODE.toString() };

	@Override
	@Bean
	public UserDetailsService userDetailsService() {
		return new NodeUserDetailsService();
	}

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// @formatter:off
	    http
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
	      
	      //.requiresChannel()
	      //  .antMatchers(HttpMethod.GET, "/**/*Collector.do").requiresSecure()
	      //  .antMatchers(HttpMethod.GET, "/api/v1/sec/**").requiresSecure()
	      //  .and()
	        
	      .x509()
	        .userDetailsService(userDetailsService())
	        .subjectPrincipalRegex("UID=(.*?),")
	        .and()
	        
	      .authorizeRequests()
	      	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
	        .antMatchers(HttpMethod.GET, 
	        		"/",
	        		"/error",
	        		"/*.html",
	        		"/css/**",
	        		"/img/**",
	        		"/ping",
	        		"/api/v1/pub/**",
	        		"/identity.do").permitAll()
	        
	        .antMatchers(HttpMethod.GET, "/**/*Collector.do").hasAnyAuthority(NODE_AUTHORITIES)
	        .antMatchers(HttpMethod.GET, "/api/v1/sec/**").hasAnyAuthority(NODE_AUTHORITIES)
	        
	        .anyRequest().authenticated()
	    ;
	    // @formatter:on
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
