/* ==================================================================
 * TransformPhase.java - 28/03/2024 11:24:24 am
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

package net.solarnetwork.central.inin.domain;

import static java.lang.String.format;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;

/**
 * Enumeration of transform phase values.
 *
 * @author matt
 * @version 1.0
 */
public enum TransformPhase implements CodedValue {

	/** The request phase. */
	Request('i'),

	/** The response phase. */
	Response('o'),

	;

	private final char code;

	private TransformPhase(char code) {
		this.code = code;
	}

	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Create an enum instance from a code value.
	 *
	 * @param code
	 *        the code value
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	public static TransformPhase forCode(int code) {
		for ( TransformPhase e : TransformPhase.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid TransformPhase code [%s]", code));
	}

	/**
	 * Create an enum instance from a string value.
	 *
	 * @param value
	 *        the string representation; both enum names and code values are
	 *        supported
	 * @return the enum instance
	 * @throws IllegalArgumentException
	 *         if {@code value} is not a valid enum value
	 */
	@JsonCreator
	public static TransformPhase fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( TransformPhase e : TransformPhase.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid TransformPhase value [%s]", value));
	}

}
