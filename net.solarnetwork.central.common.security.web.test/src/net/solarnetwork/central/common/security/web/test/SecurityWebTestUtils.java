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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilities for unit tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class SecurityWebTestUtils {

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

}
