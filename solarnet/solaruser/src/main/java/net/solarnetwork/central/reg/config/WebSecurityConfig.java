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

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.RequestRejectedHandler;
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
 * @version 1.0
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

	/**
	 * Browser app security rules, with cookie-based session management and form
	 * login.
	 */
	@Configuration
	@Order(1)
	public static class BrowserWebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
		    http
		      // limit this configuration to specific paths
		      .requestMatchers()
		        .antMatchers("/login")
		        .antMatchers("/logout")
		        .antMatchers("/*.do")
		        .antMatchers("/u/**")
		        .and()
		      
		      // no sessions
		      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
		      
		      .authorizeRequests()
		        .antMatchers("/login").hasAnyAuthority(ANONYMOUS_AUTHORITY)
		        .antMatchers("/*.do").hasAnyAuthority(ANONYMOUS_AUTHORITY, Role.ROLE_USER.toString())
		      	.antMatchers("/u/sec/user/billing/**").hasAnyAuthority(BILLING_AUTHORITY)
		      	.antMatchers("/u/sec/user/event/**").hasAnyAuthority(EVENT_AUTHORITY)
		      	.antMatchers("/u/sec/user/export/**").hasAnyAuthority(EXPORT_AUTHORITY)
		      	.antMatchers("/u/sec/user/import/**").hasAnyAuthority(IMPORT_AUTHORITY)
		        .antMatchers("/u/sec/**").hasAnyAuthority(Role.ROLE_USER.toString())
		        .antMatchers("/u/**").hasAnyAuthority(ANONYMOUS_AUTHORITY, Role.ROLE_USER.toString())
		        .anyRequest().denyAll()
		        .and()
			      
		      // form login
		      .formLogin()
		        .loginPage("/login")
		        .defaultSuccessUrl("/u/sec/home")
		        .failureUrl("/login?login_error=1")
		        .and()
		        
		      // logout
		      .logout()
		        .logoutUrl("/logout")
		        .logoutSuccessUrl("/logoutSuccess.do")
		    ;
		    // @formatter:on
		}
	}

	/**
	 * API security rules, for stateless REST access.
	 */
	@Configuration
	@Order(2)
	public static class ApiWebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Autowired
		private DataSource dataSource;

		@Autowired
		private HandlerExceptionResolver handlerExceptionResolver;

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
			filter.setUserDetailsService(tokenUserDetailsService());
			filter.setAuthenticationEntryPoint(unauthorizedEntryPoint());

			SecurityTokenFilterSettings settings = tokenAuthenticationFilterSettings();
			filter.setMaxDateSkew(settings.getMaxDateSkew());
			filter.setMaxRequestBodySize((int) settings.getMaxRequestBodySize().toBytes());

			return filter;
		}

		@Bean
		public AuthenticationTokenService authenticationTokenService() {
			return new UserDetailsAuthenticationTokenService(tokenUserDetailsService());
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
		    http
		      // limit this configuration to specific paths
		      .requestMatchers()
		        .antMatchers("/api/**")
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
		      
		      .authorizeRequests()
		      	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		      	.antMatchers("/api/v1/sec/user/billing/**").hasAnyAuthority(BILLING_AUTHORITY)
		      	.antMatchers("/api/v1/sec/user/event/**").hasAnyAuthority(EVENT_AUTHORITY)
		      	.antMatchers("/api/v1/sec/user/export/**").hasAnyAuthority(EXPORT_AUTHORITY)
		      	.antMatchers("/api/v1/sec/user/import/**").hasAnyAuthority(IMPORT_AUTHORITY)
		        .antMatchers("/api/v1/sec/**").hasAnyAuthority(Role.ROLE_USER.toString())
		        .antMatchers("/api/v1/pub/**").permitAll()
		        .anyRequest().denyAll()
		    ;   
		    // @formatter:on
		}
	}

	/**
	 * Last set of security rules, for public resources else deny all others.
	 */
	@Configuration
	@Order(3)
	public static class PublicWebSecurityConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			// @formatter:off
		    http
		      // CSRF not needed for stateless calls
		      .csrf().disable()
		      
		      // make sure CORS honored
		      .cors().and()
		      
		      // no sessions
		      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
		      
		      .authorizeRequests()
		      	.antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		        .antMatchers(HttpMethod.GET, 
		        		"/", 
		        		"/error", 
		        		"/*.html",
		        		"/cert.*",
		        		"/css/**",
		        		"/fonts/**",
		        		"/img/**",
		        		"/js/**",
		        		"/js-lib/**",
		        		"/ping", 
		        		"/api/v1/pub/**").permitAll()
		        .antMatchers(HttpMethod.POST,
		        		"/associate.*").permitAll()
		        .anyRequest().denyAll();
		    // @formatter:on
		}

	}

}
