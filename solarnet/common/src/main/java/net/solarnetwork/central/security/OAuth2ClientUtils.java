/* ==================================================================
 * OAuth2ClientUtils.java - 22/09/2024 11:26:28 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * OAuth2 client utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class OAuth2ClientUtils {

	/** The delimited to use between client registration ID components. */
	public static final char CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR = ':';

	/** The delimited to use between client registration ID components. */
	public static final String CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER = Character
			.toString(CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR);

	private OAuth2ClientUtils() {
		// not available
	}

	/**
	 * Compose a client registration ID for a user ID and set of component
	 * values.
	 * 
	 * <p>
	 * This generates a registration ID of the {@code userId} and all
	 * {@code components} joined with the
	 * {@link OAuth2ClientUtils#CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param components
	 *        the components to include
	 * @return the client registration ID, or {@literal null} if {@code userId}
	 *         is {@literal null}
	 */
	public static String userIdClientRegistrationId(Long userId, Object... components) {
		if ( userId == null ) {
			return null;
		}
		StringBuilder buf = new StringBuilder(32);
		buf.append(userId).append(CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR);
		if ( components != null ) {
			boolean added = false;
			for ( Object o : components ) {
				if ( o != null ) {
					buf.append(o);
					added = true;
				}
				buf.append(CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR);
			}
			if ( added ) {
				// remove trailing colon
				buf.setLength(buf.length() - 1);
			}
		}
		return buf.toString();
	}

	/**
	 * Extract the user ID from a client registration ID previously encoded via
	 * {@link #userIdClientRegistrationId(Long, Object...)}.
	 * 
	 * @param clientRegistrationId
	 *        the client registration ID to extract the user ID from
	 * @return the extracted user ID, or {@literal null} if
	 *         {@code clientRegistrationId} is {@literal null} or empty
	 */
	public static Long userIdFromClientRegistrationId(String clientRegistrationId) {
		if ( clientRegistrationId == null || clientRegistrationId.isEmpty() ) {
			return null;
		}
		final int idx = clientRegistrationId.indexOf(CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR);
		if ( idx < 1 ) {
			return null;
		}
		try {
			return Long.parseLong(clientRegistrationId.substring(0, idx));
		} catch ( NumberFormatException e ) {
			// not available
			return null;
		}
	}

	/**
	 * Decompose a client registration ID.
	 * 
	 * <p>
	 * The {@code clientRegistrationId} will be split on the
	 * {@link #CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param clientRegistrationId
	 *        the client registration ID to decompose
	 * @param mapper
	 *        a function to map each string component to a value
	 * @return the client registration ID components
	 */
	public static <T> List<T> clientRegistrationIdComponents(String clientRegistrationId,
			Function<String, T> mapper) {
		if ( clientRegistrationId == null || clientRegistrationId.isEmpty() ) {
			return null;
		}
		requireNonNullArgument(mapper, "mapper");
		final int len = clientRegistrationId.length();
		final List<T> result = new ArrayList<>(4);
		int idx = 0;
		while ( idx < len ) {
			int nextIdx = clientRegistrationId.indexOf(CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR,
					idx);
			if ( nextIdx < 0 ) {
				nextIdx = len;
			}
			if ( nextIdx == idx ) {
				result.add(mapper.apply(null));
			} else {
				result.add(mapper.apply(clientRegistrationId.substring(idx, nextIdx)));
			}
			idx = nextIdx + 1;
		}
		if ( result.size() > 1 && clientRegistrationId
				.charAt(len - 1) == CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER_CHAR ) {
			// add trailing value
			result.add(mapper.apply(null));
		}
		return (result.isEmpty() ? null : result);
	}

	/**
	 * Decompose a client registration ID.
	 * 
	 * <p>
	 * The {@code clientRegistrationId} will be split on the
	 * {@link #CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param clientRegistrationId
	 *        the client registration ID to decompose
	 * @return the client registration ID components
	 */
	public static List<String> clientRegistrationIdComponents(String clientRegistrationId) {
		return clientRegistrationIdComponents(clientRegistrationId, Function.identity());
	}

	/**
	 * Decompose a client registration ID of {@code Long} components.
	 * 
	 * <p>
	 * The {@code clientRegistrationId} will be split on the
	 * {@link #CLIENT_REGISTRATION_ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param clientRegistrationId
	 *        the client registration ID to decompose
	 * @return the client registration ID components
	 */
	public static List<Long> clientRegistrationIdLongComponents(String clientRegistrationId) {
		return clientRegistrationIdComponents(clientRegistrationId, (s) -> {
			if ( s != null ) {
				try {
					return Long.parseLong(s);
				} catch ( NumberFormatException e ) {
					// ignore
				}
			}
			return null;
		});
	}

}
