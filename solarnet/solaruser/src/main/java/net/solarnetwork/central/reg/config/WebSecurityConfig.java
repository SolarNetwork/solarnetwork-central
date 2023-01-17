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

package net.solarnetwork.central.reg.config;

import static java.lang.String.format;
import java.io.IOException;
import java.nio.file.Files;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;
import net.solarnetwork.central.security.Role;
import net.solarnetwork.central.security.config.SecurityTokenFilterSettings;
import net.solarnetwork.central.security.jdbc.JdbcUserDetailsService;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.central.security.web.HandlerExceptionResolverRequestRejectedHandler;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationEntryPoint;
import net.solarnetwork.central.security.web.SecurityTokenAuthenticationFilter;
import net.solarnetwork.central.security.web.support.UserDetailsAuthenticationTokenService;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.3
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	/** The anonymous authority. */
	public static final String ANONYMOUS_AUTHORITY = "ROLE_ANONYMOUS";

	/** The billing authority. */
	public static final String BILLING_AUTHORITY = "ROLE_BILLING";

	/** The event authority. */
	public static final String EVENT_AUTHORITY = "ROLE_EVENT";

	/** The export authority. */
	public static final String EXPORT_AUTHORITY = "ROLE_EXPORT";

	/** The import authority. */
	public static final String IMPORT_AUTHORITY = "ROLE_IMPORT";

	/** The OCPP authority. */
	public static final String OCPP_AUTHORITY = "ROLE_OCPP";

	/** A HTTP header to indicate the response contains the login form page. */
	public static final String LOGIN_PAGE_HEADER = "X-LoginFormPage";

	private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);

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

	/**
	 * Browser app security rules, with cookie-based session management and form
	 * login.
	 */
	@Configuration
	@Order(1)
	public static class BrowserWebSecurityConfig {

		@Order(1)
		@Bean
		public SecurityFilterChain filterChainBrowser(HttpSecurity http) throws Exception {
			// Add a special header to the login page, so JavaScript can reliably detect when redirected there
			HeaderWriter loginHeaderWriter = new DelegatingRequestMatcherHeaderWriter(
					new AntPathRequestMatcher("/login"),
					new StaticHeadersWriter(LOGIN_PAGE_HEADER, "true"));

			// opt-in to Spring Security 6 behavior
			// https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_defer_loading_csrftoken
			XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();

			// @formatter:off
		    http
		      // limit this configuration to specific paths
		      .securityMatchers()
		        .requestMatchers("/login")
		        .requestMatchers("/logout")
		        .requestMatchers("/*.do")
		        .requestMatchers("/register/**")
		        .requestMatchers("/u/**")
		        .and()
		      
		      .headers()
		        .addHeaderWriter(loginHeaderWriter)
		        .and()
		      
		      // opt-in to v6 https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_defer_loading_csrftoken
		      .csrf((csrf) -> csrf
		  	    .csrfTokenRequestHandler(requestHandler))
		      
		      .sessionManagement((sessions) -> sessions
		        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
		    	.requireExplicitAuthenticationStrategy(true))
		      
		      .authorizeHttpRequests()
		        .requestMatchers("/*.do").permitAll()
		        .requestMatchers("/register/**").permitAll()
		      	.requestMatchers("/u/sec/user/billing/**").hasAnyAuthority(BILLING_AUTHORITY)
		      	.requestMatchers("/u/sec/user/event/**").hasAnyAuthority(EVENT_AUTHORITY)
		      	.requestMatchers("/u/sec/user/export/**").hasAnyAuthority(EXPORT_AUTHORITY)
		      	.requestMatchers("/u/sec/user/import/**").hasAnyAuthority(IMPORT_AUTHORITY)
		      	.requestMatchers("/u/sec/user/ocpp/**").hasAnyAuthority(OCPP_AUTHORITY)
		        .requestMatchers("/u/sec/**").hasAnyAuthority(Role.ROLE_USER.toString())
		        .requestMatchers("/u/**").hasAnyAuthority(ANONYMOUS_AUTHORITY, Role.ROLE_USER.toString())
		        .anyRequest().denyAll()
		        .and()
			      
		      // form login
		      .formLogin()
		        .permitAll()
		        .loginPage("/login")
		        .defaultSuccessUrl("/u/sec/home")
		        .failureUrl("/login?login_error=1")
		        .and()
		        
		      // logout
		      .logout()
		        .permitAll()
		        .logoutUrl("/logout")
		        .logoutSuccessUrl("/logoutSuccess.do")
		        
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
			try {
				if ( !Files.isDirectory(securityTokenFilterSettings.getSpoolDirectory()) ) {
					Files.createDirectories(securityTokenFilterSettings.getSpoolDirectory());
					log.info("Created security token spool directory: {}",
							securityTokenFilterSettings.getSpoolDirectory());
				}
			} catch ( IOException e ) {
				throw new RuntimeException(format("Error setting up security token spool directory %s",
						securityTokenFilterSettings.getSpoolDirectory()), e);
			}

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

		@Order(2)
		@Bean
		public SecurityFilterChain filterChainApi(HttpSecurity http) throws Exception {
			// @formatter:off
		    http
		      // limit this configuration to specific paths
		      .securityMatchers()
		        .requestMatchers("/api/**")
		        .and()

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
		      
		      .authorizeHttpRequests()
		      	.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		      	.requestMatchers("/api/v1/sec/user/billing/**").hasAnyAuthority(BILLING_AUTHORITY)
		      	.requestMatchers("/api/v1/sec/user/event/**").hasAnyAuthority(EVENT_AUTHORITY)
		      	.requestMatchers("/api/v1/sec/user/export/**").hasAnyAuthority(EXPORT_AUTHORITY)
		      	.requestMatchers("/api/v1/sec/user/import/**").hasAnyAuthority(IMPORT_AUTHORITY)
		      	.requestMatchers("/api/v1/sec/user/ocpp/**").hasAnyAuthority(OCPP_AUTHORITY)
		        .requestMatchers("/api/v1/sec/**").hasAnyAuthority(Role.ROLE_USER.toString())
		        .requestMatchers("/api/v1/pub/**").permitAll()
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
		      
		      .authorizeHttpRequests()
		      	.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		        .requestMatchers(HttpMethod.GET, 
		        		"/", 
		        		"/error",
		        		"/session-expired",
		        		"/*.html",
		        		"/cert.*",
		        		"/css/**",
		        		"/fonts/**",
		        		"/img/**",
		        		"/js/**",
		        		"/js-lib/**",
		        		"/ping", 
		        		"/api/v1/pub/**").permitAll()
		        .requestMatchers(HttpMethod.POST,
		        		"/associate.*").permitAll()
		        .anyRequest().denyAll();
		    // @formatter:on
			return http.build();
		}

	}

}
