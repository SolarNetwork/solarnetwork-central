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

import static net.solarnetwork.central.din.app.config.SolarDinAppConfiguration.CACHING;
import static net.solarnetwork.central.din.security.SecurityUtils.ROLE_DIN;
import static net.solarnetwork.central.inin.security.SecurityUtils.ROLE_ININ;
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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.din.app.security.DatumEndpointAuthenticationDetailsSource;
import net.solarnetwork.central.din.app.security.DatumEndpointAuthenticationProvider;
import net.solarnetwork.central.din.app.security.InstructionEndpointAuthenticationDetailsSource;
import net.solarnetwork.central.din.app.security.InstructionEndpointAuthenticationProvider;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.security.CredentialJwtAuthenticationConverter;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.service.AuthenticationUserEventPublisher;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;

/**
 * Security configuration.
 *
 * @author matt
 * @version 1.4
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Value("${app.meta.key}")
	private String appKey = "";

	@Value("${app.security.auth-events.pub-success:false}")
	private boolean authPubSuccess = false;

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

					// CORS not needed
					.cors((cors) -> cors.disable())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					.httpBasic((httpBasic) -> httpBasic.realmName("SN Operations"))

					.authenticationProvider(opsAuthenticationProvider())

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
	 * Datum API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(2)
	public static class DatumApiWebSecurityConfig {

		@Value("${app.security.endpoint-id-url-pattern:}")
		private String endpointIdUrlPattern;

		@Autowired
		private JdbcOperations jdbcOperations;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Qualifier(CACHING)
		@Autowired
		private EndpointConfigurationDao endpointDao;

		@Bean
		public DatumEndpointAuthenticationDetailsSource datumEndpointAuthenticationDetailsSource() {
			Pattern pat = DatumEndpointAuthenticationDetailsSource.DEFAULT_ENDPOINT_ID_PATTERN;
			if ( endpointIdUrlPattern != null && !endpointIdUrlPattern.isEmpty() ) {
				pat = Pattern.compile(endpointIdUrlPattern, Pattern.CASE_INSENSITIVE);
			}
			return new DatumEndpointAuthenticationDetailsSource(endpointDao, pat);
		}

		@Bean
		public net.solarnetwork.central.din.security.CredentialAuthorizationDao datumCredentialAuthorizationDao() {
			return new net.solarnetwork.central.din.security.jdbc.JdbcCredentialAuthorizationDao(
					jdbcOperations);
		}

		@Order(2)
		@Bean
		public SecurityFilterChain datumFilterChainApi(HttpSecurity http,
				net.solarnetwork.central.din.security.CredentialAuthorizationDao authDao)
				throws Exception {
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

					.authenticationProvider(new DatumEndpointAuthenticationProvider(authDao, passwordEncoder))

					.httpBasic((basic) -> {
						basic.realmName("SolarDIN")
							.authenticationDetailsSource(datumEndpointAuthenticationDetailsSource());
					})

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

	/**
	 * Instruction API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(3)
	public static class InstructionApiWebSecurityConfig {

		@Value("${app.security.endpoint-id-url-pattern:}")
		private String endpointIdUrlPattern;

		@Autowired
		private JdbcOperations jdbcOperations;

		@Autowired
		private PasswordEncoder passwordEncoder;

		@Qualifier(CACHING)
		@Autowired
		private net.solarnetwork.central.inin.dao.EndpointConfigurationDao endpointDao;

		@Bean
		public InstructionEndpointAuthenticationDetailsSource instructionEndpointAuthenticationDetailsSource() {
			Pattern pat = InstructionEndpointAuthenticationDetailsSource.DEFAULT_ENDPOINT_ID_PATTERN;
			if ( endpointIdUrlPattern != null && !endpointIdUrlPattern.isEmpty() ) {
				pat = Pattern.compile(endpointIdUrlPattern, Pattern.CASE_INSENSITIVE);
			}
			return new InstructionEndpointAuthenticationDetailsSource(endpointDao, pat);
		}

		@Bean
		public net.solarnetwork.central.inin.security.CredentialAuthorizationDao instructionCredentialAuthorizationDao() {
			return new net.solarnetwork.central.inin.security.jdbc.JdbcCredentialAuthorizationDao(
					jdbcOperations);
		}

		@Order(3)
		@Bean
		public SecurityFilterChain instructionFilterChainApi(HttpSecurity http,
				AuthenticationEntryPoint unauthorizedEntryPoint,
				net.solarnetwork.central.inin.security.CredentialAuthorizationDao authDao)
				throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> {
						matchers.requestMatchers("/api/v1/instr/**");
					})

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((mgmt) -> mgmt.sessionCreationPolicy(STATELESS))

					.authenticationProvider(new InstructionEndpointAuthenticationProvider(authDao, passwordEncoder))

					.httpBasic((basic) -> {
						basic.realmName("SolarININ")
							.authenticationDetailsSource(instructionEndpointAuthenticationDetailsSource());
					})

					.authorizeHttpRequests((matchers) -> matchers
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/**").hasAnyAuthority(ROLE_ININ)
						.anyRequest().denyAll()
					)

					.oauth2ResourceServer((oauth) -> oauth
						.jwt((jwt) -> jwt
							//.authenticationManager(instructionEndpointAuthenticationManager())
							.jwtAuthenticationConverter(new CredentialJwtAuthenticationConverter(authDao))
						)
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
