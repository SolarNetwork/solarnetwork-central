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
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.web.WebConstants;
import org.apache.commons.codec.binary.Base64;
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

/**
 * Authentication filter for "SolarNetworkWS" style authentication.
 * 
 * <p>
 * This authentication method has been modeled after the Amazon Web Service
 * authentication scheme used by the S3 service
 * (http://docs.amazonwebservices.com
 * /AmazonS3/latest/dev/S3_Authentication2.html)
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>authenticationDetailsSource</dt>
 * <dd>Defaults to {@link WebAuthenticationDetailsSource}.</dd>
 * 
 * <dt>userDetailsService</dt>
 * <dd>The details service, which must return users with valid SolarNetwork
 * usernames (email addresses) and plain-text authorization token secret
 * passwords via {@link UserDetails#getUsername()} and
 * {@link UserDetails#getPassword()}. After validating the request
 * authorization, this filter will authenticate the user with Spring Security.</dd>
 * 
 * <dt>maxDateSkew</dt>
 * <dd>The maximum amount of difference in the supplied HTTP {@code Date} (or
 * {@code X-SN-Date}) header value with the current time as reported by the
 * system. If this difference is exceeded, authorization fails.</dd>
 * 
 * <dt>entryPoint</dt>
 * <dd>The {@link UserAuthTokenAuthenticationEntryPoint} to use as the entry
 * point.</dd>
 * 
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class UserAuthTokenAuthenticationFilter extends GenericFilterBean implements Filter {

	/** The HTTP Authorization scheme used by this filter. */
	public static final String AUTHORIZATION_SCHEME = "SolarNetworkWS";

	private static final String AUTH_HEADER_PREFIX = AUTHORIZATION_SCHEME + " ";

	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private UserAuthTokenAuthenticationEntryPoint authenticationEntryPoint;
	private UserDetailsService userDetailsService;
	private long maxDateSkew = 15 * 60 * 1000; // 15 minutes default

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(userDetailsService, "A UserDetailsService is required");
		Assert.notNull(authenticationEntryPoint, "A UserAuthTokenAuthenticationEntryPoint is required");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		final String header = request.getHeader("Authorization");

		if ( header == null || !header.startsWith(AUTH_HEADER_PREFIX) ) {
			chain.doFilter(request, response);
			return;
		}

		log.debug("Digest Authorization header received from user agent: {}", header);

		AuthData data;
		try {
			data = new AuthData(request, header.substring(AUTH_HEADER_PREFIX.length()));
		} catch ( AuthenticationException e ) {
			fail(request, response, e);
			return;
		}

		UserDetails user = userDetailsService.loadUserByUsername(data.authToken);
		final String computedDigest = computeDigest(data, user.getPassword());
		if ( !computedDigest.equals(data.signatureDigest) ) {
			log.debug("Expected response: '{}' but received: '{}'", computedDigest, data.signatureDigest);
			SecurityContextHolder.getContext().setAuthentication(null);
			fail(request, response, new BadCredentialsException("Bad credentials."));
			return;
		}

		if ( !data.isDateValid() ) {
			log.debug("Request date '{}' diff too large: {}", data.date, data.dateSkew);
			fail(request, response, new BadCredentialsException("Date skew too large."));
			return;
		}

		log.debug("Authentication success for user: '{}'", user.getUsername());

		SecurityContextHolder.getContext().setAuthentication(
				createSuccessfulAuthentication(request, user));

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

	final String computeDigest(final AuthData data, final String password) {
		Mac hmacSha1;
		try {
			hmacSha1 = Mac.getInstance("HmacSHA1");
			hmacSha1.init(new SecretKeySpec(password.getBytes("UTF-8"), "HmacSHA1"));
			byte[] result = hmacSha1.doFinal(data.signature.getBytes("UTF-8"));
			return Base64.encodeBase64String(result).trim();
		} catch ( NoSuchAlgorithmException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		} catch ( InvalidKeyException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		}
	}

	private class AuthData {

		private final String authToken;
		private final String signatureDigest;
		private final String signature;
		private final Date date;
		private final long dateSkew;

		private AuthData(HttpServletRequest request, String headerValue) {
			String dateHeader = WebConstants.HEADER_DATE;
			long dateValue = request.getDateHeader(dateHeader);
			if ( dateValue < 0 ) {
				dateHeader = "Date";
				dateValue = request.getDateHeader(dateHeader);
				if ( dateValue < 0 ) {
					throw new BadCredentialsException("Missing or invalid HTTP Date header value");
				}
			}
			date = new Date(dateValue);
			int colonIdx = headerValue.indexOf(':');
			if ( colonIdx < 0 || colonIdx >= headerValue.length() ) {
				throw new BadCredentialsException("Invalid Authorization header value.");
			}
			authToken = headerValue.substring(0, colonIdx);
			signatureDigest = headerValue.substring(colonIdx + 1);

			StringBuilder buf = new StringBuilder(request.getMethod());
			buf.append("\n");
			buf.append(nullSafeHeaderValue(request, "Content-MD5")).append("\n");
			buf.append(nullSafeHeaderValue(request, "Content-Type")).append("\n");
			buf.append(nullSafeHeaderValue(request, dateHeader)).append("\n");
			buf.append(request.getRequestURI());
			signature = buf.toString();

			dateSkew = Math.abs(System.currentTimeMillis() - date.getTime());
		}

		private boolean isDateValid() {
			return dateSkew < maxDateSkew;
		}

	}

	private static String nullSafeHeaderValue(HttpServletRequest request, String headerName) {
		final String result = request.getHeader(headerName);
		return (result == null ? "" : result);
	}

	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public void setAuthenticationDetailsSource(
			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
		this.authenticationDetailsSource = authenticationDetailsSource;
	}

	public void setMaxDateSkew(long maxDateSkew) {
		this.maxDateSkew = maxDateSkew;
	}

	public void setAuthenticationEntryPoint(UserAuthTokenAuthenticationEntryPoint entryPoint) {
		this.authenticationEntryPoint = entryPoint;
	}

}
