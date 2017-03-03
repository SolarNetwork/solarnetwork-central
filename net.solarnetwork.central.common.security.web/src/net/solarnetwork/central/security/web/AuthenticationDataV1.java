/* ==================================================================
 * AuthenticationDataV1.java - 1/03/2017 5:22:24 PM
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
import java.util.Arrays;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Base64;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Original HMAC-SHA1 authentication token scheme.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public class AuthenticationDataV1 extends AuthenticationData {

	private final String authTokenId;
	private final String signatureDigest;
	private final String signature;

	public AuthenticationDataV1(SecurityHttpServletRequestWrapper request, String headerValue)
			throws IOException {
		super(AuthenticationScheme.V1, request, headerValue);

		// the header must be in the form TOKEN-ID:HMAC-SHA1-SIGNATURE
		if ( AUTH_TOKEN_ID_LENGTH + 2 >= headerValue.length() ) {
			throw new BadCredentialsException("Invalid Authorization header value");
		}
		authTokenId = headerValue.substring(0, AUTH_TOKEN_ID_LENGTH);
		signatureDigest = headerValue.substring(AUTH_TOKEN_ID_LENGTH + 1);
		signature = computeSignatureData(request);

		validateContentDigest(request);
	}

	private String computeSignatureData(SecurityHttpServletRequestWrapper request) {
		StringBuilder buf = new StringBuilder(request.getMethod());
		buf.append("\n");
		buf.append(nullSafeHeaderValue(request, "Content-MD5")).append("\n");
		buf.append(nullSafeHeaderValue(request, "Content-Type")).append("\n");
		buf.append(httpDate(getDate())).append("\n");
		buf.append(request.getRequestURI());
		appendQueryParameters(request, buf);
		return buf.toString();
	}

	private void appendQueryParameters(HttpServletRequest request, StringBuilder buf) {
		Set<String> paramKeys = request.getParameterMap().keySet();
		if ( paramKeys.size() < 1 ) {
			return;
		}
		String[] keys = paramKeys.toArray(new String[paramKeys.size()]);
		Arrays.sort(keys);
		boolean first = true;
		for ( String key : keys ) {
			if ( first ) {
				buf.append('?');
				first = false;
			} else {
				buf.append('&');
			}
			buf.append(key).append('=').append(request.getParameter(key));
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
		return signature;
	}

	@Override
	public String computeSignatureDigest(String secretKey) {
		return Base64.encodeBase64String(computeMACDigest(secretKey, "HmacSHA1"));
	}

}
