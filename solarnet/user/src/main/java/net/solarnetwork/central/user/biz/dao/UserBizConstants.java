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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Constants for common user items.
 * 
 * @author matt
 * @version 1.4
 */
public final class UserBizConstants {

	/** The number of bytes used in a security token. */
	public static final int RANDOM_AUTH_TOKEN_BYTE_COUNT = 20;

	/** The number of random bytes used for the unconfirmed email prefix. */
	public static final int UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT = 20;

	/**
	 * The delimiter used between the unconfirmed prefix and the actual email.
	 */
	public static final char UNCONFIRMED_EMAIL_DELIMITER = '@';

	/**
	 * The number of characters for the unconfirmed email prefix, including the
	 * delimiter.
	 */
	public static final int UNCONFIRMED_EMAIL_PREFIX_LENGTH = UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT + 1;

	private static final char[] TOKEN_ALPHABET = new char[] { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
			'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'-', '.', '_', };

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
		return generateRandomToken(rng, UNCONFIRMED_EMAIL_PREFIX_BYTE_COUNT)
				+ UNCONFIRMED_EMAIL_DELIMITER + email;
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
	 * @param length
	 *        The number of random characters to generate.
	 * @return the random token
	 * @since 1.3
	 */
	public static String generateRandomToken(final SecureRandom rng, final int length) {
		char[] data = new char[length];
		for ( int i = 0; i < length; i++ ) {
			data[i] = TOKEN_ALPHABET[rng.nextInt(TOKEN_ALPHABET.length)];
		}
		return new String(data);
	}

	// can't construct me!
	private UserBizConstants() {
		super();
	}

}
