/* ==================================================================
 * SecurityTokenAuthenticationFilter.java - Nov 26, 2012 11:01:46 AM
 * 
 * Copyright 2012 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.SecurityPolicy.INVERTED_PATH_MATCH_PREFIX;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.transaction.TransactionException;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.config.SecurityTokenFilterSettings;
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
 * @version 1.7
 */
public class SecurityTokenAuthenticationFilter extends OncePerRequestFilter implements Filter {

	/** The fixed length of the auth token. */
	public static final int AUTH_TOKEN_LENGTH = 20;

	/**
	 * The default value for the {@code maxRequestBodySize} property.
	 * 
	 * @since 1.3
	 */
	public static final int DEFAULT_MAX_REQUEST_BODY_SIZE = 65535;

	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private SecurityTokenAuthenticationEntryPoint authenticationEntryPoint;
	private UserDetailsService userDetailsService;
	private final PathMatcher pathMatcher;
	private final String pathMatcherPrefixStrip;
	private final SecurityTokenFilterSettings settings;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Default constructor.
	 */
	public SecurityTokenAuthenticationFilter() {
		this(null, null, null);
	}

	/**
	 * Construct with a {@link PathMatcher}.
	 * 
	 * @param pathMatcher
	 *        the matcher to use, or {@literal null} if not supported
	 * @param pathMatcherPrefixStrip
	 *        a path prefix to strip from
	 *        {@link HttpServletRequest#getRequestURI()} <i>after</i> any
	 *        {@link HttpServletRequest#getContextPath()} has been removed,
	 *        before comparing paths, or {@literal null} to not strip any prefix
	 * @since 1.5
	 */
	public SecurityTokenAuthenticationFilter(PathMatcher pathMatcher, String pathMatcherPrefixStrip) {
		this(pathMatcher, pathMatcherPrefixStrip, null);
	}

	/**
	 * Construct with a {@link PathMatcher}.
	 * 
	 * @param pathMatcher
	 *        the matcher to use, or {@literal null} if not supported
	 * @param pathMatcherPrefixStrip
	 *        a path prefix to strip from
	 *        {@link HttpServletRequest#getRequestURI()} <i>after</i> any
	 *        {@link HttpServletRequest#getContextPath()} has been removed,
	 *        before comparing paths, or {@literal null} to not strip any prefix
	 * @param settings,
	 *        or {@literal null} to create a default instance
	 * @since 1.7
	 */
	public SecurityTokenAuthenticationFilter(PathMatcher pathMatcher, String pathMatcherPrefixStrip,
			SecurityTokenFilterSettings settings) {
		super();
		this.pathMatcher = pathMatcher;
		this.pathMatcherPrefixStrip = pathMatcherPrefixStrip;
		this.settings = (settings != null ? settings : new SecurityTokenFilterSettings());
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(userDetailsService, "A UserDetailsService is required");
		Assert.notNull(authenticationEntryPoint, "A SecurityTokenAuthenticationEntryPoint is required");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		SecurityHttpServletRequestWrapper request = new SecurityHttpServletRequestWrapper(req,
				(int) settings.getMaxRequestBodySize().toBytes(), true,
				(int) settings.getMinimumCompressLength().toBytes(),
				settings.getCompressibleContentTypePattern(),
				(int) settings.getMinimumSpoolLength().toBytes(), settings.getSpoolDirectory());
		HttpServletResponse response = res;

		AuthenticationData data;
		try {
			data = AuthenticationDataFactory.authenticationDataForAuthorizationHeader(request);
		} catch ( net.solarnetwork.web.security.SecurityException e ) {
			deny(request, response, new MaxUploadSizeExceededException(
					(int) settings.getMaxRequestBodySize().toBytes(), e));
			return;
		} catch ( SecurityException e ) {
			deny(request, response, e);
			return;
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
			log.debug("Auth token [{}] exception: {}", data.getAuthTokenId(), e.getMessage());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		} catch ( DataAccessException | TransactionException e ) {
			log.debug("Auth token [{}] transient DAO exception: {}", data.getAuthTokenId(),
					e.getMessage());
			failDao(request, response, e);
			return;
		} catch ( Exception e ) {
			log.debug("Auth token [{}] exception: {}", data.getAuthTokenId(), e.getMessage());
			fail(request, response,
					new AuthenticationServiceException("Unable to verify credentials", e));
			return;
		}

		if ( user instanceof SecurityToken ) {
			SecurityPolicy policy = ((SecurityToken) user).getPolicy();
			if ( policy != null && !policy.isValidAt(Instant.now()) ) {
				fail(request, response, new CredentialsExpiredException("Expired token"));
				return;
			}
			if ( !isValidApiPath(request, policy) ) {
				fail(request, response, new BadCredentialsException("Access denied"));
				return;
			}
		}

		final String computedDigest = data.computeSignatureDigest(user.getPassword());
		if ( !computedDigest.equals(data.getSignatureDigest()) ) {
			log.debug("Expected response: [{}] but received: [{}]", computedDigest,
					data.getSignatureDigest());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		}

		if ( !data.isDateValid(settings.getMaxDateSkew()) ) {
			log.debug("Request date [{}] diff too large: {}", data.getDate(), data.getDateSkew());
			fail(request, response, new BadCredentialsException("Date skew too large"));
			return;
		}

		log.debug("Authentication success for user: [{}]", user.getUsername());

		SecurityContextHolder.getContext()
				.setAuthentication(createSuccessfulAuthentication(request, user));

		chain.doFilter(request, response);
	}

