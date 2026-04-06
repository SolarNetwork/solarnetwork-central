/* ==================================================================
 * ObjectDatumStreamAliasMatchType.java - 29/03/2026 10:41:03 am
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

package net.solarnetwork.central.datum.v2.domain;

import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import net.solarnetwork.domain.KeyCodedValue;

/**
 * An enumeration of object datum stream alias matching types.
 *
 * @author matt
 * @version 1.0
 */
public enum ObjectDatumStreamAliasMatchType implements KeyCodedValue {

	/** Match original or alias attributes. */
	OriginalOrAlias('b'),

	/** Match original attributes only. */
	OriginalOnly('o'),

	/** Match alias attributes only. */
	AliasOnly('a'),

	;

	private final String key;

	private ObjectDatumStreamAliasMatchType(char key) {
		this.key = String.valueOf(key);
	}

	/**
	 * Get the key.
	 *
	 * <p>
	 * This is an alias for {@link #getKeyCode()}.
	 * </p>
	 *
	 * @return the key
	 */
	public char getKey() {
		return getKeyCode();
	}

	@Override
	public char getKeyCode() {
		return key.charAt(0);
	}

	/**
	 * Get a key value for this enum.
	 *
	 * @return the key as a string
	 */
	@JsonValue
	public String keyValue() {
		return key;
	}

	/**
	 * Get an enum instance for a key value.
	 *
	 * @param key
	 *        the key value
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not supported
	 */
	public static ObjectDatumStreamAliasMatchType forKey(@Nullable String key) {
		if ( key == null || key.isEmpty() ) {
			throw new IllegalArgumentException("Key must not be null.");
		}
		if ( key.length() == 1 ) {
			return switch (key.charAt(0)) {
				case 'b' -> OriginalOrAlias;

				case 'o' -> OriginalOnly;

				case 'a' -> AliasOnly;

				default -> throw new IllegalArgumentException(
						"Invalid ObjectDatumStreamAliasMatchType value [" + key + "]");
			};
		}
		// try name() value for convenience
		return ObjectDatumStreamAliasMatchType.valueOf(key);
	}

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum, or {@code null} if value is {@code null} or empty
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static @Nullable ObjectDatumStreamAliasMatchType fromValue(@Nullable String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		final char key = value.length() == 1 ? Character.toLowerCase(value.charAt(0)) : 0;
		for ( ObjectDatumStreamAliasMatchType e : ObjectDatumStreamAliasMatchType.values() ) {
			if ( key == e.key.charAt(0) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException(
				"Unknown ObjectDatumStreamAliasMatchType value [" + value + "]");
	}

}
