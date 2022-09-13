/* ==================================================================
 * Phase.java - 14/08/2022 5:50:41 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain;

import static java.lang.String.format;
import com.fasterxml.jackson.annotation.JsonCreator;
import net.solarnetwork.domain.CodedValue;
import oscp.v20.ForecastedBlock.PhaseIndicator;

/**
 * An enumeration of phases for an asset.
 * 
 * @author matt
 * @version 1.0
 */
public enum Phase implements CodedValue {

	/** Phase on which is measured is not known or irrelevant. */
	Unknown('u'),

	/** Represents measurement on phase 1. */
	A('a'),

	/** Represents measurement on phase 2. */
	B('b'),

	/** Represents measurement on phase 2. */
	C('c'),

	/** Represents the sum of all phases (1, 2, and 3). */
	All('z')

	;

	private final char code;

	private Phase(char code) {
		this.code = code;
	}

	/**
	 * Get the number of seconds represented by this period.
	 * 
	 * @return the number of seconds
	 */
	@Override
	public int getCode() {
		return code;
	}

	/**
	 * Get an OSCP 2.0 value for this instance.
	 * 
	 * @return the OSCP 2.0 value
	 */
	public PhaseIndicator toOscp20Value() {
		return switch (this) {
			case Unknown -> PhaseIndicator.UNKNOWN;
			case A -> PhaseIndicator.ONE;
			case B -> PhaseIndicator.TWO;
			case C -> PhaseIndicator.THREE;
			case All -> PhaseIndicator.ALL;
		};
	}

	/**
	 * Get an instance for an OSCP 2.0 value.
	 * 
	 * @param category
	 *        the OSCP 2.0 value to get an instance for
	 * @return the instance
	 */
	public static Phase forOscp20Value(PhaseIndicator phase) {
		return switch (phase) {
			case UNKNOWN -> Unknown;
			case ONE -> A;
			case TWO -> B;
			case THREE -> C;
			case ALL -> All;
		};
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
	public static Phase forCode(int code) {
		for ( Phase e : Phase.values() ) {
			if ( code == e.code ) {
				return e;
			}
		}
		throw new IllegalArgumentException(format("Invalid Phase code [%s]", code));
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
	public static Phase fromValue(String value) {
		if ( value != null && value.length() > 0 ) {
			final boolean coded = (value.length() == 1);
			final char code = value.charAt(0);
			for ( Phase e : Phase.values() ) {
				if ( coded && code == e.code ) {
					return e;
				} else if ( e.name().equalsIgnoreCase(value) ) {
					return e;
				}
			}
		}
		throw new IllegalArgumentException(format("Invalid Phase value [%s]", value));
	}

}
