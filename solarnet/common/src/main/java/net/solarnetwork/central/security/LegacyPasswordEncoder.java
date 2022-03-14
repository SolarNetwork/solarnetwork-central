/* ==================================================================
 * LegacyPasswordEncoder.java - Mar 19, 2013 9:57:44 AM
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

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Password encoder using unsalted SHA-256 hashes.
 * 
 * @author matt
 * @version 1.0
 * @deprecated do not use this encoder for anything other than supporting legacy
 *             passwords
 */
@Deprecated
public class LegacyPasswordEncoder implements
		org.springframework.security.crypto.password.PasswordEncoder {

	@Override
	public String encode(CharSequence rawPassword) {
		return (rawPassword == null ? null : "{SHA}" + DigestUtils.sha256Hex(rawPassword.toString()));
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return (rawPassword == null || encodedPassword == null ? false : encode(rawPassword).equals(
				encodedPassword));
	}

}
