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

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.createAuthorizationHeaderV1Value;
import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.createAuthorizationHeaderV2Value;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.web.UserAuthTokenAuthenticationEntryPoint;
import net.solarnetwork.central.security.web.UserAuthTokenAuthenticationFilter;
import net.solarnetwork.web.security.AuthenticationScheme;

/**
 * Unit tests for the {@link UserAuthTokenAuthenticationFilter} class.
 * 
 * @author matt
 * @version 2.0
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

	private void setupAuthorizationHeader(MockHttpServletRequest request, String value) {
		request.addHeader(HTTP_HEADER_AUTH, value);
	}

	private void validateAuthentication() {
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(auth);
		assertEquals(TEST_AUTH_TOKEN, auth.getName());
	}

	private void validateUnauthorizedResponse(AuthenticationScheme scheme, String expectedMessage) {
		assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
		assertEquals(scheme.getSchemeName(), response.getHeader("WWW-Authenticate"));
		assertNotNull(response.getErrorMessage());
		assertTrue("Error message [" + response.getErrorMessage() + "] must match [" + expectedMessage
				+ "]", response.getErrorMessage().matches(expectedMessage));
		assertEquals(expectedMessage, response.getErrorMessage());
	}

	@Before
	public void setup() {
		filterChain = EasyMock.createMock(FilterChain.class);
		response = new MockHttpServletResponse();
		userDetailsService = EasyMock.createMock(UserDetailsService.class);
		entryPoint = new UserAuthTokenAuthenticationEntryPoint();
		List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
		roles.add(new SimpleGrantedAuthority("ROLE_TEST"));
		userDetails = new User(TEST_AUTH_TOKEN, TEST_PASSWORD, roles);
		filter = new UserAuthTokenAuthenticationFilter(new AntPathMatcher(), "/mock");
		filter.setUserDetailsService(userDetailsService);
		filter.setAuthenticationEntryPoint(entryPoint);
	}

	@Test
	public void noAuthorizationHeader() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
	}

	@Test
	public void invalidScheme() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		replay(filterChain, userDetailsService);
		setupAuthorizationHeader(request, "FooScheme ABC:DOEIJLSIEWOSEIHLSISYEOIHEOIJ");
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);

	}

	@Test
	public void missingDateV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		replay(filterChain, userDetailsService);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, new Date()));
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V1,
				"Missing or invalid HTTP Date header value");
	}

	@Test
	public void missingDateV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		replay(filterChain, userDetailsService);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, new Date()));
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2,
				"Missing or invalid HTTP Date header value");
	}

	@Test
	public void badPasswordV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, "foobar", request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V1, "Bad credentials");
	}

	@Test
	public void badPasswordV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, "foobar", request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Bad credentials");
	}

	@Test
	public void tooMuchSkewV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V1, "Date skew too large");
	}

	@Test
	public void tooMuchSkewV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Date skew too large");
	}

	@Test
	public void simplePathV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void simplePathV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void tokenWithEqualSignV1() throws ServletException, IOException {
		final String tokenId = "2^=3^rz}fgu0twxj;*fb";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(tokenId, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(tokenId)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void tokenWithEqualSignV2() throws ServletException, IOException {
		final String tokenId = "2^=3^rz}fgu0twxj;*fb";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(tokenId, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(tokenId)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void simplePathWithXDateV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("X-SN-Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void simplePathWithXDateV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("X-SN-Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void pathWithQueryParamsV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void pathWithQueryParamsV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentTypeV1() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, "application/x-www-form-urlencoded; charset=UTF-8"));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentTypeV2() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		Map<String, String> params = new HashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, "application/x-www-form-urlencoded; charset=UTF-8"));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentMD5HexV1() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "9bb58f26192e4ba00f01e2e7b136bbd8";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentMD5HexV2() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "9bb58f26192e4ba00f01e2e7b136bbd8";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void invalidContentMD5HexV1() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "9bb58f26192e4ba00f01e2e7b136bbFF";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		validateUnauthorizedResponse(AuthenticationScheme.V1, "Content md5 digest value mismatch");
		verify(filterChain, userDetailsService);
	}

	@Test
	public void invalidContentMD5HexV2() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "9bb58f26192e4ba00f01e2e7b136bbFF";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Content md5 digest value mismatch");
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentMD5Base64V1() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "m7WPJhkuS6APAeLnsTa72A==";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void contentMD5Base64V2() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "m7WPJhkuS6APAeLnsTa72A==";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void digestSHA256V2() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String digestSHA256 = "eji/gfOD9pQzrW6QDTWz4jhVk/dqe3q11DVbi6Qe4ks=";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Digest", "sha-256=" + digestSHA256);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(userDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
		verify(filterChain, userDetailsService);
	}

	@Test
	public void invalidDigestSHA256V2() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String digestSHA256 = "Ix2SImWvBXHmqXTuPAaDHz16KeaOlIokZObv6cU+Ie8=";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Digest", "sha-256=" + digestSHA256);
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request, createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN,
				TEST_PASSWORD, request, now, contentType));
		request.setContent(content.getBytes("UTF-8")); // reset InputStream
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Content sha-256 digest value mismatch");
		verify(filterChain, userDetailsService);
	}

	@Test
	public void expiredToken() throws ServletException, IOException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNotAfter(Instant.now().minusSeconds(1)).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.ReadNodeData, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Expired token");
	}

	@Test
	public void apiPathV2SimpleAllowed() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(singleton("/path/**")).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		request.setPathInfo("/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void apiPathV2SimpleDenied() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withApiPaths(singleton("/foo/**"))
				.build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		request.setPathInfo("/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Access denied");
	}

	@Test
	public void apiPathV2InvertedAllowed() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(singleton("!/foo/**")).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		request.setPathInfo("/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void apiPathV2InvertedDenied() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(singleton("!/path/**")).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		request.setPathInfo("/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Access denied");
	}

	@Test
	public void apiPathV2MultiWithInvertedAllowed() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(new LinkedHashSet<>(asList("/path/do/thing", "!/path/do/**"))).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/do/thing");
		request.setPathInfo("/mock/path/do/thing");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void apiPathV2MultiWithInvertedReversedOrderAllowed() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(new LinkedHashSet<>(asList("!/path/do/**", "/path/do/thing"))).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/do/thing");
		request.setPathInfo("/mock/path/do/thing");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		filterChain.doFilter(anyObject(HttpServletRequest.class), same(response));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		validateAuthentication();
	}

	@Test
	public void apiPathV2MultiWithInvertedDenied() throws ServletException, IOException {
		// given
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withApiPaths(new LinkedHashSet<>(asList("!/path/do/**", "/path/do/thing"))).build();
		AuthenticatedToken tokenDetails = new AuthenticatedToken(this.userDetails,
				SecurityTokenType.User, -1L, policy);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/do/other");
		request.setPathInfo("/mock/path/do/other");
		final Date now = new Date();
		request.addHeader("Date", now);
		setupAuthorizationHeader(request,
				createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now));
		expect(userDetailsService.loadUserByUsername(TEST_AUTH_TOKEN)).andReturn(tokenDetails);

		// when
		replay(filterChain, userDetailsService);
		filter.doFilter(request, response, filterChain);

		// then
		verify(filterChain, userDetailsService);
		validateUnauthorizedResponse(AuthenticationScheme.V2, "Access denied");
	}

}
