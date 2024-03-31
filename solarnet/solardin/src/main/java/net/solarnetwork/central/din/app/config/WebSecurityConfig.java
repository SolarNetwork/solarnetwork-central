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

package net.solarnetwork.central.din.app.config;

import static net.solarnetwork.central.din.app.config.DatumInputConfiguration.CACHING;
import static net.solarnetwork.central.din.security.SecurityUtils.ROLE_DIN;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcOperations;
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
import net.solarnetwork.central.din.app.security.DatumEndpointAuthenticationDetailsSource;
import net.solarnetwork.central.din.app.security.DatumEndpointAuthenticationProvider;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.security.jdbc.JdbcCredentialAuthorizationDao;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.service.AuthenticationUserEventPublisher;
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

	private static final String OPS_AUTHORITY = Role.ROLE_OPS.toString();

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
							.anyRequest().hasAnyAuthority(OPS_AUTHORITY))

			;
			// @formatter:on
			return http.build();
		}
	}

	/**
	 * Datum API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(2)
	public static class DatumApiWebSecurityConfig {

		@Value("${app.security.endpoint-id-url-pattern:}")
		private String endpointIdUrlPattern;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Autowired
		private JdbcOperations jdbcOperations;

		@Qualifier(CACHING)
		@Autowired
		private EndpointConfigurationDao endpointDao;

		@Autowired
		private AuthenticationEventPublisher authEventPublisher;

		@Bean
		public DatumEndpointAuthenticationDetailsSource endpointAuthenticationDetailsSource() {
			Pattern pat = DatumEndpointAuthenticationDetailsSource.DEFAULT_ENDPOINT_ID_PATTERN;
			if ( endpointIdUrlPattern != null && !endpointIdUrlPattern.isEmpty() ) {
				pat = Pattern.compile(endpointIdUrlPattern, Pattern.CASE_INSENSITIVE);
			}
			return new DatumEndpointAuthenticationDetailsSource(endpointDao, pat);
		}

		private AuthenticationManager endpointAuthenticationManager() {
			JdbcCredentialAuthorizationDao dao = new JdbcCredentialAuthorizationDao(jdbcOperations);
			var mgr = new ProviderManager(new DatumEndpointAuthenticationProvider(dao, passwordEncoder));
			mgr.setAuthenticationEventPublisher(authEventPublisher);
			return mgr;
		}

		@Order(2)
		@Bean
		public SecurityFilterChain filterChainApi(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> {
						matchers.requestMatchers("/api/v1/datum/**");
					})

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((mgmt) -> mgmt.sessionCreationPolicy(STATELESS))

					.httpBasic((basic) -> {
						basic.realmName("SolarDIN")
							.authenticationDetailsSource(endpointAuthenticationDetailsSource());
					})

					.authenticationManager(endpointAuthenticationManager())

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
							.requestMatchers("/**").hasAnyAuthority(ROLE_DIN)
							.anyRequest().denyAll()
					)
			;
			// @formatter:on
			return http.build();
		}
	}

	/*-
	 * Instruction API security rules, for stateless REST access.
	 *
	@Configuration
	@Order(3)
	public static class InstructionApiWebSecurityConfig {

		@Value("${app.security.endpoint-id-url-pattern:}")
		private String endpointIdUrlPattern;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Autowired
		private JdbcOperations jdbcOperations;

		@Qualifier(CACHING)
		@Autowired
		private net.solarnetwork.central.inin.dao.EndpointConfigurationDao endpointDao;

		@Autowired
		private AuthenticationEventPublisher authEventPublisher;

		@Bean
		public DatumEndpointAuthenticationDetailsSource endpointAuthenticationDetailsSource() {
			Pattern pat = DatumEndpointAuthenticationDetailsSource.DEFAULT_ENDPOINT_ID_PATTERN;
			if ( endpointIdUrlPattern != null && !endpointIdUrlPattern.isEmpty() ) {
				pat = Pattern.compile(endpointIdUrlPattern, Pattern.CASE_INSENSITIVE);
			}
			return new DatumEndpointAuthenticationDetailsSource(endpointDao, pat);
		}

		private AuthenticationManager endpointAuthenticationManager() {
			net.solarnetwork.central.inin.security.jdbc.JdbcCredentialAuthorizationDao dao = new net.solarnetwork.central.inin.security.jdbc.JdbcCredentialAuthorizationDao(
					jdbcOperations);
			var mgr = new ProviderManager(new DatumEndpointAuthenticationProvider(dao, passwordEncoder));
			mgr.setAuthenticationEventPublisher(authEventPublisher);
			return mgr;
		}

		@Order(3)
		@Bean
		public SecurityFilterChain filterChainApi(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> {
						matchers.requestMatchers("/api/v1/instr/endpoint/**");
					})

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((mgmt) -> mgmt.sessionCreationPolicy(STATELESS))

					.httpBasic((basic) -> {
						basic.realmName("SolarININ")
							.authenticationDetailsSource(endpointAuthenticationDetailsSource());
					})

					.authenticationManager(endpointAuthenticationManager())

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
							.requestMatchers("/**").hasAnyAuthority(ROLE_ININ)
							.anyRequest().denyAll()
					)
			;
			// @formatter:on
			return http.build();
		}
	}
	*/

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
									"/css/**",
									"/img/**",
									"/ping")
									.permitAll()
							.anyRequest().denyAll())
			;
			// @formatter:on
			return http.build();
		}

	}

}
