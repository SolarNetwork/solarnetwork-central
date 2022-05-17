/* ==================================================================
 * UserBizConstants.java - Dec 12, 2012 2:42:05 PM
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

package net.solarnetwork.central.user.biz.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.io.RFC1924OutputStream;

/**
 * Constants for common user items.
 * 
 * @author matt
 * @version 1.4
 */
public final class UserBizConstants {

	/** The number of bytes used in a security token. */
	public static final int RANDOM_AUTH_TOKEN_BYTE_COUNT = 16;

	/** The number of random bytes used for the unconfirmed email prefix. */
	public static final int UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT = 10;

	/**
	 * The delimiter used between the unconfirmed prefix and the actual email.
	 */
	public static final char UNCONFIRMED_EMAIL_DELIMITER = '@';

	/**
	 * The number of characters for the unconfirmed email prefix, including the
	 * delimiter.
	 */
	public static final int UNCONFIRMED_EMAIL_PREFIX_LENGTH = UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT * 2
			+ 1;

	/**
	 * Get an "unconfirmed" value for a given email address.
	 * 
	 * <p>
	 * The unconfirmed value will have
	 * {@link #UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT} Hex-encoded random bytes
	 * prepended, along with {@link #UNCONFIRMED_EMAIL_DELIMITER}.
	 * </p>
	 * 
	 * @param email
	 *        the email
	 * @return the encoded "unconfirmed" value
	 */
	public static String getUnconfirmedEmail(String email) {
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstance("SHA1PRNG");
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Unable to generate random bytes", e);
		}
		byte[] data = new byte[UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT];
		rng.nextBytes(data);
		return Hex.encodeHexString(data) + UNCONFIRMED_EMAIL_DELIMITER + email;
	}

	/**
	 * Test if an email is encoded as "unconfirmed".
	 * 
	 * @param email
	 *        the email to test
	 * @return <em>true</em> if the email is considered an "unconfirmed" email
	 */
	public static boolean isUnconfirmedEmail(String email) {
		// validate email starts with unconfirmed key and also contains
		// another @ character, in case somebody does have an email name
		// the same as our unconfirmed key
		return email != null && email.length() > UNCONFIRMED_EMAIL_PREFIX_LENGTH
				&& email.charAt(UNCONFIRMED_EMAIL_PREFIX_LENGTH - 1) == UNCONFIRMED_EMAIL_DELIMITER
				&& email.indexOf('@', UNCONFIRMED_EMAIL_PREFIX_LENGTH) != -1;
	}

	/**
	 * Get the original email value from an unconfirmed email value.
	 * 
	 * @param unconfirmedEmail
	 *        The unconfirmed email, previously returned from
	 *        {@link UserBizConstants#getUnconfirmedEmail(String)}.
	 * @return The original email.
	 * @throws IllegalArgumentException
	 *         if the email does not appear to be an unconfirmed email.
	 * @since 1.2
	 */
	public static String getOriginalEmail(String unconfirmedEmail) {
		if ( isUnconfirmedEmail(unconfirmedEmail) ) {
			return unconfirmedEmail.substring(UNCONFIRMED_EMAIL_PREFIX_LENGTH);
		}
		throw new IllegalArgumentException(
				"[" + unconfirmedEmail + "] is not a valid unconfirmed email");
	}

	/**
	 * Generate a 20-character random authorization token.
	 * 
	 * @param rng
	 *        the generator to use
	 * @return the random token
	 */
	public static String generateRandomAuthToken(SecureRandom rng) {
		return generateRandomToken(rng, RANDOM_AUTH_TOKEN_BYTE_COUNT);
	}

	/**
	 * Generate a random token string.
	 * 
	 * @param rng
	 *        the generator to use
	 * @param byteCount
	 *        The number of random bytes to use.
	 * @return the random token, encoded in a base-85 form
	 * @since 1.3
	 */
	public static String generateRandomToken(SecureRandom rng, int byteCount) {
		try {
			byte[] randomBytes = new byte[byteCount];
			rng.nextBytes(randomBytes);
			ByteArrayOutputStream byos = new ByteArrayOutputStream((int) Math.ceil(byteCount * 1.25));
			FileCopyUtils.copy(randomBytes, new RFC1924OutputStream(byos));
			return byos.toString("US-ASCII");
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	// can't construct me!
	private UserBizConstants() {
		super();
	}

}
