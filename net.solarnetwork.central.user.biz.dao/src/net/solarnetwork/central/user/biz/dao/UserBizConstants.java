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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import net.solarnetwork.io.RFC1924OutputStream;
import org.springframework.util.FileCopyUtils;

/**
 * Constants for common user items.
 * 
 * @author matt
 * @version 1.1
 */
public final class UserBizConstants {

	/** The unconfirmed user email prefix. */
	public static final String UNCONFIRMED_EMAIL_PREFIX = "UNCONFIRMED@";

	/**
	 * Get an "unconfirmed" value for a given email address.
	 * 
	 * @param email
	 *        the email
	 * @return the encoded "unconfirmed" value
	 */
	public static String getUnconfirmedEmail(String email) {
		return UNCONFIRMED_EMAIL_PREFIX + email;
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
		return email != null && email.startsWith(UNCONFIRMED_EMAIL_PREFIX)
				&& email.length() > UNCONFIRMED_EMAIL_PREFIX.length()
				&& email.indexOf('@', UNCONFIRMED_EMAIL_PREFIX.length()) != -1;
	}

	/**
	 * Generate a 20-character random authorization token.
	 * 
	 * @return the random token
	 */
	public static String generateRandomAuthToken() {
		UUID uuid = UUID.randomUUID();
		ByteArrayOutputStream byos = new ByteArrayOutputStream(16);
		DataOutputStream dos = new DataOutputStream(byos);
		try {
			dos.writeLong(uuid.getMostSignificantBits());
			dos.writeLong(uuid.getLeastSignificantBits());
			dos.flush();
			dos.close();
			byte[] uuidBytes = byos.toByteArray();
			byos.reset();
			FileCopyUtils.copy(uuidBytes, new RFC1924OutputStream(byos));
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
