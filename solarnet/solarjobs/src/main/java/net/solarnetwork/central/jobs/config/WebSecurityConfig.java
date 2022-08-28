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

package net.solarnetwork.central.jobs.config;

import java.util.Arrays;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.config.SecurityTokenFilterSettings;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationEntryPoint;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationFilter;
import net.solarnetwork.central.security.web.support.UserDetailsAuthenticationTokenService;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	private static String[] ACCESS_AUTHORITIES = new String[] { Role.ROLE_OPS.toString() };

	@Autowired
	private DataSource dataSource;

	@Autowired
	private HandlerExceptionResolver handlerExceptionResolver;

	@Bean
	public UserDetailsService userDetailsService() {
		JdbcUserDetailsService service = new JdbcUserDetailsService();
		service.setDataSource(dataSource);
		service.setUsersByUsernameQuery(JdbcUserDetailsService.DEFAULT_TOKEN_USERS_BY_USERNAME_SQL);
		service.setAuthoritiesByUsernameQuery(
				JdbcUserDetailsService.DEFAULT_TOKEN_AUTHORITIES_BY_USERNAME_SQL);
		return service;
	}

	@Bean
	public SecurityTokenAuthenticationEntryPoint unauthorizedEntryPoint() {
		SecurityTokenAuthenticationEntryPoint ep = new SecurityTokenAuthenticationEntryPoint();
		ep.setHandlerExceptionResolver(handlerExceptionResolver);
		return ep;
	}

	@ConfigurationProperties(prefix = "app.web.security.token")
	@Bean
	public SecurityTokenFilterSettings tokenAuthenticationFilterSettings() {
		return new SecurityTokenFilterSettings();
	}

	@Bean
	public SecurityTokenAuthenticationFilter tokenAuthenticationFilter() {
		AntPathMatcher pathMatcher = new AntPathMatcher();
		pathMatcher.setCachePatterns(true);
		pathMatcher.setCaseSensitive(true);
		SecurityTokenAuthenticationFilter filter = new SecurityTokenAuthenticationFilter(pathMatcher,
				"/api/v1/sec");
		filter.setUserDetailsService(userDetailsService());
		filter.setAuthenticationEntryPoint(unauthorizedEntryPoint());

		SecurityTokenFilterSettings settings = tokenAuthenticationFilterSettings();
		filter.setMaxDateSkew(settings.getMaxDateSkew());
		filter.setMaxRequestBodySize((int) settings.getMaxRequestBodySize().toBytes());

		return filter;
	}

	@Bean
	public AuthenticationTokenService authenticationTokenService() {
		return new UserDetailsAuthenticationTokenService(userDetailsService());
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(false);
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "PATCH"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "X-SN-Date"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// @formatter:off
	    http
	      // CSRF not needed for stateless calls
	      .csrf().disable()
	      
	      // make sure CORS honored
	      .cors().and()
	      
	      // can simply return 401 on auth failures
	      .exceptionHandling()
	      	.authenticationEntryPoint(unauthorizedEntryPoint())
	      	.accessDeniedHandler(unauthorizedEntryPoint())
	      	.and()
	      
	      // no sessions
	      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
	      
	      // token auth filter
	      .addFilterBefore(tokenAuthenticationFilter(),
					UsernamePasswordAuthenticationFilter.class)
	      
	      .authorizeRequests()
	      	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
	        .antMatchers(HttpMethod.GET, "/", "/error", "/*.html", "/ping", 
	            "/api/v1/pub/**").permitAll()
	        
	        .antMatchers(HttpMethod.GET, "/api/v1/sec/**").hasAnyAuthority(ACCESS_AUTHORITIES)
	        .antMatchers(HttpMethod.HEAD, "/api/v1/sec/**").hasAnyAuthority(ACCESS_AUTHORITIES)
	        
	        .anyRequest().authenticated()
	    ;
	    // @formatter:on
		return http.build();
	}

}
