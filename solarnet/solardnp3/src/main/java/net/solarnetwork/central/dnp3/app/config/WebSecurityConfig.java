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

package net.solarnetwork.central.dnp3.app.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.service.AuthenticationUserEventPublisher;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Security configuration.
 *
 * @author matt
 * @version 2.0
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Value("${app.meta.key}")
	private String appKey = "";

	@Value("${app.security.auth-events.pub-success:false}")
	private boolean authPubSuccess = false;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private HandlerExceptionResolver handlerExceptionResolver;

	@Autowired
	private ObjectMapper objectMapper;

	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HandlerExceptionResolverRequestRejectedHandler(handlerExceptionResolver);
	}

	@Bean
	public AuthenticationEntryPoint unauthorizedEntryPoint() {
		return new Http403ForbiddenEntryPoint();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		JdbcUserDetailsService service = new JdbcUserDetailsService(objectMapper);
		service.setDataSource(dataSource);
		service.setUsersByUsernameQuery(JdbcUserDetailsService.DEFAULT_USERS_BY_USERNAME_SQL);
		service.setAuthoritiesByUsernameQuery(
				JdbcUserDetailsService.DEFAULT_AUTHORITIES_BY_USERNAME_SQL);
		return service;
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationEventPublisher authEventPublisher) {
		var mgr = new ProviderManager(authenticationProvider());
		mgr.setAuthenticationEventPublisher(authEventPublisher);
		return mgr;
	}

	@Bean
	public AuthenticationEventPublisher authenticationEventPublisher(
			ApplicationEventPublisher appEventPublisher) {
		return new DefaultAuthenticationEventPublisher(appEventPublisher);
	}

	@Bean
	public AuthenticationUserEventPublisher authenticationUserEventPublisher(
			UserEventAppenderBiz userEventAppenderBiz) {
		AuthenticationUserEventPublisher pub = new AuthenticationUserEventPublisher(appKey,
				userEventAppenderBiz);
		pub.setFailureOnly(!authPubSuccess);
		return pub;
	}

	/**
	 * Security rules for the management API.
	 */
	@Configuration
	@Order(1)
	public static class ManagementWebSecurityConfig {

		@Order(1)
		@Bean
		public SecurityFilterChain filterChainManagement(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> matchers.requestMatchers("/ops/**"))

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// CORS not needed
					.cors((cors) -> cors.disable())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					.httpBasic((httpBasic) -> httpBasic.realmName("SN Operations"))

					.authorizeHttpRequests((matchers) -> matchers
						.requestMatchers(HttpMethod.GET,
								"/ops/health"
								).permitAll()
						.anyRequest().hasAnyAuthority(Role.ROLE_OPS.toString()))

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
