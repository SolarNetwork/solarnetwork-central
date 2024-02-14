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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.security.NodeUserDetailsService;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.2
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	private static final String OPS_AUTHORITY = Role.ROLE_OPS.toString();

	private static final String NODE_AUTHORITY = Role.ROLE_NODE.toString();

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
	public StrictHttpFirewall httpFirewall() {
		StrictHttpFirewall firewall = new StrictHttpFirewall();

		// this following is disabled to allow for the SSL_CLIENT_CERT header value
		// which is a full PEM encoded certificate with newline characters
		firewall.setAllowedHeaderValues((header) -> true);

		return firewall;
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

					// make sure CORS honored
					.cors(Customizer.withDefaults())

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
	 * Security rules for the SolarIn API.
	 */
	@Configuration
	@Order(2)
	public static class AppWebSecurityConfig {

		@Order(2)
		@Bean
		public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					// limit this configuration to specific paths
					.securityMatchers((matchers) -> matchers.requestMatchers("/solarin/**"))

					// CSRF not needed for stateless calls
					.csrf((csrf) -> csrf.disable())

					// make sure CORS honored
					.cors(Customizer.withDefaults())

					// no sessions
					.sessionManagement((sm) -> sm.sessionCreationPolicy(STATELESS))

					.x509((x509) -> x509
							.userDetailsService(new NodeUserDetailsService())
							.subjectPrincipalRegex("UID=(.*?),"))

					.authorizeHttpRequests((matchers) -> matchers
							.requestMatchers(HttpMethod.OPTIONS, "/solarin/**").permitAll()
							
							.requestMatchers(HttpMethod.GET,
									"/solarin/",
									"/solarin/error",
									"/solarin/*.html",
									"/solarin/css/**",
									"/solarin/img/**",
									"/solarin/ping",
									"/solarin/api/v1/pub/**",
									"/solarin/identity.do")
									.permitAll()

							.requestMatchers(
									"/solarin/bulkCollector.do",
									"/solarin/u/bulkCollector.do")
									.hasAnyAuthority(NODE_AUTHORITY)
							
							.requestMatchers("/solarin/api/v1/sec/**").hasAnyAuthority(NODE_AUTHORITY)

							.anyRequest().authenticated())
			;
			// @formatter:on
			return http.build();
		}
	}

}
