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

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import java.util.Arrays;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.config.SecurityTokenFilterSettings;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationFilter;
import net.solarnetwork.central.security.web.support.UserDetailsAuthenticationTokenService;
import net.solarnetwork.web.security.SecurityTokenAuthenticationEntryPoint;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.4
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private HandlerExceptionResolver handlerExceptionResolver;

	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HandlerExceptionResolverRequestRejectedHandler(handlerExceptionResolver);
	}

	@Bean
	public UserDetailsService userDetailsService() {
		JdbcUserDetailsService service = new JdbcUserDetailsService();
		service.setDataSource(dataSource);
		service.setUsersByUsernameQuery(JdbcUserDetailsService.DEFAULT_USERS_BY_USERNAME_SQL);
		service.setAuthoritiesByUsernameQuery(
				JdbcUserDetailsService.DEFAULT_AUTHORITIES_BY_USERNAME_SQL);
		return service;
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService());
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		return new ProviderManager(authenticationProvider());
	}

	@Bean
	public AuthenticationEventPublisher authenticationEventPublisher(
			ApplicationEventPublisher appEventPublisher) {
		return new DefaultAuthenticationEventPublisher(appEventPublisher);
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

	/**
	 * Security rules for the management API.
	 */
	@Configuration
	@Order(1)
	public static class ManagementWebSecurityConfig {

		@Order(3)
		@Bean
		public SecurityFilterChain filterChainManagement(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> matchers.requestMatchers("/ops/**"))

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					.httpBasic((httpBasic) -> httpBasic.realmName("SN Operations"))

					.authorizeHttpRequests((matchers) -> matchers
							.anyRequest().hasAnyAuthority(Role.ROLE_OPS.toString()))

			;
			// @formatter:on
			return http.build();
		}
	}

	/**
	 * API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(2)
	public static class ApiWebSecurityConfig {

		@Autowired
		private DataSource dataSource;

		@Autowired
		private HandlerExceptionResolver handlerExceptionResolver;

		@Autowired
		private SecurityTokenFilterSettings securityTokenFilterSettings;

		public UserDetailsService tokenUserDetailsService() {
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

		@Bean
		public SecurityTokenAuthenticationFilter tokenAuthenticationFilter() {
			AntPathMatcher pathMatcher = new AntPathMatcher();
			pathMatcher.setCachePatterns(true);
			pathMatcher.setCaseSensitive(true);
			SecurityTokenAuthenticationFilter filter = new SecurityTokenAuthenticationFilter(pathMatcher,
					"/api/v1/sec", securityTokenFilterSettings);
			filter.setUserDetailsService(tokenUserDetailsService());
			filter.setAuthenticationEntryPoint(unauthorizedEntryPoint());

			return filter;
		}

		@Bean
		public AuthenticationTokenService authenticationTokenService() {
			return new UserDetailsAuthenticationTokenService(tokenUserDetailsService());
		}

		@Bean
		public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					// can simply return 401 on auth failures
					.exceptionHandling((exceptionHandling) -> exceptionHandling
							.authenticationEntryPoint(unauthorizedEntryPoint())
							.accessDeniedHandler(unauthorizedEntryPoint()))

					// token auth filter
					.addFilterBefore(tokenAuthenticationFilter(),
							UsernamePasswordAuthenticationFilter.class)

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
							.requestMatchers(HttpMethod.GET,
									"/",
									"/error",
									"/*.html",
									"/ping",
									"/api/v1/pub/**")
							.permitAll()

							.requestMatchers("/api/v1/sec/**").hasAnyAuthority(Role.ROLE_OPS.toString())

							.anyRequest().authenticated());
			// @formatter:on
			return http.build();
		}
	}

}
