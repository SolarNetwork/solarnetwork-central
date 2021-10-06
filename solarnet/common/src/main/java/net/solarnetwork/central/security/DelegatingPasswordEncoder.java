/* ==================================================================
 * DelegatingPasswordEncoder.java - Mar 19, 2013 9:37:08 AM
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

import java.util.Map;
import net.solarnetwork.service.PasswordEncoder;
import net.solarnetwork.util.ObjectUtils;

/**
 * Password encoder that delegates to a configurable list of Spring Security
 * {@code org.springframework.security.crypto.password.PasswordEncoder}
 * instances, returning passwords with a prefix tag to be able to recognize what
 * encryption technique was used.
 * 
 * <p>
 * The first entry in the map according to iteration order will be used as the
 * primary encoder. Thus a map implementation like
 * {@link java.util.LinkedHashMap} is recommended.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class DelegatingPasswordEncoder
		implements PasswordEncoder, org.springframework.security.crypto.password.PasswordEncoder {

	private final Map<String, org.springframework.security.crypto.password.PasswordEncoder> encoders;

	/**
	 * Constructor.
	 * 
	 * @param encoders
	 *        the encoders to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DelegatingPasswordEncoder(
			Map<String, org.springframework.security.crypto.password.PasswordEncoder> encoders) {
		super();
		this.encoders = ObjectUtils.requireNonNullArgument(encoders, "encoders");
	}

	@Override
	public boolean isPasswordEncrypted(CharSequence password) {
		if ( encoders == null || password == null ) {
			return false;
		}
		for ( String prefix : encoders.keySet() ) {
			if ( password.length() > prefix.length()
					&& password.subSequence(0, prefix.length()).equals(prefix) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		if ( encoders == null || encoders.size() < 1 ) {
			throw new RuntimeException("No password encoders configured");
		}
		Map.Entry<String, org.springframework.security.crypto.password.PasswordEncoder> entry = encoders
				.entrySet().iterator().next();
		return entry.getValue().encode(rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if ( encodedPassword == null || rawPassword == null ) {
			return false;
		}
		for ( Map.Entry<String, org.springframework.security.crypto.password.PasswordEncoder> entry : encoders
				.entrySet() ) {
			String prefixTag = entry.getKey();
			if ( encodedPassword.startsWith(prefixTag) ) {
				return entry.getValue().matches(rawPassword, encodedPassword);
			}
		}
		return false;
	}

}
