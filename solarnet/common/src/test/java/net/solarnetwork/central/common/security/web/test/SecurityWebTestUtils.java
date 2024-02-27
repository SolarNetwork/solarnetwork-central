/* ==================================================================
 * SecurityWebTestUtils.java - 2/03/2017 12:37:16 PM
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

import static net.solarnetwork.web.jakarta.security.AuthenticationData.nullSafeHeaderValue;
import static net.solarnetwork.web.jakarta.security.AuthenticationUtils.uriEncode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import net.solarnetwork.web.jakarta.security.AuthenticationScheme;
import net.solarnetwork.web.jakarta.security.SecurityHttpServletRequestWrapper;
import net.solarnetwork.web.jakarta.security.WebConstants;

/**
 * Utilities for unit tests.
 * 
 * @author matt
 * @version 1.1
 */
public final class SecurityWebTestUtils {

	private static final Logger log = LoggerFactory.getLogger(SecurityWebTestUtils.class);

	public static final String HTTP_HEADER_HOST = "Host";
	public static final String TEST_HOST = "host.example.com";

	public static byte[] computeHMACSHA1(final String password, final String msg) {
		return computeDigest(password, msg, "HmacSHA1");
	}

	public static byte[] computeHMACSHA256(final String password, final String msg) {
		return computeDigest(password, msg, "HmacSHA256");
	}

	public static byte[] computeHMACSHA256(final byte[] password, final String msg) {
		try {
			return computeDigest(password, msg.getBytes("UTF-8"), "HmacSHA256");
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error loading HmacSHA1 crypto function", e);
		}
	}

	public static byte[] computeDigest(final String password, final String msg, String alg) {
		try {
			return computeDigest(password.getBytes("UTF-8"), msg.getBytes("UTF-8"), alg);
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error encoding secret or message for crypto function", e);
		}
	}

	public static byte[] computeDigest(final byte[] password, final byte[] msg, String alg) {
		Mac hmacSha1;
		try {
			hmacSha1 = Mac.getInstance(alg);
			hmacSha1.init(new SecretKeySpec(password, alg));
			byte[] result = hmacSha1.doFinal(msg);
			return result;
		} catch ( NoSuchAlgorithmException e ) {
			throw new SecurityException("Error loading " + alg + " crypto function", e);
		} catch ( InvalidKeyException e ) {
			throw new SecurityException("Error loading " + alg + " crypto function", e);
		}
	}

	public static String httpDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	public static String iso8601Date(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	private static String[] lowercaseSortedArray(String[] headerNames) {
		String[] sortedHeaderNames = new String[headerNames.length];
		for ( int i = 0; i < headerNames.length; i++ ) {
			sortedHeaderNames[i] = headerNames[i].toLowerCase();
		}
		Arrays.sort(sortedHeaderNames);
		return sortedHeaderNames;
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

		String result = buf.toString();
		log.debug("Canonical req data: \n{}", result);
		return result;

	}

	private static byte[] computeSigningKey(String secretKey, Date date) {
		/*- signing key is like:
		 
		HMACSHA256(HMACSHA256("SNWS2"+secretKey, "20160301"), "snws2_request")
		*/
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String sigDate = sdf.format(date);
		log.debug("Sign date: " + sigDate);
		return computeHMACSHA256(
				computeHMACSHA256(AuthenticationScheme.V2.getSchemeName() + secretKey, sigDate),
				"snws2_request");
	}

	private static String computeSignatureData(String canonicalRequestData, Date date) {
		/*- signature data is like:
		 
		 	SNWS2-HMAC-SHA256\n
		 	20170301T120000Z\n
		 	Hex(SHA256(canonicalRequestData))
		*/
		String result = "SNWS2-HMAC-SHA256\n" + iso8601Date(date) + "\n"
				+ Hex.encodeHexString(DigestUtils.sha256(canonicalRequestData));
		log.debug("Signature data: \n{}", result);
		return result;
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

}