	private boolean isValidApiPath(final HttpServletRequest request, final SecurityPolicy policy) {
		Set<String> apiPaths = (policy != null ? policy.getApiPaths() : null);
		if ( apiPaths == null || apiPaths.isEmpty() ) {
			return true;
		} else if ( request == null ) {
			return false;
		}
		String path = request.getRequestURI();
		if ( path == null ) {
			return false;
		}
		String ctxPath = request.getContextPath();
		if ( ctxPath != null && !ctxPath.isEmpty() ) {
			path = path.substring(ctxPath.length());
		}
		if ( pathMatcherPrefixStrip != null && !pathMatcherPrefixStrip.isEmpty()
				&& path.startsWith(pathMatcherPrefixStrip) ) {
			path = path.substring(pathMatcherPrefixStrip.length());
		}
		for ( String allowedPath : apiPaths ) {
			if ( allowedPath == null || allowedPath.isEmpty() ) {
				continue;
			}
			final boolean inverted;
			if ( allowedPath.startsWith(INVERTED_PATH_MATCH_PREFIX) ) {
				inverted = true;
				allowedPath = allowedPath.substring(INVERTED_PATH_MATCH_PREFIX.length());
			} else {
				inverted = false;
			}
			boolean match;
			if ( pathMatcher != null ) {
				match = pathMatcher.match(allowedPath, path);
			} else {
				match = allowedPath.equals(path);
			}
			if ( inverted ) {
				match = !match;
			}
			if ( match ) {
				return true;
			}
		}
		return false;
	}

	private Authentication createSuccessfulAuthentication(HttpServletRequest request, UserDetails user) {
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(user,
				user.getPassword(), user.getAuthorities());
		authRequest.eraseCredentials();
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
		return authRequest;
	}

	private void fail(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		authenticationEntryPoint.commence(request, response, failed);
	}

	private void deny(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			Exception e) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		String msg = e.getMessage();
		if ( msg == null ) {
			msg = "Access denied.";
		}
		authenticationEntryPoint.handle(request, response, new AccessDeniedException(msg, e));
	}

	private void failDao(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			Exception failed) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		authenticationEntryPoint.handleTransientResourceException(request, response, failed);
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
		this.settings.setMaxDateSkew(maxDateSkew);
	}

	/**
	 * The {@link SecurityTokenAuthenticationEntryPoint} to use as the entry
	 * point.
	 * 
	 * @param entryPoint
	 *        the entry point to use
	 */
	public void setAuthenticationEntryPoint(SecurityTokenAuthenticationEntryPoint entryPoint) {
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
		this.settings.setMaxRequestBodySize(DataSize.ofBytes(maxRequestBodySize));
	}

	/**
	 * Get the filter settings.
	 * 
	 * @return the settings, never {@literal null}
	 * @since 1.7
	 */
	public SecurityTokenFilterSettings getSettings() {
		return settings;
	}

}
