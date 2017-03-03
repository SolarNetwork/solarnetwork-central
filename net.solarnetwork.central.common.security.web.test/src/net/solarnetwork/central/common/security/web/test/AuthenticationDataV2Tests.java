/* ==================================================================
 * AuthenticationDataV2Tests.java - 2/03/2017 12:29:20 PM
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

import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.computeHMACSHA256;
import static net.solarnetwork.central.common.security.web.test.SecurityWebTestUtils.iso8601Date;
import static net.solarnetwork.central.security.web.AuthenticationData.nullSafeHeaderValue;
import static net.solarnetwork.central.security.web.AuthenticationData.uriEncode;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.security.web.AuthenticationDataV2;
import net.solarnetwork.central.security.web.AuthenticationScheme;
import net.solarnetwork.central.security.web.SecurityHttpServletRequestWrapper;
import net.solarnetwork.central.security.web.WebConstants;

/**
 * Unit tests for the {@link AuthenticationDataV2} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuthenticationDataV2Tests {

	private static final String HTTP_HEADER_AUTH = "Authorization";
	private static final String HTTP_HEADER_HOST = "Host";
	private static final String TEST_HOST = "host.example.com";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_PASSWORD = "lsdjfpse9jfoeijfe09j";
	private static final long TEST_MAX_DATE_SKEW = 15 * 60 * 1000L;

	private static String[] lowercaseSortedArray(String[] headerNames) {
		String[] sortedHeaderNames = new String[headerNames.length];
		for ( int i = 0; i < headerNames.length; i++ ) {
			sortedHeaderNames[i] = headerNames[i].toLowerCase();
		}
		Arrays.sort(sortedHeaderNames);
		return sortedHeaderNames;
	}

	private static void appendHeaders(HttpServletRequest request, String[] headerNames,
			StringBuilder buf) {
		for ( String headerName : lowercaseSortedArray(headerNames) ) {
			buf.append(headerName).append(':').append(nullSafeHeaderValue(request, headerName).trim())
					.append('\n');
		}
	}

	private static void appendQueryParameters(HttpServletRequest request, StringBuilder buf) {
		Set<String> paramKeys = request.getParameterMap().keySet();
		if ( paramKeys.size() < 1 ) {
			buf.append('\n');
			return;
		}
		String[] keys = paramKeys.toArray(new String[paramKeys.size()]);
		Arrays.sort(keys);
		boolean first = true;
		for ( String key : keys ) {
			if ( first ) {
				first = false;
			} else {
				buf.append('&');
			}
			buf.append(uriEncode(key)).append('=').append(uriEncode(request.getParameter(key)));
		}
		buf.append('\n');
	}

	private static void appendSignedHeaderNames(String[] headerNames, StringBuilder buf) {
		boolean first = true;
		for ( String headerName : lowercaseSortedArray(headerNames) ) {
			if ( first ) {
				first = false;
			} else {
				buf.append(';');
			}
			buf.append(headerName);
		}
		buf.append('\n');
	}

	private static void appendContentSHA256(SecurityHttpServletRequestWrapper request, StringBuilder buf)
			throws IOException {
		byte[] digest = request.getContentSHA256();
		buf.append(digest == null ? WebConstants.EMPTY_STRING_SHA256_HEX : Hex.encodeHexString(digest));
	}

	private static String computeCanonicalRequestData(SecurityHttpServletRequestWrapper request,
			String[] headerNames) throws IOException {
		// 1: HTTP verb
		StringBuilder buf = new StringBuilder(request.getMethod()).append('\n');

		// 2: Canonical URI
		buf.append(request.getRequestURI()).append('\n');

		// 3: Canonical query string
		appendQueryParameters(request, buf);

		// 4: Canonical headers
		appendHeaders(request, headerNames, buf);

		// 5: Signed headers
		appendSignedHeaderNames(headerNames, buf);

		// 6: Content SHA256
		appendContentSHA256(request, buf);

		return buf.toString();

	}

	private static byte[] computeSigningKey(String secretKey, Date date) {
		/*- signing key is like:
		 
		HMACSHA256(HMACSHA256("SNWS2"+secretKey, "20160301"), "snws2_request")
		*/
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return computeHMACSHA256(
				computeHMACSHA256(AuthenticationScheme.V2.getSchemeName() + secretKey, sdf.format(date)),
				"snws2_request");
	}

	private static String computeSignatureData(String canonicalRequestData, Date date) {
		/*- signature data is like:
		 
		 	SNWS2-HMAC-SHA256\n
		 	20170301T120000Z\n
		 	Hex(SHA256(canonicalRequestData))
		*/
		return "SNWS2-HMAC-SHA256\n" + iso8601Date(date) + "\n"
				+ Hex.encodeHexString(DigestUtils.sha256(canonicalRequestData));
	}

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
	static String createAuthorizationHeaderV2Value(String token, String secret,
			MockHttpServletRequest request, Date date) throws IOException {
		return createAuthorizationHeaderV2Value(token, secret, request, date, null);
	}

	/**
	 * Create an {@code Authorization} HTTP header using the
	 * {@link AuthenticationScheme#V2} scheme.
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
	static String createAuthorizationHeaderV2Value(String authTokenId, String authTokenSecret,
			MockHttpServletRequest request, Date date, String contentType) throws IOException {
		return createAuthorizationHeaderV2Value(authTokenId, authTokenSecret, request, date, date,
				contentType);
	}

	/**
	 * Create an {@code Authorization} HTTP header using the
	 * {@link AuthenticationScheme#V2} scheme.
	 * 
	 * @param authTokenId
	 *        The auth token ID.
	 * @param authTokenSecret
	 *        The auth token secret.
	 * @param request
	 *        The HTTP request.
	 * @param date
	 *        The request date to use.
	 * @param signDate
	 *        The signing date to use. If not provided, the {@code date} value
	 *        will be used.
	 * @param contentType
	 *        The content type.
	 * @return The {@code Authorization} HTTP header value.
	 * @throws IOException
	 *         If an IO error occurs.
	 */
	static String createAuthorizationHeaderV2Value(String authTokenId, String authTokenSecret,
			MockHttpServletRequest request, Date date, Date signDate, String contentType)
			throws IOException {
		if ( request.getHeader(HTTP_HEADER_HOST) == null ) {
			request.addHeader(HTTP_HEADER_HOST, TEST_HOST);
		}
		List<String> headerNames = new ArrayList<String>(3);
		headerNames.add("Host");
		if ( request.getHeader("X-SN-Date") != null ) {
			headerNames.add("X-SN-Date");
		} else if ( request.getHeader("Date") != null ) {
			headerNames.add("Date");
		}
		if ( request.getHeader("Content-MD5") != null ) {
			headerNames.add("Content-MD5");
		}
		if ( request.getHeader("Content-Type") != null ) {
			headerNames.add("Content-Type");
		}
		if ( request.getHeader("Digest") != null ) {
			headerNames.add("Digest");
		}
		final String[] sortedHeaderNames = lowercaseSortedArray(
				headerNames.toArray(new String[headerNames.size()]));
		final SecurityHttpServletRequestWrapper secRequest = new SecurityHttpServletRequestWrapper(
				request, 1024);
		final byte[] signingKey = computeSigningKey(authTokenSecret, signDate != null ? signDate : date);
		final String signatureData = computeSignatureData(computeCanonicalRequestData(secRequest,
				headerNames.toArray(new String[headerNames.size()])), date);
		final String signature = Hex.encodeHexString(computeHMACSHA256(signingKey, signatureData));
		final StringBuilder buf = new StringBuilder(AuthenticationScheme.V2.getSchemeName());
		buf.append(' ');
		buf.append("Credential=").append(authTokenId);
		buf.append(",SignedHeaders=").append(StringUtils.arrayToDelimitedString(sortedHeaderNames, ";"));
		buf.append(",Signature=").append(signature);
		return buf.toString();
	}

	private AuthenticationDataV2 verifyRequest(HttpServletRequest request, String secretKey)
			throws IOException {
		AuthenticationDataV2 authData = new AuthenticationDataV2(
				new SecurityHttpServletRequestWrapper(request, 1024), request.getHeader(HTTP_HEADER_AUTH)
						.substring(AuthenticationScheme.V2.getSchemeName().length() + 1));
		Assert.assertTrue("The date skew is OK", authData.isDateValid(TEST_MAX_DATE_SKEW));
		String computedDigest = authData.computeSignatureDigest(secretKey);
		Assert.assertEquals(computedDigest, authData.getSignatureDigest());
		return authData;
	}

	@Test
	public void missingDate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD,
				request, new Date());
		try {
			new AuthenticationDataV2(new SecurityHttpServletRequestWrapper(request, 1024), authHeader);
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, "foobar", request, now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		AuthenticationDataV2 authData = new AuthenticationDataV2(
				new SecurityHttpServletRequestWrapper(request, 1024),
				authHeader.substring(AuthenticationScheme.V2.getSchemeName().length() + 1));
		Assert.assertTrue("The date skew is OK", authData.isDateValid(TEST_MAX_DATE_SKEW));
		String computedDigest = authData.computeSignatureDigest(TEST_PASSWORD);
		Assert.assertNotEquals(computedDigest, authData.getSignatureDigest());
	}

	@Test
	public void tooMuchSkew() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date(System.currentTimeMillis() - 16L * 60L * 1000L);
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		AuthenticationDataV2 authData = new AuthenticationDataV2(
				new SecurityHttpServletRequestWrapper(request, 1024),
				authHeader.substring(AuthenticationScheme.V2.getSchemeName().length() + 1));
		Assert.assertFalse("The date skew is too large.", authData.isDateValid(TEST_MAX_DATE_SKEW));
	}

	@Test
	public void simplePath() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}

	@Test
	public void simplePathSignedAcrossSevenDays() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		final Date now = cal.getTime();
		request.addHeader("Date", now);
		for ( int i = 0; i < 7; i++, cal.add(Calendar.DATE, -1) ) {
			String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
					now, cal.getTime(), null);
			request.addHeader(HTTP_HEADER_AUTH, authHeader);
			verifyRequest(request, TEST_PASSWORD);
		}
	}

	@Test
	public void simplePathSignedMoreThanSevenDays() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		final Date now = cal.getTime();
		cal.add(Calendar.DATE, -8);
		request.addHeader("Date", now);
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now, cal.getTime(), null);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		AuthenticationDataV2 authData = new AuthenticationDataV2(
				new SecurityHttpServletRequestWrapper(request, 1024),
				authHeader.substring(AuthenticationScheme.V2.getSchemeName().length() + 1));
		Assert.assertTrue("The date skew is OK", authData.isDateValid(TEST_MAX_DATE_SKEW));
		String computedDigest = authData.computeSignatureDigest(TEST_PASSWORD);
		Assert.assertNotEquals(computedDigest, authData.getSignatureDigest());
	}

	@Test
	public void simplePathWithXDate() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mock/path/here");
		final Date now = new Date();
		request.addHeader("X-SN-Date", now);
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now);
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now);
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now);
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now, "application/x-www-form-urlencoded; charset=UTF-8");
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now, contentType);
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
		String authHeader = createAuthorizationHeaderV2Value(TEST_AUTH_TOKEN, TEST_PASSWORD, request,
				now, contentType);
		request.addHeader(HTTP_HEADER_AUTH, authHeader);
		verifyRequest(request, TEST_PASSWORD);
	}
}
