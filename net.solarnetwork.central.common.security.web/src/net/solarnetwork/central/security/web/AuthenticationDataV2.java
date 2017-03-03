/* ==================================================================
 * AuthenticationDataV2.java - 1/03/2017 8:41:00 PM
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

package net.solarnetwork.central.security.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.BadCredentialsException;
import net.solarnetwork.util.StringUtils;

/**
 * Version 2 authentication token scheme based on HMAC-SHA256.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public class AuthenticationDataV2 extends AuthenticationData {

	public static final String TOKEN_COMPONENT_KEY_CREDENTIAL = "Credential";
	public static final String TOKEN_COMPONENT_KEY_SIGNED_HEADERS = "SignedHeaders";
	public static final String TOKEN_COMPONENT_KEY_SIGNATURE = "Signature";

	private final String authTokenId;
	private final String signatureDigest;
	private final String signatureData;
	private final Set<String> signedHeaderNames;
	private final String[] sortedSignedHeaderNames;

	public AuthenticationDataV2(SecurityHttpServletRequestWrapper request, String headerValue)
			throws IOException {
		super(AuthenticationScheme.V2, request, headerValue);

		// the header must be in the form Credential=TOKEN-ID,SignedHeaders=x;y;z,Signature=HMAC-SHA1-SIGNATURE

		Map<String, String> tokenData = StringUtils.commaDelimitedStringToMap(headerValue);
		if ( AUTH_TOKEN_ID_LENGTH + 2 >= headerValue.length() ) {
			throw new BadCredentialsException("Invalid Authorization header value");
		}
		authTokenId = tokenData.get(TOKEN_COMPONENT_KEY_CREDENTIAL);
		signatureDigest = tokenData.get(TOKEN_COMPONENT_KEY_SIGNATURE);

		String signedHeaders = tokenData.get(TOKEN_COMPONENT_KEY_SIGNED_HEADERS);
		signedHeaderNames = StringUtils.delimitedStringToSet(signedHeaders, ";");

		sortedSignedHeaderNames = signedHeaderNames.toArray(new String[signedHeaderNames.size()]);
		for ( int i = 0; i < sortedSignedHeaderNames.length; i++ ) {
			sortedSignedHeaderNames[i] = sortedSignedHeaderNames[i].toLowerCase();
		}
		Arrays.sort(sortedSignedHeaderNames);

		validateSignedHeaderNames(request);

		validateContentDigest(request);

		signatureData = computeSignatureData(computeCanonicalRequestData(request));
	}

	@Override
	public String computeSignatureDigest(String secretKey) {
		final byte[] signingKey = computeSigningKey(secretKey);
		return Hex.encodeHexString(computeMACDigest(signingKey, signatureData, "HmacSHA256"));
	}

	private byte[] computeSigningKey(String secretKey) {
		/*- signing key is like:
		 
		HMACSHA256(HMACSHA256("SNWS2"+secretKey, "20160301"), "snws2_request")
		*/
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return computeMACDigest(computeMACDigest(AuthenticationScheme.V2.getSchemeName() + secretKey,
				sdf.format(getDate()), "HmacSHA256"), "snws2_request", "HmacSHA256");
	}

	private String computeSignatureData(String canonicalRequestData) {
		/*- signature data is like:
		 
		 	SNWS2-HMAC-SHA256\n
		 	20170301T120000Z\n
		 	Hex(SHA256(canonicalRequestData))
		*/
		return "SNWS2-HMAC-SHA256\n" + iso8601Date(getDate()) + "\n"
				+ Hex.encodeHexString(DigestUtils.sha256(canonicalRequestData));
	}

	private String computeCanonicalRequestData(SecurityHttpServletRequestWrapper request)
			throws IOException {
		// 1: HTTP verb
		StringBuilder buf = new StringBuilder(request.getMethod()).append('\n');

		// 2: Canonical URI
		buf.append(request.getRequestURI()).append('\n');

		// 3: Canonical query string
		appendQueryParameters(request, buf);

		// 4: Canonical headers
		appendHeaders(request, buf);

		// 5: Signed headers
		appendSignedHeaderNames(buf);

		// 6: Content SHA256
		appendContentSHA256(request, buf);

		return buf.toString();
	}

	private void appendContentSHA256(SecurityHttpServletRequestWrapper request, StringBuilder buf)
			throws IOException {
		byte[] digest = request.getContentSHA256();
		buf.append(digest == null ? WebConstants.EMPTY_STRING_SHA256_HEX : Hex.encodeHexString(digest));
	}

	private void appendSignedHeaderNames(StringBuilder buf) {
		boolean first = true;
		for ( String headerName : sortedSignedHeaderNames ) {
			if ( first ) {
				first = false;
			} else {
				buf.append(';');
			}
			buf.append(headerName);
		}
		buf.append('\n');
	}

	private void appendHeaders(HttpServletRequest request, StringBuilder buf) {
		for ( String headerName : sortedSignedHeaderNames ) {
			buf.append(headerName).append(':').append(nullSafeHeaderValue(request, headerName).trim())
					.append('\n');
		}
	}

	private void appendQueryParameters(HttpServletRequest request, StringBuilder buf) {
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

	private void validateSignedHeaderNames(SecurityHttpServletRequestWrapper request) {
		// MUST include host
		if ( !signedHeaderNames.contains("host") ) {
			throw new BadCredentialsException(
					"The 'Host' HTTP header must be included in SignedHeaders");
		}
		// MUST include one of Date or X-SN-Date
		if ( !(signedHeaderNames.contains(WebConstants.HEADER_DATE.toLowerCase())
				|| signedHeaderNames.contains("date")) ) {
			throw new BadCredentialsException(
					"One of the 'Date' or 'X-SN-Date' HTTP headers must be included in SignedHeaders");
		}
		Enumeration<String> headerNames = request.getHeaderNames();
		final String snHeaderPrefix = WebConstants.HEADER_PREFIX.toLowerCase();
		while ( headerNames.hasMoreElements() ) {
			String headerName = headerNames.nextElement().toLowerCase();
			// ALL X-SN-* headers must be included; also Content-Type, Content-MD5, Digest
			boolean mustInclude = (headerName.startsWith(snHeaderPrefix)
					|| headerName.equals("content-type") || headerName.equals("content-md5")
					|| headerName.equals("digest"));
			if ( mustInclude && !signedHeaderNames.contains(headerName) ) {
				throw new BadCredentialsException(
						"The '" + headerName + "' HTTP header must be included in SignedHeaders");
			}
		}
	}

	@Override
	public String getAuthTokenId() {
		return authTokenId;
	}

	@Override
	public String getSignatureDigest() {
		return signatureDigest;
	}

	@Override
	public String getSignatureData() {
		return signatureData;
	}

	/**
	 * Get the set of signed header names.
	 * 
	 * @return The signed header names, or {@code null}.
	 */
	public Set<String> getSignedHeaderNames() {
		return signedHeaderNames;
	}

}
