/* ==================================================================
 * AuthenticationData.java - 1/03/2017 5:23:56 PM
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
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Abstract base class for parsing and exposing the authentication data included
 * in a HTTP authentication header.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public abstract class AuthenticationData {

	/** The fixed length of a SolarNetwork authentication token ID. */
	public static final int AUTH_TOKEN_ID_LENGTH = 20;

	private final Date date;
	private final long dateSkew;
	private final AuthenticationScheme scheme;

	/**
	 * Constructor.
	 * 
	 * @param scheme
	 *        The scheme associated with the data.
	 * @param request
	 *        The request.
	 * @param headerValue
	 *        The authentication HTTP header value.
	 * @throws BadCredentialsException
	 *         if the request date is not available or no data is associated
	 *         with the authentication header
	 */
	public AuthenticationData(AuthenticationScheme scheme, SecurityHttpServletRequestWrapper request,
			String headerValue) {
		this.scheme = scheme;

		String dateHeader = WebConstants.HEADER_DATE;
		long dateValue = request.getDateHeader(dateHeader);
		if ( dateValue < 0 ) {
			dateHeader = "Date";
			dateValue = request.getDateHeader(dateHeader);
			if ( dateValue < 0 ) {
				throw new BadCredentialsException("Missing or invalid HTTP Date header value");
			}
		}
		this.date = new Date(dateValue);
		this.dateSkew = Math.abs(System.currentTimeMillis() - date.getTime());
	}

	/**
	 * Validate a digest header value presented in a request against the request
	 * body content.
	 * 
	 * @param request
	 *        The request.
	 * @throws IOException
	 *         If an IO error occurs.
	 * @throws BadCredentialsException
	 *         If a digest does not match.
	 */
	public static void validateContentDigest(SecurityHttpServletRequestWrapper request)
			throws IOException {
		// try Digest HTTP header first
		String headerValue = request.getHeader("Digest");
		if ( headerValue != null ) {
			final String delimitedString = headerValue + ",";
			int prevDelimIdx = 0;
			int delimIdx = 0;
			for ( delimIdx = delimitedString.indexOf(','); delimIdx >= 0; prevDelimIdx = delimIdx
					+ 1, delimIdx = delimitedString.indexOf(',', prevDelimIdx) ) {
				String oneDigest = headerValue.substring(prevDelimIdx, delimIdx);
				int splitIdx = oneDigest.indexOf('=');
				if ( splitIdx > 0 ) {
					String algName = oneDigest.substring(0, splitIdx);
					try {
						DigestAlgorithm digestAlg = DigestAlgorithm.forAlgorithmName(algName);
						String providedDigest = (oneDigest.length() > splitIdx + 1
								? oneDigest.substring(splitIdx + 1) : null);
						validateContentDigest(digestAlg, providedDigest, request);
						// if we get past this, we have validated a digest so can stop looking for more
						return;
					} catch ( IllegalArgumentException e ) {
						// ignore and move on
					}
				}
			}
		} else {
			// try the Content-MD5 HTTP header
			headerValue = request.getHeader("Content-MD5");
			if ( headerValue != null ) {
				validateContentDigest(DigestAlgorithm.MD5, headerValue, request);
			}
		}
	}

	private static void validateContentDigest(DigestAlgorithm alg, String providedDigestString,
			SecurityHttpServletRequestWrapper request) throws IOException {
		byte[] computedDigest = null;
		byte[] providedDigest = null;
		int hexLength = 0;
		try {
			switch (alg) {
				case MD5:
					hexLength = 32;
					computedDigest = request.getContentMD5();
					break;

				case SHA1:
					hexLength = 40;
					computedDigest = request.getContentSHA1();
					break;

				case SHA256:
					hexLength = 64;
					computedDigest = request.getContentSHA256();
					break;
			}
		} catch ( net.solarnetwork.central.security.SecurityException e ) {
			throw new BadCredentialsException("Content too large", e);
		}
		try {
			if ( providedDigestString.length() == hexLength ) {
				// treat as hex
				providedDigest = Hex.decodeHex(providedDigestString.toCharArray());
			} else {
				// treat as Base64
				providedDigest = Base64.decodeBase64(providedDigestString);
			}
		} catch ( DecoderException e ) {
			throw new BadCredentialsException("Invalid Digest SHA-256 encoding");
		}
		if ( !Arrays.equals(computedDigest, providedDigest) ) {
			throw new BadCredentialsException(
					"Content " + alg.getAlgorithmName() + " digest value mismatch");
		}
	}

	/**
	 * Get a string value of a specific HTTP header, returning an empty string
	 * if not available.
	 * 
	 * @param request
	 *        The request.
	 * @param headerName
	 *        The name of the HTTP header to get.
	 * @return The header value, or an empty string if not found.
	 */
	public static String nullSafeHeaderValue(HttpServletRequest request, String headerName) {
		final String result = request.getHeader(headerName);
		return (result == null ? "" : result);
	}

	/**
	 * Get a HTTP formatted date.
	 * 
	 * @param date
	 *        The date to format.
	 * @return The formatted date.
	 */
	public static String httpDate(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	/**
	 * AWS style implementation of "uri encoding" using UTF-8 encoding.
	 * 
	 * @param input
	 *        The text input to encode.
	 * @return The URI escaped string.
	 */
	public static String uriEncode(CharSequence input) {
		StringBuilder result = new StringBuilder();
		byte[] tmpByteArray = new byte[1];
		for ( int i = 0; i < input.length(); i++ ) {
			char ch = input.charAt(i);
			if ( (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
					|| ch == '_' || ch == '-' || ch == '~' || ch == '.' ) {
				result.append(ch);
			} else {
				try {
					byte[] bytes = String.valueOf(ch).getBytes("UTF-8");
					for ( byte b : bytes ) {
						tmpByteArray[0] = b;
						result.append('%').append(Hex.encodeHex(tmpByteArray, false));
					}
				} catch ( UnsupportedEncodingException e ) {
					// ignore, should never be here
				}
			}
		}
		return result.toString();
	}

	/**
	 * Get an ISO8601 formatted date.
	 * 
	 * @param date
	 *        The date to format.
	 * @return The formatted date.
	 */
	public static String iso8601Date(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(date);
	}

	/**
	 * Get the date associated with the request.
	 * 
	 * @return The date.
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Get the date skew (in milliseconds) associated with the request (from the
	 * system date).
	 * 
	 * @return The date skew.
	 */
	public long getDateSkew() {
		return dateSkew;
	}

	/**
	 * The scheme of the authentication data.
	 * 
	 * @return The scheme.
	 */
	public AuthenticationScheme getScheme() {
		return scheme;
	}

	/**
	 * Test if the date skew is less than a maximum date skew.
	 * 
	 * @param maxDateSkew
	 *        The maximum allowed date skew.
	 * @return {@code true} if the date skew is within the allowed skew
	 */
	public boolean isDateValid(long maxDateSkew) {
		return dateSkew < maxDateSkew;
	}

	/**
	 * Compute a Base64 MAC digest from the signature data.
	 * 
	 * @param secretKey
	 *        the secret key
	 * @param macAlgorithm
	 * @return The base64 encoded digest.
	 * @throws SecurityException
	 *         if any error occurs
	 */
	protected final byte[] computeMACDigest(final String secretKey, String macAlgorithm) {
		return computeMACDigest(secretKey, getSignatureData(), macAlgorithm);
	}

	/**
	 * Compute a Base64 MAC digest from signature data.
	 * 
	 * @param secretKey
	 *        the secret key
	 * @param data
	 *        the data to sign
	 * @param macAlgorithm
	 * @return The base64 encoded digest.
	 * @throws SecurityException
	 *         if any error occurs
	 */
	public static final byte[] computeMACDigest(final byte[] secretKey, final String data,
			String macAlgorithm) {
		try {
			return computeMACDigest(secretKey, data.getBytes("UTF-8"), macAlgorithm);
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error loading " + macAlgorithm + " crypto function", e);
		}
	}

	/**
	 * Compute a Base64 MAC digest from signature data.
	 * 
	 * @param secretKey
	 *        the secret key
	 * @param data
	 *        the data to sign
	 * @param macAlgorithm
	 * @return The base64 encoded digest.
	 * @throws SecurityException
	 *         if any error occurs
	 */
	public static final byte[] computeMACDigest(final String secretKey, final String data,
			String macAlgorithm) {
		try {
			return computeMACDigest(secretKey.getBytes("UTF-8"), data.getBytes("UTF-8"), macAlgorithm);
		} catch ( UnsupportedEncodingException e ) {
			throw new SecurityException("Error loading " + macAlgorithm + " crypto function", e);
		}
	}

	/**
	 * Compute a Base64 MAC digest from signature data.
	 * 
	 * @param secretKey
	 *        the secret key
	 * @param data
	 *        the data to sign
	 * @param macAlgorithm
	 * @return The base64 encoded digest.
	 * @throws SecurityException
	 *         if any error occurs
	 */
	public static final byte[] computeMACDigest(final byte[] secretKey, final byte[] data,
			String macAlgorithm) {
		Mac mac;
		try {
			mac = Mac.getInstance(macAlgorithm);
			mac.init(new SecretKeySpec(secretKey, macAlgorithm));
			byte[] result = mac.doFinal(data);
			return result;
		} catch ( NoSuchAlgorithmException e ) {
			throw new SecurityException("Error loading " + macAlgorithm + " crypto function", e);
		} catch ( InvalidKeyException e ) {
			throw new SecurityException("Error loading " + macAlgorithm + " crypto function", e);
		}
	}

	/**
	 * Compute the signature digest from the request data and a given secret
	 * key.
	 * 
	 * @param secretKey
	 *        The secret key.
	 * @return The computed digest, as a Base64 encoded string.
	 */
	public abstract String computeSignatureDigest(String secretKey);

	/**
	 * Get the authentication token ID.
	 * 
	 * @return The authentication token ID.
	 */
	public abstract String getAuthTokenId();

	/**
	 * Get the signature digest as presented in the HTTP header value.
	 * 
	 * @return The presented signature digest.
	 */
	public abstract String getSignatureDigest();

	/**
	 * Get the extracted signature data from this request.
	 * 
	 * @return The raw signature data.
	 */
	public abstract String getSignatureData();

}
