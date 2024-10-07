/* ==================================================================
 * CloudDatumStreamValueType.java - 8/10/2024 6:33:24 am
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

package net.solarnetwork.central.c2c.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A cloud datum stream value type.
 *
 * @author matt
 * @version 1.0
 */
public enum CloudDatumStreamValueType {

	/** A value reference to a cloud data source. */
	Reference('r', false),

	/** A SPEL expression. */
	SpelExpression('s', true),

	;

	private final String key;
	private final boolean expression;

	private CloudDatumStreamValueType(char key, boolean expression) {
		this.key = String.valueOf(key);
		this.expression = expression;
	}

	/**
	 * Test if this enum represents an expression value type.
	 *
	 * @return {@literal true} if this is an expression value type
	 */
	public final boolean isExpression() {
		return expression;
	}

	/**
	 * Get a key value for this enum.
	 *
	 * @return the key
	 */
	public char toKey() {
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
	 *        the key
	 * @return the enum
	 * @throws IllegalArgumentException
	 *         if {@code key} is not a valid value
	 */
	public static CloudDatumStreamValueType valueOf(char key) {
		for ( CloudDatumStreamValueType e : CloudDatumStreamValueType.values() ) {
			if ( key == e.key.charAt(0) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown CloudDatumStreamValueType key [" + key + "]");
	}

	/**
	 * Get an enum instance for a name or key value.
	 *
	 * @param value
	 *        the enumeration name or key value, case-insensitve
	 * @return the enum, or {@literal null} if value is {@literal null} or empty
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid value
	 */
	@JsonCreator
	public static CloudDatumStreamValueType fromValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return null;
		}
		final char key = value.length() == 1 ? Character.toLowerCase(value.charAt(0)) : 0;
		for ( CloudDatumStreamValueType e : CloudDatumStreamValueType.values() ) {
			if ( key == e.key.charAt(0) || value.equalsIgnoreCase(e.name()) ) {
				return e;
			}
		}
		throw new IllegalArgumentException("Unknown CloudDatumStreamValueType value [" + value + "]");
	}

}
