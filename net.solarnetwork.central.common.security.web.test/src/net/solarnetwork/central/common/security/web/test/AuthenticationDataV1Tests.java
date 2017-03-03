/* ==================================================================
 * AuthenticationDataV1Tests.java - 3/03/2017 9:08:45 AM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.computeHMACSHA1;
import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.httpDate;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import net.solarnetwork.central.security.web.AuthenticationDataV1;
import net.solarnetwork.central.security.web.AuthenticationScheme;
import net.solarnetwork.central.security.web.SecurityHttpServletRequestWrapper;

/**
 * Test cases for the {@link AuthenticationDataV1} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthenticationDataV1Tests {

	private static final String HTTP_HEADER_AUTH = "Authorization";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_PASSWORD = "lsdjfpse9jfoeijfe09j";
	private static final long TEST_MAX_DATE_SKEW = 15 * 60 * 1000L;

	/**
	 * Create an {@code Authorization} HTTP header using the
	 * {@link AuthenticationScheme#V2} scheme and no body content.
	 * 
	 * @param authTokenId
	 *        The auth token ID.
	 * @param authTokenSecret
	 *        The auth token secret.
	 * @param request
	 *        The HTTP request.
	 * @param date
	 *        The date to use.
	 * @return The {@code Authorization} HTTP header value.
	 * @throws IOException
	 *         If an IO error occurs.
	 */
	static String createAuthorizationHeaderV1Value(String token, String secret,
			MockHttpServletRequest request, Date date) {
		return createAuthorizationHeaderV1Value(token, secret, request, date, null);
	}

	/**
	 * Create an {@code Authorization} HTTP header using the
	 * {@link AuthenticationScheme#V1} scheme.
	 * 
	 * @param authTokenId
	 *        The auth token ID.
	 * @param authTokenSecret
	 *        The auth token secret.
	 * @param request
	 *        The HTTP request.
	 * @param date
	 *        The date to use.
	 * @param contentType
	 *        The content type.
	 * @return The {@code Authorization} HTTP header value.
	 * @throws IOException
	 *         If an IO error occurs.
	 */
	static String createAuthorizationHeaderV1Value(String token, String secret,
			MockHttpServletRequest request, Date date, String contentType) {
		String contentMD5 = request.getHeader("Content-MD5");
		String msg = request.getMethod() + "\n" + (contentMD5 != null ? contentMD5 : "") + "\n"
				+ (contentType != null ? contentType : "") + "\n" + httpDate(date) + "\n"
				+ request.getRequestURI();
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
		final StringBuilder buf = new StringBuilder(AuthenticationScheme.V1.getSchemeName());
		buf.append(' ');
		buf.append(token).append(':')
				.append(Base64.encodeBase64String(computeHMACSHA1(secret, msg)).trim());
		return buf.toString();
	}

	private AuthenticationDataV1 verifyRequest(HttpServletRequest request, String secretKey)
			throws IOException {
		AuthenticationDataV1 authData = new AuthenticationDataV1(
				new SecurityHttpServletRequestWrapper(request, 1024), request.getHeader(HTTP_HEADER_AUTH)
						.substring(AuthenticationScheme.V1.getSchemeName().length() + 1));
		Assert.assertTrue("The date skew is OK", authData.isDateValid(TEST_MAX_DATE_SKEW));
		String computedDigest = authData.computeSignatureDigest(secretKey);
		Assert.assertEquals(computedDigest, authData.getSignatureDigest());
		return authData;
	}

	@Test
	public void missingDate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				new Date());
		try {
			new AuthenticationDataV1(new SecurityHttpServletRequestWrapper(request, 1024), authHeader);
			Assert.fail("Should have thrown BadCredentialsException");
		} catch ( BadCredentialsException e ) {
			Assert.assertEquals("Missing or invalid HTTP Date header value", e.getMessage());
		}
	}

	@Test
	public void badPassword() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, "foobar", request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		AuthenticationDataV1 authData = new AuthenticationDataV1(
				new SecurityHttpServletRequestWrapper(request, 1024),
				authHeader.substring(AuthenticationScheme.V1.getSchemeName().length() + 1));
		Assert.assertTrue("The date skew is OK", authData.isDateValid(TEST_MAX_DATE_SKEW));
		String computedDigest = authData.computeSignatureDigest(TEST_PASSWORD);
		Assert.assertNotEquals(computedDigest, authData.getSignatureDigest());
	}

	@Test
	public void tooMuchSkew() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		AuthenticationDataV1 authData = new AuthenticationDataV1(
				new SecurityHttpServletRequestWrapper(request, 1024),
				authHeader.substring(AuthenticationScheme.V1.getSchemeName().length() + 1));
		Assert.assertFalse("The date skew is too large.", authData.isDateValid(TEST_MAX_DATE_SKEW));
	}

	@Test
	public void simplePath() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void simplePathWithXDate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("X-SN-Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void pathWithQueryParams() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void pathWithQueryParamsNeedingURIEscape() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("/foo/bar", "bam!");
		params.put("bar.bim[1][blah]", "foo yes!");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void contentType() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		Map<String, String> params = new LinkedHashMap<String, String>();
		params.put("foo", "bar");
		params.put("bar", "foo");
		params.put("zog", "dog");
		request.setParameters(params);
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now,
				"application/x-www-form-urlencoded; charset=UTF-8");
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void contentMD5Hex() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "9bb58f26192e4ba00f01e2e7b136bbd8";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now,
				contentType);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void contentMD5Base64() throws ServletException, IOException {
		final String contentType = "application/json; charset=UTF-8";
		final String content = "{\"foo\":\"bar\"}";
		final String contentMD5 = "m7WPJhkuS6APAeLnsTa72A==";
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/path/here");
		request.setContentType(contentType);
		request.setContent(content.getBytes("UTF-8"));
		request.addHeader("Content-MD5", contentMD5);
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV1Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request, now,
				contentType);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}
}
