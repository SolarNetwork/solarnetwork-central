/* ==================================================================
 * UserIdentifiableSystem.java - 2/10/2024 10:10:42 am
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

package net.solarnetwork.central.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * API for something that provides a "system identity".
 * 
 * <p>
 * A "system identity" is something that uniquely identifies an object within a
 * given context, or system.
 * </p>
 * 
 * @param <T>
 *        the type of identifier
 * @author matt
 * @version 1.0
 */
public interface UserIdentifiableSystem extends UserIdRelated {

	/** A delimited to use between identifier components. */
	public static final char ID_COMPONENT_DELIMITER_CHAR = ':';

	/** A delimited to use between identifier components. */
	public static final String ID_COMPONENT_DELIMITER = Character.toString(ID_COMPONENT_DELIMITER_CHAR);

	/**
	 * Get a unique system identifier.
	 *
	 * @return the system identifier
	 */
	String systemIdentifier();

	/**
	 * Get a system identifier for a set of components.
	 * 
	 * <p>
	 * The {@link #getUserId()} will be returned as the first component.
	 * </p>
	 * 
	 * @param components
	 *        the components
	 * @return the identifier
	 */
	default String systemIdentifierForComponents(Object... components) {
		return userIdSystemIdentifier(getUserId(), components);
	}

	/**
	 * Compose a system identifier for a user ID and set of component values.
	 * 
	 * <p>
	 * This generates a system identifier of the {@code userId} and all
	 * {@code components} joined with the {@link #ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param userId
	 *        the user ID
	 * @param components
	 *        the components to include
	 * @return the system identifier, or {@literal null} if {@code userId} is
	 *         {@literal null}
	 */
	static String userIdSystemIdentifier(Long userId, Object... components) {
		if ( userId == null ) {
			return null;
		}
		StringBuilder buf = new StringBuilder(32);
		buf.append(userId).append(ID_COMPONENT_DELIMITER_CHAR);
		if ( components != null ) {
			boolean added = false;
			for ( Object o : components ) {
				if ( o != null ) {
					buf.append(o);
					added = true;
				}
				buf.append(ID_COMPONENT_DELIMITER_CHAR);
			}
			if ( added ) {
				// remove trailing colon
				buf.setLength(buf.length() - 1);
			}
		}
		return buf.toString();
	}

	/**
	 * Extract the user ID from a system identifier previously encoded via
	 * {@link #userIdSystemIdentifier(Long, Object...)}.
	 * 
	 * @param systemIdentifier
	 *        the system identifier to extract the user ID from
	 * @return the extracted user ID, or {@literal null} if
	 *         {@code systemIdentifier} is {@literal null} or empty
	 */
	static Long userIdFromSystemIdentifier(String systemIdentifier) {
		if ( systemIdentifier == null || systemIdentifier.isEmpty() ) {
			return null;
		}
		final int idx = systemIdentifier.indexOf(ID_COMPONENT_DELIMITER_CHAR);
		if ( idx < 1 ) {
			return null;
		}
		try {
			return Long.parseLong(systemIdentifier.substring(0, idx));
		} catch ( NumberFormatException e ) {
			// not available
			return null;
		}
	}

	/**
	 * Decompose a system identifier.
	 * 
	 * <p>
	 * The {@code systemIdentifier} will be split on the
	 * {@link #ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param systemIdentifier
	 *        the system identifier to decompose
	 * @param omitNulls
	 *        {@literal true} to omit {@literal null} values from the returned
	 *        list
	 * @param mapper
	 *        a function to map each string component to a value
	 * @return the system identifier components
	 */
	static <T> List<T> systemIdentifierComponents(String systemIdentifier, boolean omitNulls,
			Function<String, T> mapper) {
		if ( systemIdentifier == null || systemIdentifier.isEmpty() ) {
			return null;
		}
		requireNonNullArgument(mapper, "mapper");
		final int len = systemIdentifier.length();
		final List<T> result = new ArrayList<>(4);
		int idx = 0;
		while ( idx < len ) {
			int nextIdx = systemIdentifier.indexOf(ID_COMPONENT_DELIMITER_CHAR, idx);
			if ( nextIdx < 0 ) {
				nextIdx = len;
			}
			T next = null;
			if ( nextIdx == idx ) {
				next = mapper.apply(null);
			} else {
				next = mapper.apply(systemIdentifier.substring(idx, nextIdx));
			}
			if ( next != null || !omitNulls ) {
				result.add(next);
			}
			idx = nextIdx + 1;
		}
		if ( result.size() > 1 && systemIdentifier.charAt(len - 1) == ID_COMPONENT_DELIMITER_CHAR ) {
			// add trailing value
			T next = null;
			next = mapper.apply(null);
			if ( next != null || !omitNulls ) {
				result.add(next);
			}
		}
		return (result.isEmpty() ? null : result);
	}

	/**
	 * Decompose a system identifier.
	 * 
	 * <p>
	 * The {@code systemIdentifier} will be split on the
	 * {@link #ID_COMPONENT_DELIMITER}.
	 * </p>
	 * 
	 * @param systemIdentifier
	 *        the system identifier to decompose
	 * @return the system identifier components
	 */
	static List<String> systemIdentifierComponents(String systemIdentifier) {
		return systemIdentifierComponents(systemIdentifier, false, Function.identity());
	}

	/**
	 * Decompose a system identifier of {@code Long} components.
	 * 
	 * <p>
	 * The {@code systemIdentifier} will be split on the
	 * {@link #ID_COMPONENT_DELIMITER}. Components that cannot be parsed as a
	 * {@link Long} will be returned as {@literal null}.
	 * </p>
	 * 
	 * @param systemIdentifier
	 *        the system identifier to decompose
	 * @return the system identifier components
	 */
	static List<Long> systemIdentifierLongComponents(String systemIdentifier) {
		return systemIdentifierLongComponents(systemIdentifier, false);
	}

	/**
	 * Decompose a system identifier of {@code Long} components.
	 * 
	 * <p>
	 * The {@code systemIdentifier} will be split on the
	 * {@link #ID_COMPONENT_DELIMITER}. Components that cannot be parsed as a
	 * {@link Long} will be returned as {@literal null} or omitted, depending on
	 * the {@code omitNulls} value.
	 * </p>
	 * 
	 * @param systemIdentifier
	 *        the system identifier to decompose
	 * @param omitNulls
	 *        {@literal true} to omit {@literal null} values from the returned
	 *        list
	 * @return the system identifier components
	 */
	static List<Long> systemIdentifierLongComponents(String systemIdentifier, boolean omitNulls) {
		return systemIdentifierComponents(systemIdentifier, omitNulls, (s) -> {
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
