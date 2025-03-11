/* ==================================================================
 * AuthorizationState.java - 11/03/2025 3:21:06 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An authorization state record.
 *
 * @author matt
 * @version 1.0
 */
public record AuthorizationState(Long integrationId, String token) {

	/** A regular expression pattern to match an encoded state value. */
	public static final Pattern STATE_VALUE_REGEX = Pattern.compile("(-?\\d+),(.+)");

	/**
	 * Decode an authorization state from an encoded value.
	 *
	 * @param state
	 *        the encoded value to decode
	 * @return the decoded value, or {@code null} it {@code state} cannot be
	 *         decoded
	 */
	public static AuthorizationState forStateValue(String state) {
		if ( state == null ) {
			return null;
		}
		Matcher m = STATE_VALUE_REGEX.matcher(state);
		if ( !m.matches() ) {
			return null;
		}
		return new AuthorizationState(Long.valueOf(m.group(1)), m.group(2));
	}

	/**
	 * Get an encoded authorization state value.
	 *
	 * @return the encoded value
	 */
	public String stateValue() {
		return integrationId + "," + token;
	}

}
