/* ==================================================================
 * StringUtils.java - 16/01/2026 10:46:05 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.flux.vernemq.webhook.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * String utility helpers.
 * 
 * @author matt
 * @version 1.0
 */
public final class StringUtils {

	private StringUtils() {
		// not available
	}

	/**
	 * Get a message payload string as bytes.
	 * 
	 * @param s
	 *        the payload string, expected to be Base64 encoded
	 * @return the bytes, or {@code null} if {@code s} is {@code null} or empty
	 */
	public static byte[] payloadBytes(String s) {
		if ( s != null && !s.isEmpty() ) {
			try {
				return Base64.getDecoder().decode(s);
			} catch ( IllegalArgumentException e ) {
				// use string data directly (as UTF-8)
				return s.getBytes(StandardCharsets.UTF_8);
			}
		}
		return null;
	}

}
