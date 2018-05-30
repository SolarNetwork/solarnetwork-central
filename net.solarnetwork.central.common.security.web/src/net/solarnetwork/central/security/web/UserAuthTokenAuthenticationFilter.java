/* ==================================================================
 * PBKDF2AuthenticationFilter.java - Nov 26, 2012 11:01:46 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.web.security.AuthenticationData;
import net.solarnetwork.web.security.AuthenticationDataFactory;
import net.solarnetwork.web.security.SecurityHttpServletRequestWrapper;

/**
 * Authentication filter for "SolarNetworkWS" style authentication.
 * 
 * <p>
 * This authentication method has been modeled after the Amazon Web Service
 * authentication scheme used by the S3 service
 * (http://docs.amazonwebservices.com/AmazonS3/latest/dev/S3_Authentication2.html).
 * The auth token is fixed at {@link #AUTH_TOKEN_LENGTH} characters. All query
 * parameters (GET or POST) are added to the request path in the message;
 * parameters are sorted lexicographically and then their keys and
 * <em>first</em> value is appended to the path following a {@code ?} character
 * and separated by a {@code &} character.
 * </p>
 * 
 * @author matt
 * @version 1.4
 */
public class UserAuthTokenAuthenticationFilter extends GenericFilterBean implements Filter {

	/** The fixed length of the auth token. */
	public static final int AUTH_TOKEN_LENGTH = 20;

	/**
	 * The default value for the {@code maxRequestBodySize} property.
	 * 
	 * @since 1.3
	 */
	public static final int DEFAULT_MAX_REQUEST_BODY_SIZE = 65535;

	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private UserAuthTokenAuthenticationEntryPoint authenticationEntryPoint;
	private UserDetailsService userDetailsService;
	private long maxDateSkew = 15 * 60 * 1000; // 15 minutes default
	private int maxRequestBodySize = 65535;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(userDetailsService, "A UserDetailsService is required");
		Assert.notNull(authenticationEntryPoint, "A UserAuthTokenAuthenticationEntryPoint is required");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		SecurityHttpServletRequestWrapper request = new SecurityHttpServletRequestWrapper(
				(HttpServletRequest) req, maxRequestBodySize);
		HttpServletResponse response = (HttpServletResponse) res;

		AuthenticationData data;
		try {
			data = AuthenticationDataFactory.authenticationDataForAuthorizationHeader(request);
		} catch ( AuthenticationException e ) {
			fail(request, response, e);
			return;
		}

		if ( data == null ) {
			log.trace("Missing Authorization header or unsupported scheme");
			chain.doFilter(request, response);
			return;
		}

		final UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(data.getAuthTokenId());
		} catch ( AuthenticationException e ) {
			log.debug("Auth token {} exception: {}", data.getAuthTokenId(), e.getMessage());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		}

		if ( user instanceof SecurityToken ) {
			SecurityPolicy policy = ((SecurityToken) user).getPolicy();
			if ( policy != null && !policy.isValidAt(System.currentTimeMillis()) ) {
				fail(request, response, new BadCredentialsException("Expired token"));
				return;
			}
		}

		final String computedDigest = data.computeSignatureDigest(user.getPassword());
		if ( !computedDigest.equals(data.getSignatureDigest()) ) {
			log.debug("Expected response: '{}' but received: '{}'", computedDigest,
					data.getSignatureDigest());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		}

		if ( !data.isDateValid(maxDateSkew) ) {
			log.debug("Request date '{}' diff too large: {}", data.getDate(), data.getDateSkew());
			fail(request, response, new BadCredentialsException("Date skew too large"));
			return;
		}

		log.debug("Authentication success for user: '{}'", user.getUsername());

		SecurityContextHolder.getContext()
				.setAuthentication(createSuccessfulAuthentication(request, user));

		chain.doFilter(request, response);
	}

	private Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user,
				user.getPassword(), user.getAuthorities());
		authRequest.eraseCredentials();
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
		return authRequest;
	}

	private void fail(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		authenticationEntryPoint.commence(request, response, failed);
	}

	/**
	 * Set the details service, which must return users with valid SolarNetwork
	 * usernames (email addresses) and plain-text authorization token secret
	 * passwords via {@link UserDetails#getUsername()} and
	 * {@link UserDetails#getPassword()}.
	 * 
	 * <p>
	 * After validating the request authorization, this filter will authenticate
	 * the user with Spring Security.
	 * </p>
	 * 
	 * @param userDetailsService
	 *        the service
	 */
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	/**
	 * Set the details source to use.
	 * 
	 * <p>
	 * Defaults to a {@link WebAuthenticationDetailsSource}.
	 * </p>
	 * 
	 * @param authenticationDetailsSource
	 *        the source to use
	 */
	public void setAuthenticationDetailsSource(
			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
		this.authenticationDetailsSource = authenticationDetailsSource;
	}

	/**
	 * Set the maximum amount of difference in the supplied HTTP {@code Date}
	 * (or {@code X-SN-Date}) header value with the current time as reported by
	 * the system.
	 * 
	 * <p>
	 * If this difference is exceeded, authorization fails.
	 * </p>
	 * 
	 * @param maxDateSkew
	 *        the maximum allowed date skew
	 */
	public void setMaxDateSkew(long maxDateSkew) {
		this.maxDateSkew = maxDateSkew;
	}

	/**
	 * The {@link UserAuthTokenAuthenticationEntryPoint} to use as the entry
	 * point.
	 * 
	 * @param entryPoint
	 *        the entry point to use
	 */
	public void setAuthenticationEntryPoint(UserAuthTokenAuthenticationEntryPoint entryPoint) {
		this.authenticationEntryPoint = entryPoint;
	}

	/**
	 * Set the maximum allowed request body size.
	 * 
	 * @param maxRequestBodySize
	 *        the maximum request body size allowed
	 * @since 1.3
	 */
	public void setMaxRequestBodySize(int maxRequestBodySize) {
		this.maxRequestBodySize = maxRequestBodySize;
	}

}
