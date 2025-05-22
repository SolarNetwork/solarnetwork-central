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

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.oscp.dao.AuthTokenAuthorizationDao;
import net.solarnetwork.central.oscp.fp.v20.web.AdjustGroupCapacityForecastController;
import net.solarnetwork.central.oscp.fp.v20.web.UpdateGroupCapacityForecastController;
import net.solarnetwork.central.oscp.security.ExternalSystemJwtAuthenticationConverter;
import net.solarnetwork.central.oscp.security.OscpTokenAuthenticationProvider;
import net.solarnetwork.central.oscp.security.OscpTokenAuthorizationHeaderAuthenticationFilter;
import net.solarnetwork.central.oscp.security.Role;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;

/**
 * Web security configuration.
 *
 * @author matt
 * @version 1.5
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
		// we can simply return 403 because of expected credentials supplied with each request
		return new Http403ForbiddenEntryPoint();
	}

	/**
	 * Security rules for the management API.
	 */
	@Configuration
	@Order(1)
	public static class ManagementWebSecurityConfig {

		@Autowired
		private DataSource dataSource;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Autowired
		private ObjectMapper objectMapper;

		private AuthenticationProvider opsAuthenticationProvider() {
			JdbcUserDetailsService service = new JdbcUserDetailsService(objectMapper);
			service.setDataSource(dataSource);
			service.setUsersByUsernameQuery(JdbcUserDetailsService.DEFAULT_USERS_BY_USERNAME_SQL);
			service.setAuthoritiesByUsernameQuery(
					JdbcUserDetailsService.DEFAULT_AUTHORITIES_BY_USERNAME_SQL);

			DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
			provider.setPasswordEncoder(passwordEncoder);
			return provider;
		}

		@Order(1)
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

					.authenticationProvider(opsAuthenticationProvider())

					.authorizeHttpRequests((matchers) -> matchers
						.requestMatchers(HttpMethod.GET,
								"/ops/health"
								).permitAll()
						.anyRequest().hasAnyAuthority(net.solarnetwork.central.security.Role.ROLE_OPS.toString()))

			;
			// @formatter:on
			return http.build();
		}
	}

	/**
	 * API security rules, for stateless OSCP Token and JWT REST access.
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

		@Bean
		public ExternalSystemJwtAuthenticationConverter jwtAuthenticationConverter() {
			ExternalSystemJwtAuthenticationConverter converter = new ExternalSystemJwtAuthenticationConverter(
					authTokenAuthorizationDao);
			return converter;
		}

		@Order(2)
		@Bean
		public SecurityFilterChain filterChainApi(HttpSecurity http,
				AuthenticationEntryPoint unauthorizedEntryPoint) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> matchers.requestMatchers("/oscp/**"))

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// can simply return 403 on auth failures
					.exceptionHandling((exc) -> exc.authenticationEntryPoint(unauthorizedEntryPoint))

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					// token auth filter
					.addFilterBefore(tokenAuthenticationFilter(),
							UsernamePasswordAuthenticationFilter.class)

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(UpdateGroupCapacityForecastController.URL_PATH).hasAuthority(Role.ROLE_CAPACITYPROVIDER.toString())
							.requestMatchers(AdjustGroupCapacityForecastController.URL_PATH).hasAuthority(Role.ROLE_CAPACITYOPTIMIZER.toString())
							.requestMatchers("/oscp/fp/**").hasAnyAuthority(
									Role.ROLE_CAPACITYOPTIMIZER.toString(),
									Role.ROLE_CAPACITYPROVIDER.toString())
							.anyRequest().authenticated())

					.oauth2ResourceServer((oauth) -> oauth
							.jwt((jwt) -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
							.authenticationEntryPoint(unauthorizedEntryPoint))

			;
			// @formatter:on
			return http.build();
		}
	}

	/**
	 * Last set of security rules, for public resources else deny all others.
	 */
	@Configuration
	@Order(Integer.MAX_VALUE)
	public static class PublicWebSecurityConfig {

		@Order(Integer.MAX_VALUE)
		@Bean
		public SecurityFilterChain filterChainPublic(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
							.requestMatchers(HttpMethod.GET,
									"/",
									"/error",
									"/*.html",
									"/ping")
									.permitAll()
							.anyRequest().denyAll())
			;
			// @formatter:on
			return http.build();
		}

	}

}
