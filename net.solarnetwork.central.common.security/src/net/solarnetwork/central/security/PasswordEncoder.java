/* ==================================================================
 * PasswordEncoder.java - Mar 19, 2013 9:32:17 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security;

/**
 * A password encoder API.
 * 
 * <p>
 * Modeled after the Spring Security <code>PasswordEncoder</code> API.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface PasswordEncoder {

	/**
	 * Return <em>true</em> if a password is already encrypted or not.
	 * 
	 * <p>
	 * This assumes the password has been encoded in such a way that it can be
	 * recognized as an ecrypted password, for example with a
	 * <code>{SSHA}</code> prefix.
	 * </p>
	 * 
	 * @param rawPassword
	 *        the password
	 * @return boolean
	 */
	boolean isPasswordEncrypted(CharSequence password);

	/**
	 * Encode a raw password.
	 * 
	 * @return the encrypted password string
	 */
	String encode(CharSequence rawPassword);

	/**
	 * Verify the encoded password obtained from storage matches the submitted
	 * raw password after it too is encoded. Returns true if the passwords
	 * match, false if they do not. The stored password itself is never decoded.
	 * 
	 * @param rawPassword
	 *        the raw password to encode and match
	 * @param encodedPassword
	 *        the encoded password from storage to compare with
	 * @return true if the raw password, after encoding, matches the encoded
	 *         password from storage
	 */
	boolean matches(CharSequence rawPassword, String encodedPassword);

}
