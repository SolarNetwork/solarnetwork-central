/* ==================================================================
 * UserAuthTokenAuthenticationFilterTest.java - Dec 13, 2012 6:08:36 AM
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

package net.solarnetwork.central.common.security.web.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.security.web.UserAuthTokenAuthenticationEntryPoint;
import net.solarnetwork.central.security.web.UserAuthTokenAuthenticationFilter;
import org.apache.commons.codec.binary.Base64;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Unit tests for the {@link UserAuthTokenAuthenticationFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
public class UserAuthTokenAuthenticationFilterTest {

	private static final String HTTP_HEADER_AUTH = "Authorization";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_PASSWORD = "lsdjfpse9jfoeijfe09j";

	private FilterChain filterChain;
	private MockHttpServletResponse response;
	private UserDetailsService userDetailsService;
	private UserAuthTokenAuthenticationEntryPoint entryPoint;
	private UserAuthTokenAuthenticationFilter filter;
	private User userDetails;

	@Before
	public void setup() {
		filterChain = EasyMock.createMock(FilterChain.class);
		response = new MockHttpServletResponse();
		userDetailsService = EasyMock.createMock(UserDetailsService.class);
		entryPoint = new UserAuthTokenAuthenticationEntryPoint();
		List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
		roles.add(new SimpleGrantedAuthority("ROLE_TEST"));
		userDetails = new User(TEST_AUTH_TOKEN, TEST_PASSWORD, roles);
		filter = new UserAuthTokenAuthenticationFilter();
		filter.setUserDetailsService(userDetailsService);
		filter.setAuthenticationEntryPoint(entryPoint);
	}

	@Test
	public void noAuthorizationHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		filterChain.doFilter(same(request), same(response));
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
	}

	final String computeDigest(final String msg, final String password) {
		Mac hmacSha1;
		try {
			hmacSha1 = Mac.getInstance("HmacSHA1");
			hmacSha1.init(new SecretKeySpec(password.getBytes("UTF-8"), "HmacSHA1"));
			byte[] result = hmacSha1.doFinal(msg.getBytes("UTF-8"));
			return Base64.encodeBase64String(result).trim();
		} catch ( NoSuchAlgorithmException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		} catch ( InvalidKeyException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error loading HmaxSHA1 crypto function", e);
		}
	}

	private String httpDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	private String createAuthorizationHeaderValue(String token, String secret,
			MockHttpServletRequest request, Date date) {
		String msg = request.getMethod() + "\n\n\n" + httpDate(date) + "\n" + request.getRequestURI();
		String[] keys = request.getParameterMap().keySet().toArray(new String[0]);
		Arrays.sort(keys);
		boolean first = true;
		for ( String key : keys ) {
			if ( first ) {
				msg += '?';
				first = false;
			} else {
				msg += '&';
			}
			msg += key + '=' + request.getParameter(key);
		}
		return token + ':' + computeDigest(msg, secret);
	}

	private void setupAuthorizationHeader(MockHttpServletRequest request, String value) {
		request.addHeader(HTTP_HEADER_AUTH, UserAuthTokenAuthenticationFilter.AUTHORIZATION_SCHEME + ' '
				+ value);
	}

	@Test
	public void invalidScheme() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		filterChain.doFilter(same(request), same(response));
		replay(filterChain, userDetailsService);
		request.addHeader(
				HTTP_HEADER_AUTH,
				"FooScheme "
						+ createAuthorizationHeaderValue(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
								new Date()));
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);

	}

	private void validateUnauthorizedResponse(String expectedMessage) {
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
		assertEquals(UserAuthTokenAuthenticationFilter.AUTHORIZATION_SCHEME,
				response.getHeader("WWW-Authenticate"));
		assertNotNull(response.getErrorMessage());
		assertTrue("Error message must match [" + expectedMessage + "]", response.getErrorMessage()
				.matches(expectedMessage));
		assertEquals(expectedMessage, response.getErrorMessage());
	}

	@Test
	public void missingDate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		replay(filterChain, userDetailsService);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderValue(TEST_AUTH_TOKEN, TEST_PASSWORD, request, new Date()));
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse("Missing or invalid HTTP Date header value");
	}

	private void validateAuthentication() {
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(auth);
		assertEquals(TEST_AUTH_TOKEN, auth.getName());
	}

	@Test
	public void badPassword() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderValue(TEST_AUTH_TOKEN, "foobar", request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse("Bad credentials");
	}

	@Test
	public void tooMuchSkew() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderValue(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse("Date skew too large");
	}

	@Test
	public void simplePath() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderValue(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(same(request), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void pathWithQueryParams() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderValue(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(same(request), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

}
